package com.workerrobotics.rcljava.ffi;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;

import org.ros2.rcl.RclLib;

/**
 * Common helpers for mapping native return codes to Java exceptions.
 * Extend this once you wire up rcutils/rcl error-string access.
 */
public final class NativeChecks {
  public static final int RCL_RET_OK = 0;

  private NativeChecks() {}

  public static void requireOk(int rc, String context) {
    if (rc == RCL_RET_OK) return;
    
    MemorySegment errorState = RclLib.rcutils_get_error_string(Arena.ofAuto());
    String errorMsg = errorState.getString(0);
    RclLib.rcutils_reset_error();
    
    throw new RclException(String.format("Native Failure in %s: %s (RC: %d)", context, errorMsg, rc), rc);
  }
}
