'use client';

import { useState, useEffect } from 'react';
import { useStore } from '@/store';
import { motion, AnimatePresence } from 'framer-motion';
import { Bell, Check, ChevronRight, Shield, Mail } from 'lucide-react';
import { createClient } from '@/lib/supabase/client';

export default function OnboardingWizard({ onComplete }: { onComplete: () => void }) {
    const [step, setStep] = useState(1);
    const [notificationsEnabled, setNotificationsEnabled] = useState(false);
    const [nameInput, setNameInput] = useState('');
    const { setUserName } = useStore();
    const supabase = createClient();

    const requestNotifications = async () => {
        if ('Notification' in window) {
            const permission = await Notification.requestPermission();
            if (permission === 'granted') {
                setNotificationsEnabled(true);
                // Here we would subscribe the user to push notifications
            }
        }
    };

    const handleNext = () => {
        if (step === 1) {
            if (!nameInput.trim()) return; // Require name
            setUserName(nameInput);
            localStorage.setItem('alfred_user_name', nameInput);
        }
        if (step < 3) setStep(step + 1);
        else onComplete();
    };

    return (
        <div className="fixed inset-0 z-[100] bg-slate-900/90 flex items-center justify-center p-4 backdrop-blur-sm">
            <motion.div
                initial={{ scale: 0.9, opacity: 0 }}
                animate={{ scale: 1, opacity: 1 }}
                className="bg-white w-full max-w-md rounded-3xl overflow-hidden shadow-2xl"
            >
                <div className="h-2 bg-slate-100 w-full">
                    <motion.div
                        className="h-full bg-blue-600"
                        animate={{ width: `${(step / 3) * 100}%` }}
                    />
                </div>

                <div className="p-8">
                    <AnimatePresence mode="wait">
                        {step === 1 && (
                            <motion.div
                                key="step1"
                                initial={{ x: 20, opacity: 0 }}
                                animate={{ x: 0, opacity: 1 }}
                                exit={{ x: -20, opacity: 0 }}
                                className="text-center"
                            >
                                <div className="w-20 h-20 bg-blue-100 rounded-full flex items-center justify-center mx-auto mb-6">
                                    <span className="text-4xl">👋</span>
                                </div>
                                <h2 className="text-2xl font-bold text-slate-900 mb-2">Hola, soy Alfred</h2>
                                <p className="text-slate-500 mb-6">
                                    Tu asistente personal inteligente. ¿Cómo te gustaría que te llame?
                                </p>
                                <input
                                    type="text"
                                    placeholder="Tu nombre..."
                                    value={nameInput}
                                    onChange={(e) => setNameInput(e.target.value)}
                                    className="w-full bg-slate-50 border border-slate-200 rounded-xl px-4 py-3 text-lg text-center font-medium focus:ring-2 ring-blue-500 outline-none transition-all"
                                    autoFocus
                                />
                            </motion.div>
                        )}

                        {step === 2 && (
                            <motion.div
                                key="step2"
                                initial={{ x: 20, opacity: 0 }}
                                animate={{ x: 0, opacity: 1 }}
                                exit={{ x: -20, opacity: 0 }}
                                className="text-center"
                            >
                                <div className="w-20 h-20 bg-yellow-100 rounded-full flex items-center justify-center mx-auto mb-6">
                                    <Bell className="w-10 h-10 text-yellow-600" />
                                </div>
                                <h2 className="text-2xl font-bold text-slate-900 mb-2">Alertas Importantes</h2>
                                <p className="text-slate-500 mb-6">
                                    Necesito permiso para avisarte de tus próximas citas y tareas urgentes.
                                </p>
                                <button
                                    onClick={requestNotifications}
                                    className={`
                                        w-full py-3 rounded-xl font-medium flex items-center justify-center gap-2 transition-all mb-4
                                        ${notificationsEnabled
                                            ? 'bg-green-100 text-green-700'
                                            : 'bg-slate-100 text-slate-700 hover:bg-slate-200'}
                                    `}
                                >
                                    {notificationsEnabled ? (
                                        <><Check className="w-5 h-5" /> Alertas Activadas</>
                                    ) : (
                                        'Activar Alertas'
                                    )}
                                </button>
                            </motion.div>
                        )}

                        {step === 3 && (
                            <motion.div
                                key="step3"
                                initial={{ x: 20, opacity: 0 }}
                                animate={{ x: 0, opacity: 1 }}
                                exit={{ x: -20, opacity: 0 }}
                                className="text-center"
                            >
                                <div className="w-20 h-20 bg-purple-100 rounded-full flex items-center justify-center mx-auto mb-6">
                                    <Mail className="w-10 h-10 text-purple-600" />
                                </div>
                                <h2 className="text-2xl font-bold text-slate-900 mb-2">Conectar Google</h2>
                                <p className="text-slate-500 mb-6">
                                    Para gestionar tu calendario y correos, necesito acceso a tu cuenta de Google.
                                </p>
                                <div className="bg-orange-50 p-4 rounded-xl text-orange-700 text-sm mb-4">
                                    ⚠️ Configuración pendiente en Panel de Control
                                </div>
                            </motion.div>
                        )}
                    </AnimatePresence>

                    <button
                        onClick={handleNext}
                        className="w-full bg-blue-600 text-white py-4 rounded-xl font-bold text-lg hover:bg-blue-700 transition-all flex items-center justify-center gap-2 mt-4"
                    >
                        {step === 3 ? 'Empezar' : 'Siguiente'} <ChevronRight className="w-5 h-5" />
                    </button>
                </div>
            </motion.div>
        </div>
    );
}
