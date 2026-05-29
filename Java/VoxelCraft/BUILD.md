# VoxelCraft — сборка

## Требования

- **Java 17** или новее
- **Maven 3.8+**
- Git (уже есть)

## Сборка

```bash
cd Java/VoxelCraft
mvn clean package -Pdefault
```

После сборки в `target/` появится `VoxelCraft-1.0.0.jar`.

## Запуск

```bash
java -jar target/VoxelCraft-1.0.0.jar
```

## macOS

На macOS могут понадобиться флаги JVM:

```bash
java -XstartOnFirstThread -jar target/VoxelCraft-1.0.0.jar
```

В BUILD.sh ниже это уже учтено.

## Windows

```bash
java -jar target/VoxelCraft-1.0.0.jar
```

## Скрипты для удобства

См. `build.sh` и `run.sh` в этой директории.
