FROM osrf/ros:jazzy-desktop

ENV DEBIAN_FRONTEND=noninteractive

# Base dependencies
RUN apt-get update && apt-get install -y \
    curl \
    wget \
    git \
    ca-certificates \
    maven \
    unzip \
    && rm -rf /var/lib/apt/lists/*

# Install Eclipse Temurin JDK 25 (LTS) - linux x64 HotSpot
# If you need a different architecture (e.g., arm64), swap the tarball accordingly.
RUN mkdir -p /opt/java/openjdk \
    && curl -L https://github.com/adoptium/temurin25-binaries/releases/download/jdk-25.0.2%2B10/OpenJDK25U-jdk_x64_linux_hotspot_25.0.2_10.tar.gz \
    | tar -xz -C /opt/java/openjdk --strip-components=1

ENV JAVA_HOME=/opt/java/openjdk
ENV PATH="${JAVA_HOME}/bin:${PATH}"

# Source ROS 2 in interactive shells
RUN echo "source /opt/ros/jazzy/setup.bash" >> /root/.bashrc

WORKDIR /workspace
