package com.workerrobotics.rcljava.core;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.function.Consumer;
import java.util.function.Function;

import org.ros2.rcl.RclLib;

import com.workerrobotics.rcljava.core.callbackgroup.CallbackGroup;
import com.workerrobotics.rcljava.core.service.ServiceCallback;
import com.workerrobotics.rcljava.core.service.ServiceType;
import com.workerrobotics.rcljava.ffi.NativeChecks;
import com.workerrobotics.rcljava.loader.RosLoader;

/**
 * A Java wrapper for the ROS 2 {@code rcl_service_t} structure.
 * 
 * <p>The {@code Service} class implements the server-side of the ROS 2 Request-Response 
 * communication pattern. It listens for incoming requests from clients, processes them 
 * via a callback, and sends back the corresponding response.</p>
 * 
 * <p>This implementation leverages the <b>Foreign Function & Memory (FFM) API</b> to 
 * perform dynamic symbol lookups for service type supports and to communicate with 
 * the native {@code librcl} library.</p>
 * 
 * @param <T_Req> The type of the service request.
 * @param <T_Res> The type of the service response.
 */
public class Service<T_Req, T_Res> implements AutoCloseable {
    private final MemorySegment handle;
    private final Node node;
    private final String serviceName;
    private final ServiceCallback callback;
    private final ServiceType<T_Req, T_Res> serviceDefinition;
    private final CallbackGroup callbackgroup;

    /**
     * Initializes a new ROS 2 service server.
     * 
     * <p>Allocates a native service handle and performs a dynamic lookup of the 
     * service's type support based on the provided service definition.</p>
     * 
     * @param node The parent node managing this service's lifecycle.
     * @param serviceName The name of the service (e.g., "add_two_ints").
     * @param serviceDefinition An object defining the request/response classes and identifiers.
     * @param callback The function executed when a request is received.
     * @param qosProfile The Quality of Service profile, or {@code null} for defaults.
     * @param callbackGroup The name of the callback group this service belongs to.
     * @throws com.workerrobotics.rcljava.ffi.RclException If initialization fails at the native level.
     */
    public Service(Node node, String serviceName, ServiceType<T_Req, T_Res> serviceDefinition, ServiceCallback callback, CallbackGroup callbackGroup) {
        this.node = node;
        this.serviceName = serviceName;
        this.serviceDefinition = serviceDefinition;
        this.callback = callback;
        this.callbackgroup = callbackGroup;

        // Haal Service Type Support op (bijv. example_interfaces/srv/SetBool)
        MemorySegment srvTypeSupport = getServiceTypeSupport(serviceDefinition.getRequestClass()); // Zie helper onder

        // serviceType = std_srvs__srv__SetBool_Request.class

        // StdSrvs_Lib.rosidl_typesupport_c__get_service_type_support_handle__std_srvs__srv__SetBool();
        // StdSrvs_Lib.rosidl_typesupport_c__get_service_type_support_handle__std_srvs__srv__Trigger();

        this.handle = RclLib.rcl_get_zero_initialized_service(node.nodeArena);
        MemorySegment options = RclLib.rcl_service_get_default_options(node.nodeArena);

        try (Arena temp = Arena.ofConfined()) {
            MemorySegment cName = temp.allocateFrom(serviceName);
            int rc = RclLib.rcl_service_init(handle, node.nodeHandle, srvTypeSupport, cName, options);
            NativeChecks.requireOk(rc, "rcl_service_init");
        }
    }

    /** @return The raw {@link MemorySegment} pointing to the native {@code rcl_service_t}. */
    public MemorySegment handle() { return handle; }

    /** @return The {@link ServiceCallback} triggered upon receiving a request. */
    public ServiceCallback callback() { return callback; }

    /** @return The {@link ServiceType} definition associated with this service. */
    public ServiceType<T_Req, T_Res> serviceDefinition() { return serviceDefinition; }

    /** @return The name of this service. */
    public String serviceName() { return serviceName; }

    /** @return The {@link CallbackGroup} applied to this service */
    public CallbackGroup callbackGroup() { return callbackgroup; }

    /**
     * Finalizes the native service and releases its handle.
     * 
     * <p>Invokes {@code rcl_service_fini} to ensure the middleware correctly 
     * stops listening for requests and cleans up internal resources.</p>
     */
    @Override
    public void close() {
        RclLib.rcl_service_fini(handle, node.nodeHandle);
    }

    /**
     * Dynamically retrieves the type support handle for the service.
     * 
     * <p>Constructs a native symbol name from the request class name (removing 
     * the {@code _Request} suffix) to locate the service's type support function.</p>
     * 
     * @param serviceType The request class used to derive the service base name.
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
     * @throws RuntimeException If the native function call fails.
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