package com.workerrobotics.rcljava.core;

import com.workerrobotics.rcljava.ffi.RclException;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;

import org.ros2.rcl.RclLib;
import org.ros2.rcl.rcl_clock_t;

import com.workerrobotics.rcljava.core.clock.ClockType;
import com.workerrobotics.rcljava.ffi.NativeChecks;
import com.workerrobotics.rcljava.ffi.RclException;

/**
 * A Java wrapper for the ROS 2 {@code rcl_clock_t} structure.
 * 
 * <p>This class provides access to the ROS 2 time sources, allowing nodes and 
 * other entities to retrieve timestamps or manage timers based on system, 
 * steady, or simulated time.</p>
 * 
 * <p>The implementation uses Java's <b>Foreign Function & Memory API (FFM)</b> 
 * to interface directly with the underlying {@code librcl} library.</p>
 */
public class Clock implements AutoCloseable {
    private final MemorySegment handle;
    private final Arena clockArena;
    private final ClockType clockType;

    /**
     * Initializes a new ROS 2 clock of the specified type.
     * 
     * <p>Allocates a zero-initialized {@code rcl_clock_t} struct and binds it to 
     * a shared memory arena. The clock is initialized using the default 
     * {@code rcutils} allocator.</p>
     * 
     * @param clockType The type of clock to initialize (e.g., ROS, System, or Steady time).
     * @throws RclException If the clock initialization fails at the native level.
     */
    public Clock(ClockType clockType) {
        this.clockArena = Arena.ofShared();
        this.clockType = clockType;
        this.handle = clockArena.allocate(rcl_clock_t.layout());

        int rc = RclLib.rcl_clock_init(
            getRosClockType(clockType), 
            handle, 
            RclLib.rcutils_get_default_allocator(clockArena)
        );
        NativeChecks.requireOk(rc, "rcl_clock_init");
    }

    /**
     * Retrieves the current time from the clock source.
     * 
     * <p>The returned value represents the number of nanoseconds elapsed since 
     * the clock's epoch. The specific meaning of the epoch depends on the 
     * {@link ClockType} used during initialization.</p>
     * 
     * @return The current time in nanoseconds.
     * @throws RclException If the native {@code rcl_clock_get_now} call fails.
     */
    public long now() {
        try (Arena temp = Arena.ofConfined()) {
            MemorySegment timePoint = temp.allocate(java.lang.foreign.ValueLayout.JAVA_LONG);
            int rc = RclLib.rcl_clock_get_now(handle, timePoint);
            NativeChecks.requireOk(rc, "rcl_clock_get_now");
            return timePoint.get(java.lang.foreign.ValueLayout.JAVA_LONG, 0);
        }
    }

    /**
     * Returns the raw memory handle pointing to the native {@code rcl_clock_t} structure.
     * 
     * <p>This handle is intended for internal use when passing the clock to 
     * other ROS 2 entities such as Timers or Nodes.</p>
     * 
     * @return A {@link MemorySegment} representing the native pointer to the clock.
     */
    public MemorySegment handle() {
        return handle;
    }

    /**
     * Finalizes the native clock and releases the associated memory arena.
     * 
     * <p>Once closed, the clock handle becomes invalid and any further 
     * attempts to retrieve time will result in an exception or undefined behavior.</p>
     */
    @Override
    public void close() {
        RclLib.rcl_clock_fini(handle);
        clockArena.close();
    }

    /**
     * Maps the Java {@link ClockType} enum to the corresponding integer 
     * constants defined in the native {@code rcl} library.
     * 
     * @param clockType The Java enum value.
     * @return The integer constant recognized by the native C API.
     * @throws RclException If an unsupported clock type is provided.
     */
    private int getRosClockType(final ClockType clockType) {
        return switch (clockType) {
            case RCL_ROS_TIME -> RclLib.RCL_ROS_TIME();
            case RCL_SYSTEM_TIME -> RclLib.RCL_SYSTEM_TIME();
            case RCL_STEADY_TIME -> RclLib.RCL_STEADY_TIME();
            default -> throw new RclException("ClockType not supported: " + clockType, 1);
        };
    }

}
