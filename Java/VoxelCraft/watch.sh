#!/bin/bash
# Автоперезапуск при изменении .java файлов
set -euo pipefail
cd "$(dirname "$0")"

source "${HOME}/.zprofile" 2>/dev/null || true
export PATH="${HOME}/.local/bin:${PATH}"

echo "Watching src/ — сохраните .java файл, игра пересоберётся и запустится."
echo "Ctrl+C чтобы остановить."
echo ""

HASH=""
while true; do
    NEW=$(find src -name '*.java' -print0 2>/dev/null | xargs -0 stat -f '%m %N' 2>/dev/null | shasum | awk '{print $1}')
    if [ -n "$HASH" ] && [ "$NEW" != "$HASH" ]; then
        echo ""
        echo "=== Изменения обнаружены, перезапуск... ==="
        pkill -f 'com.voxelcraft.VoxelCraft' 2>/dev/null || true
        sleep 0.3
        ./dev.sh || true
    fi
    HASH="$NEW"
    sleep 0.5
done
