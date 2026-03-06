package com.workerrobotics.rcljava.core.clock;

/**
 * Defines the clock types available in the ROS 2 Client Library (rcl).
 * 
 * <p>This enum is a Java representation of the {@code rcl_clock_type_t} C-enum, 
 * specifying the source and behavior of time reporting.</p>
 * 
 * @see <a href="https://docs.ros2.org">RCL Time API Documentation</a>
 */
public enum ClockType {
    /**
     * Reports time from a ROS-specific source, often simulated time.
     * 
     * <p>If the {@code use_sim_time} parameter is set to true, this clock will report 
     * time from the {@code /clock} topic (e.g., in Gazebo or ROS bags). If false, 
     * it typically defaults to the behavior of {@code RCL_SYSTEM_TIME}.</p>
     */
    RCL_ROS_TIME,
    /**
     * Reports absolute wall clock time.
     * 
     * <p>This clock reports the time since the Unix Epoch (January 1, 1970). 
     * Note that this time can jump forward or backward due to system clock 
     * adjustments or NTP (Network Time Protocol) synchronizations.</p>
     */
    RCL_SYSTEM_TIME,
     /**
     * Reports monotonic time that never jumps backwards.
     * 
     * <p>This clock measures time since an unspecified point in the past. 
     * It is not affected by system clock changes, making it the recommended 
     * choice for timers, control loops, and measuring durations.</p>
     */
    RCL_STEADY_TIME
}
