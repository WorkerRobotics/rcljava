package com.workerrobotics.rcljava.core;

import com.workerrobotics.rcljava.ffi.RclNative;
import com.workerrobotics.rcljava.loader.RosConfig;
import com.workerrobotics.rcljava.loader.RosLoader;

import java.lang.foreign.MemorySegment;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;


/**
 * The central management entry point for the ROS 2 Java runtime.
 * 
 * <p>This class acts as a high-level orchestrator responsible for loading native ROS 2 
 * libraries and managing the global {@code rcl_context_t}. It ensures that the 
 * communication infrastructure is properly initialized before any nodes, 
 * publishers, or subscribers are created.</p>
 * 
 * <p><b>Lifecycle Contract:</b></p>
 * <ul>
 *   <li>Call {@link #initialize(String[], RosConfig)} exactly once at application startup.</li>
 *   <li>Call {@link #shutdown()} before exit to ensure clean resource deallocation.</li>
 * </ul>
 */
public final class RosRuntime {
  private static final AtomicBoolean INITIALIZED = new AtomicBoolean(false);
  private static volatile MemorySegment context;

  /**
   * Private constructor to prevent instantiation of this static utility class.
   */
  private RosRuntime() {}

  /**
   * Initializes the ROS 2 runtime environment.
   * 
   * <p>This method triggers the {@link RosLoader} to map native libraries based on the 
   * provided configuration and initializes the global ROS context. If the runtime is 
   * already initialized, subsequent calls will have no effect.</p>
   * 
   * @param args Command-line arguments passed to the ROS 2 initialization logic.
   * @param cfg  The configuration object specifying library paths and environment settings.
   * @throws NullPointerException if {@code cfg} is null.
   * @throws RuntimeException if native library loading or rcl_init fails.
   */
  public static synchronized void initialize(String[] args, RosConfig cfg) {
    Objects.requireNonNull(cfg, "RosConfig must not be null");
    if (INITIALIZED.get()) return;
    RosLoader.load(cfg);
    context = RosContext.get();
    INITIALIZED.set(true);
  }

  /**
   * Shuts down the ROS 2 runtime and releases native resources.
   * 
   * <p>Signals the middleware to stop all communications and invalidates the 
   * global context. This should be called to ensure that the process terminates 
   * gracefully and all shared memory or network handles are closed.</p>
   */
  public static synchronized void shutdown() {
    if (!INITIALIZED.get()) return;

    RosContext.shutdown();

    context = null;
    INITIALIZED.set(false);
  }

  /**
   * Checks whether the ROS 2 runtime is currently initialized and active.
   * 
   * @return {@code true} if the runtime is initialized, {@code false} otherwise.
   */
  public static boolean isInitialized() {
    return INITIALIZED.get();
  }

  /**
   * Retrieves the global ROS context handle.
   * 
   * <p>The context handle is a pointer to the native {@code rcl_context_t} struct 
   * required for the creation of most ROS 2 entities.</p>
   * 
   * @return A {@link MemorySegment} representing the pointer to the global context.
   * @throws IllegalStateException if called before {@link #initialize(String[], RosConfig)}.
   */
  public static MemorySegment getContext() {
    if (!INITIALIZED.get() || context == null) {
      throw new IllegalStateException("RosRuntime is not initialized. Call RosRuntime.initialize(...) first.");
    }
    return context;
  }
}
