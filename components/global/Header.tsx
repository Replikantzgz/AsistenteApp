'use client';

import { useStore } from '@/store';
import { Settings, Bell, Search, Menu } from 'lucide-react';
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

    return (
        <header className="h-16 bg-white/80 backdrop-blur-md border-b border-slate-200 flex items-center justify-between px-6 sticky top-0 z-10">
            <div className="flex items-center gap-4">
                <button
                    onClick={onMenuClick}
                    className="p-2 -ml-2 text-slate-500 hover:bg-slate-100 rounded-lg lg:hidden"
                >
                    <Menu className="w-6 h-6" />
                </button>
                <h1 className="text-xl font-bold text-slate-800 tracking-tight">{title}</h1>
            </div>

            <div className="flex items-center gap-2">
                {/* Search Bar (Optional visual only for now) */}
                <div className="hidden md:flex items-center bg-slate-100 rounded-full px-4 py-1.5 mr-2">
                    <Search className="w-4 h-4 text-slate-400 mr-2" />
                    <input
                        type="text"
                        placeholder="Buscar..."
                        className="bg-transparent border-none outline-none text-sm text-slate-600 placeholder:text-slate-400 w-48"
                    />
                </div>

                <div className="flex items-center gap-1">
                    <button className="p-2 text-slate-500 hover:bg-slate-100 rounded-full transition-colors relative">
                        <Bell className="w-5 h-5" />
                        <span className="absolute top-1.5 right-1.5 w-2 h-2 bg-red-500 rounded-full border-2 border-white"></span>
                    </button>

                    <button
                        onClick={() => setView('settings')}
                        className={clsx(
                            "p-2 rounded-full transition-colors",
                            currentView === 'settings'
                                ? "bg-slate-100 text-blue-600"
                                : "text-slate-500 hover:bg-slate-100"
                        )}
                        title="Configuración"
                    >
                        <Settings className="w-5 h-5" />
                    </button>

                    {/* Tiny User Avatar Placeholder */}
                    <div className="w-8 h-8 rounded-full bg-gradient-to-br from-blue-500 to-purple-600 ml-2 border-2 border-white shadow-sm cursor-pointer hover:opacity-90"></div>
                </div>
            </div>
        </header>
    );
}
