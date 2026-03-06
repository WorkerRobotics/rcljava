package com.workerrobotics.rcljava.core.events;

/**
 * Defines the types of middleware events that can be monitored for Publishers and Subscriptions.
 * 
 * <p>This enum represents various Quality of Service (QoS) and status events 
 * reported by the underlying RMW implementation. These events allow nodes to 
 * react to communication issues such as incompatible settings, missed deadlines, 
 * or lost messages.</p>
 * 
 * @see <a href="https://docs.ros2.org">RCL Event API Documentation</a>
 */
public enum EventType {
     /** 
     * Triggered when a subscription's requested deadline has been missed. 
     */
     RCL_SUBSCRIPTION_REQUESTED_DEADLINE_MISSED,

     /** 
     * Triggered when the liveliness status of a subscription's publisher has changed. 
     */
     RCL_SUBSCRIPTION_LIVELINESS_CHANGED,

     /** 
     * Triggered when a subscription's requested QoS profile is incompatible with a publisher. 
     */
     RCL_SUBSCRIPTION_REQUESTED_INCOMPATIBLE_QOS,

     /** 
     * Triggered when one or more messages were lost by the subscription. 
     */
     RCL_SUBSCRIPTION_MESSAGE_LOST,

     /** 
     * Triggered when a subscription's message type is incompatible with the publisher's type. 
     */
     RCL_SUBSCRIPTION_INCOMPATIBLE_TYPE,

     /** 
     * Triggered when a subscription has successfully matched with a new publisher. 
     */
     RCL_SUBSCRIPTION_MATCHED,

     /** 
     * Triggered when a publisher's offered deadline has been missed. 
     */
     RCL_PUBLISHER_OFFERED_DEADLINE_MISSED,

     /** 
     * Triggered when a publisher's liveliness is no longer being maintained. 
     */
     RCL_PUBLISHER_LIVELINESS_LOST,

     /** 
     * Triggered when a publisher's offered QoS profile is incompatible with a subscription. 
     */
     RCL_PUBLISHER_OFFERED_INCOMPATIBLE_QOS,

     /** 
     * Triggered when a publisher's message type is incompatible with a subscription's type. 
     */
     RCL_PUBLISHER_INCOMPATIBLE_TYPE,

     /** 
     * Triggered when a publisher has successfully matched with a new subscription. 
     */
     RCL_PUBLISHER_MATCHED
}
