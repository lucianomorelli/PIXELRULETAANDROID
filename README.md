# Ruleta Pixel Rosario para Android

Proyecto Android que empaqueta el `INDEX.html` original dentro de un WebView local.

- Conserva el HTML, CSS y JavaScript de la ruleta sin rediseñarlos.
- WebViewAssetLoader sirve el archivo desde un origen HTTPS local para habilitar APIs seguras como Web Crypto.
- DOM Storage queda habilitado para persistir configuración, contador de demo e ID/licencia en el perfil WebView.
- Pantalla inmersiva, pantalla siempre encendida y orientación adaptable a teléfono/tablet.
- Los enlaces externos (por ejemplo WhatsApp) se abren fuera de la aplicación.

## Compilar en la nube con GitHub Actions

1. Crear un repositorio privado en GitHub.
2. Subir el contenido de esta carpeta al repositorio (no la carpeta contenedora).
3. Abrir **Actions** y ejecutar **Compilar APK Android**.
4. Al terminar, abrir la ejecución y descargar el artefacto **Ruleta-Pixel-Rosario-APK**.
5. Dentro del ZIP descargado está `app-debug.apk`.

El APK de depuración sirve para instalar y probar. Para distribución comercial conviene generar un APK/AAB firmado con una clave de publicación propia.

## Nota importante sobre licencias

El HTML conserva el mecanismo actual: el identificador es del perfil WebView, no un identificador físico inviolable del teléfono/tablet. La activación quedará asociada a los datos persistentes de esta instalación/perfil. Borrar datos de la app o desinstalarla puede regenerar el identificador y requerir una nueva activación. Antes de vender esta versión, probar el flujo real de generación y activación de licencia en un dispositivo Android.


## Protección local de las 10 tiradas demo (Android 10+)

- El WebView consulta un puente nativo Android para leer y guardar el contador demo.
- El contador se guarda como archivo propio en `Download/PixelRuleta/`, identificado mediante un hash de `ANDROID_ID`; no se guarda el identificador original en el nombre del archivo.
- Al reinstalar el mismo APK en el mismo dispositivo, la aplicación intenta recuperar el contador anterior.
- No requiere conexión ni servidor. En Android anterior a 10, o si el usuario elimina el archivo/restablece el dispositivo, se mantiene el respaldo local y no se garantiza la recuperación tras desinstalar.
- Es una barrera práctica para esta etapa, no una protección inviolable contra manipulación deliberada.
- Las licencias comerciales y su identificador actual no se modifican.
