# Aqua Motion Live Wallpaper

Отдельное Android-приложение для установки присланного пользователем видео как живых обоев.

## Видео
Поместить исходный файл без перекодирования в:
`app/src/main/assets/aqua_motion.mp4`

Видео воспроизводится без звука, с повтором по кругу. Для заполнения экрана используется масштабирование с обрезкой по краям (без растяжения изображения).

## Сборка
Из каталога `AquaMotionLiveWallpaper` выполнить:
`gradle --no-daemon assembleDebug`

APK: `app/build/outputs/apk/debug/app-debug.apk`
