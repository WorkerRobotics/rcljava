package com.workerrobotics.rcljava.core.callbackgroup;

import java.util.concurrent.ExecutorService;

/**
 * Abstract base class that defines the execution strategy for ROS2 callbacks.
 * * <p>A {@code CallbackGroup} controls how the executor schedules and runs callbacks
 * (such as subscriptions, timers, and service servers). By subclassing this, you 
 * can implement different concurrency policies, such as mutually exclusive 
 * execution or reentrant parallel execution.</p>
 * * <p>In the context of RCLJava, this class acts as a bridge between the ROS2 
 * wait-set (which identifies ready tasks) and the Java {@link ExecutorService} 
 * (which provides the execution resources, typically Virtual Threads).</p>
 * * @see ReentrantCallbackGroup
 * @see MutuallyExclusiveCallbackGroup
 */
public abstract class CallbackGroup {
    /**
     * Dispatches a specific ROS2 task for execution according to the group's policy.
     * * <p>This method is called by the {@code Executor} when a handle (subscription, 
     * timer, etc.) associated with this group is triggered in the wait-set.</p>
     * * @param task The logic to be performed (usually involving an {@code rcl_take} 
     * and the subsequent user-defined callback).
     * @param vThreadExecutor The {@link ExecutorService} provided by the Executor, 
     * optimized for handling high-concurrency tasks via 
     * Virtual Threads.
     */
    public abstract void execute(Runnable task, ExecutorService vThreadExecutor);
}