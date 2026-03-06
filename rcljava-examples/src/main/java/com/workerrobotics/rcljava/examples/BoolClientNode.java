package com.workerrobotics.rcljava.examples;

import com.workerrobotics.rcljava.core.Client;
import com.workerrobotics.rcljava.core.Node;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;

import org.ros2.rcl.srv.std_srvs__srv__SetBool_Request;
import org.ros2.rcl.srv.std_srvs__srv__SetBool_Response;

import com.workerrobotics.rcljava.core.service.GenericServiceType;

public class BoolClientNode extends Node {
    private final Client<?,?> client;

    public BoolClientNode() {
        super("example_bool_client");
        
        var serviceDefinition = new GenericServiceType<>(std_srvs__srv__SetBool_Request.class, std_srvs__srv__SetBool_Response.class);

        // Maak een client aan voor de service
        Client<std_srvs__srv__SetBool_Request, std_srvs__srv__SetBool_Response> client = this.createClient(
            serviceDefinition.getServiceIdentifier(), serviceDefinition,
            (msgSegment) -> {
                System.out.println("Client callback");
            }
        );
        
        this.client = client;
        
        // Start de executor voor de client in een aparte thread
        // Executor executor = new Executor();
        // Thread clientThread = Thread.ofVirtual().start(() -> executor.spin(client));
    }

    public void publishBool(boolean setTurtle) {
        // Gebruik een confined arena voor dit specifieke bericht (stack-like allocation)
        try (Arena msgArena = Arena.ofConfined()) {
            // 1. Allokeer geheugen op basis van de gegenereerde layout
            MemorySegment msg = msgArena.allocate(std_srvs__srv__SetBool_Request.layout());
            std_srvs__srv__SetBool_Request.data(msg, setTurtle);

            // 4. Publiceer het volledige msg segment
            // this.publisher.publish(msg);
            this.client.sendRequest(msg).thenAccept(response -> {
                // Deze code draait in de callbackExecutor (Virtual Thread)
                // zodra de Executor de response heeft 'gedispatched'.
                
                boolean success = std_srvs__srv__SetBool_Response.success(response);
                MemorySegment messageStruct = std_srvs__srv__SetBool_Response.message(response);
                String message = rosStringToJava(messageStruct);

                System.out.println("Antwoord ontvangen: " + message);
                System.out.println("Antwoord inhoud is: " + String.valueOf(success));
            }).exceptionally(exception -> {
                exception.printStackTrace(); // DIT laat zien waarom de callback stopt
                return null;
            });
        }
    }

    private String rosStringToJava(MemorySegment rosStringSegment) {
        // 1. Haal de layout op van de ROS String struct
        var layout = org.ros2.rcl.msgs.rosidl_runtime_c__String.layout();
        
        // 2. Vind dynamisch de offsets voor 'data' en 'size'
        // Dit werkt op basis van de namen in de C-struct
        long dataOffset = layout.byteOffset(java.lang.foreign.MemoryLayout.PathElement.groupElement("data"));
        long sizeOffset = layout.byteOffset(java.lang.foreign.MemoryLayout.PathElement.groupElement("size"));

        // 3. Lees de waarden uit het segment met de juiste offsets
        MemorySegment dataPtr = rosStringSegment.get(java.lang.foreign.ValueLayout.ADDRESS, dataOffset);
        long size = rosStringSegment.get(java.lang.foreign.ValueLayout.JAVA_LONG, sizeOffset);

        if (dataPtr.equals(MemorySegment.NULL) || size <= 0) {
            return "";
        }

        // 4. Converteer naar Java String zonder null-terminator scan
        byte[] bytes = dataPtr.reinterpret(size).toArray(java.lang.foreign.ValueLayout.JAVA_BYTE);
        return new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
    }
}
