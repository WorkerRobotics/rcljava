package com.workerrobotics.rcljava.loader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Resolves likely ROS 2 library directories.
 *
 * Assumptions:
 * - ROS installation is correctly configured outside the JVM.
 * - We use standard env vars when present, otherwise fall back to /opt/ros/<distro>/lib.
 */
public final class RosPathResolver {
  private RosPathResolver() {}

  public static List<Path> resolveNativeLibraryDirs(RosConfig cfg) {
    // 1) If user explicitly provided dirs, honor them.
    if (cfg != null && !cfg.nativeLibraryDirs().isEmpty()) {
      return cfg.nativeLibraryDirs();
    }

    // 2) Optional override env var (simple, deterministic).
    //    Format: path-separator separated list.
    String override = System.getenv("RCLJAVA_NATIVE_LIB_DIRS");
    if (override != null && !override.isBlank()) {
      return existingLibDirsFromPrefixes(splitPaths(override));
    }

    // 3) Use AMENT_PREFIX_PATH / COLCON_PREFIX_PATH overlays when present.
    String ament = System.getenv("AMENT_PREFIX_PATH");
    if (ament != null && !ament.isBlank()) {
      List<Path> dirs = existingLibDirsFromPrefixes(splitPaths(ament));
      if (!dirs.isEmpty()) return dirs;
    }

    String colcon = System.getenv("COLCON_PREFIX_PATH");
    if (colcon != null && !colcon.isBlank()) {
      List<Path> dirs = existingLibDirsFromPrefixes(splitPaths(colcon));
      if (!dirs.isEmpty()) return dirs;
    }

    // 4) Fall back to /opt/ros/<ROS_DISTRO>/lib
    String distro = System.getenv("ROS_DISTRO");
    if (distro == null || distro.isBlank()) distro = "jazzy";

    Path opt = Path.of("/opt/ros", distro);
    List<Path> fallback = existingLibDirsFromPrefixes(List.of(opt));
    if (!fallback.isEmpty()) return fallback;

    // 5) Last resort: empty (caller can decide what to do).
    return List.of();
  }

  private static List<Path> splitPaths(String pathList) {
    String sep = System.getProperty("path.separator"); // ":" on Unix, ";" on Windows
    String[] parts = pathList.split(java.util.regex.Pattern.quote(sep));
    List<Path> out = new ArrayList<>();
    for (String p : parts) {
      if (p == null || p.isBlank()) continue;
      out.add(Path.of(p));
    }
    return out;
  }

  private static List<Path> existingLibDirsFromPrefixes(List<Path> prefixes) {
    Set<Path> dirs = new LinkedHashSet<>();
    for (Path prefix : prefixes) {
      if (prefix == null) continue;
      // Typical layouts
      addIfDir(dirs, prefix.resolve("lib"));
      addIfDir(dirs, prefix.resolve("lib64"));

      // Some installations may place libs directly under the prefix (rare), include just in case.
      addIfDir(dirs, prefix);
    }
    return new ArrayList<>(dirs);
  }

  private static void addIfDir(Set<Path> dirs, Path candidate) {
    try {
      if (candidate != null && Files.isDirectory(candidate)) {
        dirs.add(candidate.toAbsolutePath().normalize());
      }
    } catch (Exception ignored) {}
  }
}
