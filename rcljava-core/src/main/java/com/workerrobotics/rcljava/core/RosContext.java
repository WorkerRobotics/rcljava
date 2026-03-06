package com.workerrobotics.rcljava.core;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;

import org.ros2.rcl.RclLib;

import com.workerrobotics.rcljava.ffi.Bindings;
import com.workerrobotics.rcljava.ffi.NativeChecks;

/**
 * Manages the global native ROS 2 context ({@code rcl_context_t}).
 * 
 * <p>This class handles the low-level initialization and shutdown of the ROS 2 
 * communication infrastructure. It maintains a single, shared {@link Arena} for 
 * global resources and provides a thread-safe singleton access to the initialized 
 * context handle.</p>
 * 
 * <p>The lifecycle includes an automatic JVM shutdown hook to ensure that 
 * {@code rcl_shutdown} is called and native memory is released when the 
 * application exits.</p>
 */
public class RosContext {
    /**
     * The global shared memory arena used for long-lived native resources, 
     * such as the context and initialization options.
     */
    public static final Arena GLOBAL_ARENA = Arena.ofShared();
    private static MemorySegment context;

    /**
     * Retrieves the global, initialized ROS 2 context handle.
     * 
     * <p>If the context has not been initialized yet, this method will:
     * <ul>
     *   <li>Verify that native bindings are loaded.</li>
     *   <li>Initialize a zero-filled {@code rcl_context_t}.</li>
     *   <li>Configure default initialization options and allocators.</li>
     *   <li>Invoke {@code rcl_init} to start the ROS 2 middleware.</li>
     *   <li>Register a JVM shutdown hook for clean resource deallocation.</li>
     * </ul>
     * </p>
     * 
     * @return A {@link MemorySegment} representing the pointer to the initialized {@code rcl_context_t}.
     * @throws com.workerrobotics.rcljava.ffi.RclException If the native {@code rcl_init} call fails.
     * @throws IllegalStateException If native bindings are missing.
     */
    public static synchronized MemorySegment get() {
        if (context == null) {
            Bindings.assertPresent();
            context = RclLib.rcl_get_zero_initialized_context(GLOBAL_ARENA);
            MemorySegment initOptions = RclLib.rcl_get_zero_initialized_init_options(GLOBAL_ARENA);
            
            MemorySegment defaultAllocator = RclLib.rcutils_get_default_allocator(GLOBAL_ARENA);
            RclLib.rcl_init_options_init(initOptions, defaultAllocator);
            
            int rc = RclLib.rcl_init(0, MemorySegment.NULL, initOptions, context);
            NativeChecks.requireOk(rc, "rcl_init");

            // Registreer een shutdown hook om netjes af te sluiten
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                RclLib.rcl_shutdown(context);
                GLOBAL_ARENA.close();
            }));
        }
        return context;
    }

    /**
     * Shuts down the global ROS 2 context.
     * 
     * <p>This method explicitly invokes {@code rcl_shutdown} if the context is valid 
     * and its memory segment is still alive. It invalidates the local context reference 
     * but defers the closing of the {@link #GLOBAL_ARENA} to the JVM shutdown hook 
     * to prevent premature memory access during application teardown.</p>
     */
    public static synchronized void shutdown() {
        // Check of de context segment zelf nog 'alive' is voor de FFM API
        if (context != null && context.scope().isAlive()) {
            // Alleen als ROS denkt dat hij nog valid is, roepen we rcl_shutdown aan
            if (RclLib.rcl_context_is_valid(context)) {
                RclLib.rcl_shutdown(context);
            }
        }
        // We roepen hier GEEN GLOBAL_ARENA.close() aan!
        // Dat laten we aan de Shutdown Hook over, of we doen het aan het einde van de suite.
        context = null;
    }
}
