# Documentación de Recuperación y Build Propel

Esta guía detalla el estado actual del proyecto y cómo generar la aplicación Android actualizada.

## Estado Actual (29/12/2025)
- **Google Login:** Configurado con Client ID Web (`969656696808-jec60cd9fi0rapd9en5.apps.googleusercontent.com`).
- **Notas:** Funcionalidad implementada.
- **Entorno Android:** Requiere configuración manual de `JAVA_HOME`.

## Pasos para Generar APK Actualizado

### 1. Construir la Web App
El proyecto usa `output: 'export'` para generar archivos estáticos.

```powershell
npm run build
```

### 2. Sincronizar con Android
Copia los archivos de `out/` a `android/app/src/main/assets/public`.

```powershell
npx cap sync android
```

### 3. Configurar Entorno Java (Windows)
Android Studio está instalado, pero no en el PATH. Usa su JDK integrado:

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
```

### 4. Compilar APK (Debug)
Ejecutar desde la carpeta `android/`.

```powershell
cd android
./gradlew assembleDebug
```

El APK se generará en:
`android/app/build/outputs/apk/debug/app-debug.apk`

---

## Solución de Problemas Comunes

### Error: "Could not find the web assets directory: out"
**Causa:** No se ejecutó `npm run build` antes de sincronizar.
**Solución:** Ejecutar paso 1.

### Error: "JAVA_HOME is not set"
**Causa:** Gradle no encuentra Java.
**Solución:** Ejecutar paso 3 antes del paso 4 en la misma terminal.
