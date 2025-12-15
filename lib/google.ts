import { google } from 'googleapis';
import { getServerSession } from 'next-auth';
import { authOptions } from './auth-options';

export async function getGoogleAuth() {
    const session = await getServerSession(authOptions);

    if (!session || !session.accessToken) {
        return null;
    }

    const auth = new google.auth.OAuth2(
        process.env.GOOGLE_CLIENT_ID,
        process.env.GOOGLE_CLIENT_SECRET
    );

    auth.setCredentials({
        access_token: session.accessToken as string,
        refresh_token: session.refreshToken as string,
    });

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
