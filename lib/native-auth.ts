import { Capacitor } from '@capacitor/core';
import { GoogleAuth } from '@codetrix-studio/capacitor-google-auth';
import { signIn } from 'next-auth/react';

export async function handleCombinedLogin() {
    if (Capacitor.isNativePlatform()) {
        try {
            await GoogleAuth.initialize();
            const googleUser = await GoogleAuth.signIn();

            // Pass the native token to NextAuth via Credentials provider
            await signIn('google-native', {
                idToken: googleUser.authentication.idToken,
                email: googleUser.email,
                name: googleUser.name,
                callbackUrl: '/',
                redirect: true
            });
        } catch (error) {
            console.error('Native Google Sign-In Error:', error);
            alert(`Error nativo: ${JSON.stringify(error)}`);
            throw error;
        }
    } else {
        // Fallback to web flow
        await signIn('google', { callbackUrl: '/' });
    }
}
