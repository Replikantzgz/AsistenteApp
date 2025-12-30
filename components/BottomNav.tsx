'use client';

import { useStore, ViewType } from '@/store';
import { Calendar, CheckSquare, Mail, Users, MessageSquare } from 'lucide-react';
import clsx from 'clsx';
import { motion } from 'framer-motion';

export default function BottomNav() {
    const { currentView, setView } = useStore();

    const tabs: { id: ViewType; icon: any; label: string }[] = [
        { id: 'calendar', icon: Calendar, label: 'Calendario' },
        { id: 'notes', icon: CheckSquare, label: 'Notas' },
        { id: 'chat', icon: MessageSquare, label: 'IA' }, // Center item
        { id: 'emails', icon: Mail, label: 'Email' },
        { id: 'contacts', icon: Users, label: 'Contactos' }, // Contacts view to be created
    ];

    return (
        <nav className="fixed bottom-0 left-0 right-0 bg-white/90 backdrop-blur-xl border-t border-slate-200 pb-[calc(env(safe-area-inset-bottom)+16px)] pt-3 px-6 z-50 lg:hidden shadow-[0_-4px_6px_-1px_rgba(0,0,0,0.05)]">
            <div className="flex items-center justify-between max-w-md mx-auto h-16">
                {tabs.map((tab, index) => {
                    const isCenter = tab.id === 'chat';
                    const isActive = currentView === tab.id;

                    if (isCenter) {
                        return (
                            <div key={tab.id} className="relative -top-6">
                                <motion.button
                                    whileTap={{ scale: 0.9 }}
                                    onClick={() => setView('chat')}
                                    className={clsx(
                                        "w-16 h-16 rounded-full flex items-center justify-center shadow-lg shadow-blue-500/30 transition-all",
                                        isActive
                                            ? "bg-gradient-to-r from-blue-500 to-cyan-400 text-white"
                                            : "bg-gradient-to-r from-blue-600 to-indigo-600 text-white"
                                    )}
                                >
                                    <MessageSquare className="w-8 h-8" />
                                </motion.button>
                            </div>
                        );
                    }

                    return (
                        <button
                            key={tab.id}
                            onClick={() => setView(tab.id)}
                            className={clsx(
                                "flex flex-col items-center justify-center w-12 h-12 transition-colors",
                                isActive ? "text-blue-600" : "text-slate-400 hover:text-slate-600"
                            )}
                        >
                            <tab.icon className="w-6 h-6" />
                            <span className="text-[10px] font-medium mt-1">{tab.label}</span>
                        </button>
                    );
                })}
            </div>
        </nav>
    );
}
