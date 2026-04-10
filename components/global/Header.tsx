'use client';

import { useStore } from '@/store';
import { Settings, Bell, Search, Menu, Plus } from 'lucide-react';
import clsx from 'clsx';
import { useState } from 'react';

export default function Header({ onMenuClick }: { onMenuClick?: () => void }) {
    const { currentView, setView } = useStore();
    const [notificationsEnabled, setNotificationsEnabled] = useState(true);

    const titles: Record<string, string> = {
        chat: 'Asistente IA',
        calendar: 'Mi Agenda',
        notes: 'Mis Notas',
        emails: 'Bandeja de Entrada',
        templates: 'Plantillas',
        contacts: 'Contactos',
        settings: 'Configuración',
    };

    const title = titles[currentView] || 'Alfred';

    return (
        <header className="h-14 bg-white/90 backdrop-blur-md border-b border-slate-200 flex items-center justify-between px-4 sticky top-0 z-10">
            <div className="flex items-center gap-3">
                <button
                    onClick={onMenuClick}
                    className="p-1.5 -ml-1 text-slate-500 hover:bg-slate-100 rounded-lg lg:hidden"
                >
                    <Menu className="w-5 h-5" />
                </button>
                <h1 className="text-lg font-bold text-slate-800 tracking-tight">{title}</h1>
            </div>

            <div className="flex items-center gap-2">
                {/* Search — desktop only */}
                <div className="hidden md:flex items-center bg-slate-100 rounded-full px-3 py-1.5 gap-2">
                    <Search className="w-3 h-3 text-slate-400" />
                    <input
                        type="text"
                        placeholder="Buscar..."
                        className="bg-transparent border-none outline-none text-xs text-slate-600 placeholder:text-slate-400 w-24 lg:w-32"
                    />
                </div>

                {/* + Action button for views that support it */}
                {['calendar', 'notes', 'contacts', 'emails', 'templates'].includes(currentView) && (
                    <button
                        onClick={() => useStore.getState().setTriggerAction(Date.now())}
                        className="p-1.5 bg-blue-600 hover:bg-blue-700 text-white rounded-full transition-all shadow-sm shadow-blue-500/30"
                    >
                        <Plus className="w-5 h-5" />
                    </button>
                )}

                {/* Notifications bell */}
                <button
                    onClick={() => setNotificationsEnabled(!notificationsEnabled)}
                    className="p-1.5 text-slate-500 hover:bg-slate-100 rounded-full transition-colors relative"
                >
                    <Bell className={clsx('w-5 h-5', notificationsEnabled ? 'text-slate-600' : 'text-slate-400')} />
                    {notificationsEnabled && (
                        <span className="absolute top-1 right-1 w-2 h-2 bg-blue-500 rounded-full border-2 border-white" />
                    )}
                </button>

                {/* Settings */}
                <button
                    onClick={() => setView('settings')}
                    className={clsx(
                        'p-1.5 rounded-full transition-colors',
                        currentView === 'settings' ? 'bg-slate-100 text-blue-600' : 'text-slate-500 hover:bg-slate-100'
                    )}
                >
                    <Settings className="w-5 h-5" />
                </button>
            </div>
        </header>
    );
}
