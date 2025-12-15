'use client';

import { useStore } from '@/store';
import { Settings, Bell, Search, Menu, Plus } from 'lucide-react';
import clsx from 'clsx';
import { useState } from 'react';

export default function Header({ onMenuClick }: { onMenuClick?: () => void }) {
    const { currentView, setView } = useStore();

    // Map view IDs to display titles
    const titles: Record<string, string> = {
        chat: 'Asistente IA',
        calendar: 'Mi Agenda',
        tasks: 'Mis Tareas',
        emails: 'Bandeja de Entrada',
        templates: 'Plantillas',
        contacts: 'Contactos',
        settings: 'Configuración'
    };

    const title = titles[currentView] || 'Propel Assistant';

    const [notificationsEnabled, setNotificationsEnabled] = useState(true);

    return (
        <header className="h-14 bg-white/80 backdrop-blur-md border-b border-slate-200 flex items-center justify-between px-4 sticky top-0 z-10">
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
                {/* Search Bar - Removed as per user request to clean up or kept? User said "menu inferior tapa botones + ... subelo arriba ... ponerlo en el sitio de la campana". 
                   Actually, let's keep search but make it small. Or remove if crowded. Let's keep small.
                */}
                <div className="hidden md:flex items-center bg-slate-100 rounded-full px-3 py-1 mr-1">
                    <Search className="w-3 h-3 text-slate-400 mr-2" />
                    <input
                        type="text"
                        placeholder="Buscar..."
                        className="bg-transparent border-none outline-none text-xs text-slate-600 placeholder:text-slate-400 w-24 lg:w-32"
                    />
                </div>

                {/* ACTION BUTTON (+). Replaces Bell position roughly */}
                {['calendar', 'tasks', 'contacts', 'emails'].includes(currentView) && (
                    <button
                        onClick={() => useStore.getState().setTriggerAction(Date.now())}
                        className="p-1.5 bg-blue-600 hover:bg-blue-700 text-white rounded-full transition-all shadow-sm shadow-blue-500/30"
                    >
                        <Plus className="w-5 h-5" />
                    </button>
                )}

                {/* BELL. Replaces Gear position roughly */}
                <button
                    onClick={() => setNotificationsEnabled(!notificationsEnabled)}
                    className="p-1.5 text-slate-500 hover:bg-slate-100 rounded-full transition-colors relative"
                >
                    <Bell className={clsx("w-5 h-5 transition-colors", notificationsEnabled ? "text-slate-600" : "text-red-400 opacity-50")} />
                    {notificationsEnabled ? (
                        <span className="absolute top-1 right-1 w-2 h-2 bg-green-500 rounded-full border-2 border-white"></span>
                    ) : (
                        <span className="absolute top-1 right-1 w-2 h-2 bg-red-500 rounded-full border-2 border-white"></span>
                    )}
                </button>

                {/* SETTINGS. Replaces Avatar position */}
                <button
                    onClick={() => setView('settings')}
                    className={clsx(
                        "p-1.5 rounded-full transition-colors",
                        currentView === 'settings'
                            ? "bg-slate-100 text-blue-600"
                            : "text-slate-500 hover:bg-slate-100"
                    )}
                    title="Configuración"
                >
                    <Settings className="w-5 h-5" />
                </button>
            </div>
        </header>
    );
}
