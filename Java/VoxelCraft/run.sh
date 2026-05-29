#!/bin/bash
cd "$(dirname "$0")"
JAR="target/VoxelCraft-1.0.0.jar"
if [ ! -f "$JAR" ]; then
    echo "Run build.sh first"
    exit 1
fi

OS="$(uname -s)"
if [ "$OS" = "Darwin" ]; then
    java -XstartOnFirstThread -jar "$JAR"
else
    java -jar "$JAR"
fi
