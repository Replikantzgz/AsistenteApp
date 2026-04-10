'use client';

export const dynamic = 'force-dynamic';

import { useState } from 'react';
import { createClient } from '@/lib/supabase/client';
import { useRouter } from 'next/navigation';
import { Mail, Lock, Loader2, ArrowRight, Sparkles, Shield, Zap } from 'lucide-react';

export default function LoginPage() {
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [success, setSuccess] = useState<string | null>(null);
    const [mode, setMode] = useState<'signin' | 'signup'>('signin');
    const router = useRouter();
    const supabase = createClient();

    const handleGoogleLogin = async () => {
        setLoading(true);
        setError(null);
        const { error } = await supabase.auth.signInWithOAuth({
            provider: 'google',
            options: {
                scopes: [
                    'openid',
                    'email',
                    'profile',
                    'https://www.googleapis.com/auth/calendar',
                    'https://www.googleapis.com/auth/gmail.modify',
                    'https://www.googleapis.com/auth/contacts',
                ].join(' '),
                queryParams: {
                    access_type: 'offline',
                    prompt: 'consent',
                },
                redirectTo: `${location.origin}/auth/callback`,
            },
        });
        if (error) {
            setError(error.message);
            setLoading(false);
        }
    };

    const handleAuth = async (e: React.FormEvent) => {
        e.preventDefault();
        setLoading(true);
        setError(null);
        setSuccess(null);

        try {
            if (mode === 'signup') {
                const { error } = await supabase.auth.signUp({
                    email,
                    password,
                    options: {
                        emailRedirectTo: `${location.origin}/auth/callback`,
                        data: { full_name: email.split('@')[0] },
                    },
                });
                if (error) throw error;
                setSuccess('¡Registro exitoso! Revisa tu correo para confirmar tu cuenta.');
                setMode('signin');
            } else {
                const { error } = await supabase.auth.signInWithPassword({ email, password });
                if (error) throw error;
                router.push('/');
                router.refresh();
            }
        } catch (err: any) {
            setError(err.message);
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="min-h-screen flex">
            {/* Left panel – branding (desktop only) */}
            <div className="hidden lg:flex lg:w-1/2 bg-slate-900 flex-col justify-between p-12 relative overflow-hidden">
                {/* Background decoration */}
                <div className="absolute inset-0 overflow-hidden pointer-events-none">
                    <div className="absolute -top-32 -left-32 w-96 h-96 bg-blue-600/20 rounded-full blur-3xl" />
                    <div className="absolute bottom-0 right-0 w-80 h-80 bg-purple-600/15 rounded-full blur-3xl" />
                </div>

                <div className="relative">
                    <div className="flex items-center gap-3 mb-2">
                        <div className="w-10 h-10 rounded-full bg-gradient-to-br from-blue-500 to-indigo-600 flex items-center justify-center overflow-hidden">
                            <img src="/web-app-manifest-192x192.png" alt="Alfred" className="w-full h-full object-cover" />
                        </div>
                        <span className="text-white text-2xl font-bold">Alfred</span>
                    </div>
                </div>

                <div className="relative space-y-8">
                    <div>
                        <h1 className="text-5xl font-bold text-white leading-tight mb-4">
                            Tu asistente<br />
                            <span className="text-transparent bg-clip-text bg-gradient-to-r from-blue-400 to-cyan-400">
                                inteligente
                            </span>
                        </h1>
                        <p className="text-slate-400 text-lg">
                            Gestiona tu agenda, correos y contactos con el poder de la IA.
                        </p>
                    </div>

                    <div className="space-y-4">
                        {[
                            { icon: Sparkles, text: 'Asistente IA en español', color: 'text-blue-400' },
                            { icon: Zap, text: 'Integración con Google Workspace', color: 'text-cyan-400' },
                            { icon: Shield, text: 'Tus datos protegidos y privados', color: 'text-purple-400' },
                        ].map(({ icon: Icon, text, color }) => (
                            <div key={text} className="flex items-center gap-3">
                                <Icon className={`w-5 h-5 ${color}`} />
                                <span className="text-slate-300 text-sm">{text}</span>
                            </div>
                        ))}
                    </div>
                </div>

                <p className="relative text-slate-600 text-xs">© 2025 Alfred. Todos los derechos reservados.</p>
            </div>

            {/* Right panel – form */}
            <div className="flex-1 flex items-center justify-center p-6 bg-slate-50">
                <div className="w-full max-w-md">
                    {/* Mobile logo */}
                    <div className="flex items-center gap-2 justify-center mb-8 lg:hidden">
                        <div className="w-9 h-9 rounded-full bg-gradient-to-br from-blue-500 to-indigo-600 overflow-hidden">
                            <img src="/web-app-manifest-192x192.png" alt="Alfred" className="w-full h-full object-cover" />
                        </div>
                        <span className="text-slate-900 text-xl font-bold">Alfred</span>
                    </div>

                    <div className="bg-white rounded-2xl shadow-sm border border-slate-100 p-8">
                        <div className="mb-8">
                            <h2 className="text-2xl font-bold text-slate-900">
                                {mode === 'signin' ? 'Bienvenido de nuevo' : 'Crear cuenta'}
                            </h2>
                            <p className="text-slate-500 text-sm mt-1">
                                {mode === 'signin'
                                    ? 'Accede a tu asistente personal'
                                    : 'Empieza a organizar tu vida con IA'}
                            </p>
                        </div>

                        {/* Google Login */}
                        <button
                            onClick={handleGoogleLogin}
                            disabled={loading}
                            className="w-full bg-white border border-slate-200 text-slate-700 py-3 rounded-xl font-medium hover:bg-slate-50 hover:border-slate-300 transition-all flex items-center justify-center gap-3 mb-6 disabled:opacity-60 disabled:cursor-not-allowed shadow-sm"
                        >
                            <svg className="w-5 h-5 shrink-0" viewBox="0 0 24 24">
                                <path d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z" fill="#4285F4" />
                                <path d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z" fill="#34A853" />
                                <path d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z" fill="#FBBC05" />
                                <path d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z" fill="#EA4335" />
                            </svg>
                            Continuar con Google
                        </button>

                        <div className="flex items-center gap-3 mb-6">
                            <div className="flex-1 h-px bg-slate-200" />
                            <span className="text-slate-400 text-xs font-medium">o continúa con email</span>
                            <div className="flex-1 h-px bg-slate-200" />
                        </div>

                        <form onSubmit={handleAuth} className="space-y-4">
                            <div className="space-y-1">
                                <label className="text-sm font-medium text-slate-700">Email</label>
                                <div className="relative">
                                    <Mail className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-400" />
                                    <input
                                        type="email"
                                        required
                                        value={email}
                                        onChange={(e) => setEmail(e.target.value)}
                                        className="w-full pl-10 pr-4 py-2.5 border border-slate-200 rounded-xl focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500 outline-none transition-all text-sm"
                                        placeholder="tu@email.com"
                                    />
                                </div>
                            </div>

                            <div className="space-y-1">
                                <label className="text-sm font-medium text-slate-700">Contraseña</label>
                                <div className="relative">
                                    <Lock className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-400" />
                                    <input
                                        type="password"
                                        required
                                        value={password}
                                        onChange={(e) => setPassword(e.target.value)}
                                        className="w-full pl-10 pr-4 py-2.5 border border-slate-200 rounded-xl focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500 outline-none transition-all text-sm"
                                        placeholder="••••••••"
                                        minLength={6}
                                    />
                                </div>
                            </div>

                            {error && (
                                <div className="p-3 bg-red-50 border border-red-100 text-red-600 text-sm rounded-xl">
                                    {error}
                                </div>
                            )}
                            {success && (
                                <div className="p-3 bg-green-50 border border-green-100 text-green-700 text-sm rounded-xl">
                                    {success}
                                </div>
                            )}

                            <button
                                type="submit"
                                disabled={loading}
                                className="w-full bg-blue-600 text-white py-2.5 rounded-xl font-medium hover:bg-blue-700 active:bg-blue-800 transition-colors flex items-center justify-center gap-2 disabled:opacity-60 disabled:cursor-not-allowed text-sm"
                            >
                                {loading ? (
                                    <Loader2 className="w-4 h-4 animate-spin" />
                                ) : (
                                    <>
                                        {mode === 'signin' ? 'Iniciar sesión' : 'Crear cuenta'}
                                        <ArrowRight className="w-4 h-4" />
                                    </>
                                )}
                            </button>
                        </form>

                        <div className="text-center mt-6 pt-6 border-t border-slate-100">
                            <button
                                onClick={() => { setMode(mode === 'signin' ? 'signup' : 'signin'); setError(null); setSuccess(null); }}
                                className="text-blue-600 hover:text-blue-700 text-sm font-medium transition-colors"
                            >
                                {mode === 'signin'
                                    ? '¿No tienes cuenta? Regístrate gratis'
                                    : '¿Ya tienes cuenta? Inicia sesión'}
                            </button>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
}
