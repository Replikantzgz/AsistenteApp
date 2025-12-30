import { Capacitor } from '@capacitor/core';
import { GoogleAuth } from '@codetrix-studio/capacitor-google-auth';
import { signIn } from 'next-auth/react';

export async function handleCombinedLogin() {
    if (Capacitor.isNativePlatform()) {
        try {
            console.log('Starting Native Google Sign-In...');
            await GoogleAuth.initialize();

            const googleUser = await GoogleAuth.signIn();
            console.log('Native Google User obtained:', JSON.stringify(googleUser, null, 2));

            if (!googleUser.authentication.idToken) {
                throw new Error('No idToken received from Google');
            }

            console.log('Attempting NextAuth sign-in with google-native...');
            // Pass the native token to NextAuth via Credentials provider
            const result = await signIn('google-native', {
                idToken: googleUser.authentication.idToken,
                email: googleUser.email,
                name: googleUser.name,
                callbackUrl: '/',
                redirect: true
            });
            console.log('NextAuth sign-in result:', result);

        } catch (error) {
            console.error('Native Google Sign-In Error:', error);
            // @ts-ignore
            alert(`Error nativo detallado: ${error.message || JSON.stringify(error)}`);
            throw error;
        }
    } else {
        // Fallback to web flow
        await signIn('google', { callbackUrl: '/' });
    }
}
