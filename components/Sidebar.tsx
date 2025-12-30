'use client';

import { useStore, ViewType } from '@/store';
import {
    MessageSquare, Calendar, CheckSquare, Mail, FileText, Settings, LogOut, User, Users
} from 'lucide-react';
import clsx from 'clsx';
import { createClient } from '@/lib/supabase/client';
import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';

import { useSession, signOut } from 'next-auth/react';

import { Lock } from 'lucide-react';

export default function Sidebar() {
    const { currentView, setView } = useStore();
    const [userEmail, setUserEmail] = useState<string | null>(null);
    const [userPlan, setUserPlan] = useState<'free' | 'eco' | 'pro'>('free'); // Default to free/basic
    const { data: session } = useSession();
    const router = useRouter();
    const supabase = createClient();

    useEffect(() => {
        const getUser = async () => {
            const { data: { user } } = await supabase.auth.getUser();
            setUserEmail(user?.email || null);

            if (user) {
                // Fetch profile for plan
                const { data: profile } = await supabase
                    .from('profiles')
                    .select('plan')
                    .eq('id', user.id)
                    .single();

                if (profile?.plan) setUserPlan(profile.plan);
            }
        };
        getUser();

        const { data: { subscription } } = supabase.auth.onAuthStateChange((_event, session) => {
            setUserEmail(session?.user?.email || null);
        });

        return () => subscription.unsubscribe();
    }, []);

    // Combine email from both sources
    const displayEmail = userEmail || session?.user?.email;

    const handleLogout = async () => {
        await supabase.auth.signOut();
        await signOut({ redirect: false });
        router.refresh();
        setUserEmail(null);
        router.push('/login');
    };

    const handleLogin = () => {
        router.push('/login');
    };

    const navItems: { id: ViewType; label: string; icon: any }[] = [
        { id: 'chat', label: 'Asistente IA', icon: MessageSquare },
        { id: 'calendar', label: 'Calendario', icon: Calendar },
        { id: 'notes', label: 'Notas', icon: CheckSquare },
        { id: 'emails', label: 'Emails', icon: Mail },
        { id: 'contacts', label: 'Contactos', icon: Users },
        { id: 'templates', label: 'Plantillas', icon: FileText },
    ];

    return (
        <aside className="w-64 bg-slate-900 text-white flex flex-col h-full shrink-0 transition-all duration-300">
            <div className="p-6">
                <h1 className="text-2xl font-bold bg-gradient-to-r from-blue-400 to-purple-500 bg-clip-text text-transparent">
                    Alfred
                </h1>
                <p className="text-xs text-slate-400 mt-1">Alfred Premium v1.0</p>
            </div>

            <nav className="flex-1 px-4 space-y-2">
                {navItems.map((item) => {
                    // All features unlocked for Premium trial users
                    const isRestricted = false;

                    return (
                        <button
                            key={item.id}
                            onClick={() => !isRestricted && setView(item.id)}
                            className={clsx(
                                'flex items-center w-full px-4 py-3 rounded-xl transition-all',
                                currentView === item.id
                                    ? 'bg-blue-600 shadow-lg shadow-blue-900/50 text-white'
                                    : 'text-slate-400 hover:bg-slate-800 hover:text-white',
                                isRestricted && 'opacity-50 cursor-not-allowed hover:bg-transparent hover:text-slate-400'
                            )}
                        >
                            <item.icon className="w-5 h-5 mr-3" />
                            <span className="font-medium flex-1 text-left">{item.label}</span>
                            {isRestricted && <Lock className="w-4 h-4 text-slate-600" />}
                        </button>
                    );
                })}
            </nav>

            <div className="p-4 border-t border-slate-800 space-y-2">
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

                <div className="px-4 py-2 border-t border-slate-800 space-y-1">
                    <button className="text-[10px] text-slate-500 hover:text-slate-300 w-full text-left transition-colors">
                        Política de Privacidad
                    </button>
                    <button className="text-[10px] text-slate-500 hover:text-slate-300 w-full text-left transition-colors">
                        Términos de Servicio
                    </button>
                </div>

                <button
                    onClick={handleLogout}
                    className="flex items-center w-full px-4 py-3 rounded-xl transition-all text-red-400 hover:bg-slate-800 hover:text-red-300"
                >
                    <LogOut className="w-5 h-5 mr-3" />
                    <span>Cerrar Sesión</span>
                </button>
            </div>
        </aside>
    );
}
