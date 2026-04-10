import { Capacitor } from '@capacitor/core';
import { GoogleAuth } from '@codetrix-studio/capacitor-google-auth';
import { createClient } from '@/lib/supabase/client';

export async function handleCombinedLogin() {
    const supabase = createClient();

    if (Capacitor.isNativePlatform()) {
        try {
            await GoogleAuth.initialize();
            const googleUser = await GoogleAuth.signIn();

            if (!googleUser.authentication.idToken) {
                throw new Error('No se recibió token de Google');
            }

            // Sign in to Supabase using Google idToken
            const { error } = await supabase.auth.signInWithIdToken({
                provider: 'google',
                token: googleUser.authentication.idToken,
            });

            if (error) throw error;
        } catch (error: any) {
            console.error('Native Google Sign-In Error:', error);
            throw error;
        }
    } else {
        // Web: use Supabase OAuth flow
        const { error } = await supabase.auth.signInWithOAuth({
            provider: 'google',
            options: {
                scopes: 'openid email profile https://www.googleapis.com/auth/calendar https://www.googleapis.com/auth/gmail.modify https://www.googleapis.com/auth/contacts',
                queryParams: { access_type: 'offline', prompt: 'consent' },
                redirectTo: `${window.location.origin}/auth/callback`,
            },
        });
        if (error) throw error;
    }
}
