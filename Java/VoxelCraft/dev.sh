#!/bin/bash
# Быстрый запуск: компиляция + старт игры (без сборки fat-jar)
set -euo pipefail
cd "$(dirname "$0")"

source "${HOME}/.zprofile" 2>/dev/null || true
export PATH="${HOME}/.local/bin:${PATH}"

JAVA_FLAGS=()
if [ "$(uname -s)" = "Darwin" ]; then
    JAVA_FLAGS=(-XstartOnFirstThread)
fi

echo "Building..."
mvn -q compile -Pdefault

echo "Launching VoxelCraft..."
CP_FILE="target/classpath.txt"
mvn -q org.apache.maven.plugins:maven-dependency-plugin:3.7.1:build-classpath \
    -Pdefault -Dmdep.pathSeparator=":" -Dmdep.outputFile="${CP_FILE}"
CP=$(cat "${CP_FILE}")
exec java "${JAVA_FLAGS[@]}" -cp "target/classes:${CP}" com.voxelcraft.VoxelCraft
