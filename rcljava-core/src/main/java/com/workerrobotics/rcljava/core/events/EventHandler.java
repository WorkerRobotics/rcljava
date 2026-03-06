package com.workerrobotics.rcljava.core.events;

import static org.ros2.rcl.RclLib.rcl_take_event;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.function.Consumer;

import org.ros2.rcl.*;

/**
 * A container for managing ROS 2 middleware (RMW) event handles and their associated callbacks.
 * 
 * <p>The {@code EventHandler} class links a specific {@link EventType} and its corresponding 
 * native {@code rcl_event_t} handle to a Java {@link Consumer}. When the {@code Executor} 
 * detects that an event is ready in the wait set, it uses this handler to dispatch the 
 * status data to the user-defined callback.</p>
 * 
 * <p>This class is typically instantiated via {@link com.workerrobotics.rcljava.core.Node#addEventHandler} 
 * for Publishers and Subscriptions to monitor QoS status or connectivity changes.</p>
 */
public class EventHandler {
    private final Consumer<MemorySegment> callback;
    private final MemorySegment eventHandle;
    private final EventType eventType;

    /**
     * Constructs a new EventHandler.
     * 
     * @param eventType   The type of middleware event being monitored (e.g., QoS Incompatibility).
     * @param eventHandle A {@link MemorySegment} pointing to the initialized native {@code rcl_event_t}.
     * @param callback    The Java function to execute when the event is triggered. 
     *                    The function receives a {@link MemorySegment} containing the event status data.
     */
    public EventHandler(final EventType eventType, MemorySegment eventHandle, Consumer<MemorySegment> callback) {
        this.eventType = eventType;
        this.eventHandle = eventHandle;
        this.callback = callback;
    }

    /**
     * Returns the type of the event managed by this handler.
     * 
     * @return The {@link EventType} constant.
     */
    public EventType getEventType() {
        return this.eventType;
    }

    /**
     * Returns the raw memory handle pointing to the native {@code rcl_event_t} structure.
     * 
     * @return A {@link MemorySegment} representing the native pointer to the event.
     */
    public MemorySegment eventHandle() {
        return this.eventHandle;
    }

    /**
     * Returns the Java callback associated with this event.
     * 
     * @return A {@link Consumer} representing the function executed upon event detection.
     */
    public Consumer<MemorySegment> getCallback() {
        return this.callback;
    }

}
