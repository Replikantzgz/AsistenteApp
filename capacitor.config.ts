import type { CapacitorConfig } from '@capacitor/cli';

const config: CapacitorConfig = {
    appId: 'com.iatualcance.propel',
    appName: 'Alfred',
    webDir: 'out',
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
            scopes: [
                "openid",
                "email",
                "profile",
                "https://www.googleapis.com/auth/calendar",
                "https://www.googleapis.com/auth/gmail.modify",
                "https://www.googleapis.com/auth/contacts"
            ],
            serverClientId: "1078256636873-i0rj5l52kf7vop8og1gl70km3qbpg436.apps.googleusercontent.com",
            forceCodeForRefreshToken: true
        }
    }
};

export default config;
