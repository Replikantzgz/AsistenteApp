'use client';

import { useState, useRef, useEffect } from 'react';
import { useStore } from '@/store';
import { Send, Mic, Calendar, CheckSquare, Bell } from 'lucide-react';
import { processUserCommand } from '@/app/ai-actions';
import { createClient } from '@/lib/supabase';

interface Message {
    role: 'user' | 'assistant';
    content: string;
}

export default function ChatView() {
    const [messages, setMessages] = useState<Message[]>([]);
    const [input, setInput] = useState('');
    const [isProcessing, setIsProcessing] = useState(false);
    const [userName, setUserName] = useState('Alex'); // Default/Placeholder
    const scrollRef = useRef<HTMLDivElement>(null);
    const supabase = createClient();

    useEffect(() => {
        // Fetch real user name
        const getUser = async () => {
            const { data: { user } } = await supabase.auth.getUser();
            if (user?.user_metadata?.full_name) {
                setUserName(user.user_metadata.full_name.split(' ')[0]);
            }
        };
        getUser();
    }, []);

    useEffect(() => {
        if (scrollRef.current) {
            scrollRef.current.scrollTop = scrollRef.current.scrollHeight;
        }
    }, [messages]);

    const handleSend = async () => {
        if (!input.trim()) return;

        const userMsg = input;
        setMessages(prev => [...prev, { role: 'user', content: userMsg }]);
        setInput('');
        setIsProcessing(true);

        try {
            const result = await processUserCommand(userMsg);
            setMessages(prev => [...prev, { role: 'assistant', content: result.message }]);

            // Handle Actions (Re-using existing logic)
            if (result.action) {
                const { type, data } = result.action;
                const store = useStore.getState();

                if (type === 'create_appointment') {
                    // ... existing logic can be imported or kept here ...
                    // For brevity, assuming store actions handle this or we keep the logic
                    // To keep it clean, we'll just log or implement basic add
                    const start = new Date();
                    start.setDate(start.getDate() + (data.startOffsetDays || 1));
                    store.addAppointment({
                        id: Date.now().toString(),
                        title: data.title,
                        start,
                        end: new Date(start.getTime() + 3600000),
                    });
                    store.setView('calendar');
                }
                // ... other actions
                if (type === 'create_task') {
                    store.addTask({ id: Date.now().toString(), title: data.title, priority: 'medium', status: 'pending' });
                    store.setView('tasks');
                }
            }

        } catch (error) {
            setMessages(prev => [...prev, { role: 'assistant', content: 'Lo siento, hubo un error.' }]);
        } finally {
            setIsProcessing(false);
        }
    };

    return (
        <div className="flex flex-col h-full bg-slate-50">
            {/* Dashboard Header */}
            <div className="p-6 pb-2">
                <div className="flex justify-between items-start mb-6">
                    <div>
                        <h1 className="text-3xl font-bold text-slate-900">Hola, {userName}</h1>
                        <p className="text-slate-500">¿Qué necesitas que haga por ti?</p>
                    </div>
                    <button className="p-2 bg-white rounded-full shadow-sm text-slate-400 hover:text-blue-600">
                        <Bell className="w-6 h-6" />
                    </button>
                </div>

                {/* Widgets Area */}
                <div className="space-y-4 mb-4">
                    {/* Up Next Widget */}
                    <div className="bg-white p-4 rounded-2xl shadow-sm border border-slate-100 flex items-center gap-4">
                        <div className="w-12 h-12 bg-blue-100 rounded-xl flex items-center justify-center text-blue-600 shrink-0">
                            <Calendar className="w-6 h-6" />
                        </div>
                        <div>
                            <p className="text-xs font-bold text-slate-400 uppercase tracking-wider">Siguiente</p>
                            <h3 className="font-semibold text-slate-900">Reunión de Equipo</h3>
                            <p className="text-sm text-slate-500">10:00 AM • Google Meet</p>
                        </div>
                    </div>

                    {/* Quick Stats */}
                    <div className="grid grid-cols-2 gap-4">
                        <div className="bg-white p-4 rounded-2xl shadow-sm border border-slate-100 flex items-center gap-3">
                            <div className="w-10 h-10 bg-orange-100 rounded-lg flex items-center justify-center text-orange-600">
                                <CheckSquare className="w-5 h-5" />
                            </div>
                            <div>
                                <p className="text-lg font-bold text-slate-900">3</p>
                                <p className="text-xs text-slate-500">Tareas</p>
                            </div>
                        </div>
                        <div className="bg-white p-4 rounded-2xl shadow-sm border border-slate-100 flex items-center gap-3">
                            <div className="w-10 h-10 bg-purple-100 rounded-lg flex items-center justify-center text-purple-600">
                                <Bell className="w-5 h-5" />
                            </div>
                            <div>
                                <p className="text-lg font-bold text-slate-900">5</p>
                                <p className="text-xs text-slate-500">Avisos</p>
                            </div>
                        </div>
                    </div>
                </div>
            </div>

            {/* Chat Area (Scrollable) */}
            <div className="flex-1 overflow-y-auto px-6 pb-4 space-y-4" ref={scrollRef}>
                {messages.length === 0 && (
                    <div className="text-center text-slate-400 text-sm mt-8">
                        <p>Intenta decir:</p>
                        <p className="italic">"Crea una reunión mañana a las 10"</p>
                    </div>
                )}
                {messages.map((msg, idx) => (
                    <div key={idx} className={`flex ${msg.role === 'user' ? 'justify-end' : 'justify-start'}`}>
                        <div className={`max-w-[85%] rounded-2xl px-5 py-3 shadow-sm ${msg.role === 'user'
                            ? 'bg-blue-600 text-white rounded-br-none'
                            : 'bg-white text-slate-800 border border-slate-100 rounded-bl-none'
                            }`}>
                            {msg.content}
                        </div>
                    </div>
                ))}
                {isProcessing && (
                    <div className="flex justify-start">
                        <div className="bg-white px-6 py-4 rounded-2xl rounded-bl-none shadow-sm border border-slate-100 flex items-center gap-2">
                            <span className="w-2 h-2 bg-blue-400 rounded-full animate-bounce"></span>
                            <span className="w-2 h-2 bg-blue-400 rounded-full animate-bounce delay-100"></span>
                            <span className="w-2 h-2 bg-blue-400 rounded-full animate-bounce delay-200"></span>
                        </div>
                    </div>
                )}
            </div>

            {/* Input Area */}
            <div className="p-4 bg-white border-t border-slate-100 pb-24 lg:pb-4">
                <div className="flex items-center gap-2 bg-slate-100 p-2 rounded-full border border-slate-200 focus-within:ring-2 ring-blue-500/20 transition-all">
                    <input
                        type="text"
                        className="flex-1 bg-transparent px-4 py-2 outline-none text-slate-800 placeholder:text-slate-400"
                        placeholder="Escribe aquí..."
                        value={input}
                        onChange={(e) => setInput(e.target.value)}
                        onKeyDown={(e) => e.key === 'Enter' && handleSend()}
                    />
                    <button className="p-3 text-slate-400 hover:text-blue-600 transition-colors">
                        <Mic className="w-5 h-5" />
                    </button>
                    <button
                        onClick={handleSend}
                        disabled={!input.trim() || isProcessing}
                        className="p-3 bg-blue-600 text-white rounded-full hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed transition-all shadow-md shadow-blue-600/20"
                    >
                        <Send className="w-5 h-5" />
                    </button>
                </div>
            </div>
        </div>
    );
}
