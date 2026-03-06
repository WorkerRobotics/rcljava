package com.workerrobotics.rcljava.core.service;

/**
 * A type-safe contract that bridges Java service message classes with ROS 2 service definitions.
 * 
 * <p>This interface is used to group the Request and Response classes of a specific 
 * ROS 2 service (SRV). It provides the necessary metadata for the {@link com.workerrobotics.rcljava.core.Service} 
 * and {@link com.workerrobotics.rcljava.core.Client} to perform dynamic symbol lookups 
 * of the native type support handles.</p>
 * 
 * @param <T_Req> The Java class type representing the service request.
 * @param <T_Res> The Java class type representing the service response.
 */
public interface ServiceType<T_Req, T_Res> {
    /**
     * Returns the Java class type of the service request.
     * 
     * @return The {@link Class} of the request message.
     */
    Class<T_Req> getRequestClass();

    /**
     * Returns the Java class type of the service response.
     * 
     * @return The {@link Class} of the response message.
     */
    Class<T_Res> getResponseClass();

    /**
     * Returns the base identifier of the ROS 2 service used for native symbol mapping.
     * 
     * <p>For example, for a service like {@code std_srvs/srv/SetBool}, this identifier 
     * is typically used to construct C function names such as 
     * {@code rosidl_typesupport_c__get_service_type_support_handle__SetBool}.</p>
     * 
     * @return The string identifier of the service.
     */
    String getServiceIdentifier();
}

