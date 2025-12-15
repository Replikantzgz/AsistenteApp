'use client';

import { signIn, signOut, useSession } from 'next-auth/react';
import { Loader2 } from 'lucide-react';

export default function SettingsView() {
    const { data: session, status } = useSession();
    const loading = status === 'loading';

    return (
        <div className="h-full bg-slate-50 p-8 overflow-y-auto">
            <h2 className="text-3xl font-bold text-slate-800 mb-8">Configuración</h2>

            {/* Integrations Section */}
            <div className="mb-12">
                <h3 className="text-xl font-semibold text-slate-700 mb-4">Integraciones</h3>
                <div className="bg-white p-6 rounded-2xl shadow-sm border border-slate-200">
                    <div className="flex items-center justify-between">
                        <div className="flex items-center gap-4">
                            <div className="w-12 h-12 bg-white border border-slate-200 rounded-full flex items-center justify-center p-2 shadow-sm">
                                <svg viewBox="0 0 24 24" className="w-6 h-6">
                                    <path
                                        d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"
                                        fill="#4285F4"
                                    />
                                    <path
                                        d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"
                                        fill="#34A853"
                                    />
                                    <path
                                        d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z"
                                        fill="#FBBC05"
                                    />
                                    <path
                                        d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z"
                                        fill="#EA4335"
                                    />
                                </svg>
                            </div>
                            <div>
                                <h4 className="font-bold text-slate-800">Google Workspace</h4>
                                <p className="text-sm text-slate-500">Conecta tu Calendario, Gmail y Contactos</p>
                            </div>
                        </div>

                        {loading ? (
                            <Loader2 className="w-5 h-5 animate-spin text-slate-400" />
                        ) : session ? (
                            <div className="flex items-center gap-4">
                                <div className="text-right hidden sm:block">
                                    <p className="text-sm font-medium text-slate-900">Conectado como</p>
                                    <p className="text-xs text-slate-500">{session.user?.email}</p>
                                </div>
                                <button
                                    onClick={() => signOut()}
                                    className="px-4 py-2 bg-red-50 text-red-600 rounded-lg text-sm font-medium hover:bg-red-100 transition-colors"
                                >
                                    Desconectar
                                </button>
                            </div>
                        ) : (
                            <button
                                onClick={() => signIn('google')}
                                className="px-5 py-2.5 bg-blue-600 text-white rounded-lg text-sm font-bold hover:bg-blue-700 shadow-sm transition-all"
                            >
                                Conectar
                            </button>
                        )}
                    </div>
                </div>
            </div>

            <h2 className="text-2xl font-bold text-slate-800 mb-6">Planes y Suscripción</h2>

            <div className="grid md:grid-cols-2 gap-8 max-w-4xl mx-auto">
                {/* Eco Plan */}
                <div className="bg-white p-8 rounded-2xl shadow-sm border border-slate-200 flex flex-col">
                    <div className="mb-4">
                        <span className="bg-green-100 text-green-700 px-3 py-1 rounded-full text-xs font-bold uppercase tracking-wide">Plan Eco</span>
                    </div>
                    <h3 className="text-4xl font-bold text-slate-900 mb-2">3,99€ <span className="text-lg font-normal text-slate-500">/mes</span></h3>
                    <p className="text-slate-500 mb-6">Ideal para organizar tu día a día.</p>
                    <ul className="space-y-3 mb-8 flex-1">
                        <li className="flex items-center text-slate-600">
                            <span className="text-green-500 mr-2">✓</span> IA Básica
                        </li>
                        <li className="flex items-center text-slate-600">
                            <span className="text-green-500 mr-2">✓</span> 50 comandos/día
                        </li>
                        <li className="flex items-center text-slate-600">
                            <span className="text-green-500 mr-2">✓</span> Calendario y Tareas
                        </li>
                    </ul>
                    <button className="w-full py-3 rounded-xl border-2 border-slate-200 text-slate-700 font-bold hover:border-slate-800 hover:text-slate-900 transition-all">
                        Elegir Eco
                    </button>
                </div>

                {/* Pro Plan */}
                <div className="bg-slate-900 p-8 rounded-2xl shadow-xl flex flex-col relative overflow-hidden">
                    <div className="absolute top-0 right-0 bg-gradient-to-l from-purple-600 to-blue-600 text-white px-4 py-1 rounded-bl-xl text-xs font-bold">
                        RECOMENDADO
                    </div>
                    <div className="mb-4">
                        <span className="bg-blue-500/20 text-blue-300 px-3 py-1 rounded-full text-xs font-bold uppercase tracking-wide">Plan Pro</span>
                    </div>
                    <h3 className="text-4xl font-bold text-white mb-2">8,99€ <span className="text-lg font-normal text-slate-400">/mes</span></h3>
                    <p className="text-slate-400 mb-6">Potencia ilimitada para profesionales.</p>
                    <ul className="space-y-3 mb-8 flex-1">
                        <li className="flex items-center text-slate-300">
                            <span className="text-blue-400 mr-2">✓</span> IA Avanzada (GPT-4)
                        </li>
                        <li className="flex items-center text-slate-300">
                            <span className="text-blue-400 mr-2">✓</span> Comandos Ilimitados
                        </li>
                        <li className="flex items-center text-slate-300">
                            <span className="text-blue-400 mr-2">✓</span> Redacción de Emails
                        </li>
                        <li className="flex items-center text-slate-300">
                            <span className="text-blue-400 mr-2">✓</span> Plantillas Ilimitadas
                        </li>
                    </ul>
                    <button className="w-full py-3 rounded-xl bg-gradient-to-r from-blue-600 to-purple-600 text-white font-bold hover:shadow-lg hover:shadow-blue-900/50 transition-all">
                        Prueba gratis 7 días
                    </button>
                </div>
            </div>
        </div>
    );
}
