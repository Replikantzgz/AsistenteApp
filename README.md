# Propel - Asistente Personal AI

Aplicación de asistencia personal con IA, notas de voz, y gestión de calendario. Integrada con Google Services y construida con Next.js + Capacitor.

## Características

- **Asistente AI:** Chat interactivo con capacidades de gestión.
- **Notas Inteligentes:** Transcripción de voz y resúmenes automáticos.
- **Integración Google:** Login nativo, Calendario, Contactos y Gmail.
- **Multi-Plataforma:** Web (PWA) y Android.

## Configuración del Entorno

### Requisitos
- Node.js 20+
- Android Studio (para build nativo)
- Clave de API de Google (Web Client ID)

### Instalación

```bash
npm install
```

### Ejecutar en Desarrollo

```bash
npm run dev
```

## Construcción para Android

1. **Build Web Assets:**
   ```bash
   npm run build
   ```

2. **Sync Native:**
   ```bash
   npx cap sync android
   ```

3. **Build APK:**
   Desde `android/`:
   ```bash
   ./gradlew assembleDebug
   ```
   *Nota: Requiere JAVA_HOME configurado (ver INSTRUCCIONES_RECUPERACION.md).*
