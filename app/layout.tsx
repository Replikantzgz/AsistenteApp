import { Metadata, Viewport } from 'next';
import './globals.css';
import NextAuthProvider from '@/components/providers/NextAuthProvider';

export const metadata: Metadata = {
    title: 'Propel',
    description: 'Tu asistente personal inteligente',
    manifest: '/manifest.json',
    appleWebApp: {
        capable: true,
        statusBarStyle: 'black-translucent',
        title: 'Propel',
    },
};

export const viewport: Viewport = {
    width: 'device-width',
    initialScale: 1,
    maximumScale: 1,
    userScalable: false,
    themeColor: '#0f172a',
};

export default function RootLayout({
    children,
}: {
    children: React.ReactNode;
}) {
    return (
        <html lang="es">
            <body className="h-screen w-screen overflow-hidden bg-slate-100 text-slate-900 antialiased">
                <NextAuthProvider>
                    {children}
                </NextAuthProvider>
            </body>
        </html>
    );
}
