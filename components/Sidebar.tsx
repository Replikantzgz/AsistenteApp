'use client';

import { useStore, ViewType } from '@/store';
import {
    MessageSquare, Calendar, CheckSquare, Mail, FileText, Settings, LogOut, Users
} from 'lucide-react';
import clsx from 'clsx';
import { createClient } from '@/lib/supabase/client';
import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { useAuth } from '@/hooks/useAuth';

export default function Sidebar() {
    const { currentView, setView } = useStore();
    const { user } = useAuth();
    const [userPlan, setUserPlan] = useState<'free' | 'eco' | 'pro'>('free');
    const router = useRouter();
    const supabase = createClient();

    useEffect(() => {
        const fetchPlan = async () => {
            if (!user) return;
            const { data: profile } = await supabase
                .from('profiles')
                .select('plan')
                .eq('id', user.id)
                .single();
            if (profile?.plan) setUserPlan(profile.plan);
        };
        fetchPlan();
    }, [user]);

    const displayEmail = user?.email;
    const displayName = user?.user_metadata?.full_name || user?.user_metadata?.name || displayEmail?.split('@')[0];

    const handleLogout = async () => {
        await supabase.auth.signOut();
        router.push('/login');
    };

    const handleLogin = () => router.push('/login');

    const navItems: { id: ViewType; label: string; icon: any }[] = [
        { id: 'chat', label: 'Asistente IA', icon: MessageSquare },
        { id: 'calendar', label: 'Calendario', icon: Calendar },
        { id: 'notes', label: 'Notas', icon: CheckSquare },
        { id: 'emails', label: 'Emails', icon: Mail },
        { id: 'contacts', label: 'Contactos', icon: Users },
        { id: 'templates', label: 'Plantillas', icon: FileText },
    ];

    return (
        <aside className="w-64 bg-slate-900 text-white flex flex-col h-full shrink-0">
            {/* Logo/Brand */}
            <div className="p-6 border-b border-slate-800">
                <div className="flex items-center gap-3">
                    <div className="w-10 h-10 rounded-full bg-gradient-to-br from-blue-600 to-purple-600 flex items-center justify-center overflow-hidden">
                        <img src="/web-app-manifest-192x192.png" alt="Alfred" className="w-full h-full object-cover" />
                    </div>
                    <div>
                        <h1 className="text-xl font-bold text-white">Alfred</h1>
                        <p className="text-xs text-slate-400 capitalize">{userPlan === 'free' ? 'Plan Gratuito' : `Plan ${userPlan}`}</p>
                    </div>
                </div>
            </div>

            <nav className="flex-1 px-4 py-4 space-y-1">
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
                        <span className="font-medium flex-1 text-left">{item.label}</span>
                    </button>
                ))}
            </nav>

            <div className="p-4 border-t border-slate-800 space-y-1">
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

                {/* User info */}
                {displayEmail && (
                    <div className="px-4 py-2 text-xs text-slate-500 truncate">
                        {displayName || displayEmail}
                    </div>
                )}

                {displayEmail ? (
                    <button
                        onClick={handleLogout}
                        className="flex items-center w-full px-4 py-3 rounded-xl transition-all text-red-400 hover:bg-slate-800 hover:text-red-300"
                    >
                        <LogOut className="w-5 h-5 mr-3" />
                        <span>Cerrar Sesión</span>
                    </button>
                ) : (
                    <button
                        onClick={handleLogin}
                        className="flex items-center w-full px-4 py-3 rounded-xl transition-all text-blue-400 hover:bg-slate-800 hover:text-blue-300"
                    >
                        <LogOut className="w-5 h-5 mr-3 rotate-180" />
                        <span>Iniciar Sesión</span>
                    </button>
                )}
            </div>
        </aside>
    );
}
