#!/bin/bash
cd "$(dirname "$0")"
mvn clean package -Pdefault -q
echo "Build complete: target/VoxelCraft-1.0.0.jar"
