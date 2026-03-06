package com.workerrobotics.rcljava.core.service;

/**
 * A generic implementation of the {@link ServiceType} interface.
 * 
 * <p>This class provides a flexible way to define ROS 2 service pairs (Request and Response) 
 * without requiring a specialized implementation for every service type. It automatically 
 * derives the native service identifier from the Java class name of the request.</p>
 * 
 * <p>Usage example:
 * <pre>{@code
 * var setBoolSrv = new GenericServiceType<>(SetBool_Request.class, SetBool_Response.class);
 * }</pre>
 * </p>
 * 
 * @param <T_Req> The Java class type representing the service request.
 * @param <T_Res> The Java class type representing the service response.
 */
public class GenericServiceType<T_Req, T_Res> implements ServiceType<T_Req, T_Res> {
    private final Class<T_Req> req;
    private final Class<T_Res> res;

    /**
     * Constructs a new GenericServiceType for the specified request and response classes.
     * 
     * @param req The {@link Class} object for the service request.
     * @param res The {@link Class} object for the service response.
     */
    public GenericServiceType(Class<T_Req> req, Class<T_Res> res) {
        this.req = req;
        this.res = res;
    }

     /**
     * Returns the Java class type of the service request.
     * 
     * @return The {@link Class} of the request message.
     */
    @Override public Class<T_Req> getRequestClass() { return req; }

    /**
     * Returns the Java class type of the service response.
     * 
     * @return The {@link Class} of the response message.
     */
    @Override public Class<T_Res> getResponseClass() { return res; }
    
    /**
     * Derives the base identifier for the ROS 2 service from the request class name.
     * 
     * <p>This method strips the {@code _Request} suffix from the simple name of the 
     * request class. For example, {@code std_srvs__srv__SetBool_Request} results 
     * in the identifier {@code std_srvs__srv__SetBool}.</p>
     * 
     * @return The derived string identifier used for native symbol lookups.
     */
    @Override 
    public String getServiceIdentifier() {
        // Haalt bijvoorbeeld "SetBool" uit "std_srvs__srv__SetBool_Request"
        return req.getSimpleName().replace("_Request", "");
    }
}

