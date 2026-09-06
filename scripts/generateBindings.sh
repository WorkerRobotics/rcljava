#!/bin/bash

JEXTRACT_BIN="$(pwd)/jextract-25/bin/jextract"

# Controleer of jextract al beschikbaar is
if ! command -v jextract &> /dev/null || [ -f "$JEXTRACT_BIN" ];
then
    echo "jextract niet gevonden. Installatie wordt gestart..."

    # Download and install JExtract
    wget https://download.java.net/java/early_access/jextract/25/2/openjdk-25-jextract+2-4_linux-x64_bin.tar.gz
    tar -xf openjdk-25-jextract+2-4_linux-x64_bin.tar.gz
    
    # Voeg toe aan het huidige pad voor dit script
    export PATH="$PATH:$(pwd)/jextract-25/bin"
    
    # Optioneel: Voor GitHub Actions
    if [ -n "$GITHUB_PATH" ]; then
        echo "$(pwd)/jextract-25/bin" >> $GITHUB_PATH
    fi
    rm openjdk-25-jextract+2-4_linux-x64_bin.tar.gz
    echo "jextract succesvol geïnstalleerd en toegevoegd aan PATH."
else
    echo "jextract is al aanwezig op dit systeem: $(command -v jextract)"
fi

# Test of het werkt
jextract --version

DISTRO=jazzy

# Run jextract for creating bindings
source /opt/ros/$DISTRO/setup.bash
mkdir -p src/main/java

ROS_INC="/opt/ros/$DISTRO/include"

jextract --output src/main/java \
    -t org.ros2.rcl \
    --header-class-name RclLib \
    -I $ROS_INC \
    -I $ROS_INC/rcl \
    -I $ROS_INC/rcutils \
    -I $ROS_INC/rmw \
    -I $ROS_INC/rcl_yaml_param_parser \
    -I $ROS_INC/rosidl_runtime_c \
    -I $ROS_INC/rosidl_typesupport_interface \
    -I $ROS_INC/type_description_interfaces \
    -I $ROS_INC/service_msgs \
    -I $ROS_INC/builtin_interfaces \
    -I $ROS_INC/rosidl_dynamic_typesupport \
    -I $ROS_INC/rosidl_dynamic_typesupport_fastrtps \
    $ROS_INC/rcl/rcl/rcl.h

for PKG in std_msgs geometry_msgs std_srvs rosidl_runtime_c rmw sensor_msgs; do
    echo "Generating bindings for $PKG..."
    
    # Maak een tijdelijke wrapper header die alle .h bestanden uit de msg/srv map includeert
    TEMP_HEADER="master_$PKG.h"
    find $ROS_INC/$PKG/$PKG/msg $ROS_INC/$PKG/$PKG/srv -name "*.h" 2>/dev/null | grep -v "cpp" | grep -v "fastrtps" | xargs -I {} echo '#include "{}"' > $TEMP_HEADER
    
    jextract --output src/main/java \
        -t org.ros2.rcl.msgs \
        --header-class-name ${PKG^^}_Lib \
        -I $ROS_INC \
        -I $ROS_INC/$PKG \
        -I $ROS_INC/rosidl_runtime_c \
        -I $ROS_INC/rosidl_typesupport_interface \
        -I $ROS_INC/builtin_interfaces \
        -I $ROS_INC/rcutils \
        -I $ROS_INC/fastcdr \
        -I $ROS_INC/std_msgs \
        -I $ROS_INC/service_msgs \
        -I $ROS_INC/geometry_msgs \
        -I $ROS_INC/sensor_msgs \
        $TEMP_HEADER
        
    rm $TEMP_HEADER
done

echo '#include <std_srvs/std_srvs/srv/set_bool.h>
#include <std_srvs/std_srvs/srv/trigger.h>' > master_srv.h
jextract --output src/main/java \
    -t org.ros2.rcl.srv \
    --header-class-name StdSrvs_Lib \
    -I $ROS_INC \
    -I $ROS_INC/std_srvs \
    -I $ROS_INC/service_msgs \
    -I $ROS_INC/builtin_interfaces \
    -I $ROS_INC/rosidl_runtime_c \
    -I $ROS_INC/rosidl_typesupport_interface \
    -I $ROS_INC/rcutils \
    master_srv.h
rm master_srv.h

# Create pom.xml for Maven Central publishing
cat <<'EOF' > pom.xml
<project xmlns="http://maven.apache.org/POM/4.0.0"
     xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
     xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
<modelVersion>4.0.0</modelVersion>

<groupId>com.github.WorkerRobotics</groupId>
<artifactId>ros2-java-bindings-jazzy</artifactId>
<version>25.0.22</version>
<packaging>jar</packaging>

<name>ROS 2 Java Bindings for Jazzy</name>
<description>Generated Java bindings for the ROS 2 Jazzy C API via jextract.</description>
<url>https://github.com/WorkerRobotics/rcljava</url>

<licenses>
    <license>
        <name>Apache License, Version 2.0</name>
        <url>https://www.apache.org/licenses/LICENSE-2.0.txt</url>
        <distribution>repo</distribution>
    </license>
</licenses>

<developers>
    <developer>
        <id>workerrobotics</id>
        <name>WorkerRobotics</name>
        <email>ricky.van.rijn@worker-robotics.com</email>
        <organization>WorkerRobotics</organization>
        <organizationUrl>https://github.com/WorkerRobotics</organizationUrl>
    </developer>
</developers>

<scm>
    <connection>scm:git:git://github.com/WorkerRobotics/rcljava.git</connection>
    <developerConnection>scm:git:ssh://git@github.com/WorkerRobotics/rcljava.git</developerConnection>
    <url>https://github.com/WorkerRobotics/rcljava</url>
</scm>

<issueManagement>
    <system>GitHub</system>
    <url>https://github.com/WorkerRobotics/rcljava/issues</url>
</issueManagement>

<properties>
    <maven.compiler.release>25</maven.compiler.release>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    <maven.source.plugin.version>3.3.1</maven.source.plugin.version>
    <maven.javadoc.plugin.version>3.10.1</maven.javadoc.plugin.version>
    <maven.gpg.plugin.version>3.2.4</maven.gpg.plugin.version>
    <central.publishing.plugin.version>0.7.0</central.publishing.plugin.version>
</properties>

<build>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-compiler-plugin</artifactId>
            <version>3.13.0</version>
            <configuration>
                <release>${maven.compiler.release}</release>
            </configuration>
        </plugin>

        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-source-plugin</artifactId>
            <version>${maven.source.plugin.version}</version>
            <executions>
                <execution>
                    <id>attach-sources</id>
                    <goals>
                        <goal>jar-no-fork</goal>
                    </goals>
                </execution>
            </executions>
        </plugin>

        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-javadoc-plugin</artifactId>
            <version>${maven.javadoc.plugin.version}</version>
            <executions>
                <execution>
                    <id>attach-javadocs</id>
                    <goals>
                        <goal>jar</goal>
                    </goals>
                </execution>
            </executions>
            <configuration>
                <doclint>none</doclint>
                <source>25</source>
            </configuration>
        </plugin>

        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-gpg-plugin</artifactId>
            <version>${maven.gpg.plugin.version}</version>
            <configuration>
                <!-- Use loopback pinentry so passphrase can be provided non-interactively (CI) -->
                <gpgArguments>
                    <arg>--pinentry-mode</arg>
                    <arg>loopback</arg>
                </gpgArguments>
                <!-- Key name may be provided at runtime via -Dgpg.keyname or environment -->
                <keyname>${gpg.keyname}</keyname>
            </configuration>
            <executions>
                <execution>
                    <id>sign-artifacts</id>
                    <phase>verify</phase>
                    <goals>
                        <goal>sign</goal>
                    </goals>
                </execution>
            </executions>
        </plugin>

        <plugin>
            <groupId>org.sonatype.central</groupId>
            <artifactId>central-publishing-maven-plugin</artifactId>
            <version>${central.publishing.plugin.version}</version>
            <extensions>true</extensions>
            <configuration>
                <publishingServerId>central</publishingServerId>
                <autoPublish>true</autoPublish>
                <waitUntil>published</waitUntil>
            </configuration>
        </plugin>
    </plugins>
</build>
</project>
EOF

# Run maven command
# mvn deploy -DskipTests --batch-mode
mvn clean install -DskipTests --batch-mode