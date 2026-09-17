# REMY.X Android V3

Proyecto Android nativo separado de REMY.X Windows.

## Objetivo
- App gratuita.
- Sin login obligatorio.
- Sin anuncios.
- Descarga local cuando el contenido sea descargable sin credenciales.
- Interfaz nativa con Kotlin + Jetpack Compose.

## Motor
V3 integra la versión gratuita `dev.ffmpegkit-maintained:yt-dlp-android:2.0.2` desde Maven Central. La biblioteca integra yt-dlp y Python dentro de la app. Requisitos documentados: Android 7+ y ABI arm64-v8a/x86_64. Esta app fija minSdk 29 para simplificar el almacenamiento moderno.

La versión gratuita no incluye la parte Pro de `curl-cffi`. Algunos sitios pueden requerir métodos adicionales y devolver error; REMY.X no pide contraseñas ni captura credenciales.

## Qué hace V3
- UI nativa.
- Inicializa YtDlp.
- Pega URL.
- Descarga con un formato de archivo único cuando está disponible para evitar depender de FFmpeg en esta fase.
- Muestra progreso.
- Mantiene el archivo en almacenamiento externo específico de la app.
- Botón de apoyo PayPal.

## Compilación sin Android Studio
El repositorio incluye `.github/workflows/build-apk.yml`.
GitHub Actions puede compilar la APK en un runner Linux. El usuario no necesita Android Studio en su PC para esa ruta; sí necesita un repositorio de GitHub con estos archivos y ejecutar el workflow.

El artefacto generado será:
`app-debug.apk`

## Próxima fase
- Guardar en la carpeta pública Descargas mediante MediaStore.
- Selector de calidad/formato.
- Historial real.
- Notificaciones.
- Mejor manejo de errores y cancelación.
- Firma de release cuando llegue el momento de distribuir una APK final.

## Licencias
La biblioteca `yt-dlp-android` 2.0.2 declara licencia MIT en su publicación de Maven Central/repo del mantenedor. Sus componentes de terceros conservan sus respectivas licencias y avisos.
