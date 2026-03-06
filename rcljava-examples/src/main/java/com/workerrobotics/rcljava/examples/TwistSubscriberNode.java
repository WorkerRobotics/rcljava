package com.workerrobotics.rcljava.examples;

import java.lang.foreign.MemorySegment;
import java.util.concurrent.CountDownLatch;

import org.ros2.rcl.msgs.geometry_msgs__msg__Twist;
import org.ros2.rcl.msgs.geometry_msgs__msg__Vector3;

import com.workerrobotics.rcljava.core.Node;
import com.workerrobotics.rcljava.core.QoS;
import com.workerrobotics.rcljava.core.Subscription;
import com.workerrobotics.rcljava.core.events.EventType;

public class TwistSubscriberNode extends Node {
    public final CountDownLatch messageReceived = new CountDownLatch(1);
    public double lastX = 0;

    public TwistSubscriberNode() {
        super("twist_subscriber_example");

        // We registreren de subscription met MemorySegment als type
        Subscription<?> sub = this.createSubscription("/cmd_vel", geometry_msgs__msg__Twist.class, (msgSegment) -> {

            // // Gebruik de jextract bindings om segmenten te bereiken:
            MemorySegment linear = geometry_msgs__msg__Twist.linear(msgSegment);
            MemorySegment angular = geometry_msgs__msg__Twist.angular(msgSegment);

            // Lees de waarden direct uit de C-struct layout
            double x = geometry_msgs__msg__Vector3.x(linear);
            double z = geometry_msgs__msg__Vector3.z(angular);

            this.lastX = x;

            System.out.printf("[TEST] Raw C-Struct Data -> X: %.2f, Z: %.2f%n", x, z);
            messageReceived.countDown();
        });

        sub.addEventHandler(EventType.RCL_SUBSCRIPTION_MATCHED, this::printOnMatch);
    }

    private void printOnMatch(MemorySegment segment) {
        System.out.printf("SUBSCRIPTION MATCHED, WOEHOEE!%n");
        QoS qos = new QoS();
        qos.retrieveFromStatus(segment);
        System.out.printf("Segment Policy: %s%n", qos.getLastPolicyName());
    }
}
