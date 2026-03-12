package com.workerrobotics.rcljava.core.callbackgroup;

import java.util.concurrent.ExecutorService;

/**
 * A Callback Group that allows parallel execution of its callbacks.
 * * <p>The {@code ReentrantCallbackGroup} is designed for scenarios where callbacks 
 * do not conflict with each other or where thread-safety is handled internally 
 * by the user logic. Unlike a mutually exclusive group, this implementation 
 * does not wait for a previous callback to finish before starting a new task 
 * from the same group.</p>
 * * <p><strong>Threading Model:</strong></p>
 * <ul>
 * <li>Utilizes a {@link java.util.concurrent.ExecutorService} (typically a Virtual Thread Per Task Executor).</li>
 * <li>Each incoming task is submitted immediately to the executor, ensuring maximum 
 * throughput and preventing the main executor loop from blocking.</li>
 * <li>Ideal for I/O-bound tasks or stateless message processing.</li>
 * </ul>
 * * @see CallbackGroup
 * @see Executor
 */
public class ReentrantCallbackGroup extends CallbackGroup {

    /**
     * Executes the provided task immediately by submitting it to the Virtual Thread executor.
     * * <p>This implementation fulfills the reentrant strategy by applying no 
     * synchronization or blocking between successive calls. If multiple events 
     * (e.g., subscription messages or timers) occur simultaneously for this group, 
     * they will be executed in parallel on separate virtual threads.</p>
     * * @param task The {@link Runnable} representing the ROS2 callback (e.g., subscription, timer, or service).
     * @param vThreadExecutor The {@link ExecutorService} responsible for managing the 
     * (virtual) threads for this executor.
     */
    @Override
    public void execute(Runnable task, ExecutorService vThreadExecutor) {
        vThreadExecutor.submit(task);
    }
}