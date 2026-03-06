package com.workerrobotics.rcljava.core;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.function.Consumer;

import org.ros2.rcl.RclLib;
import org.ros2.rcl.msgs.GEOMETRY_MSGS_Lib;

import com.workerrobotics.rcljava.core.events.EventType;
import com.workerrobotics.rcljava.core.events.HandleType;
import com.workerrobotics.rcljava.ffi.NativeChecks;

import static org.ros2.rcl.RclLib.*;

/**
 * A Java wrapper for the ROS 2 {@code rcl_publisher_t} structure.
 * 
 * <p>The {@code Publisher} class allows a ROS 2 node to send messages of a specific type 
 * to a named topic. It handles the native initialization of the publisher entity and 
 * provides methods to broadcast data to the middleware.</p>
 * 
 * <p>Implementation details: This class utilizes the <b>Foreign Function & Memory (FFM) API</b> 
 * to interface with the {@code librcl} library and requires a valid type support handle 
 * to correctly serialize messages for the underlying RMW layer.</p>
 * 
 * @param <T> The message type class this publisher handles.
 */
public class Publisher<T> implements AutoCloseable {
    private final MemorySegment handle;
    private final Class<T> messageType;
    private final String topicName;
    private final Node node;

    /**
     * Initializes a new ROS 2 publisher.
     * 
     * <p>Allocates a native publisher handle and binds it to the provided topic name 
     * and message type support. Default options are fetched from the middleware and 
     * initialized via {@code rcl_publisher_init}.</p>
     * 
     * @param node The parent node managing this publisher's lifecycle.
     * @param topicName The name of the topic to publish to (e.g., "cmd_vel").
     * @param messageType The Java class representing the ROS 2 message type.
     * @param typeSupport The memory handle pointing to the native {@code rosidl_message_type_support_t}.
     * @throws com.workerrobotics.rcljava.ffi.RclException If initialization fails at the native level.
     */
    public Publisher(Node node, String topicName, Class<T> messageType, MemorySegment typeSupport) {
        this.node = node;
        this.messageType = messageType;
        this.topicName = topicName;
        
        // Gebruik de arena van de node voor de levensduur van de handle
        this.handle = rcl_get_zero_initialized_publisher(node.nodeArena);
        
        MemorySegment topic = node.nodeArena.allocateFrom(topicName);
        MemorySegment options = rcl_publisher_get_default_options(node.nodeArena);

        int rc = rcl_publisher_init(handle, node.nodeHandle, typeSupport, topic, options);
        NativeChecks.requireOk(rc, "rcl_publisher_init");
    }

    /**
     * Publishes a message to the associated topic.
     * 
     * <p>The message must be provided as a {@link MemorySegment} that points to a correctly 
     * formatted native IDL structure. Serialization is handled by the underlying RMW 
     * implementation specified in the {@link RosContext}.</p>
     * 
     * @param message The native memory segment containing the serialized message data.
     * @throws com.workerrobotics.rcljava.ffi.RclException If the native {@code rcl_publish} call fails.
     */
    public void publish(MemorySegment messageSegment) {
        // In ROS2 rcl_publish geef je de handle en een pointer naar de ROS message struct
        int rc = rcl_publish(handle, messageSegment, MemorySegment.NULL);
        NativeChecks.requireOk(rc, "rcl_publish");
    }

    /**
     * Attaches an event handler to this publisher (e.g., for monitoring offered QoS incompatibility).
     * 
     * @param eventType The type of event to monitor.
     * @param callback The function to execute when the event occurs.
     */
    public void addEventHandler(final EventType eventType, final Consumer<MemorySegment> callback) {
        this.node.addEventHandler(eventType, this.handle, HandleType.PUBLISHER, callback);
    }

    /**
     * Finalizes the native publisher and releases its handle.
     * 
     * <p>Invokes {@code rcl_publisher_fini} to ensure the middleware stops broadcasting 
     * and cleans up internal resources associated with this entity.</p>
     */
    @Override
    public void close() {
        rcl_publisher_fini(handle, node.nodeHandle);
    }

    /** @return The raw {@link MemorySegment} pointing to the native {@code rcl_publisher_t}. */
    public MemorySegment handle() {
        return handle;
    }

     /** @return The Java class type of the message being handled. */
    public Class<T> messageType() { return messageType; }

    /** @return The name of the topic this publisher is broadcasting to. */
    public String topicName() { return topicName; }
}
