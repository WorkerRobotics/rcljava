package com.workerrobotics.rcljava.ffi;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.SegmentAllocator;
import java.lang.reflect.Method;

/**
 * Reflective facade around the generated RclLib entrypoint.
 *
 * This lets rcljava-core stay stable even if the generated package name changes.
 * Once your bindings stabilize, you can replace reflection with direct calls for performance.
 */
public final class RclLibFacade {
  private static volatile Method zeroInitNodeMethod;

  private RclLibFacade() {}

  /**
   * Calls: RclLib.rcl_get_zero_initialized_node(SegmentAllocator)
   *
   * NOTE: Your message used a spelling close to "rcl_get_zero_initalized_node".
   * This method tries both spellings to be tolerant.
   */
  public static MemorySegment rclGetZeroInitializedNode(SegmentAllocator allocator) {
    try {
      Method m = zeroInitNodeMethod;
      if (m == null) {
        m = resolveZeroInitNodeMethod();
        zeroInitNodeMethod = m;
      }
      Object result = m.invoke(null, allocator);
      return (MemorySegment) result;
    } catch (ReflectiveOperationException e) {
      throw new IllegalStateException("Failed invoking RclLib.rcl_get_zero_initialized_node(...)", e);
    }
  }

  private static Method resolveZeroInitNodeMethod() throws NoSuchMethodException {
    Class<?> cls = Bindings.rclLibClass();

    // Try the most likely correct spelling first
    try {
      return cls.getMethod("rcl_get_zero_initialized_node", SegmentAllocator.class);
    } catch (NoSuchMethodException ignored) {
      // Try common typo spelling (as in the user's note)
      return cls.getMethod("rcl_get_zero_initalized_node", SegmentAllocator.class);
    }
  }
}
