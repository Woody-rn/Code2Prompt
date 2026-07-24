# Code2Prompt

Собирает файлы проекта в txt-файлы для отправки в AI-модели.

## Что делает

- Сканирует папку с проектом
- Собирает содержимое всех файлов
- Разбивает на части по лимиту символов модели
- Копирует содержимое в буфер

## Требования для сборки

Для сборки автономного приложения (app-image) требуется **Liberica JDK 21 Full** (с JavaFX).

Скачать: https://bell-sw.com/pages/downloads/

Выбрать: Windows, JDK 21, Full

### Задачи Gradle

| Команда | Что делает |
|---------|------------|
| `gradle run` | Запустить |
| `gradle test` | Тесты |
| `gradle fatJar` | JAR с зависимостями |
| `gradle jpackage` | EXE (app-image) |
| `gradle zipApp` | ZIP с EXE |

### Собрать portable-версию

```bash
gradle clean fatJar jpackage zipApp