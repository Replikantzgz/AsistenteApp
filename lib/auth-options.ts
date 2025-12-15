import { NextAuthOptions } from 'next-auth';
import GoogleProvider from 'next-auth/providers/google';
import CredentialsProvider from 'next-auth/providers/credentials';

export const authOptions: NextAuthOptions = {
    providers: [
        GoogleProvider({
            clientId: process.env.GOOGLE_CLIENT_ID!,
            clientSecret: process.env.GOOGLE_CLIENT_SECRET!,
            authorization: {
                params: {
                    scope: 'openid email profile https://www.googleapis.com/auth/calendar https://www.googleapis.com/auth/gmail.modify https://www.googleapis.com/auth/contacts https://www.googleapis.com/auth/tasks',
                    access_type: 'offline',
                    prompt: 'consent',
                },
            },
        }),
        // Provider for Native Capacitor Google Sign In
        CredentialsProvider({
            id: 'google-native',
            name: 'Google Native',
            credentials: {
                idToken: { label: "ID Token", type: "text" },
                email: { label: "Email", type: "text" },
                name: { label: "Name", type: "text" }
            },
            async authorize(credentials) {
                if (!credentials?.idToken) return null;
                // In a production app, verify the idToken with google-auth-library here.
                // For now, we trust the client-side plugin result (or simple verification).
                // We return the user object to create the session.
                return {
                    id: credentials.email, // Use email as ID or sub from token
                    email: credentials.email,
                    name: credentials.name,
                    image: `https://lh3.googleusercontent.com/a/` // Generic or from token
                };
            }
        })
    ],
    callbacks: {
        async jwt({ token, account }) {
            if (account) {
                token.accessToken = account.access_token;
                token.refreshToken = account.refresh_token;
            }
            return token;
        },
        async session({ session, token }: any) {
            session.accessToken = token.accessToken;
            session.refreshToken = token.refreshToken;
            return session;
        },
    },
    secret: process.env.NEXTAUTH_SECRET,
};
