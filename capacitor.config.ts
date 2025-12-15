import type { CapacitorConfig } from '@capacitor/cli';

const config: CapacitorConfig = {
    appId: 'com.iatualcance.propel',
    appName: 'Propel',
    webDir: 'public',
    server: {
        url: 'https://asistente-app-teal.vercel.app',
        cleartext: true,
        allowNavigation: [
            'asistente-app-teal.vercel.app',
            'accounts.google.com',
            '*.google.com',
            '*.stripe.com'
        ]
    }
};

export default config;
