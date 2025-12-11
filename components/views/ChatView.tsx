'use client';


import { useState, useRef, useEffect } from 'react';
import { useStore } from '@/store';
import { Send, Mic } from 'lucide-react';
import { processUserCommand } from '@/app/ai-actions';

interface Message {
    role: 'user' | 'assistant';
    content: string;
}

export default function ChatView() {
    const [messages, setMessages] = useState<Message[]>([
        { role: 'assistant', content: '¡Hola! Soy tu asistente. ¿Qué necesitas organizar hoy?' }
    ]);
    const [input, setInput] = useState('');
    const [isProcessing, setIsProcessing] = useState(false);
    const scrollRef = useRef<HTMLDivElement>(null);

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
            // Call Server Action
            const result = await processUserCommand(userMsg);

            // Show AI Message
            setMessages(prev => [...prev, { role: 'assistant', content: result.message }]);

            // Execute Client-Side Action (if any)
            if (result.action) {
                const { type, data } = result.action;
                const store = useStore.getState(); // Access store imperatively

                if (type === 'create_appointment') {
                    const start = new Date();
                    start.setDate(start.getDate() + (data.startOffsetDays || 1));
                    start.setHours(10, 0, 0, 0); // Default to 10 AM

                    const end = new Date(start);
                    end.setHours(start.getHours() + (data.durationHours || 1));

                    store.addAppointment({
                        id: Date.now().toString(),
                        title: data.title,
                        start,
                        end,
                    });
                    store.setView('calendar');
                }

                if (type === 'create_task') {
                    store.addTask({
                        id: Date.now().toString(),
                        title: data.title,
                        priority: data.priority || 'medium',
                        status: 'pending'
                    });
                    store.setView('tasks');
                }

                if (type === 'create_email') {
                    store.addEmail({
                        id: Date.now().toString(),
                        subject: data.subject,
                        body: data.body,
                        recipient: data.recipient || '',
                        type: 'draft'
                    });
                    store.setView('emails');
                }

                if (type === 'create_template') {
                    store.addTemplate({
                        id: Date.now().toString(),
                        name: data.name,
                        content: data.content
                    });
                    store.setView('templates');
                }
            }

        } catch (error) {
            console.error(error);
            setMessages(prev => [...prev, { role: 'assistant', content: 'Lo siento, hubo un error conectando con la IA.' }]);
        } finally {
            setIsProcessing(false);
        }
    };

    return (
        <div className="flex flex-col h-full bg-slate-50">
            {/* Header */}
            <div className="p-6 border-b border-slate-200 bg-white">
                <h2 className="text-xl font-bold text-slate-800">Chat con Asistente</h2>
                <p className="text-sm text-slate-500">Pide crear citas, tareas o emails...</p>
            </div>

            {/* Messages */}
            <div className="flex-1 overflow-y-auto p-6 space-y-6" ref={scrollRef}>
                {messages.map((msg, idx) => (
                    <div key={idx} className={`flex ${msg.role === 'user' ? 'justify-end' : 'justify-start'}`}>
                        <div className={`max-w-[80%] rounded-2xl px-6 py-4 shadow-sm ${msg.role === 'user'
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

            {/* Input */}
            <div className="p-4 bg-white border-t border-slate-200">
                <div className="flex items-center gap-2 bg-slate-100 p-2 rounded-full border border-slate-200 focus-within:ring-2 ring-blue-500/20 transition-all">
                    <input
                        type="text"
                        className="flex-1 bg-transparent px-4 py-2 outline-none text-slate-800 placeholder:text-slate-400"
                        placeholder="Escribe tu instrucción..."
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
