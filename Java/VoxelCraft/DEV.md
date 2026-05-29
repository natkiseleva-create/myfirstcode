# VoxelCraft (Java) — разработка

Voxel-игра на **Java 17 + LWJGL 3**. Рабочая директория: `Java/VoxelCraft`.

## Быстрый старт

```bash
cd Java/VoxelCraft
./dev.sh
```

Откроется окно игры. Правите `.java` → снова `./dev.sh` (или используйте watch ниже).

## Режим watch (автоперезапуск)

```bash
./watch.sh
```

Следит за `src/**/*.java`. После сохранения файла игра пересобирается и перезапускается.

## Из Cursor / VS Code

1. Установите расширение **Extension Pack for Java** (если ещё нет).
2. **Run → Run VoxelCraft** (F5) — запуск с отладчиком.
3. **Terminal → Run Build Task** (Cmd+Shift+B) — `./dev.sh`.
4. **Tasks: Run Task → dev: watch** — автоперезапуск.

## Сборка JAR (релиз)

```bash
mvn package -Pdefault
java -XstartOnFirstThread -jar target/VoxelCraft-1.0.0.jar
```

## Структура кода

```
src/main/java/com/voxelcraft/
  VoxelCraft.java          — главный цикл игры
  block/                   — типы блоков
  control/                 — управление от первого лица
  inventory/               — инвентарь и крафт
  render/                  — OpenGL, шейдеры, окно
  world/                   — генерация мира, чанки
src/main/resources/shaders/
```

## Требования

- Java 17+ (`~/.local/opt/jdk-17`)
- Maven 3.9+ (`~/.local/bin/mvn`)
- macOS: флаг `-XstartOnFirstThread` (уже в `dev.sh` и launch.json)
