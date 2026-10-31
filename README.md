# Ruleta Pixel Rosario – Android

Proyecto Android WebView para compilar el APK.

## Protección local de demostración
- Límite de demostración: 10 tiradas.
- El registro se guarda en `Download/PixelRuleta/pixel_ruleta_demo_<hash>.dat`, asociado al `ANDROID_ID`.
- En Android 11 o superior, la primera apertura solicita el permiso especial **Administrar todos los archivos**. Es necesario para que el archivo compartido pueda leerse después de reinstalar la aplicación.
- Se conserva el mismo nombre de archivo usado por la versión previa para intentar recuperar registros existentes.
- Las lecturas y escrituras se realizan en un hilo de trabajo. El contador en memoria se actualiza inmediatamente, sin bloquear el inicio de la animación.
- Si no se concede el permiso o no se puede leer el registro, la demo no se habilita: no se regalan 10 tiradas nuevas por un fallo de lectura.

## Importante
Esta protección es local y no es infalible. El usuario puede borrar manualmente el archivo, limpiar el almacenamiento o restablecer el dispositivo. El permiso especial puede estar sujeto a restricciones del fabricante. Las aplicaciones distribuidas por Google Play tienen restricciones adicionales para solicitar `MANAGE_EXTERNAL_STORAGE`; este proyecto está pensado para instalación directa del APK.

## Compilación
Abrir el proyecto en Android Studio o ejecutar `gradle assembleDebug` en un entorno con Android SDK configurado.
