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
    },
    // UserAgent override to mimic Chrome on Android and bypass Google's disallowed_useragent check for WebViews
    overrideUserAgent: "Mozilla/5.0 (Linux; Android 14; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36",
    plugins: {
        GoogleAuth: {
            scopes: ["openid", "email", "profile"],
            serverClientId: "REEMPLAZAR_CON_TU_GOOGLE_CLIENT_ID_DE_FIREBASE_O_CLOUD",
            forceCodeForRefreshToken: true
        }
    }
};

export default config;
