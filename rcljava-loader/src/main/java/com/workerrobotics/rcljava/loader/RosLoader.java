package com.workerrobotics.rcljava.loader;

import java.io.File;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Loads ROS 2 native libraries into the JVM.
 *
 * Strategy:
 * - Prefer System.load(absolutePath) to avoid reliance on java.library.path.
 * - User supplies nativeLibraryDirs (e.g., /opt/ros/jazzy/lib).
 * - Optionally user supplies explicit library base names (e.g., "rcl", "rmw",
 * "rcutils").
 */
public final class RosLoader {
  private static final AtomicBoolean LOADED = new AtomicBoolean(false);

  private RosLoader() {
  }

  public static void load(RosConfig cfg) {
    if (cfg == null)
      throw new IllegalArgumentException("RosConfig must not be null");
    if (LOADED.get())
      return;

    synchronized (RosLoader.class) {
      if (LOADED.get())
        return;

      List<Path> dirs = RosPathResolver.resolveNativeLibraryDirs(cfg);
      if (dirs.isEmpty()) {
        // Keep behavior explicit: if nothing is found and strict is on, we'll fail
        // later when searching.
        dirs = List.of();
      }

      List<String> libs = cfg.libraries();
      if (libs.isEmpty()) {
        // Expand as you implement more functionality. 
        libs = List.of("rcutils", "rmw", "rcl", "rcl_yaml_param_parser", "rosidl_runtime_c",
            "geometry_msgs__rosidl_typesupport_c", "geometry_msgs__rosidl_typesupport_introspection_c", 
            "std_srvs__rosidl_typesupport_c", "rmw");
      }

      List<String> attempted = new ArrayList<>();
      List<Throwable> errors = new ArrayList<>();

      for (String lib : libs) {
        Path found = findLibraryFile(dirs, lib);
        if (found == null) {
          attempted.add("NOT FOUND: " + lib + " in " + dirs);
          if (cfg.strict()) {
            throw buildLoadException("Could not find native library '" + lib + "'", attempted, errors, dirs, libs);
          }
          continue;
        }

        try {
          System.load(found.toAbsolutePath().toString());
          attempted.add("LOADED: " + found);
        } catch (Throwable t) {
          errors.add(t);
          attempted.add("FAILED: " + found + " (" + t.getClass().getSimpleName() + ": " + t.getMessage() + ")");
          if (cfg.strict()) {
            throw buildLoadException("Failed to load native library '" + lib + "'", attempted, errors, dirs, libs);
          }
        }
      }

      LOADED.set(true);
    }
  }

  public static MemorySegment findSymbol(String symbolName) {
      // Laad de library specifiek voor dit berichttype
      // In Linux zoekt dit in de rpath/LD_LIBRARY_PATH
      
      return SymbolLookup.loaderLookup().find(symbolName)
          .orElseThrow(() -> new RuntimeException("Symbol ["+symbolName+"] not found."))
          .reinterpret(0); // Of de juiste grootte als je die weet
  }

  private static RuntimeException buildLoadException(
      String headline,
      List<String> attempted,
      List<Throwable> errors,
      List<Path> dirs,
      List<String> libs) {
    StringBuilder sb = new StringBuilder();
    sb.append(headline).append("\n\n");
    sb.append("OS: ").append(System.getProperty("os.name")).append(" / ").append(System.getProperty("os.arch"))
        .append("\n");
    sb.append("java.version: ").append(System.getProperty("java.version")).append("\n");
    sb.append("nativeLibraryDirs: ").append(dirs).append("\n");
    sb.append("libraries: ").append(libs).append("\n\n");
    sb.append("Attempts:\n");
    for (String a : attempted)
      sb.append("  - ").append(a).append("\n");
    if (!errors.isEmpty()) {
      sb.append("\nErrors:\n");
      for (Throwable e : errors) {
        sb.append("  - ").append(e.getClass().getName()).append(": ").append(e.getMessage()).append("\n");
      }
    }
    return new RuntimeException(sb.toString());
  }

  private static Path findLibraryFile(List<Path> dirs, String baseName) {
    String fileName = platformLibraryFileName(baseName);
    for (Path dir : dirs) {
      if (dir == null)
        continue;
      Path candidate = dir.resolve(fileName);
      if (Files.isRegularFile(candidate))
        return candidate;
    }

    // Also try "lib<name>" variant if user passed without lib prefix and platform
    // expects it.
    if (!baseName.startsWith("lib") && isUnixLike()) {
      String alt = platformLibraryFileName("lib" + baseName);
      for (Path dir : dirs) {
        if (dir == null)
          continue;
        Path candidate = dir.resolve(alt);
        if (Files.isRegularFile(candidate))
          return candidate;
      }
    }
    return null;
  }

  private static boolean isUnixLike() {
    String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
    return os.contains("linux") || os.contains("mac") || os.contains("darwin") || os.contains("nix")
        || os.contains("nux");
  }

  private static String platformLibraryFileName(String baseName) {
    String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);

    if (os.contains("win")) {
      // On Windows, the "lib" prefix is uncommon; callers should pass the real DLL
      // base name if needed.
      return baseName.endsWith(".dll") ? baseName : baseName + ".dll";
    }
    if (os.contains("mac") || os.contains("darwin")) {
      String bn = baseName.startsWith("lib") ? baseName : "lib" + baseName;
      return bn.endsWith(".dylib") ? bn : bn + ".dylib";
    }
    // Linux / other Unix
    String bn = baseName.startsWith("lib") ? baseName : "lib" + baseName;

    return bn.endsWith(".so") ? bn : bn + ".so";
  }
}