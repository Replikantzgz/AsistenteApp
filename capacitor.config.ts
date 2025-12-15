import type { CapacitorConfig } from '@capacitor/cli';

const config: CapacitorConfig = {
    appId: 'com.iatualcance.propel',
    appName: 'Propel',
    webDir: 'public',
    server: {
        url: 'https://asistente-app-teal.vercel.app',
        cleartext: true
    }
};

export default config;
