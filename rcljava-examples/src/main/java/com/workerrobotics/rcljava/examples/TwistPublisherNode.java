package com.workerrobotics.rcljava.examples;

import com.workerrobotics.rcljava.core.Node;
import com.workerrobotics.rcljava.core.Publisher;
import org.ros2.rcl.RclLib;
import org.ros2.rcl.msgs.GEOMETRY_MSGS_Lib;
import org.ros2.rcl.msgs.geometry_msgs__msg__Twist;

import static org.ros2.rcl.RclLib.rosidl_get_dynamic_message_type_support_type_description_function;
import static org.ros2.rcl.RclLib.rosidl_typesupport_c__get_message_type_support_handle__type_description_interfaces__srv__GetTypeDescription_Event;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;

public class TwistPublisherNode extends Node {

    private final Publisher<geometry_msgs__msg__Twist> publisher;

    public TwistPublisherNode() {
        super("twist_publisher_example");

        // 1. Haal de Type Support op voor geometry_msgs/msg/Twist
        // Deze methode komt uit je gegenereerde bindings (meestal in een TypeSupport of Msg class)
        MemorySegment twistTypeSupport = GEOMETRY_MSGS_Lib.rosidl_typesupport_c__get_message_type_support_handle__geometry_msgs__msg__Twist();        
        
        // 2. Maak de publisher aan op topic "cmd_vel"
        this.publisher = createPublisher("cmd_vel", geometry_msgs__msg__Twist.class, twistTypeSupport);
    }

    public void publishVelocity(double linearX, double angularZ) {
        // Gebruik een confined arena voor dit specifieke bericht (stack-like allocation)
        try (Arena msgArena = Arena.ofConfined()) {
            // 1. Allokeer geheugen op basis van de gegenereerde layout
            MemorySegment msg = msgArena.allocate(geometry_msgs__msg__Twist.layout());

            // 2. Verkrijg de segmenten voor de geneste structs (Vector3)
            // jextract genereert 'linear$get' om het sub-segment te bereiken
            MemorySegment linearSegment = geometry_msgs__msg__Twist.linear(msg);
            MemorySegment angularSegment = geometry_msgs__msg__Twist.angular(msg);

            // 3. Vul de waarden in de Vector3 structs
            // Gebruik de static 'x$set', 'y$set', etc. van de Vector3 binding 
            // (Vermoedelijk geometry_msgs__msg__Vector3)
            org.ros2.rcl.msgs.geometry_msgs__msg__Vector3.x(linearSegment, linearX);
            org.ros2.rcl.msgs.geometry_msgs__msg__Vector3.y(linearSegment, 0.0);
            org.ros2.rcl.msgs.geometry_msgs__msg__Vector3.z(linearSegment, 0.0);

            org.ros2.rcl.msgs.geometry_msgs__msg__Vector3.x(angularSegment, 0.0);
            org.ros2.rcl.msgs.geometry_msgs__msg__Vector3.y(angularSegment, 0.0);
            org.ros2.rcl.msgs.geometry_msgs__msg__Vector3.z(angularSegment, angularZ);

            // 4. Publiceer het volledige msg segment
            this.publisher.publish(msg);
        }
    }
}
