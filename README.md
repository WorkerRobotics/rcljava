# rcljava (skeleton)

This is a Maven multi-module skeleton for building a Java ROS 2 client library targeting ROS 2 Jazzy and newer.

> Note: This project is still Work In Progress!

## Modules
- **rcljava-loader**: runtime native library loader (System.load with absolute paths).
- **rcljava-ffi**: placeholder module to depend on your jextract-generated bindings repo/artifact.
- **rcljava-core**: high-level Java API surface (RosRuntime/Context/Node placeholders).
- **rcljava-examples**: example app entrypoint.

## Devcontainer
A VS Code devcontainer is included under `.devcontainer/` based on `osrf/ros:jazzy-desktop` with Temurin JDK 25 and Maven.

## Build
```bash
mvn -q -DskipTests package
```

## Run example (inside container)
```bash
source /opt/ros/jazzy/setup.bash
mvn -q -pl rcljava-examples -am exec:java
```

> Note: rcljava-core is a skeleton; wire your jextract bindings into `rcljava-ffi` and implement the native calls in `rcljava-core`.


## Wiring your existing jextract bindings
Edit the parent `pom.xml` properties:

- `ros.bindings.groupId`
- `ros.bindings.artifactId`
- `ros.bindings.version`

`rcljava-ffi` depends on these coordinates and is the only module that should directly reference jextract-generated classes.

## Native library auto-detection
If you don't provide `RosConfig.nativeLibraryDirs`, the loader will try (in order):
1) `RCLJAVA_NATIVE_LIB_DIRS` (path-separator separated)
2) `AMENT_PREFIX_PATH` (prefixes -> `<prefix>/lib`, `<prefix>/lib64`)
3) `COLCON_PREFIX_PATH` (same)
4) `/opt/ros/${ROS_DISTRO}/lib` (defaults to `jazzy`)

You can still override explicitly:
```java
RosConfig cfg = RosConfig.builder().addNativeLibraryDir(Path.of("/opt/ros/jazzy/lib")).build();
```

## Quickstart (library usage)

In a *consumer* project, add:

```xml
<dependency>
  <groupId>com.workerrobotics.rcljava</groupId>
  <artifactId>rcljava-core</artifactId>
  <version>0.1.0-SNAPSHOT</version>
</dependency>
```

Then initialize once at startup (example skeleton):

```java
import com.workerrobotics.rcljava.core.RosRuntime;
import com.workerrobotics.rcljava.loader.RosConfig;

RosRuntime.initialize(args, RosConfig.builder().strict(true).build());
// create nodes, executors, etc...
RosRuntime.shutdown();
```

`rcljava` is intended as a library; there is no required `public static void main`.


## Verifying your bindings quickly

A lightweight FFI smoke call is available (does not call `rcl_init`, only a trivial "zero init" function).

Inside the devcontainer, run:

```bash
export RCLJAVA_RUN_FFI_SMOKE=true
mvn -q -pl rcljava-examples test
```

Make sure the JVM is started with:
- `--enable-native-access=ALL-UNNAMED`

VS Code's devcontainer launch/test configuration already includes this flag.
