package com.workerrobotics.rcljava.examples;

import java.lang.foreign.MemorySegment;

import org.ros2.rcl.msgs.std_srvs__srv__SetBool_Request;
import org.ros2.rcl.msgs.std_srvs__srv__SetBool_Response;

import com.workerrobotics.rcljava.core.Node;
import com.workerrobotics.rcljava.core.callbackgroup.CallbackGroup;
import com.workerrobotics.rcljava.core.callbackgroup.ReentrantCallbackGroup;
import com.workerrobotics.rcljava.core.service.GenericServiceType;

public class MyComplexNode extends Node {
    public MyComplexNode() {
        super("complex_node");

        // Deze groep staat parallelle uitvoering toe
        CallbackGroup reentrantGroup = this.createCallbackGroup(ReentrantCallbackGroup.class);
        var serviceDefinition = new GenericServiceType<>(std_srvs__srv__SetBool_Request.class, std_srvs__srv__SetBool_Response.class);

        // Service A kan nu meerdere requests tegelijk afhandelen
        this.createService("parallel_service", serviceDefinition, (req, alloc) -> {
            try {
            Thread.sleep(2000); // Blokkeert andere calls naar DEZE service NIET
            } catch (Exception e) {}
            MemorySegment response = std_srvs__srv__SetBool_Response.allocate(alloc);

            std_srvs__srv__SetBool_Response.success(response, true);
            
            // Gebruik dezelfde allocator voor de string
            MemorySegment msg = createRosString(alloc, "Succes!");
            std_srvs__srv__SetBool_Response.message(response, msg);

            return response;
        }, reentrantGroup);

        // Service B gebruikt de default MutuallyExclusiveGroup
        this.createService("sequential_service", serviceDefinition, (req, alloc) -> {
            // Als deze draait, moeten andere calls naar DEZE service wachten in de queue
            MemorySegment response = std_srvs__srv__SetBool_Response.allocate(alloc);

            std_srvs__srv__SetBool_Response.success(response, true);
            
            // Gebruik dezelfde allocator voor de string
            MemorySegment msg = createRosString(alloc, "Succes!");
            std_srvs__srv__SetBool_Response.message(response, msg);

            return response;
        });
    }
}