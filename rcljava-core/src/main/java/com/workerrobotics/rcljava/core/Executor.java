package com.workerrobotics.rcljava.core;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.ros2.rcl.RclLib;
import org.ros2.rcl.rcl_wait_set_t;
import org.ros2.rcl.rmw_request_id_t;
import org.ros2.rcl.rmw_requested_qos_incompatible_event_status_t;
import org.ros2.rcl.msgs.GEOMETRY_MSGS_Lib;
import org.ros2.rcl.msgs.STD_MSGS_Lib;
import org.ros2.rcl.msgs.geometry_msgs__msg__Twist;

import com.workerrobotics.rcljava.core.callbackgroup.CallbackGroup;
import com.workerrobotics.rcljava.core.events.EventHandler;
import com.workerrobotics.rcljava.core.service.ServiceCallback;
import com.workerrobotics.rcljava.ffi.NativeChecks;
import com.workerrobotics.rcljava.ffi.RclException;

import static org.ros2.rcl.RclLib.*;

/**
 * The execution engine responsible for coordinating callbacks and processing middleware events.
 * 
 * <p>The {@code Executor} implements the ROS 2 "wait and dispatch" loop. It manages a 
 * native {@code rcl_wait_set_t} to monitor various entities (Subscriptions, Timers, 
 * Services, Clients) for ready status. When an entity is ready, the Executor 
 * retrieves the data and dispatches the corresponding Java callback.</p>
 * 
 * <p>This implementation leverages <b>Java Virtual Threads (Project Loom)</b> to 
 * handle callbacks asynchronously, preventing long-running user code from 
 * blocking the main execution loop.</p>
 */
public class Executor {
    /** 
     * Internal executor service using virtual threads to process callbacks 
     * without consuming platform thread resources.
     */
    private final ExecutorService callbackExecutor = Executors.newVirtualThreadPerTaskExecutor();

    /**
     * Starts the execution loop for the provided node.
     * 
     * <p>This method blocks the calling thread and continuously monitors the node's 
     * entities until the ROS context is shut down. It performs the following steps 
     * in each iteration:
     * <ul>
     *   <li>Populates a wait set with all node handles.</li>
     *   <li>Calls {@code rcl_wait} with a 100ms timeout.</li>
     *   <li>Dispatches ready entities to their respective handler methods.</li>
     * </ul>
     * </p>
     * 
     * @param node The {@link Node} whose entities should be processed.
     * @throws RuntimeException If the native wait set initialization fails.
     */
    public void spin(Node node) {
        MemorySegment context = RosContext.get();
        try (Arena spinArena = Arena.ofConfined()) {
            MemorySegment waitSet = rcl_get_zero_initialized_wait_set(spinArena);
            
            int rc = rcl_wait_set_init(
                waitSet, 
                node.subscriptions.size(), 
                node.guardconditions.size(),
                node.timers.size(), 
                node.clients.size(),
                node.services.size(),
                node.events.size(), 
                context, 
                rcutils_get_default_allocator(RosContext.GLOBAL_ARENA)
            );

            if (rc != 0) throw new RuntimeException("Waitset init failed with code " + rc);

            while (rcl_context_is_valid(context)) {
                rcl_wait_set_clear(waitSet);
                
                for (Subscription<?> sub : node.subscriptions) {
                    rcl_wait_set_add_subscription(waitSet, sub.handle(), MemorySegment.NULL);                    
                }
                for (Service<?,?> service: node.services) {
                    rcl_wait_set_add_service(waitSet, service.handle(), MemorySegment.NULL);
                }
                for (Timer timer : node.timers) {
                    RclLib.rcl_wait_set_add_timer(waitSet, timer.handle(), MemorySegment.NULL);
                }
                for (Client<?, ?> client : node.clients) {
                    rcl_wait_set_add_client(waitSet, client.handle(), MemorySegment.NULL);
                }
                for (EventHandler eventHandler: node.events) {
                    rcl_wait_set_add_event(waitSet, eventHandler.eventHandle(), MemorySegment.NULL);
                }

                int ret = rcl_wait(waitSet, 100_000_000L);
                
                if (ret == 0) {
                    this.dispatchSubscriptions(waitSet, node.subscriptions);
                    this.dispatchServices(waitSet, node.services);
                    this.dispatchTimers(waitSet, node.timers);
                    this.dispatchClients(waitSet, node.clients);
                    this.dispatchEvents(waitSet, node);
                } else if (ret != 1) { // 1 is RCL_RET_TIMEOUT
                    // Handel andere errors af
                    
                }
            }
            rcl_wait_set_fini(waitSet);
        }
    }

    /**
     * Dispatches ready subscriptions by taking data from the middleware.
     * 
     * <p>Data is captured immediately in the main loop thread to ensure consistency 
     * and then passed to a virtual thread for processing via the user-defined callback.</p>
     * 
     * @param waitSet The active native wait set.
     * @param subs The list of subscriptions to check.
     */
    private void dispatchSubscriptions(MemorySegment waitSet, List<Subscription<?>> subs) {
        // De waitSet bevat een pointer naar een array van pointers. 
        // Als een entry NIET NULL is, is er data.
        
        // Is een pointer naar pointers:
        MemorySegment subscriptionsPtr = rcl_wait_set_t.subscriptions(waitSet);

        for (int i = 0; i < subs.size(); i++) {
            MemorySegment subPtr = subscriptionsPtr.getAtIndex(ValueLayout.ADDRESS, i);
            
            if (!subPtr.equals(MemorySegment.NULL)) {
                Subscription<?> sub = subs.get(i);
                
                Arena messageArena = Arena.ofShared();

                // // 1. Haal de data ONMIDDELLIJK op in de hoofdthread
                MemorySegment safeData = captureData(sub, messageArena);

                if (safeData != null) {
                    // 2. Geef de veilige kopie door aan de Virtual Thread
                    sub.callbackGroup().execute(() -> {
                        try {
                            sub.callback().accept(safeData);
                        } finally {
                            // Optioneel: als je een specifieke arena gebruikt voor dit bericht, 
                            // sluit die dan hier.
                            messageArena.close();
                        }
                    }, callbackExecutor);
                }
            }
        }
    }

    /**
     * Processes incoming service requests and automatically sends responses.
     * 
     * <p>This method implements the "Service Hook" pattern. It takes the request, 
     * executes the {@link ServiceCallback}, and uses the return value to call 
     * {@code rcl_send_response} automatically.</p>
     * 
     * @param waitSet The active native wait set.
     * @param services The list of services to check.
     */
    private void dispatchServices(MemorySegment waitSet, List<Service<?, ?>> services) {
        MemorySegment servicesPtr = rcl_wait_set_t.services(waitSet);

        for (int i = 0; i < services.size(); i++) {
            MemorySegment srvPtr = servicesPtr.getAtIndex(ValueLayout.ADDRESS, i);
            
            if (!srvPtr.equals(MemorySegment.NULL)) {
                Service<?, ?> srv = services.get(i);
                
                // Maak een Shared Arena voor de volledige Request-Response transactie
                Arena transactionArena = Arena.ofShared();
                
                // Allokeer header en request buffer
                MemorySegment requestHeader = transactionArena.allocate(rmw_request_id_t.layout());
                // Let op: getMessageSize moet de grootte van de _Request struct teruggeven
                MemorySegment requestBuffer = transactionArena.allocate(getMessageSize(srv.serviceDefinition().getRequestClass()));

                int rc = RclLib.rcl_take_request(srv.handle(), requestHeader, requestBuffer);

                if (rc == 0) {
                    srv.callbackGroup().execute(() -> {
                        try {
                            // Voer de callback uit. De programmeur gebruikt de transactionArena als allocator.
                            // We casten de callback naar de generieke Function/Interface vorm.
                            MemorySegment responseBuffer = ((ServiceCallback) srv.callback()).handle(requestBuffer, transactionArena);

                            if (responseBuffer != null) {
                                int sendRc = RclLib.rcl_send_response(srv.handle(), requestHeader, responseBuffer);
                                if (sendRc != 0) {
                                    System.err.println("Failed to send response: " + sendRc);
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        } finally {
                            transactionArena.close();
                        }
                    }, callbackExecutor);
                } else {
                    transactionArena.close();
                }
            }
        }
    }

    /**
     * Handles expired timers by resetting their internal state and triggering callbacks.
     * 
     * <p>Calls {@code rcl_timer_call} to acknowledge the timer event before 
     * dispatching the callback to a virtual thread.</p>
     * 
     * @param waitSet The active native wait set.
     * @param timers The list of timers to check.
     */
    private void dispatchTimers(MemorySegment waitSet, List<Timer> timers) {
        // Haal de pointer naar de timer-array uit de wait_set
        MemorySegment timersPtr = org.ros2.rcl.rcl_wait_set_t.timers(waitSet);

        for (int i = 0; i < timers.size(); i++) {
            MemorySegment timerPtr = timersPtr.getAtIndex(ValueLayout.ADDRESS, i);
            
            // Als de pointer in de wait_set NIET NULL is, is de timer "ready"
            if (!timerPtr.equals(MemorySegment.NULL)) {
                Timer timer = timers.get(i);

                // 1. Essentieel: Bevestig aan RCL dat we de timer gaan afhandelen
                // Dit reset de interne timer en berekent de volgende deadline
                int rc = RclLib.rcl_timer_call(timer.handle());
                
                if (rc == 0) { 
                    timer.callbackGroup().execute(() -> {
                        try {
                            timer.callback().accept(timer); 
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }, callbackExecutor);
                }
            }
        }
    }

    /**
     * Monitors service clients for incoming responses.
     * 
     * @param waitSet The active native wait set.
     * @param clients The list of clients to check.
     */
    private void dispatchClients(MemorySegment waitSet, List<Client<?, ?>> clients) {
        MemorySegment clientsPtr = rcl_wait_set_t.clients(waitSet);

        for (int i = 0; i < clients.size(); i++) {
            MemorySegment clientPtr = clientsPtr.getAtIndex(ValueLayout.ADDRESS, i);
            
            if (!clientPtr.equals(MemorySegment.NULL)) {
                Client<?, ?> client = clients.get(i);
                
                // Gebruik een Shared Arena: de data moet blijven leven tot de Future verwerkt is
                Arena responseArena = Arena.ofShared();
                
                // 1. Header voor de response (bevat het sequence number)
                MemorySegment responseHeader = responseArena.allocate(rmw_request_id_t.layout());
                
                // 2. Buffer voor de data (T_Res)
                long size = getMessageSize(client.serviceType().getResponseClass());
                MemorySegment responseBuffer = responseArena.allocate(size);

                // 3. Haal de response op
                int rc = RclLib.rcl_take_response(client.handle(), responseHeader, responseBuffer);

                if (rc == 0) {
                    // Haal het sequence number uit de header
                    long sequenceNumber = responseHeader.get(ValueLayout.JAVA_LONG, rmw_request_id_t.sequence_number$offset());
                    // System.out.println("[Executor] Received response with SeqNum: " + sequenceNumber);
                    // System.out.println("[Executor] Pending keys: " + client.pendingRequests.keySet());
                    // Zoek de bijbehorende future in de client
                    var future = client.pendingRequests.remove(sequenceNumber);
                    
                    if (future != null) {
                        // Debug: Kijk of we hier komen
                        // System.out.println("[Executor] Completing future for SeqNum: " + sequenceNumber);
                        client.callbackGroup().execute(() -> {
                            try {
                                future.complete(responseBuffer);
                                // System.out.println("[Executor] Future.complete() aangeroepen");
                            } finally {
                                responseArena.close();
                            }
                        }, callbackExecutor);
                    } else {
                        responseArena.close();
                    }
                } else {
                    responseArena.close();
                }
            }
        }
    }

    /**
     * Processes middleware events such as QoS status changes or message loss.
     * 
     * @param waitSet The active native wait set.
     * @param node The node containing the event handlers.
     */
    private void dispatchEvents(MemorySegment waitSet, Node node) {
        MemorySegment eventsPtr = rcl_wait_set_t.events(waitSet);

        int eventWaitSetIndex = 0; 

        for (EventHandler eventHandler : node.events) {
            MemorySegment triggeredEventPtr = eventsPtr.getAtIndex(ValueLayout.ADDRESS, eventWaitSetIndex);
            
            if (!triggeredEventPtr.equals(MemorySegment.NULL)) {
                handleQosEvent(eventHandler);
            }
            eventWaitSetIndex++;
        }
    }

    private void handleQosEvent(EventHandler handler) {
            Arena eventArena = Arena.ofShared();
            MemorySegment safeStatus = captureEventToArena(handler, eventArena);
            
            if (safeStatus != null) {
                callbackExecutor.submit(() -> {
                    try {
                        QoS qos = new QoS();
                        qos.retrieveFromStatus(safeStatus);
                        System.out.printf("[QoS] Policy: %s | Total: %d%n", 
                            qos.getLastPolicyName(), qos.getTotalCount());
                        
                        handler.getCallback().accept(safeStatus);
                    } finally {
                        eventArena.close();
                    }
                });
            } else {
                eventArena.close();
            }
    }

    private <T> void handleSubscriptionCallback(Subscription<T> sub) {
        try (Arena takeArena = Arena.ofShared()) {
            long size = getMessageSize(sub.messageType()); 
            MemorySegment messageBuffer = takeArena.allocate(size);

            int ret = rcl_take(sub.handle(), messageBuffer, MemorySegment.NULL, MemorySegment.NULL);

            if (ret == 0) { 
                MemorySegment twistObject = convertToIdl(messageBuffer);
                sub.callback().accept(twistObject);
            }
        } catch (Exception e) {
            System.err.println("Fout bij verwerken subscription: " + e.getMessage());
        }
    }

    /**
     * Captures message data from the middleware into a persistent memory arena.
     * 
     * @param sub The subscription to take data from.
     * @param targetArena The arena where the captured data should reside.
     * @return A {@link MemorySegment} containing the message data, or {@code null} if take failed.
     */
    private MemorySegment captureData(Subscription<?> sub, Arena targetArena) {
        try (Arena tempArena = Arena.ofConfined()) {
            long size = getMessageSize(sub.messageType());
            MemorySegment messageBuffer = tempArena.allocate(size);

            int ret = rcl_take(sub.handle(), messageBuffer, MemorySegment.NULL, MemorySegment.NULL);

            if (ret == 0) {
                MemorySegment persistentCopy = targetArena.allocate(size);
                persistentCopy.copyFrom(messageBuffer);
                return persistentCopy;
            }
        }
        return null;
    }

    private MemorySegment captureEventToArena(EventHandler handler, Arena target) {
        try (Arena temp = Arena.ofConfined()) {
            long size = rmw_requested_qos_incompatible_event_status_t.layout().byteSize();
            MemorySegment buf = temp.allocate(size);
            
            if (rcl_take_event(handler.eventHandle(), buf) == 0) {
                MemorySegment copy = target.allocate(size);
                copy.copyFrom(buf);
                return copy;
            }
        }
        return null;
    }

    private long getMessageSize(Class<?> messageType) {        
        try {
            java.lang.foreign.StructLayout layout = (java.lang.foreign.StructLayout) 
                messageType.getField("LAYOUT").get(null);
            return layout.byteSize();
        } catch (Exception e) {
            return 256L; 
        }
    }

    private <T> MemorySegment convertToIdl(MemorySegment segment) {
        MemorySegment copy = Arena.global().allocate(segment.byteSize());
        copy.copyFrom(segment);
        
        return copy;
    }
}