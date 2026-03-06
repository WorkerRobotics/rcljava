package com.workerrobotics.rcljava.core;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import org.ros2.rcl.rmw_qos_profile_t;
import org.ros2.rcl.rmw_time_t;
import org.ros2.rcl.rmw_requested_qos_incompatible_event_status_t;

/**
 * A Java wrapper for ROS 2 Quality of Service (QoS) profiles and event status.
 * 
 * <p>This class allows for the configuration of communication policies such as reliability, 
 * durability, and liveliness. It also provides methods to parse QoS-related events 
 * (e.g., incompatible QoS requested or offered) directly from native memory segments.</p>
 * 
 * <p>The internal state can be applied to native {@code rmw_qos_profile_t} structures 
 * using the {@link #applyTo(MemorySegment)} method.</p>
 */
public class QoS {
        // Constants for Reliability, Durability, Liveliness, and Policy Kinds...
    private long depth = 10;
    private int reliability = RELIABILITY_RELIABLE;
    private int durability = DURABILITY_VOLATILE;
    private long deadlineSeconds = 0;
    private long deadlineNanos = 0;
    private int liveliness = LIVELINESS_SYSTEM_DEFAULT;
    private long livelinessLeaseSeconds = 0;
    private long livelinessLeaseNanos = 0;

    private int totalCount;
    private int lastPolicyKind;

    // --- Reliability ---
    public static final int RELIABILITY_SYSTEM_DEFAULT = 0;
    public static final int RELIABILITY_RELIABLE = 1;
    public static final int RELIABILITY_BEST_EFFORT = 2;

    // --- Durability ---
    public static final int DURABILITY_SYSTEM_DEFAULT = 0;
    public static final int DURABILITY_TRANSIENT_LOCAL = 1;
    public static final int DURABILITY_VOLATILE = 2;

    // --- Liveliness ---
    public static final int LIVELINESS_SYSTEM_DEFAULT = 0;
    public static final int LIVELINESS_AUTOMATIC = 1;
    public static final int LIVELINESS_MANUAL_BY_TOPIC = 3;

    // --- Policy Kinds ---
    public static final int POLICY_INVALID = 0;
    public static final int POLICY_RELIABILITY = 1;
    public static final int POLICY_DURABILITY = 2;
    public static final int POLICY_DEADLINE = 3;
    public static final int POLICY_LIVELINESS = 4;

    public QoS withDepth(int depth) { this.depth = depth; return this; }
    public QoS withReliability(int r) { this.reliability = r; return this; }
    public QoS withDurability(int d) { this.durability = d; return this; }
    public QoS withDeadline(long sec, long nano) {
        this.deadlineSeconds = sec;
        this.deadlineNanos = nano;
        return this;
    }
    public QoS withLiveliness(int l, long sec, long nano) {
        this.liveliness = l;
        this.livelinessLeaseSeconds = sec;
        this.livelinessLeaseNanos = nano;
        return this;
    }

    public int getTotalCount() { return totalCount; }
    public int getLastPolicyKind() { return lastPolicyKind; }
    public long getDepth() { return depth; }
    public int getReliability() { return reliability; }
    public int getDurability() { return durability; }
    public int getLiveliness() { return liveliness; }
    public long getDeadlineSeconds() { return deadlineSeconds; }
    public long getDeadlineNanos() { return deadlineNanos; }
    public long getLivelinessLeaseSeconds() { return livelinessLeaseSeconds; }
    public long getLivelinessLeaseNanos() { return livelinessLeaseNanos; }

    /**
     * Updates the local QoS state using data from a native incompatible event status buffer.
     * 
     * <p>This method works for both {@code rmw_requested_qos_incompatible_event_status_t} 
     * and {@code rmw_offered_qos_incompatible_event_status_t}, as their binary layouts 
     * are identical in the RMW layer.</p>
     * 
     * @param eventStatus A {@link MemorySegment} pointing to the native event status struct.
     */
    public void retrieveFromStatus(MemorySegment eventStatus) {
        // We gebruiken hier de Requested variant layout (deze is binair gelijk aan Offered)
        this.totalCount = eventStatus.get(
            ValueLayout.JAVA_INT, 
            rmw_requested_qos_incompatible_event_status_t.total_count$offset()
        );
        this.lastPolicyKind = eventStatus.get(
            ValueLayout.JAVA_INT, 
            rmw_requested_qos_incompatible_event_status_t.last_policy_kind$offset()
        );
    }

    /**
     * Returns a human-readable string representation of the last incompatible policy kind.
     * 
     * @return The policy name (e.g., "RELIABILITY", "DEADLINE") or "UNKNOWN".
     */
    public String getLastPolicyName() {
        return switch (this.lastPolicyKind) {
            case POLICY_RELIABILITY -> "RELIABILITY";
            case POLICY_DURABILITY -> "DURABILITY";
            case POLICY_DEADLINE -> "DEADLINE";
            case POLICY_LIVELINESS -> "LIVELINESS";
            default -> "UNKNOWN (" + lastPolicyKind + ")";
        };
    }

    /**
     * Applies the Java QoS settings to a native options segment.
     * 
     * <p>Maps Java fields to the offsets of an {@code rmw_qos_profile_t} C structure. 
     * Complex types like {@code rmw_time_t} (for deadline and lease duration) 
     * are handled via memory slicing.</p>
     * 
     * @param optionsSegment A {@link MemorySegment} representing the native QoS profile or options.
     */
    public void applyTo(MemorySegment optionsSegment) {
        optionsSegment.set(ValueLayout.JAVA_LONG, rmw_qos_profile_t.depth$offset(), depth);
        optionsSegment.set(ValueLayout.JAVA_INT, rmw_qos_profile_t.reliability$offset(), reliability);
        optionsSegment.set(ValueLayout.JAVA_INT, rmw_qos_profile_t.durability$offset(), durability);
        optionsSegment.set(ValueLayout.JAVA_INT, rmw_qos_profile_t.liveliness$offset(), liveliness);

        setRmwTime(optionsSegment.asSlice(rmw_qos_profile_t.deadline$offset()), deadlineSeconds, deadlineNanos);
        setRmwTime(optionsSegment.asSlice(rmw_qos_profile_t.liveliness_lease_duration$offset()), livelinessLeaseSeconds, livelinessLeaseNanos);
    }

    /**
     * Internal helper to set {@code rmw_time_t} fields within a memory segment.
     * 
     * @param timeSegment The slice of memory where the time struct is located.
     * @param sec Seconds value.
     * @param nano Nanoseconds value.
     */
    private void setRmwTime(MemorySegment timeSegment, long sec, long nano) {
        timeSegment.set(ValueLayout.JAVA_LONG, rmw_time_t.sec$offset(), sec);
        timeSegment.set(ValueLayout.JAVA_LONG, rmw_time_t.nsec$offset(), nano);
    }
}