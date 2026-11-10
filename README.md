# Neon Launcher (Android TV)

Лёгкий лаунчер: сетка приложений, статусы (время, Wi-Fi/Ethernet, VPN), неоновое «стекло» со свечением, обои из картинки или видео.

## Сборка
Заливаешь проект в GitHub: Actions сам соберёт APK (Actions -> Build APK -> Artifacts, либо Releases -> Latest build, если репозиторий публичный).

## Установка
adb install -r NeonLauncher.apk

## Обои
Положи файл wallpaper.mp4 (или wallpaper.jpg/png) в папку приложения на ТВ:
adb push wallpaper.mp4 /sdcard/Android/data/com.neonlauncher.tv/files/wallpaper.mp4
Видео: H.264, 1080p, 10-15 секунд, чем меньше размер, тем лучше.

## Управление
OK по плитке: запуск. Долгое OK: скрыть приложение. Кнопка НАСТРОЙКИ вверху справа (или кнопка Menu на пульте).
В настройках влево/вправо меняют значение.
