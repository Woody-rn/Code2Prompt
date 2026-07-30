# Code2Prompt

Собирает файлы проекта в txt-файлы для отправки в AI-модели.

## Что делает

- Сканирует папку с проектом
- Собирает содержимое всех файлов
- Разбивает на части по лимиту символов модели
- Копирует содержимое в буфер
- Запускает HTTP-сервер для браузерного расширения
- Автоматически отправляет контекст в чат DeepSeek

## Быстрый старт

1. Запусти `Code2Prompt.exe`
2. Выбери папку с проектом → **Старт**
3. Нажми **🚀 Запустить сервер**
4. Установи расширение из папки `extension-chromium/` (chrome://extensions → Загрузить распакованное)
5. Открой `chat.deepseek.com`
6. Нажми `Ctrl+Enter` — контекст отправится в чат

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
| `gradle packageZip` | ZIP с EXE и расширением |

### Собрать portable-версию

```bash
gradle clean fatJar jpackage packageZip
```
