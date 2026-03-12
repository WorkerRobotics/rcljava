package com.workerrobotics.rcljava.core.callbackgroup;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A Callback Group that guarantees strictly sequential execution of callbacks.
 * * <p>The {@code MutuallyExclusiveCallbackGroup} ensures that no two callbacks 
 * within this group run at the same time. It is ideal for handling stateful 
 * components where thread-safety is required without using heavy synchronization 
 * inside the user-defined callbacks.</p>
 * * <p><strong>Implementation Strategy:</strong></p>
 * <ul>
 * <li><strong>FIFO Ordering:</strong> Uses an internal {@link LinkedBlockingQueue} 
 * to manage pending tasks in the order they were received.</li>
 * <li><strong>Non-blocking Locking:</strong> Uses an {@link AtomicBoolean} to 
 * manage the execution state, ensuring that the "wait-loop" thread 
 * never blocks when submitting a task.</li>
 * <li><strong>Virtual Thread Hand-off:</strong> When a task completes, it 
 * automatically triggers the execution of the next task in the queue 
 * using the provided {@link ExecutorService}.</li>
 * </ul>
 * * @see CallbackGroup
 * @see ReentrantCallbackGroup
 */
public class MutuallyExclusiveCallbackGroup extends CallbackGroup {
    private final LinkedBlockingQueue<Runnable> queue = new LinkedBlockingQueue<>();
    private final AtomicBoolean isRunning = new AtomicBoolean(false);

    /**
     * Adds a task to the internal queue and attempts to trigger execution.
     * * <p>If no other task from this group is currently running, the task will 
     * be submitted to the {@code vThreadExecutor} immediately. Otherwise, it 
     * remains in the queue until the current task finishes.</p>
     * * @param task The ROS2 callback task to be executed.
     * @param vThreadExecutor The executor service used to run the tasks.
     */
    @Override
    public void execute(Runnable task, ExecutorService vThreadExecutor) {
        queue.offer(task);
        tryExecuteNext(vThreadExecutor);
    }

    /**
     * Attempts to transition the state from idle to running and execute the next 
     * task in the FIFO queue.
     * * <p>This method uses a Compare-And-Set (CAS) operation to ensure thread-safe 
     * access to the execution slot. If a task is executed, the {@code finally} 
     * block of that task will call {@code tryExecuteNext} again to drain 
     * the queue.</p>
     * * @param vThreadExecutor The executor service used to run the tasks.
     */
    private void tryExecuteNext(ExecutorService vThreadExecutor) {
        // Only attempt to grab the 'lock' if the group is currently idle
        if (isRunning.compareAndSet(false, true)) {
            Runnable nextTask = queue.poll();
            if (nextTask != null) {
                vThreadExecutor.submit(() -> {
                    try {
                        nextTask.run();
                    } finally {
                        isRunning.set(false);
                        tryExecuteNext(vThreadExecutor);
                    }
                });
            } else {
                isRunning.set(false);
            }
        }
    }
}