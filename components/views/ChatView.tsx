'use client';

import { useState, useRef, useEffect } from 'react';
import { useStore } from '@/store';
import { Send, Mic, Calendar, CheckSquare, Bell } from 'lucide-react';
import { processUserCommand } from '@/app/ai-actions';
import { NotesService } from '@/lib/notes-service';
import { clsx } from 'clsx';
import { format } from 'date-fns';
import { es } from 'date-fns/locale';

export default function ChatView() {
    const { messages, addMessage, setView, addAppointment, addTask, addEmail, addTemplate, userName, appointments, tasks } = useStore();
    const [input, setInput] = useState('');
    const [isProcessing, setIsProcessing] = useState(false);
    const [isListening, setIsListening] = useState(false);
    const [noteCount, setNoteCount] = useState(0);
    const [feedback, setFeedback] = useState<{ msg: string; type: 'success' | 'error' } | null>(null);
    const scrollRef = useRef<HTMLDivElement>(null);
    const recognitionRef = useRef<any>(null);

    const showFeedback = (msg: string, type: 'success' | 'error' = 'success') => {
        setFeedback({ msg, type });
        setTimeout(() => setFeedback(null), 3000);
    };

    // Dynamic widget data
    const nextAppointment = appointments
        .filter((a) => new Date(a.start) >= new Date())
        .sort((a, b) => new Date(a.start).getTime() - new Date(b.start).getTime())[0];

    const pendingCount = tasks.filter((t) => t.status === 'pending').length;

    // Load note count async from Supabase
    useEffect(() => {
        NotesService.getNotes()
            .then((notes) => setNoteCount(notes.length))
            .catch(() => {});
    }, []);

    // Speech recognition setup
    useEffect(() => {
        const SpeechRecognition = (window as any).SpeechRecognition || (window as any).webkitSpeechRecognition;
        if (typeof window !== 'undefined' && SpeechRecognition) {
            const recognition = new SpeechRecognition();
            recognition.continuous = false;
            recognition.interimResults = false;
            recognition.lang = 'es-ES';

            recognition.onresult = (event: any) => {
                const transcript = event.results[0][0].transcript;
                setInput((prev) => prev + (prev ? ' ' : '') + transcript);
                setIsListening(false);
            };
            recognition.onerror = (event: any) => {
                setIsListening(false);
                if (event.error === 'not-allowed') {
                    showFeedback('Permiso de micrófono denegado', 'error');
                }
            };
            recognition.onend = () => setIsListening(false);
            recognitionRef.current = recognition;
        }
    }, []);

    useEffect(() => {
        if (scrollRef.current) {
            scrollRef.current.scrollTop = scrollRef.current.scrollHeight;
        }
    }, [messages]);

    const toggleListening = async () => {
        if (!recognitionRef.current) {
            showFeedback('Tu dispositivo no soporta reconocimiento de voz', 'error');
            return;
        }
        if (isListening) {
            recognitionRef.current.stop();
        } else {
            try {
                await navigator.mediaDevices.getUserMedia({ audio: true });
                recognitionRef.current.start();
                setIsListening(true);
            } catch {
                showFeedback('Acceso al micrófono denegado', 'error');
            }
        }
    };

    const handleSend = async () => {
        if (!input.trim()) return;
        const userMsg = input;
        addMessage({ role: 'user', content: userMsg });
        setInput('');
        setIsProcessing(true);

        try {
            const result = await processUserCommand(userMsg);
            addMessage({ role: 'assistant', content: result.message });

            if (result.actions?.length) {
                result.actions.forEach((action) => {
                    const { type, data } = action;
                    if (type === 'create_appointment') {
                        const start = new Date();
                        start.setDate(start.getDate() + (data.startOffsetDays || 1));
                        start.setHours(10, 0, 0, 0);
                        addAppointment({
                            id: Date.now().toString(),
                            title: data.title,
                            start,
                            end: new Date(start.getTime() + (data.durationHours || 1) * 3600000),
                        });
                    }
                    if (type === 'create_note') {
                        addTask({ id: Date.now().toString(), title: data.title, priority: 'medium', status: 'pending' });
                        setNoteCount((c) => c + 1);
                    }
                    if (type === 'create_email') {
                        addEmail({ id: Date.now().toString(), recipient: data.recipient || '', subject: data.subject, body: data.body, type: 'draft' });
                    }
                    if (type === 'create_template') {
                        addTemplate({ id: Date.now().toString(), name: data.name, content: data.content });
                    }
                });
            }
        } catch {
            addMessage({ role: 'assistant', content: 'Lo siento, hubo un error procesando tu solicitud. Inténtalo de nuevo.' });
        } finally {
            setIsProcessing(false);
        }
    };

    const suggestions = [
        'Crea una reunión mañana a las 10',
        'Anota que debo llamar al cliente',
        'Redacta un email de seguimiento',
    ];

    return (
        <div className="flex flex-col h-full bg-slate-50 relative">
            {/* Feedback banner */}
            {feedback && (
                <div className={`fixed top-16 left-1/2 -translate-x-1/2 z-50 px-5 py-2 rounded-full shadow-lg text-sm font-medium text-white ${
                    feedback.type === 'success' ? 'bg-green-500' : 'bg-red-500'
                }`}>
                    {feedback.msg}
                </div>
            )}

            {/* Dashboard Header */}
            <div className="p-5 pb-2 shrink-0">
                <div className="mb-5">
                    <h1 className="text-3xl font-bold text-slate-900">Hola, {userName} 👋</h1>
                    <p className="text-slate-500 text-sm mt-0.5">¿Qué necesitas que haga por ti?</p>
                </div>

                {/* Widgets */}
                <div className="space-y-3">
                    {/* Next appointment */}
                    <div
                        className="bg-white p-4 rounded-2xl shadow-sm border border-slate-100 flex items-center gap-4 cursor-pointer hover:border-blue-200 transition-colors"
                        onClick={() => setView('calendar')}
                    >
                        <div className="w-11 h-11 bg-blue-100 rounded-xl flex items-center justify-center text-blue-600 shrink-0">
                            <Calendar className="w-5 h-5" />
                        </div>
                        <div className="min-w-0">
                            <p className="text-xs font-bold text-slate-400 uppercase tracking-wide">Siguiente</p>
                            {nextAppointment ? (
                                <>
                                    <h3 className="font-semibold text-slate-900 truncate">{nextAppointment.title}</h3>
                                    <p className="text-sm text-slate-500 truncate">
                                        {format(new Date(nextAppointment.start), "HH:mm · EEEE d MMM", { locale: es })}
                                    </p>
                                </>
                            ) : (
                                <>
                                    <h3 className="font-semibold text-slate-600">Sin eventos próximos</h3>
                                    <p className="text-sm text-slate-400">Tu agenda está libre</p>
                                </>
                            )}
                        </div>
                    </div>

                    {/* Quick stats */}
                    <div className="grid grid-cols-2 gap-3">
                        <div
                            className="bg-white p-4 rounded-2xl shadow-sm border border-slate-100 flex items-center gap-3 cursor-pointer hover:border-orange-200 transition-colors"
                            onClick={() => setView('notes')}
                        >
                            <div className="w-10 h-10 bg-orange-100 rounded-xl flex items-center justify-center text-orange-600 shrink-0">
                                <CheckSquare className="w-5 h-5" />
                            </div>
                            <div>
                                <p className="text-xl font-bold text-slate-900">{noteCount}</p>
                                <p className="text-xs text-slate-500">Notas</p>
                            </div>
                        </div>
                        <div
                            className="bg-white p-4 rounded-2xl shadow-sm border border-slate-100 flex items-center gap-3 cursor-pointer hover:border-purple-200 transition-colors"
                            onClick={() => setView('notes')}
                        >
                            <div className="w-10 h-10 bg-purple-100 rounded-xl flex items-center justify-center text-purple-600 shrink-0">
                                <Bell className="w-5 h-5" />
                            </div>
                            <div>
                                <p className="text-xl font-bold text-slate-900">{pendingCount}</p>
                                <p className="text-xs text-slate-500">Tareas</p>
                            </div>
                        </div>
                    </div>
                </div>
            </div>

            {/* Chat Area */}
            <div className="flex-1 overflow-y-auto px-5 py-3 space-y-3" ref={scrollRef}>
                {messages.length === 0 && (
                    <div className="text-center mt-4">
                        <p className="text-slate-400 text-sm mb-3">Prueba a decir:</p>
                        <div className="space-y-2">
                            {suggestions.map((s) => (
                                <button
                                    key={s}
                                    onClick={() => { setInput(s); }}
                                    className="block w-full max-w-xs mx-auto text-left bg-white border border-slate-200 text-slate-600 text-sm px-4 py-2 rounded-xl hover:border-blue-300 hover:bg-blue-50/50 transition-all"
                                >
                                    "{s}"
                                </button>
                            ))}
                        </div>
                    </div>
                )}
                {messages.map((msg, idx) => (
                    <div key={idx} className={`flex ${msg.role === 'user' ? 'justify-end' : 'justify-start'}`}>
                        <div className={clsx(
                            'max-w-[85%] rounded-2xl px-4 py-3 shadow-sm text-sm leading-relaxed',
                            msg.role === 'user'
                                ? 'bg-blue-600 text-white rounded-br-sm'
                                : 'bg-white text-slate-800 border border-slate-100 rounded-bl-sm'
                        )}>
                            {msg.content}
                        </div>
                    </div>
                ))}
                {isProcessing && (
                    <div className="flex justify-start">
                        <div className="bg-white px-5 py-4 rounded-2xl rounded-bl-sm shadow-sm border border-slate-100 flex items-center gap-1.5">
                            <span className="w-2 h-2 bg-blue-400 rounded-full animate-bounce" style={{ animationDelay: '0ms' }} />
                            <span className="w-2 h-2 bg-blue-400 rounded-full animate-bounce" style={{ animationDelay: '150ms' }} />
                            <span className="w-2 h-2 bg-blue-400 rounded-full animate-bounce" style={{ animationDelay: '300ms' }} />
                        </div>
                    </div>
                )}
            </div>

            {/* Input Area */}
            <div className="p-4 bg-white border-t border-slate-100 shrink-0 pb-[calc(1rem+env(safe-area-inset-bottom))] lg:pb-4">
                <div className="flex items-center gap-2 bg-slate-100 p-2 rounded-full border border-slate-200 focus-within:ring-2 ring-blue-500/20 transition-all">
                    <input
                        type="text"
                        className="flex-1 bg-transparent px-3 py-1.5 outline-none text-slate-800 placeholder:text-slate-400 text-sm"
                        placeholder={isListening ? 'Escuchando...' : 'Escribe o habla con Alfred...'}
                        value={input}
                        onChange={(e) => setInput(e.target.value)}
                        onKeyDown={(e) => e.key === 'Enter' && !e.shiftKey && handleSend()}
                    />
                    <button
                        onClick={toggleListening}
                        className={clsx(
                            'p-2.5 rounded-full transition-all',
                            isListening ? 'bg-red-100 text-red-600 animate-pulse' : 'text-slate-400 hover:text-blue-600 hover:bg-slate-200'
                        )}
                    >
                        <Mic className="w-5 h-5" />
                    </button>
                    <button
                        onClick={handleSend}
                        disabled={!input.trim() || isProcessing}
                        className="p-2.5 bg-blue-600 text-white rounded-full hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed transition-all shadow-sm shadow-blue-600/20"
                    >
                        <Send className="w-5 h-5" />
                    </button>
                </div>
            </div>
        </div>
    );
}
