package com.workerrobotics.rcljava.core;

import org.ros2.rcl.RclLib;
import org.ros2.rcl.msgs.GEOMETRY_MSGS_Lib;
import org.ros2.rcl.msgs.rosidl_runtime_c__String;

import com.workerrobotics.rcljava.core.callbackgroup.CallbackGroup;
import com.workerrobotics.rcljava.core.callbackgroup.MutuallyExclusiveCallbackGroup;
import com.workerrobotics.rcljava.core.clock.ClockType;
import com.workerrobotics.rcljava.core.events.EventHandler;
import com.workerrobotics.rcljava.core.events.EventType;
import com.workerrobotics.rcljava.core.events.HandleType;
import com.workerrobotics.rcljava.core.service.ServiceCallback;
import com.workerrobotics.rcljava.core.service.ServiceType;
import com.workerrobotics.rcljava.ffi.NativeChecks;
import com.workerrobotics.rcljava.ffi.RclException;
import com.workerrobotics.rcljava.loader.RosLoader;

import static org.ros2.rcl.RclLib.rcl_get_zero_initialized_event;
import static org.ros2.rcl.RclLib.rcl_publisher_event_init;
import static org.ros2.rcl.RclLib.rcl_subscription_event_init;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SegmentAllocator;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * An abstract base class representing a ROS 2 Node.
 * 
 * <p>A Node is the primary participant in the ROS 2 graph. This class manages the 
 * lifecycle of various ROS entities such as Publishers, Subscriptions, Timers, 
 * Clients, and Services. It handles the native {@code rcl_node_t} initialization 
 * and ensures that all associated resources are allocated within a managed memory arena.</p>
 * 
 * <p>Implementation details: This class uses the <b>Foreign Function & Memory (FFM) API</b> 
 * to interface with the native {@code librcl} library.</p>
 */
public abstract class Node implements AutoCloseable {
    
    /** The memory arena managing the lifecycle of native resources for this node. */
    protected final Arena nodeArena;

    /** The native memory handle for the {@code rcl_node_t} structure. */
    protected final MemorySegment nodeHandle;

    /** The name of the node. */
    protected final String name;

    private final CallbackGroup defaultGroup = new MutuallyExclusiveCallbackGroup();
    protected final List<CallbackGroup> callbackGroups = new ArrayList<>();

    protected final List<Publisher<?>> publishers = new ArrayList<>();
    protected final List<Subscription<?>> subscriptions = new ArrayList<>();
    protected final List<MemorySegment> guardconditions = new ArrayList<>();
    protected final List<Timer> timers = new ArrayList<>();
    protected final List<Client<?,?>> clients = new ArrayList<>();
    protected final List<Service<?,?>> services = new ArrayList<>();
    protected final List<EventHandler> events = new ArrayList<>();

    private final Clock clock;

    private Node(String name, Arena arena, MemorySegment nodeHandle, ClockType clockType) {
        this.name = name;
        this.nodeArena = arena;
        this.nodeHandle = nodeHandle;

        Clock clock = new Clock(clockType);
        this.clock = clock;
    }

    /**
     * Initializes a new ROS 2 Node with a specific clock type.
     * 
     * @param name The name of the node.
     * @param clockType The type of clock to be used by the node.
     * @throws RclException If native node initialization fails.
     */
    protected Node(String name, ClockType clockType) {
      Arena ecosystemArena = Arena.ofShared();
      MemorySegment nodeHandle = RclLib.rcl_get_zero_initialized_node(ecosystemArena);
      MemorySegment nodeName = ecosystemArena.allocateFrom(name);
      MemorySegment namespace = ecosystemArena.allocateFrom("");
      MemorySegment nodeOptions = RclLib.rcl_node_get_default_options(ecosystemArena);
      
      int rc = RclLib.rcl_node_init(nodeHandle, nodeName, namespace, RosContext.get(), nodeOptions);
      NativeChecks.requireOk(rc, "rcl_node_init");

      this(name, ecosystemArena, nodeHandle, clockType);
    }

    /**
     * Initializes a new ROS 2 Node with the default ROS time clock.
     * 
     * @param name The name of the node.
     */
    protected Node(String name) {
        this(name, ClockType.RCL_ROS_TIME);
    }

    /**
     * Creates a new Publisher for the specified topic and message type.
     * 
     * @param <T> The message type.
     * @param topicName The topic name to publish to.
     * @param messageType The Java class of the message.
     * @param typeSupport The native memory handle for the type support.
     * @return A new {@link Publisher} instance.
     */
    public <T> Publisher<T> createPublisher(String topicName, Class<T> messageType, MemorySegment typeSupport) {
        Publisher<T> pub = new Publisher<>(this, topicName, messageType, typeSupport);
        this.publishers.add(pub);
        return pub;
    }

     /**
     * Creates a new Subscription for the specified topic.
     * 
     * @param <T> The message type.
     * @param topicName The topic name to subscribe to.
     * @param messageType The Java class of the message.
     * @param qos The Quality of Service profile (can be null).
     * @param callback The function executed when a message is received.
     * @param callbackGroup The callbackgroup where the callback is executed.
     * @return A new {@link Subscription} instance.
     */
    public <T> Subscription<T> createSubscription(String topicName, Class<T> messageType, QoS qos, Consumer<MemorySegment> callback, CallbackGroup callbackGroup) {
        Subscription<T> sub = new Subscription<>(this, topicName, messageType, qos, callback, callbackGroup);
        this.subscriptions.add(sub);
        return sub;
    }

    public <T> Subscription<T> createSubscription(String topicName, Class<T> messageType, Consumer<MemorySegment> callback, CallbackGroup callbackGroup) {
      return this.createSubscription(topicName, messageType, null, callback, callbackGroup);
    }

    public <T> Subscription<T> createSubscription(String topicName, Class<T> messageType, Consumer<MemorySegment> callback) {
      return this.createSubscription(topicName, messageType, null, callback, this.defaultGroup);
    }

    /**
     * Creates a new Service server.
     * 
     * @param <T_Req> The request type.
     * @param <T_Res> The response type.
     * @param serviceName The name of the service.
     * @param serviceDefinition The request/response type definitions.
     * @param callback The function executed when a request is received.
     * @param qosProfile The QoS profile for the service.
     * @param callbackGroup The name of the callback group.
     * @return A new {@link Service} instance.
     */
    public <T_Req, T_Res> Service<T_Req, T_Res> createService(String serviceName, ServiceType<T_Req, T_Res> serviceDefinition, ServiceCallback callback, CallbackGroup callbackGroup) {
      Service<T_Req, T_Res> service = new Service<T_Req, T_Res>(this, serviceName, serviceDefinition, callback, callbackGroup);
      this.services.add(service);
      return service;
    }

    public <T_Req, T_Res> Service<T_Req, T_Res> createService(String serviceName, ServiceType<T_Req, T_Res> serviceDefinition, ServiceCallback callback) {
      return this.createService(serviceName, serviceDefinition, callback, this.defaultGroup);
    }

    /**
     * Creates a new Client for a remote service.
     * 
     * @param <T_Req> The request type.
     * @param <T_Res> The response type.
     * @param serviceName The name of the service to call.
     * @param serviceDefinition The request/response type definitions.
     * @param callbackGroup The callbackgroup where the client is executed when a response is received.
     * @return A new {@link Client} instance.
     */
    public <T_Req, T_Res> Client<T_Req, T_Res> createClient(String serviceName, ServiceType<T_Req, T_Res> serviceDefinition) {
        return this.createClient(serviceName, serviceDefinition, this.defaultGroup);
    }

    public <T_Req, T_Res> Client<T_Req, T_Res> createClient(String serviceName, ServiceType<T_Req, T_Res> serviceDefinition, CallbackGroup callbackGroup) {
        Client<T_Req, T_Res> client = new Client<T_Req, T_Res>(this, serviceName, serviceDefinition, callbackGroup);
        this.clients.add(client);
        return client;
    }

    /**
     * Creates a new periodic Timer.
     * 
     * @param periodMillis The timer period in milliseconds.
     * @param callback The function executed upon timer expiry.
     * @param callbackGroup The callbackgroup where the callback is executed.
     * @return A new {@link Timer} instance.
     */
    public Timer createTimer(long periodMillis, Consumer<Timer> callback, CallbackGroup callbackGroup) {
        var timer = new Timer(this, periodMillis * 1_000_000L, callback, callbackGroup);
        this.timers.add(timer);
        return timer;
    }

    public Timer createTimer(long periodMillis, Consumer<Timer> callback) {
        var timer = new Timer(this, periodMillis * 1_000_000L, callback, this.defaultGroup);
        this.timers.add(timer);
        return timer;
    }

    /** @return The name of the node. */
    public String getName() {
      return this.name;
    }

    /** @return The {@link Clock} instance associated with this node. */
    public Clock getClock() {
        return this.clock;
    }

    public CallbackGroup createCallbackGroup(Class<? extends CallbackGroup> type) {
        try {
            CallbackGroup group = type.getDeclaredConstructor().newInstance();
            callbackGroups.add(group);
            return group;
        } catch (Exception e) {
            throw new RuntimeException("Could not create callback group", e);
        }
    }

    /**
     * Attaches an event handler to a publisher or subscription.
     * 
     * <p>This allows monitoring of specific middleware events like QoS incompatibility 
     * or deadline misses.</p>
     * 
     * @param eventType The type of event to monitor.
     * @param handle The native handle of the entity (publisher/subscription).
     * @param handleType The type of the handle being monitored.
     * @param callback The function to execute when the event is triggered.
     */
    public void addEventHandler(final EventType eventType, final MemorySegment handle, final HandleType handleType, final Consumer<MemorySegment> callback) {
        MemorySegment event = rcl_get_zero_initialized_event(this.nodeArena);

        int eType = 0;

        switch (eventType) {
            case RCL_SUBSCRIPTION_REQUESTED_DEADLINE_MISSED: 
                eType = RclLib.RCL_SUBSCRIPTION_REQUESTED_DEADLINE_MISSED();
                break;
            case RCL_SUBSCRIPTION_INCOMPATIBLE_TYPE:
                eType = RclLib.RCL_SUBSCRIPTION_INCOMPATIBLE_TYPE();
                break;
            case RCL_SUBSCRIPTION_LIVELINESS_CHANGED:
                eType = RclLib.RCL_SUBSCRIPTION_LIVELINESS_CHANGED();
                break;
            case RCL_SUBSCRIPTION_MATCHED:
                eType = RclLib.RCL_SUBSCRIPTION_MATCHED();
                break;
            case RCL_SUBSCRIPTION_MESSAGE_LOST:
                eType = RclLib.RCL_SUBSCRIPTION_MESSAGE_LOST();
                break;
            case RCL_SUBSCRIPTION_REQUESTED_INCOMPATIBLE_QOS:
                eType = RclLib.RCL_SUBSCRIPTION_REQUESTED_INCOMPATIBLE_QOS();
                break;
            case RCL_PUBLISHER_OFFERED_DEADLINE_MISSED: 
                eType = RclLib.RCL_PUBLISHER_OFFERED_DEADLINE_MISSED();
                break;
            case RCL_PUBLISHER_LIVELINESS_LOST: 
                eType = RclLib.RCL_PUBLISHER_LIVELINESS_LOST();
                break;
            case RCL_PUBLISHER_OFFERED_INCOMPATIBLE_QOS: 
                eType = RclLib.RCL_PUBLISHER_OFFERED_INCOMPATIBLE_QOS();
                break;
            case RCL_PUBLISHER_INCOMPATIBLE_TYPE: 
                eType = RclLib.RCL_PUBLISHER_INCOMPATIBLE_TYPE();
                break;
            case RCL_PUBLISHER_MATCHED: 
                eType = RclLib.RCL_PUBLISHER_MATCHED();
                break;
            default:
                throw new RclException("No compatible event type found for event.", eType);
        }
        int returncode = 0;
        switch(handleType) {
          case PUBLISHER:
            returncode = rcl_publisher_event_init(event, handle, eType);
            NativeChecks.requireOk(returncode, "rcl_publisher_event_init");
            break;
          case SUBSCRIPTION:
            returncode = rcl_subscription_event_init(event, handle, eType);
            NativeChecks.requireOk(returncode, "rcl_subscription_event_init");
            break;
          default:
            throw new RclException("No compatible handle type found for handle.", 1);          
        }

        this.events.add(new EventHandler(eventType, event, callback));
    }

    public MemorySegment createRosString(SegmentAllocator allocator, String value) {
        // Allokeer de ROS String struct
        
        MemorySegment rosString = rosidl_runtime_c__String.allocate(allocator);
        
        // Allokeer de eigenlijke tekst in het geheugen
        MemorySegment cStr = allocator.allocateFrom(value);
        
        // Vul de velden van de ROS String struct (data, size, capacity)
        rosidl_runtime_c__String.data(rosString, cStr);
        rosidl_runtime_c__String.size(rosString, value.length());
        rosidl_runtime_c__String.capacity(rosString, value.length() + 1);
        
        return rosString;
    }

    /**
     * Finalizes the native node and releases all associated resources.
     * 
     * <p>Once closed, the node handle and all associated entities become invalid.</p>
     */
    @Override
    public void close() {
      for (Publisher<?> pub : publishers) {
        pub.close();
      }
      for (Subscription<?> sub : subscriptions) {
        sub.close();
      }
      for (Client client : clients) {
        client.close();
      }
      for (Service service : services) {
        service.close();
      }
      for (Timer timer : timers) {
        timer.close();
      }
      
      RclLib.rcl_node_fini(nodeHandle);
      if (nodeArena.scope().isAlive()) {
          nodeArena.close();
      }
    }

    public void printQosSummary() {
      System.out.println("\n" + "=".repeat(50));
      System.out.println(" ROS 2 NODE QoS SUMMARY: " + this.name);
      System.out.println("=".repeat(50));
      System.out.printf("%-20s | %-12s | %-12s | %-5s%n", "TOPIC", "RELIABILITY", "DURABILITY", "DEPTH");
      System.out.println("-".repeat(50));

      for (Subscription<?> sub : subscriptions) {
          QoS q = sub.qos(); // Zorg dat je de QoS bewaart in je Subscription klasse
          System.out.printf("%-20s | %-12s | %-12s | %-5d%n",
              sub.topicName(),
              decodeReliability(q.getReliability()),
              decodeDurability(q.getDurability()),
              q.getDepth()
          );
      }
      System.out.println("=".repeat(50) + "\n");
    }

  private String decodeReliability(int r) {
      return switch (r) {
          case QoS.RELIABILITY_RELIABLE -> "RELIABLE";
          case QoS.RELIABILITY_BEST_EFFORT -> "BEST_EFFORT";
          default -> "SYSTEM_DEF";
      };
  }

  private String decodeDurability(int d) {
      return switch (d) {
          case QoS.DURABILITY_TRANSIENT_LOCAL -> "TRANSIENT_L";
          case QoS.DURABILITY_VOLATILE -> "VOLATILE";
          default -> "SYSTEM_DEF";
      };
  }
}
