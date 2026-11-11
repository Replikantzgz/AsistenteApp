# Actualizaciones automáticas

La app comprueba si hay una versión nueva **cada vez que se abre**, la descarga sola y abre el instalador de Android.

## Cómo funciona

1. Cada `git push` ejecuta el workflow **Compilar y publicar APK**: pasa los tests, compila, firma y comprueba que la
   firma es la de Alfred. Si todo va bien, publica una *Release* de GitHub con `Alfred.apk` y un `update.json`.
2. Al abrirse, la app lee
   `https://github.com/Replikantzgz/AsistenteApp/releases/latest/download/update.json`.
   Si su `versionCode` es mayor que el de la app instalada, descarga el APK (comprueba su SHA-256) y se lo entrega al
   instalador de Android.
3. Android solo instala el APK si está firmado con **la misma clave** que la app instalada; si no, lo rechaza.

El número de versión sube solo: `versionCode = nº de ejecución del workflow + 10`, y el nombre es `1.3.<nº>`.

## Preparación (una sola vez)

1. **Secretos del repositorio** (sin ellos no se publica nada): en
   `Settings → Secrets and variables → Actions` crea `ALFRED_KEYSTORE_BASE64` y `ALFRED_PASSWORD`
   (ver README, apartado Firma).
2. **Instala a mano el APK de esta versión una sola vez** (el que ya trae el actualizador). Las versiones anteriores
   no saben actualizarse.
3. La primera vez que haya una actualización, Alfred pedirá el permiso **«Permitir desde esta fuente»**
   (instalar apps). Se concede una vez.

## Qué verás

- Un banner amarillo con el progreso de la descarga y, después, la pantalla de confirmación de Android
  («¿Quieres actualizar esta aplicación?»).
- En Android 12 o superior, cuando Alfred ya ha instalado una actualización por este camino, las siguientes pueden
  instalarse **sin ninguna confirmación**. Es una decisión de Android: no se puede forzar.
- Ajustes → **Actualizaciones**: versión instalada, interruptor «Buscar e instalar al abrir» y botón
  «Buscar actualizaciones ahora».

## Límites

- Android no permite instalar apps en silencio sin permisos de sistema; por eso puede salir la confirmación.
- El repositorio es público, así que las versiones publicadas también lo son. Si lo haces privado, las descargas
  necesitarán otro alojamiento.
- Si el APK de la Release no está firmado con la clave de Alfred, Android lo rechaza y la app avisa. El workflow lo
  evita comprobando la huella antes de publicar.
