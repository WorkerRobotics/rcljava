package com.workerrobotics.rcljava.loader;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Configuration for native library discovery/loading.
 *
 * Assumption: the ROS installation itself is correct; this config only tells the JVM where to load native libs from.
 */
public final class RosConfig {
  private final List<Path> nativeLibraryDirs;
  private final List<String> libraries;
  private final boolean strict;

  private RosConfig(Builder b) {
    this.nativeLibraryDirs = Collections.unmodifiableList(new ArrayList<>(b.nativeLibraryDirs));
    this.libraries = Collections.unmodifiableList(new ArrayList<>(b.libraries));
    this.strict = b.strict;
  }

  public List<Path> nativeLibraryDirs() { return nativeLibraryDirs; }
  public List<String> libraries() { return libraries; }
  public boolean strict() { return strict; }

  public static Builder builder() { return new Builder(); }

  public static final class Builder {
    private final List<Path> nativeLibraryDirs = new ArrayList<>();
    private final List<String> libraries = new ArrayList<>();
    private boolean strict = true;

    public Builder addNativeLibraryDir(Path dir) {
      if (dir != null) nativeLibraryDirs.add(dir);
      return this;
    }

    public Builder nativeLibraryDirs(List<Path> dirs) {
      nativeLibraryDirs.clear();
      if (dirs != null) nativeLibraryDirs.addAll(dirs);
      return this;
    }

    public Builder addLibrary(String name) {
      if (name != null && !name.isBlank()) libraries.add(name);
      return this;
    }

    public Builder libraries(List<String> names) {
      libraries.clear();
      if (names != null) libraries.addAll(names);
      return this;
    }

    public Builder strict(boolean strict) {
      this.strict = strict;
      return this;
    }

    public RosConfig build() {
      return new RosConfig(this);
    }
  }
}
