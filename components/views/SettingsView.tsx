'use client';

import { useState, useEffect } from 'react';
import { Loader2 } from 'lucide-react';
import { createClient } from '@/lib/supabase/client';
import { useAuth } from '@/hooks/useAuth';
import { useRouter } from 'next/navigation';

export default function SettingsView() {
    const { user, session, loading } = useAuth();
    const router = useRouter();
    const supabase = createClient();

    const [referralData, setReferralData] = useState({ code: '', balance: 0, count: 0 });
    const [inputCode, setInputCode] = useState('');
    const [applying, setApplying] = useState(false);
    const [hasReferrer, setHasReferrer] = useState(true);
    const [notificationsEnabled, setNotificationsEnabled] = useState(true);
    const [showPrivacyDialog, setShowPrivacyDialog] = useState(false);
    const [showDeleteDialog, setShowDeleteDialog] = useState(false);
    const [feedback, setFeedback] = useState<{ msg: string; type: 'success' | 'error' } | null>(null);

    const showFeedback = (msg: string, type: 'success' | 'error' = 'success') => {
        setFeedback({ msg, type });
        setTimeout(() => setFeedback(null), 3000);
    };

    useEffect(() => {
        if (user) fetchReferralData();
    }, [user]);

    const fetchReferralData = async () => {
        try {
            const res = await fetch('/api/referral');
            if (res.ok) {
                const data = await res.json();
                setReferralData(data);
                setHasReferrer(!!data.referred_by);
            }
        } catch (err) {
            console.error('Error fetching referral data:', err);
        }
    };

    const handleApplyCode = async () => {
        if (!inputCode.trim()) return;
        setApplying(true);
        try {
            const res = await fetch('/api/referral', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ code: inputCode }),
            });
            if (res.ok) {
                showFeedback('¡Código aplicado con éxito!');
                setHasReferrer(true);
            } else {
                const msg = await res.text();
                showFeedback(`Error: ${msg}`, 'error');
            }
        } catch (err) {
            showFeedback('Error al aplicar el código', 'error');
        } finally {
            setApplying(false);
        }
    };

    const handleCheckout = async (plan: 'pro') => {
        try {
            const res = await fetch('/api/stripe/checkout', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ plan }),
            });
            const data = await res.json();
            if (data.url) window.location.href = data.url;
        } catch {
            showFeedback('Error al procesar el pago. Inténtalo de nuevo.', 'error');
        }
    };

    const handleNotificationsToggle = () => {
        const next = !notificationsEnabled;
        setNotificationsEnabled(next);
        showFeedback(next ? 'Notificaciones activadas' : 'Notificaciones desactivadas');
    };

    const handleDeleteAccount = async () => {
        if (!user) return;
        try {
            await supabase.auth.signOut();
            showFeedback('Cuenta eliminada. Redirigiendo...');
            setTimeout(() => router.push('/login'), 2000);
            setShowDeleteDialog(false);
        } catch {
            showFeedback('Error al eliminar la cuenta', 'error');
        }
    };

    const handleDisconnectGoogle = async () => {
        await supabase.auth.signOut();
        router.push('/login');
    };

    const handleConnectGoogle = async () => {
        await supabase.auth.signInWithOAuth({
            provider: 'google',
            options: {
                scopes: 'openid email profile https://www.googleapis.com/auth/calendar https://www.googleapis.com/auth/gmail.modify https://www.googleapis.com/auth/contacts',
                queryParams: { access_type: 'offline', prompt: 'consent' },
                redirectTo: `${location.origin}/auth/callback`,
            },
        });
    };

    return (
        <div className="h-full bg-slate-50 p-6 md:p-8 overflow-y-auto relative">
            {/* Feedback banner */}
            {feedback && (
                <div className={`fixed top-16 left-1/2 -translate-x-1/2 z-50 px-5 py-2 rounded-full shadow-lg text-sm font-medium text-white transition-all ${
                    feedback.type === 'success' ? 'bg-green-500' : 'bg-red-500'
                }`}>
                    {feedback.msg}
                </div>
            )}

            <h2 className="text-3xl font-bold text-slate-800 mb-8">Configuración</h2>

            {/* Integrations */}
            <div className="mb-10">
                <h3 className="text-lg font-semibold text-slate-700 mb-3">Integraciones</h3>
                <div className="bg-white p-5 rounded-2xl shadow-sm border border-slate-200">
                    <div className="flex items-center justify-between gap-4">
                        <div className="flex items-center gap-4">
                            <div className="w-11 h-11 bg-white border border-slate-200 rounded-full flex items-center justify-center p-2 shadow-sm shrink-0">
                                <svg viewBox="0 0 24 24" className="w-6 h-6">
                                    <path d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z" fill="#4285F4" />
                                    <path d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z" fill="#34A853" />
                                    <path d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z" fill="#FBBC05" />
                                    <path d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z" fill="#EA4335" />
                                </svg>
                            </div>
                            <div>
                                <h4 className="font-bold text-slate-800">Google Workspace</h4>
                                <p className="text-sm text-slate-500">Calendario, Gmail y Contactos</p>
                            </div>
                        </div>

                        {loading ? (
                            <Loader2 className="w-5 h-5 animate-spin text-slate-400 shrink-0" />
                        ) : user ? (
                            <div className="flex items-center gap-3 shrink-0">
                                <div className="text-right hidden sm:block">
                                    <p className="text-xs font-medium text-green-600">Conectado</p>
                                    <p className="text-xs text-slate-500 truncate max-w-[140px]">{user.email}</p>
                                </div>
                                <button
                                    onClick={handleDisconnectGoogle}
                                    className="px-3 py-1.5 bg-red-50 text-red-600 rounded-lg text-xs font-medium hover:bg-red-100 transition-colors"
                                >
                                    Desconectar
                                </button>
                            </div>
                        ) : (
                            <button
                                onClick={handleConnectGoogle}
                                className="px-4 py-2 bg-blue-600 text-white rounded-lg text-sm font-bold hover:bg-blue-700 shadow-sm transition-all shrink-0"
                            >
                                Conectar
                            </button>
                        )}
                    </div>
                </div>
            </div>

            {/* Referrals */}
            <div className="mb-10">
                <h3 className="text-lg font-semibold text-slate-700 mb-3">Sistema de Referidos</h3>
                <div className="bg-gradient-to-r from-blue-600 to-indigo-700 p-6 rounded-2xl shadow-lg text-white">
                    <div className="grid md:grid-cols-2 gap-6 items-center">
                        <div>
                            <h4 className="text-xl font-bold mb-2">¡Gana 1€ por amigo!</h4>
                            <p className="text-blue-100 mb-4 text-sm opacity-90">
                                Comparte tu código y gana saldo cuando un amigo se registre y pague.
                            </p>
                            <div className="flex items-center gap-2 bg-white/10 p-2 rounded-xl border border-white/20">
                                <code className="flex-1 font-mono text-xl font-bold text-center tracking-widest">
                                    {referralData.code || '------'}
                                </code>
                                <button
                                    onClick={() => {
                                        navigator.clipboard.writeText(referralData.code);
                                        showFeedback('Código copiado al portapapeles');
                                    }}
                                    className="px-3 py-2 bg-white text-blue-600 rounded-lg text-sm font-bold hover:bg-blue-50 transition-colors"
                                >
                                    Copiar
                                </button>
                            </div>
                        </div>
                        <div className="bg-white/10 p-5 rounded-xl border border-white/20">
                            <div className="flex justify-between items-center mb-4">
                                <div>
                                    <p className="text-xs text-blue-200">Saldo</p>
                                    <p className="text-2xl font-bold">{referralData.balance} €</p>
                                </div>
                                <div className="text-right">
                                    <p className="text-xs text-blue-200">Referidos</p>
                                    <p className="text-2xl font-bold">{referralData.count}</p>
                                </div>
                            </div>
                            {!hasReferrer && (
                                <div className="pt-4 border-t border-white/10">
                                    <p className="text-xs text-blue-200 mb-2">¿Te han invitado? Introduce el código:</p>
                                    <div className="flex gap-2">
                                        <input
                                            type="text"
                                            placeholder="Código..."
                                            value={inputCode}
                                            onChange={(e) => setInputCode(e.target.value.toUpperCase())}
                                            className="bg-white/10 border border-white/20 rounded-lg px-3 py-2 flex-1 outline-none focus:ring-2 ring-white/50 text-sm"
                                        />
                                        <button
                                            onClick={handleApplyCode}
                                            disabled={applying}
                                            className="px-4 py-2 bg-white text-blue-600 rounded-lg text-sm font-bold hover:bg-blue-50 transition-colors disabled:opacity-50"
                                        >
                                            {applying ? '...' : 'Aplicar'}
                                        </button>
                                    </div>
                                </div>
                            )}
                        </div>
                    </div>
                </div>
            </div>

            {/* General */}
            <h3 className="text-lg font-semibold text-slate-700 mb-3">General</h3>
            <div className="bg-white rounded-2xl shadow-sm border border-slate-200 overflow-hidden mb-10">
                <div onClick={handleNotificationsToggle} className="p-4 border-b border-slate-100 flex items-center justify-between hover:bg-slate-50 transition-colors cursor-pointer">
                    <div className="flex items-center gap-3">
                        <div className="w-8 h-8 rounded-full bg-blue-50 text-blue-600 flex items-center justify-center">
                            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9" /></svg>
                        </div>
                        <span className="font-medium text-slate-700">Notificaciones</span>
                    </div>
                    <div className={`relative inline-flex h-6 w-11 items-center rounded-full transition-colors ${notificationsEnabled ? 'bg-blue-600' : 'bg-slate-200'}`}>
                        <span className={`inline-block h-4 w-4 transform rounded-full bg-white shadow transition ${notificationsEnabled ? 'translate-x-6' : 'translate-x-1'}`} />
                    </div>
                </div>
                <div onClick={() => setShowPrivacyDialog(true)} className="p-4 border-b border-slate-100 flex items-center justify-between hover:bg-slate-50 transition-colors cursor-pointer">
                    <div className="flex items-center gap-3">
                        <div className="w-8 h-8 rounded-full bg-indigo-50 text-indigo-600 flex items-center justify-center">
                            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z" /></svg>
                        </div>
                        <span className="font-medium text-slate-700">Privacidad y Seguridad</span>
                    </div>
                    <svg className="w-5 h-5 text-slate-400" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7" /></svg>
                </div>
                <div onClick={() => setShowDeleteDialog(true)} className="p-4 flex items-center justify-between hover:bg-red-50 transition-colors cursor-pointer">
                    <div className="flex items-center gap-3">
                        <div className="w-8 h-8 rounded-full bg-red-50 text-red-600 flex items-center justify-center">
                            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" /></svg>
                        </div>
                        <span className="font-medium text-red-600">Eliminar Cuenta</span>
                    </div>
                    <svg className="w-5 h-5 text-red-400" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7" /></svg>
                </div>
            </div>

            {/* Premium */}
            <h3 className="text-lg font-semibold text-slate-700 mb-3">Suscripción</h3>
            <div className="max-w-md">
                <div className="bg-gradient-to-br from-slate-900 to-blue-900 p-7 rounded-2xl shadow-xl relative overflow-hidden">
                    <div className="absolute top-0 right-0 bg-gradient-to-l from-purple-600 to-blue-600 text-white px-4 py-1 rounded-bl-xl text-xs font-bold">
                        7 DÍAS GRATIS
                    </div>
                    <div className="mb-3">
                        <span className="bg-blue-500/20 text-blue-300 px-3 py-1 rounded-full text-xs font-bold uppercase tracking-wide">Plan Premium</span>
                    </div>
                    <h3 className="text-3xl font-bold text-white mb-1">Alfred Premium</h3>
                    <p className="text-slate-300 text-sm mb-5">Potencia ilimitada para profesionales.</p>
                    <ul className="space-y-2 mb-6 text-sm">
                        {['IA Avanzada sin límites', 'Integración Google completa', 'Redacción de Emails', 'Calendario y Tareas', 'Notas con Audio', 'Plantillas Ilimitadas', 'Soporte Prioritario'].map(f => (
                            <li key={f} className="flex items-center text-slate-200 gap-2">
                                <span className="text-blue-400">✓</span> {f}
                            </li>
                        ))}
                    </ul>
                    <button
                        onClick={() => handleCheckout('pro')}
                        className="w-full py-3 rounded-xl bg-gradient-to-r from-blue-600 to-purple-600 text-white font-bold hover:shadow-lg hover:shadow-blue-900/50 transition-all text-sm"
                    >
                        Prueba gratis 7 días
                    </button>
                    <p className="text-xs text-slate-500 text-center mt-2">Cancela cuando quieras.</p>
                </div>
            </div>

            {/* Privacy Dialog */}
            {showPrivacyDialog && (
                <div className="fixed inset-0 bg-black/50 backdrop-blur-sm flex items-center justify-center z-50 p-4" onClick={() => setShowPrivacyDialog(false)}>
                    <div className="bg-white rounded-2xl p-6 max-w-md w-full shadow-xl" onClick={(e) => e.stopPropagation()}>
                        <h3 className="text-xl font-bold text-slate-900 mb-4">Privacidad y Seguridad</h3>
                        <div className="space-y-3 text-sm text-slate-600 mb-6">
                            <p>• Tus datos están encriptados y protegidos</p>
                            <p>• No compartimos tu información con terceros</p>
                            <p>• Puedes exportar tus datos en cualquier momento</p>
                            <p>• Cumplimos con GDPR y regulaciones de privacidad</p>
                        </div>
                        <button onClick={() => setShowPrivacyDialog(false)} className="w-full py-3 bg-blue-600 text-white rounded-xl font-medium hover:bg-blue-700 transition-colors">
                            Entendido
                        </button>
                    </div>
                </div>
            )}

            {/* Delete Dialog */}
            {showDeleteDialog && (
                <div className="fixed inset-0 bg-black/50 backdrop-blur-sm flex items-center justify-center z-50 p-4" onClick={() => setShowDeleteDialog(false)}>
                    <div className="bg-white rounded-2xl p-6 max-w-md w-full shadow-xl" onClick={(e) => e.stopPropagation()}>
                        <h3 className="text-xl font-bold text-red-600 mb-3">¿Eliminar Cuenta?</h3>
                        <p className="text-slate-600 text-sm mb-6">
                            Esta acción es permanente. Se eliminarán todos tus datos, notas, eventos y configuraciones.
                        </p>
                        <div className="flex gap-3">
                            <button onClick={() => setShowDeleteDialog(false)} className="flex-1 py-3 bg-slate-100 text-slate-700 rounded-xl font-medium hover:bg-slate-200 transition-colors">
                                Cancelar
                            </button>
                            <button onClick={handleDeleteAccount} className="flex-1 py-3 bg-red-600 text-white rounded-xl font-medium hover:bg-red-700 transition-colors">
                                Eliminar
                            </button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
}
