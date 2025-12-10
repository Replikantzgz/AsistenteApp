import type { Metadata } from 'next';
import './globals.css';

export const metadata: Metadata = {
    title: 'IATUALCANCE',
    description: 'Tu asistente personal inteligente',
    viewport: 'width=device-width, initial-scale=1, maximum-scale=1, user-scalable=0'
};

export default function RootLayout({
    children,
}: {
    children: React.ReactNode;
}) {
    return (
        <html lang="es">
            <body className="h-screen w-screen overflow-hidden bg-slate-100 text-slate-900 antialiased">
                {children}
            </body>
        </html>
    );
}
