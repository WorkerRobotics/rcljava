package com.workerrobotics.rcljava.ffi;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;

import org.ros2.rcl.RclLib;

/**
 * Native façade used by rcljava-core.
 *
 * Wire these methods to your jextract-generated bindings (Panama).
 * Keep this class small and focused; do not put high-level API decisions here.
 */
public final class RclNative {
  private RclNative() {}

  /**
   * Lightweight smoke call to verify:
   * - the bindings artifact is on the classpath
   * - the generated RclLib entrypoint is resolvable
   * - Panama native access is enabled for the running JVM
   *
   * This does NOT initialize ROS; it just invokes a trivial rcl function that returns a "zero" struct.
   */
  public static void smokeCall() {
    Bindings.assertPresent();
    try (Arena arena = Arena.ofConfined()) {
      MemorySegment node = RclLibFacade.rclGetZeroInitializedNode(arena);
      if (node == null) {
        throw new IllegalStateException("smokeCall returned null MemorySegment");
      }
    }
  }

  
  public static void rclInit(String[] args) {
    //Deprecated
  }

  public static void rclShutdown() {
    Bindings.assertPresent();
    RclLib.rcl_shutdown(RclLib.rcutils_get_default_allocator(Arena.ofAuto()));
  }
}
