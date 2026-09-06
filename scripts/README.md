# Bindings generation for rcljava

Author: WorkerRobotics <ricky.van.rijn@worker-robotics.com>

This directory contains the script used to generate the Java bindings for the ROS 2 native libraries consumed by rcljava.

## Purpose

`generateBindings.sh` is the entry point for creating the jextract-based Java bindings from the ROS 2 installation that ships the native C headers and libraries used by rcl/rclc and related packages.

The script is designed to run against a ROS 2 installation such as:

- `/opt/ros/jazzy`

This installation provides the header files and include directories needed for the generated Java API, including the native C API exposed by the ROS 2 C libraries.

## What the script does

The script:

- locates or installs the `jextract` binary
- sources the ROS environment with `source /opt/ros/jazzy/setup.bash`
- adds the ROS include directories to the jextract command line
- generates Java bindings for the native C APIs, not C++ APIs
- emits Java source under `src/main/java`
- writes a minimal Maven `pom.xml` for installing the generated artifact locally
- runs `mvn clean install -DskipTests --batch-mode`

## Important detail: C headers only

This generation flow intentionally targets the native C interfaces, not the C++ headers.

The script includes files such as:

- `rcl/rcl/rcl.h`
- message and service headers under `.../msg/*.h` and `.../srv/*.h`
- other ROS 2 packages using the C API surface

It explicitly filters out C++-related files by avoiding generated C++ headers and by using the C API include paths and `.h` files from the ROS installation.

This matters because the generated bindings are used through the Java Foreign Function & Memory API and must match the native C ABI, not the C++ implementation layer.

## Maven usage

After generation, the script installs the generated artifact with Maven:

```bash
mvn clean install -DskipTests --batch-mode
```

The generated artifact is intended to be published or consumed as a Maven dependency by the core Java runtime module.

The parent project currently expects a dependency like:

```xml
<dependency>
  <groupId>com.github.WorkerRobotics</groupId>
  <artifactId>ros2-java-bindings-jazzy</artifactId>
  <version>25.0.22</version>
</dependency>
```

## Typical usage

From the repository root:

```bash
cd scripts
./generateBindings.sh
```

This produces the Java binding sources and installs the generated artifact into the local Maven repository so the Java runtime can use them.

## Notes

- The script expects a valid ROS 2 installation under `/opt/ros/jazzy`.
- The process is based on `jextract`, which generates Java wrappers over the native C headers.
- The generated output is a library artifact for the Java runtime and should not be treated as the public application API layer.
