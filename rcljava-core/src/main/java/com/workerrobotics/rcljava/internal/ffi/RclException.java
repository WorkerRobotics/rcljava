package com.workerrobotics.rcljava.internal.ffi;

public final class RclException extends RuntimeException {
  private final int returncode;

  public RclException(String message, int returncode) {
    super(message);
    this.returncode = returncode;
  }

  public int rc() {
    return returncode;
  }
}
