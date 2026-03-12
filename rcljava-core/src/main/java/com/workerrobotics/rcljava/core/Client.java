package com.workerrobotics.rcljava.core;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import org.ros2.rcl.RclLib;
import org.ros2.rcl.rmw_request_id_t;

import com.workerrobotics.rcljava.core.callbackgroup.CallbackGroup;
import com.workerrobotics.rcljava.core.service.ServiceType;
import com.workerrobotics.rcljava.ffi.NativeChecks;
import com.workerrobotics.rcljava.loader.RosLoader;

import static org.ros2.rcl.RclLib.*;

/**
 * A Java wrapper for the ROS 2 {@code rcl_client_t} structure.
 * 
 * <p>The {@code Client} class enables a ROS 2 node to send requests to a remote service 
 * and receive responses asynchronously. It uses a sequence-number-based tracking system 
 * to match incoming responses with their original requests via {@link CompletableFuture}.</p>
 * 
 * <p>Implementation details: This class utilizes the <b>Foreign Function & Memory (FFM) API</b> 
 * to perform dynamic symbol lookups for service type support and to communicate with 
 * the native {@code librcl} client functions.</p>
 * 
 * @param <T_Req> The service request type.
 * @param <T_Res> The service response type.
 */
public class Client<T_Req, T_Res> implements AutoCloseable {
    private final MemorySegment handle;
    private final Node node;
    private final ServiceType<T_Req, T_Res> serviceType;
    private final CallbackGroup callbackGroup;
    
    /** 
     * A thread-safe map associating native sequence numbers with Java futures. 
     * The {@code Executor} uses this map to fulfill futures when a response arrives.
     */
    protected final Map<Long, CompletableFuture<MemorySegment>> pendingRequests = new ConcurrentHashMap<>();

    /**
     * Initializes a new ROS 2 service client.
     * 
     * <p>Allocates a native client handle and initializes it with the provided service 
     * name and type support. The initialization uses the node's shared memory arena 
     * and default middleware options.</p>
     * 
     * @param node The parent node managing this client's lifecycle.
     * @param serviceName The name of the service to call (e.g., "add_two_ints").
     * @param serviceType An object defining the request/response classes and identifiers.
     * @param qosProfile The Quality of Service profile, or {@code null} for defaults.
     * @param callback Optional consumer for manual response handling (can be {@code null}).
     * @throws com.workerrobotics.rcljava.ffi.RclException If native initialization fails.
     */
    public Client(Node node, String serviceName, ServiceType<T_Req, T_Res> serviceType, CallbackGroup callbackGroup) {
        this.node = node;
        this.serviceType = serviceType;
        this.callbackGroup = callbackGroup;

        MemorySegment srvTypeSupport = getServiceTypeSupport(serviceType.getRequestClass());
        this.handle = rcl_get_zero_initialized_client(node.nodeArena);
        MemorySegment options = rcl_client_get_default_options(node.nodeArena);

        try (Arena temp = Arena.ofConfined()) {
            MemorySegment cName = temp.allocateFrom(serviceName);
            int rc = rcl_client_init(handle, node.nodeHandle, srvTypeSupport, cName, options);
            NativeChecks.requireOk(rc, "rcl_client_init");
        }
        
    }

    /**
     * Sends an asynchronous request to the service server.
     * 
     * <p>This method invokes {@code rcl_send_request} and registers the resulting 
     * sequence number. It returns a {@link CompletableFuture} that will be completed 
     * by the {@code Executor} once the service response is received.</p>
     * 
     * @param request A {@link MemorySegment} containing the serialized request data.
     * @return A future that will contain the response data as a {@link MemorySegment}.
     * @throws com.workerrobotics.rcljava.ffi.RclException If the native send operation fails.
     */
    public CompletableFuture<MemorySegment> sendRequest(MemorySegment request) {
        CompletableFuture<MemorySegment> future = new CompletableFuture<>();
        
        try (Arena temp = Arena.ofConfined()) {
            MemorySegment seqNumPtr = temp.allocate(java.lang.foreign.ValueLayout.JAVA_LONG);

            int rc = rcl_send_request(handle, request, seqNumPtr);
            NativeChecks.requireOk(rc, "rcl_send_request");

            long sequenceNumber = seqNumPtr.get(java.lang.foreign.ValueLayout.JAVA_LONG, 0);
            System.out.println("[Client] Sending request with SeqNum: " + sequenceNumber);
            pendingRequests.put(sequenceNumber, future);
        }
        
        return future;
    }

    /** @return The raw {@link MemorySegment} pointing to the native {@code rcl_client_t}. */
    public MemorySegment handle() { return handle; }

    /** @return The {@link ServiceType} definition associated with this client. */
    public ServiceType<T_Req, T_Res> serviceType() { return serviceType; }

    /** @return The {@link CallbackGroup} applied to this client */
    public CallbackGroup callbackGroup() { return this.callbackGroup;}

    /**
     * Finalizes the native client and releases its handle.
     * 
     * <p>Invokes {@code rcl_client_fini} to ensure the middleware stops tracking 
     * this client and releases internal communication resources.</p>
     */
    @Override
    public void close() {
        rcl_client_fini(handle, node.nodeHandle);
    }

    /**
     * Dynamically retrieves the service type support handle.
     * 
     * <p>Constructs a native symbol name from the request class name to locate 
     * the service's type support function in the loaded libraries.</p>
     * 
     * @param serviceType The request class used to derive the service identifier.
     * @return A {@link MemorySegment} pointing to the service type support handle.
     */
    private MemorySegment getServiceTypeSupport(Class<?> serviceType) {
        String className = serviceType.getSimpleName();
        String serviceBaseName = className.replace("_Request", "");
        
        String symbolName = "rosidl_typesupport_c__get_service_type_support_handle__" + serviceBaseName;

        MemorySegment functionAddress = RosLoader.findSymbol(symbolName);
        return callHandleFunction(functionAddress);
    }

    /**
     * Invokes the native type support function using the FFM Linker.
     * 
     * @param address The memory address of the C function to invoke.
     * @return The returned type support handle address.
     */
    private MemorySegment callHandleFunction(MemorySegment address) {
        java.lang.foreign.Linker linker = java.lang.foreign.Linker.nativeLinker();
        java.lang.foreign.FunctionDescriptor desc = java.lang.foreign.FunctionDescriptor.of(java.lang.foreign.AddressLayout.ADDRESS);
        try {
            java.lang.invoke.MethodHandle handle = linker.downcallHandle(address, desc);
            return (MemorySegment) handle.invokeExact();
        } catch (Throwable t) {
            throw new RuntimeException("Failed to invoke typesupport handle function", t);
        }
    }
}

