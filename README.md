# Ruleta Pixel Rosario – Android

Proyecto Android WebView para compilar el APK.

## Cambios en esta versión
- Límite de demostración: 10 tiradas.
- Lectura del registro compartido de demostración al iniciar la aplicación.
- Escritura del contador nativo asíncrona: no se consulta ni escribe almacenamiento en el momento de iniciar la animación de giro.
- El contador en memoria y `localStorage` se actualiza inmediatamente; el registro de Descargas se actualiza en segundo plano.

## Compilación
Abrir el proyecto en Android Studio o ejecutar `./gradlew assembleDebug` en un entorno con Android SDK configurado.

## Nota sobre la persistencia local
El registro se guarda como archivo en `Download/PixelRuleta/`, con nombre derivado del `ANDROID_ID`. Está pensado para permanecer tras una reinstalación normal, pero Android/OEM pueden eliminar o restringir archivos de aplicaciones desinstaladas; el usuario también puede borrarlo manualmente. Sin un servidor no existe garantía antifraude absoluta.
