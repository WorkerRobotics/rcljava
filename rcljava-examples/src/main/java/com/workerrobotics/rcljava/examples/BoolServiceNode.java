package com.workerrobotics.rcljava.examples;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SegmentAllocator;

import org.ros2.rcl.msgs.rosidl_runtime_c__String;
import org.ros2.rcl.srv.std_srvs__srv__SetBool_Request;
import org.ros2.rcl.srv.std_srvs__srv__SetBool_Response;

import com.workerrobotics.rcljava.core.Node;
import com.workerrobotics.rcljava.core.Service;
import com.workerrobotics.rcljava.core.service.GenericServiceType;

public class BoolServiceNode extends Node {

    public BoolServiceNode() {
        super("service_set_bool_example");

        var serviceDefinition = new GenericServiceType<>(std_srvs__srv__SetBool_Request.class, std_srvs__srv__SetBool_Response.class);

        Service<?,?> service = this.createService(serviceDefinition.getServiceIdentifier(), serviceDefinition, (request, allocator) -> {
                System.out.println("Service started!");

                // Gebruik de allocator van de Executor! (Niet een eigen try-with-resources)
                MemorySegment response = std_srvs__srv__SetBool_Response.allocate(allocator);

                std_srvs__srv__SetBool_Response.success(response, true);
                
                // Gebruik dezelfde allocator voor de string
                MemorySegment msg = createRosString(allocator, "Succes!");
                std_srvs__srv__SetBool_Response.message(response, msg);

                return response; // Dit segment blijft geldig tot de executor klaar is

        });
    }

    public MemorySegment createRosString(SegmentAllocator allocator, String value) {
        // Allokeer de ROS String struct
        
        MemorySegment rosString = rosidl_runtime_c__String.allocate(allocator);
        
        // Allokeer de eigenlijke tekst in het geheugen
        MemorySegment cStr = allocator.allocateFrom(value);
        
        // Vul de velden van de ROS String struct (data, size, capacity)
        rosidl_runtime_c__String.data(rosString, cStr);
        rosidl_runtime_c__String.size(rosString, value.length());
        rosidl_runtime_c__String.capacity(rosString, value.length() + 1);
        
        return rosString;
    }


}
