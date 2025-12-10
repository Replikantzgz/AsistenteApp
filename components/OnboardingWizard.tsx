"use client";

import { useState, useEffect } from 'react';
import { useStore } from '@/store/useStore';
import { signIn, useSession } from 'next-auth/react';
import { Check, ChevronRight, Key, Shield, Sparkles, Zap } from 'lucide-react';

export default function OnboardingWizard() {
    const { onboardingCompleted, setOnboardingCompleted, setPlan, setApiKeys } = useStore();
    const { data: session } = useSession();
    const [step, setStep] = useState(1);
    const [selectedPlan, setSelectedPlan] = useState<'eco' | 'pro' | null>(null);
    const [geminiKey, setGeminiKey] = useState('');
    const [geminiProKey, setGeminiProKey] = useState('');
    const [useFallback, setUseFallback] = useState(false);

    const [mounted, setMounted] = useState(false);
    useEffect(() => {
        setMounted(true);
    }, []);

    if (!mounted) return null;
    if (onboardingCompleted) return null;

    const handleNext = async () => {
        if (step === 1) setStep(2);
        else if (step === 2) {
            if (session) setStep(3);
            else signIn('google');
        } else if (step === 3) {
            if (selectedPlan) {
                // Stripe Checkout Logic
                try {
                    const response = await fetch('/api/stripe/checkout', {
                        method: 'POST',
                        headers: { 'Content-Type': 'application/json' },
                        body: JSON.stringify({ plan: selectedPlan }),
                    });

                    if (response.ok) {
                        const data = await response.json();
                        if (data.url) {
                            window.location.href = data.url;
                            return;
                        }
                    } else {
                        console.error("Checkout failed");
                        // Fallback for demo/dev without Stripe keys set up
                        setPlan(selectedPlan);
                        setStep(4);
                    }
                } catch (error) {
                    console.error("Checkout Error:", error);
                    // Fallback
                    setPlan(selectedPlan);
                    setStep(4);
                }
            }
        } else if (step === 4) {
            setApiKeys({
                gemini: geminiKey,
                geminiPro: geminiProKey,
            });
            setOnboardingCompleted(true);
        }
    };

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/80 backdrop-blur-sm p-4">
            <div className="bg-zinc-900 border border-zinc-800 rounded-2xl shadow-2xl w-full max-w-2xl overflow-hidden flex flex-col max-h-[90vh]">

                {/* Header */}
                <div className="p-6 border-b border-zinc-800 flex justify-between items-center bg-zinc-950/50">
                    <div className="flex items-center gap-2">
                        <div className="w-8 h-8 bg-blue-600 rounded-lg flex items-center justify-center">
                            <Sparkles className="w-5 h-5 text-white" />
                        </div>
                        <h1 className="text-xl font-bold text-white">Configuración Inicial</h1>
                    </div>
                    <div className="flex gap-1">
                        {[1, 2, 3, 4].map((s) => (
                            <div
                                key={s}
                                className={`h-1.5 w-8 rounded-full transition-colors ${s <= step ? 'bg-blue-600' : 'bg-zinc-800'}`}
                            />
                        ))}
                    </div>
                </div>

                {/* Content */}
                <div className="p-8 overflow-y-auto flex-1">

                    {step === 1 && (
                        <div className="space-y-6 text-center py-8">
                            <h2 className="text-3xl font-bold text-white">Bienvenido a IATUALCANCE</h2>
                            <p className="text-zinc-400 text-lg max-w-md mx-auto">
                                Tu asistente personal inteligente. Antes de empezar, necesitamos configurar algunas cosas para personalizar tu experiencia.
                            </p>
                            <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mt-8">
                                <FeatureCard icon={Zap} title="Rápido" desc="Respuestas instantáneas y acciones fluidas." />
                                <FeatureCard icon={Shield} title="Seguro" desc="Tus datos y claves se guardan localmente." />
                                <FeatureCard icon={Sparkles} title="Inteligente" desc="Potenciado por Gemini y GPT-4." />
                            </div>
                        </div>
                    )}

                    {step === 2 && (
                        <div className="space-y-6 text-center py-8">
                            <h2 className="text-2xl font-bold text-white">Conecta tu cuenta</h2>
                            <p className="text-zinc-400">
                                Para gestionar tu calendario, correos y tareas, necesitamos acceso a tu cuenta de Google.
                            </p>

                            {session ? (
                                <div className="bg-green-500/10 border border-green-500/20 p-4 rounded-xl flex items-center justify-center gap-3 text-green-400">
                                    <Check className="w-5 h-5" />
                                    <span>Conectado como {session.user?.email}</span>
                                </div>
                            ) : (
                                <div className="flex justify-center py-4">
                                    <button
                                        onClick={() => signIn('google')}
                                        className="flex items-center gap-3 bg-white text-black px-6 py-3 rounded-xl font-medium hover:bg-zinc-200 transition-colors"
                                    >
                                        <svg className="w-5 h-5" viewBox="0 0 24 24">
                                            <path fill="currentColor" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z" />
                                            <path fill="currentColor" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z" />
                                            <path fill="currentColor" d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z" />
                                            <path fill="currentColor" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z" />
                                        </svg>
                                        Iniciar sesión con Google
                                    </button>
                                </div>
                            )}
                        </div>
                    )}

                    {step === 3 && (
                        <div className="space-y-6">
                            <h2 className="text-2xl font-bold text-white text-center">Elige tu Plan</h2>
                            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                                <PlanCard
                                    title="Plan Basic"
                                    price="Gratis"
                                    features={['Gemini Flash', 'Gestión básica', 'Tu propia API Key']}
                                    selected={selectedPlan === 'eco'}
                                    onClick={() => setSelectedPlan('eco')}
                                />
                                <PlanCard
                                    title="Plan Pro"
                                    price="Premium"
                                    features={['Gemini Pro / GPT-4', 'Prioridad alta', 'Soporte avanzado', 'Fallback a ChatGPT']}
                                    selected={selectedPlan === 'pro'}
                                    onClick={() => setSelectedPlan('pro')}
                                    highlight
                                />
                            </div>
                        </div>
                    )}

                    {step === 4 && (
                        <div className="space-y-6">
                            <h2 className="text-2xl font-bold text-white text-center">Configuración de IA</h2>

                            {selectedPlan === 'eco' ? (
                                <div className="space-y-4">
                                    <div className="bg-blue-500/10 border border-blue-500/20 p-4 rounded-xl">
                                        <h3 className="font-medium text-blue-400 mb-2">Gemini API Key</h3>
                                        <p className="text-sm text-zinc-400 mb-4">
                                            Para usar el plan gratuito, necesitas tu propia API Key de Google Gemini.
                                            <a href="https://aistudio.google.com/app/apikey" target="_blank" className="text-blue-400 underline ml-1">Obtener aquí</a>
                                        </p>
                                        <div className="relative">
                                            <Key className="absolute left-3 top-3 w-5 h-5 text-zinc-500" />
                                            <input
                                                type="password"
                                                placeholder="Pegar API Key aquí..."
                                                value={geminiKey}
                                                onChange={(e) => setGeminiKey(e.target.value)}
                                                className="w-full bg-black/50 border border-zinc-700 rounded-lg py-2.5 pl-10 pr-4 text-white focus:outline-none focus:border-blue-500 transition-colors"
                                            />
                                        </div>
                                    </div>
                                </div>
                            ) : (
                                <div className="space-y-4">
                                    <div className="bg-purple-500/10 border border-purple-500/20 p-4 rounded-xl">
                                        <h3 className="font-medium text-purple-400 mb-2">Gemini Pro API Key (Opcional)</h3>
                                        <p className="text-sm text-zinc-400 mb-4">
                                            Si tienes una clave de Gemini Pro, ingrésala aquí. Si no, usaremos nuestro sistema de respaldo.
                                        </p>
                                        <div className="relative">
                                            <Key className="absolute left-3 top-3 w-5 h-5 text-zinc-500" />
                                            <input
                                                type="password"
                                                placeholder="Pegar API Key aquí..."
                                                value={geminiProKey}
                                                onChange={(e) => setGeminiProKey(e.target.value)}
                                                className="w-full bg-black/50 border border-zinc-700 rounded-lg py-2.5 pl-10 pr-4 text-white focus:outline-none focus:border-purple-500 transition-colors"
                                            />
                                        </div>
                                    </div>

                                    {!geminiProKey && (
                                        <div className="bg-zinc-800/50 p-4 rounded-xl border border-zinc-700">
                                            <div className="flex items-center gap-3">
                                                <input
                                                    type="checkbox"
                                                    id="fallback"
                                                    checked={useFallback}
                                                    onChange={(e) => setUseFallback(e.target.checked)}
                                                    className="w-5 h-5 rounded border-zinc-600 bg-zinc-900 text-blue-600 focus:ring-blue-500"
                                                />
                                                <label htmlFor="fallback" className="text-sm text-zinc-300 cursor-pointer select-none">
                                                    No tengo clave. Usar ChatGPT (OpenAI) con límites de uso del plan.
                                                </label>
                                            </div>
                                        </div>
                                    )}
                                </div>
                            )}
                        </div>
                    )}

                </div>

                {/* Footer */}
                <div className="p-6 border-t border-zinc-800 bg-zinc-950/50 flex justify-between items-center">
                    {step > 1 && (
                        <button
                            onClick={() => setStep(step - 1)}
                            className="text-zinc-400 hover:text-white transition-colors"
                        >
                            Atrás
                        </button>
                    )}
                    <div className="flex-1"></div>
                    <button
                        onClick={handleNext}
                        disabled={
                            (step === 2 && !session) ||
                            (step === 3 && !selectedPlan) ||
                            (step === 4 && selectedPlan === 'eco' && !geminiKey) ||
                            (step === 4 && selectedPlan === 'pro' && !geminiProKey && !useFallback)
                        }
                        className="flex items-center gap-2 bg-blue-600 hover:bg-blue-500 disabled:bg-zinc-800 disabled:text-zinc-500 text-white px-6 py-2.5 rounded-lg font-medium transition-all"
                    >
                        {step === 4 ? 'Finalizar' : 'Continuar'}
                        <ChevronRight className="w-4 h-4" />
                    </button>
                </div>

            </div>
        </div>
    );
}

function FeatureCard({ icon: Icon, title, desc }: { icon: any, title: string, desc: string }) {
    return (
        <div className="bg-zinc-800/30 p-4 rounded-xl border border-zinc-800 flex flex-col items-center text-center">
            <div className="w-10 h-10 bg-zinc-800 rounded-full flex items-center justify-center mb-3">
                <Icon className="w-5 h-5 text-zinc-400" />
            </div>
            <h3 className="font-medium text-white mb-1">{title}</h3>
            <p className="text-xs text-zinc-400">{desc}</p>
        </div>
    );
}

function PlanCard({ title, price, features, selected, onClick, highlight }: any) {
    return (
        <div
            onClick={onClick}
            className={`relative p-6 rounded-xl border-2 cursor-pointer transition-all ${selected
                    ? 'border-blue-500 bg-blue-500/5'
                    : 'border-zinc-800 bg-zinc-900/50 hover:border-zinc-700'
                }`}
        >
            {highlight && (
                <div className="absolute -top-3 left-1/2 -translate-x-1/2 bg-gradient-to-r from-blue-600 to-purple-600 text-white text-xs font-bold px-3 py-1 rounded-full">
                    RECOMENDADO
                </div>
            )}
            <h3 className="text-xl font-bold text-white mb-1">{title}</h3>
            <div className="text-2xl font-bold text-zinc-200 mb-4">{price}</div>
            <ul className="space-y-2">
                {features.map((f: string, i: number) => (
                    <li key={i} className="flex items-center gap-2 text-sm text-zinc-400">
                        <Check className="w-4 h-4 text-green-500" />
                        {f}
                    </li>
                ))}
            </ul>
        </div>
    );
}
