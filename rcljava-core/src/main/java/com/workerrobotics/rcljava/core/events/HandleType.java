package com.workerrobotics.rcljava.core.events;

/**
 * Specifies the type of ROS 2 communication handle being referenced.
 * 
 * <p>This enum is used as a discriminator to determine whether an event (such as 
 * QoS incompatibility or message loss) should be attached to a {@code rcl_publisher_t} 
 * or a {@code rcl_subscription_t} handle.</p>
 * 
 * <p>Used internally by {@link com.workerrobotics.rcljava.core.Node#addEventHandler} 
 * to invoke the correct native initialization function ({@code rcl_publisher_event_init} 
 * vs {@code rcl_subscription_event_init}).</p>
 */
public enum HandleType {
    /** 
     * Indicates the handle refers to a ROS 2 Publisher entity. 
     */
    PUBLISHER,

    /** 
     * Indicates the handle refers to a ROS 2 Subscription entity. 
     */
    SUBSCRIPTION
}
