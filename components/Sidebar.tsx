'use client';

import { useStore, ViewType } from '@/store';
import {
    MessageSquare, Calendar, CheckSquare, Mail, FileText, Settings, LogOut
} from 'lucide-react';
import clsx from 'clsx';

export default function Sidebar() {
    const { currentView, setView } = useStore();

    const navItems: { id: ViewType; label: string; icon: any }[] = [
        { id: 'chat', label: 'Asistente IA', icon: MessageSquare },
        { id: 'calendar', label: 'Agenda', icon: Calendar },
        { id: 'tasks', label: 'Tareas', icon: CheckSquare },
        { id: 'emails', label: 'Emails', icon: Mail },
        { id: 'templates', label: 'Plantillas', icon: FileText },
    ];

    return (
        <aside className="w-64 bg-slate-900 text-white flex flex-col h-full shrink-0 transition-all duration-300">
            <div className="p-6">
                <h1 className="text-2xl font-bold bg-gradient-to-r from-blue-400 to-purple-500 bg-clip-text text-transparent">
                    IATUALCANCE
                </h1>
                <p className="text-xs text-slate-400 mt-1">Asistente Personal Pro</p>
            </div>

            <nav className="flex-1 px-4 space-y-2">
                {navItems.map((item) => (
                    <button
                        key={item.id}
                        onClick={() => setView(item.id)}
                        className={clsx(
                            'flex items-center w-full px-4 py-3 rounded-xl transition-all',
                            currentView === item.id
                                ? 'bg-blue-600 shadow-lg shadow-blue-900/50 text-white'
                                : 'text-slate-400 hover:bg-slate-800 hover:text-white'
                        )}
                    >
                        <item.icon className="w-5 h-5 mr-3" />
                        <span className="font-medium">{item.label}</span>
                    </button>
                ))}
            </nav>

            <div className="p-4 border-t border-slate-800">
                <button
                    onClick={() => setView('settings')}
                    className={clsx(
                        'flex items-center w-full px-4 py-3 rounded-xl transition-all text-slate-400 hover:bg-slate-800 hover:text-white',
                        currentView === 'settings' && 'bg-slate-800 text-white'
                    )}
                >
                    <Settings className="w-5 h-5 mr-3" />
                    <span>Configuración</span>
                </button>
            </div>
        </aside>
    );
}
