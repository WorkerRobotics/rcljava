package com.workerrobotics.rcljava.core;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import org.ros2.rcl.RclLib;

import com.workerrobotics.rcljava.core.events.EventHandler;
import com.workerrobotics.rcljava.core.events.EventType;
import com.workerrobotics.rcljava.core.events.HandleType;
import com.workerrobotics.rcljava.ffi.NativeChecks;
import com.workerrobotics.rcljava.ffi.RclException;
import com.workerrobotics.rcljava.loader.RosLoader;

import static org.ros2.rcl.RclLib.*;

/**
 * A Java wrapper for the ROS 2 {@code rcl_subscription_t} structure.
 * 
 * <p>This class enables a ROS 2 node to subscribe to a topic and receive messages of a specific type.
 * It uses dynamic symbol lookup to retrieve message type support handles at runtime.</p>
 * 
 * <p>Implementation uses the <b>Foreign Function & Memory (FFM) API</b> to interact with 
 * the native ROS 2 Client Library (rcl).</p>
 * 
 * @param <T> The message type class this subscription handles.
 */
public class Subscription<T> implements AutoCloseable {
    private final MemorySegment handle;
    private final Class<T> messageType;
    private final Consumer<MemorySegment> callback;
    private final String topicName;
    private final QoS qos;
    private final Node node;

    /**
     * Creates a new subscription with optional Quality of Service (QoS) settings.
     * 
     * @param node The parent node that will manage this subscription's lifecycle.
     * @param topicName The name of the topic to subscribe to (e.g., "chatter").
     * @param messageType The Java class representing the ROS 2 message type.
     * @param qos The QoS profile to apply, or {@code null} to use default options.
     * @param callback The function to execute when a new message is received.
     * @throws com.workerrobotics.rcljava.ffi.RclException If initialization fails at the native level.
     */
    public Subscription(Node node, String topicName, Class<T> messageType, QoS qos, Consumer<MemorySegment> callback) {
        this.node = node;
        this.qos = qos;
        this.topicName = topicName;
        this.messageType = messageType;
        this.callback = callback;

        MemorySegment typeSupport = getDynamicTypeSupport(messageType);

        // 1. Initialiseer de handle in de shared arena van de node
        this.handle = rcl_get_zero_initialized_subscription(node.nodeArena);
        
        // 2. Opties ophalen
        MemorySegment options = rcl_subscription_get_default_options(node.nodeArena);

        if (qos != null) {
            qos.applyTo(options);
        }

        // 3. Subscription initialiseren
        try (Arena tempArena = Arena.ofConfined()) {
            MemorySegment cTopicName = tempArena.allocateFrom(topicName);
            
            int rc = rcl_subscription_init(
                handle, 
                node.nodeHandle, 
                typeSupport, 
                cTopicName, 
                options
            );
            NativeChecks.requireOk(rc, "rcl_subscription_init");
        }
    }

    /**
     * Creates a new subscription with default Quality of Service (QoS) settings.
     * 
     * @param node The parent node.
     * @param topicName The name of the topic.
     * @param messageType The Java class representing the ROS 2 message type.
     * @param callback The function to execute when a new message is received.
     */
    public Subscription(Node node, String topicName, Class<T> messageType, Consumer<MemorySegment> callback) {
        this(node, topicName, messageType, null, callback);
    }

    /** @return The parent {@link Node} instance. */
    public Node node(){
        return node;
    }

    /** @return The {@link MemorySegment} pointing to the native {@code rcl_subscription_t}. */
    public MemorySegment handle() {
        return handle;
    }

    /** @return The Java class type of the message being handled. */
    public Class<T> messageType() {
        return messageType;
    }

    /** @return The {@link QoS} profile applied to this subscription. */
    public QoS qos() {
        return qos;
    }

    /** @return The name of the subscribed topic. */
    public String topicName()  {
        return this.topicName;
    }

    /** @return The {@link Consumer} callback function. */
    public Consumer<MemorySegment> callback() {
        return callback;
    }

    /**
     * Attaches an event handler to this subscription (e.g., for monitoring QoS incompatibility).
     * 
     * @param eventType The type of event to monitor.
     * @param callback The function to execute when the event occurs.
     */
    public void addEventHandler(final EventType eventType, final Consumer<MemorySegment> callback) {
        this.node.addEventHandler(eventType, this.handle, HandleType.SUBSCRIPTION, callback);
    }

    /**
     * Finalizes the native subscription and releases its handle.
     * 
     * <p>Uses {@code rcl_subscription_fini} to ensure the middleware correctly
     * cleans up its internal tracking of this subscriber.</p>
     */
    @Override
    public void close() {
        // ROS 2 vereist de node handle om de subscription netjes af te sluiten
        rcl_subscription_fini(handle, node.nodeHandle);
    }

    /**
     * Dynamically retrieves the type support handle for the given message class.
     * 
     * <p>This method constructs the expected C symbol name based on the class name
     * and performs a look-up using {@link com.workerrobotics.rcljava.loader.RosLoader}.</p>
     * 
     * @param messageType The class to look up.
     * @return The {@link MemorySegment} pointing to the type support handle.
     */
    private MemorySegment getDynamicTypeSupport(Class<?> messageType) {
    // Converteer org.ros2.rcl.msgs.geometry_msgs__msg__Twist 
    // naar de C-functie naam die de typesupport handle teruggeeft.
    String className = messageType.getSimpleName();
    
    // De standaard ROS 2 C-functie naamgeving voor typesupport:
    String symbolName = "rosidl_typesupport_c__get_message_type_support_handle__" + className;

    // Roep de universele findSymbol aan
    MemorySegment functionAddress = RosLoader.findSymbol(symbolName);
    
    // De handle-functies in ROS hebben geen argumenten en retourneren een pointer
    // We moeten deze functie-pointer dus even aanroepen.
    return callHandleFunction(functionAddress);
  }

  /**
     * Invokes the native type support function using the FFM Linker.
     * 
     * @param address The address of the C function to invoke.
     * @return The returned type support handle address.
     */
  private MemorySegment callHandleFunction(MemorySegment address) {
    // Gebruik Linker om de C-functie daadwerkelijk uit te voeren
    java.lang.foreign.Linker linker = java.lang.foreign.Linker.nativeLinker();
    java.lang.foreign.FunctionDescriptor desc = java.lang.foreign.FunctionDescriptor.of(java.lang.foreign.AddressLayout.ADDRESS);
    try {
        java.lang.invoke.MethodHandle handle = linker.downcallHandle(address, desc);
        return (MemorySegment) handle.invokeExact();
    } catch (Throwable t) {
        throw new RuntimeException("Failed to invoke typesupport handle function", t);
    }
  }
}