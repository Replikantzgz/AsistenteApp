import { google } from 'googleapis';
import { createServerSupabaseClient } from '@/lib/supabase/server';

export async function getGoogleAuth() {
    const supabase = await createServerSupabaseClient();
    const { data: { session } } = await supabase.auth.getSession();
    const accessToken = (session as any)?.provider_token;

    if (!session || !accessToken) return null;

    const auth = new google.auth.OAuth2(
        process.env.GOOGLE_CLIENT_ID,
        process.env.GOOGLE_CLIENT_SECRET
    );
    auth.setCredentials({ access_token: accessToken });
    return auth;
}

export async function getCalendarService() {
    const auth = await getGoogleAuth();
    if (!auth) return null;
    return google.calendar({ version: 'v3', auth });
}

export async function getGmailService() {
    const auth = await getGoogleAuth();
    if (!auth) return null;
    return google.gmail({ version: 'v1', auth });
}

export async function getPeopleService() {
    const auth = await getGoogleAuth();
    if (!auth) return null;
    return google.people({ version: 'v1', auth });
}
