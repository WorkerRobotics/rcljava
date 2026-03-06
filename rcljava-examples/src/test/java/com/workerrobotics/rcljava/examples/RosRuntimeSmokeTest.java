package com.workerrobotics.rcljava.examples;

import com.workerrobotics.rcljava.core.Node;
import com.workerrobotics.rcljava.core.RosContext;
import com.workerrobotics.rcljava.ffi.RclNative;
import com.workerrobotics.rcljava.core.RosRuntime;
import com.workerrobotics.rcljava.loader.RosConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Skeleton smoke test.
 *
 * Today this only tests loader + lifecycle wiring of the Java side.
 * Once you wire up native rcl_init/rcl_shutdown, this becomes an integration test.
 */
public class RosRuntimeSmokeTest {

  @AfterEach
  void tearDown() {
    RosContext.shutdown();
  }

  @Test
  void canInitializeAndCreateNodePlaceholder() {
    RosConfig cfg = RosConfig.builder()
        // Intentionally do not set nativeLibraryDirs to exercise auto-detection.
        .strict(false) // skeleton: don't fail hard if libs not present on the test machine
        .build();

    RosRuntime.initialize(new String[0], cfg);

    RclNative.smokeCall();

    assertTrue(RosRuntime.isInitialized());
    try (Node node = new CustomNode("test_node")) {
      assertEquals("test_node", node.getName());
    } 
  }
}
