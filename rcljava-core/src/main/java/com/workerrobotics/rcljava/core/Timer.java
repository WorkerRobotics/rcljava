package com.workerrobotics.rcljava.core;

import com.workerrobotics.rcljava.core.callbackgroup.CallbackGroup;

import org.ros2.rcl.RclLib;
import java.lang.foreign.MemorySegment;
import java.util.function.Consumer;

import com.workerrobotics.rcljava.ffi.NativeChecks;

/**
 * A Java wrapper for the ROS 2 {@code rcl_timer_t} structure.
 * 
 * <p>The Timer class allows for the execution of a periodic callback at a specified 
 * interval. It is managed by an {@code Executor} that monitors the timer's 
 * deadline and triggers the provided Java consumer.</p>
 * 
 * <p>Implementation details: This class uses the <b>Foreign Function & Memory API (FFM)</b> 
 * to interact with the underlying {@code librcl} timer functions.</p>
 */
public class Timer implements AutoCloseable {
    private final MemorySegment handle;
    private final Node node;
    private final Consumer<Timer> callback;
    private final CallbackGroup callbackGroup;
    private final long periodNanoseconds;

    /**
     * Initializes a new ROS 2 timer and registers it with the provided Node.
     * 
     * <p>Allocates a zero-initialized timer handle in the node's memory arena and 
     * initializes it using the clock associated with the node and the global 
     * ROS context.</p>
     * 
     * @param node The parent node managing this timer's lifecycle.
     * @param periodNanoseconds The interval between timer executions in nanoseconds.
     * @param callback The Java function to be executed when the timer expires. 
     *                 The function receives the timer instance as an argument.
     * @throws com.workerrobotics.rcljava.ffi.RclException If the native {@code rcl_timer_init} call fails.
     */
    public Timer(Node node, long periodNanoseconds, Consumer<Timer> callback, CallbackGroup callbackGroup) {
        this.node = node;
        this.callback = callback;
        this.callbackGroup = callbackGroup;
        this.periodNanoseconds = periodNanoseconds;

        this.handle = RclLib.rcl_get_zero_initialized_timer(node.nodeArena);
        int rc = RclLib.rcl_timer_init(
            handle, 
            node.getClock().handle(), 
            RosContext.get(), 
            periodNanoseconds, 
            MemorySegment.NULL,
            RclLib.rcutils_get_default_allocator(node.nodeArena)
        );
        NativeChecks.requireOk(rc, "rcl_timer_init");
    }

    /**
     * Returns the raw memory handle pointing to the native {@code rcl_timer_t} structure.
     * 
     * <p>This handle is used by the {@code Executor} to add the timer to a wait set 
     * and to check for ready status.</p>
     * 
     * @return A {@link MemorySegment} representing the native pointer to the timer.
     */
    public MemorySegment handle() { return handle; }

    /**
     * Returns the Java callback associated with this timer.
     * 
     * @return A {@link Consumer} representing the function executed upon timer expiry.
     */
    public Consumer<Timer> callback() { return callback; }

    /** @return The {@link CallbackGroup} applied to this Timer. */
    public CallbackGroup callbackGroup() {return this.callbackGroup;}

    /**
     * Finalizes the native timer and releases associated resources.
     * 
     * <p>Invokes {@code rcl_timer_fini} to ensure the timer is correctly stopped 
     * and removed from the middleware's internal tracking.</p>
     */
    @Override
    public void close() {
        RclLib.rcl_timer_fini(handle);
    }
    
}
