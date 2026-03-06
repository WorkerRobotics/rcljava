package com.workerrobotics.rcljava.core.service;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.SegmentAllocator;

/**
 * A functional interface for processing ROS 2 service requests.
 * 
 * <p>The {@code ServiceCallback} is triggered by the {@link com.workerrobotics.rcljava.core.Executor} 
 * when a service request is received. It defines the logic for generating a response 
 * based on the incoming request data.</p>
 * 
 * <p>Implementation note: This interface is designed for high-performance, non-blocking 
 * execution within <b>Java Virtual Threads</b>. It provides a {@link SegmentAllocator} 
 * to ensure that the response memory remains valid until the middleware has 
 * successfully sent the data back to the client.</p>
 */
@FunctionalInterface
public interface ServiceCallback {
    /**
     * Handles an incoming ROS 2 service request and returns the corresponding response.
     * 
     * <p>The implementation should read the request data from the provided 
     * {@code request} segment and allocate the response structure using the 
     * provided {@code allocator}.</p>
     * 
     * @param request A {@link MemorySegment} pointing to the native request structure 
     *                (e.g., {@code std_srvs__srv__SetBool_Request}).
     * @param allocator A {@link SegmentAllocator} (typically backed by the Executor's 
     *                  transaction arena) used to safely allocate the response segment.
     * @return A {@link MemorySegment} pointing to the populated native response structure. 
     *         The Executor will automatically send this response using {@code rcl_send_response}.
     */
    MemorySegment handle(MemorySegment request, SegmentAllocator allocator);
}
