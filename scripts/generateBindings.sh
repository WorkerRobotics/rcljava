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

for PKG in std_msgs geometry_msgs std_srvs rosidl_runtime_c rmw; do
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

# Create pom.xml for maven install/deploy
cat <<EOF > pom.xml
    <project xmlns="http://maven.apache.org/POM/4.0.0">
    <modelVersion>4.0.0</modelVersion>
    <groupId>com.github.WorkerRobotics</groupId>
    <artifactId>ros2-java-bindings-jazzy</artifactId>
    <version>25.0.22</version>
    <properties>
        <maven.compiler.source>25</maven.compiler.source>
        <maven.compiler.target>25</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    </properties>
    </project>
EOF

# Run maven command
# mvn deploy -DskipTests --batch-mode
mvn clean install -DskipTests --batch-mode