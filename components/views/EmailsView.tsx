'use client';
import { useStore } from '@/store';
import { useState, useEffect } from 'react';
import { ChevronDown } from 'lucide-react';

type InboxType = 'inbox' | 'sent' | 'drafts' | 'spam';

export default function EmailsView() {
    const { emails, triggerAction, setTriggerAction } = useStore();
    const [selectedInbox, setSelectedInbox] = useState<InboxType>('inbox');

    useEffect(() => {
        if (triggerAction) {
            alert("Función de redactar manual próximamente");
            setTriggerAction(null);
        }
    }, [triggerAction, setTriggerAction]);

    const inboxLabels: Record<InboxType, string> = {
        inbox: 'Bandeja de Entrada',
        sent: 'Enviados',
        drafts: 'Borradores',
        spam: 'Spam'
    };

    // Filter emails by inbox type (mock filtering for now)
    const filteredEmails = emails; // TODO: Implement real filtering

    return (
        <div className="h-full bg-slate-50 flex flex-col">
            {/* Inbox Selector */}
            <div className="p-4 bg-white border-b border-slate-200">
                <select
                    value={selectedInbox}
                    onChange={(e) => setSelectedInbox(e.target.value as InboxType)}
                    className="w-full p-3 border border-slate-300 rounded-lg bg-white text-slate-900 font-medium focus:outline-none focus:ring-2 focus:ring-blue-500 appearance-none cursor-pointer"
                    style={{
                        backgroundImage: `url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' fill='none' viewBox='0 0 24 24' stroke='%23475569'%3E%3Cpath stroke-linecap='round' stroke-linejoin='round' stroke-width='2' d='M19 9l-7 7-7-7'%3E%3C/path%3E%3C/svg%3E")`,
                        backgroundRepeat: 'no-repeat',
                        backgroundPosition: 'right 0.75rem center',
                        backgroundSize: '1.25rem',
                        paddingRight: '2.5rem'
                    }}
                >
                    {Object.entries(inboxLabels).map(([value, label]) => (
                        <option key={value} value={value}>
                            {label}
                        </option>
                    ))}
                </select>
            </div>

            {/* Emails List */}
            <div className="flex-1 p-4 overflow-y-auto">
                {filteredEmails.length === 0 ? (
                    <div className="bg-white rounded-xl p-10 text-center border border-dashed border-slate-300 text-slate-400">
                        No hay correos pendientes
                    </div>
                ) : (
                    <div className="space-y-4">
                        {filteredEmails.map((email) => (
                            <div key={email.id} className="bg-white p-6 rounded-xl shadow-sm border border-slate-200">
                                <div className="flex justify-between items-start mb-2">
                                    <h3 className="font-bold text-lg text-slate-800">{email.subject}</h3>
                                    <span className="bg-yellow-100 text-yellow-700 text-xs px-2 py-1 rounded-full">{email.type}</span>
                                </div>
                                <p className="text-slate-500 text-sm mb-4">Para: {email.recipient}</p>
                                <div className="bg-slate-50 p-4 rounded-lg text-slate-600 text-sm font-mono whitespace-pre-wrap">
                                    {email.body}
                                </div>
                            </div>
                        ))}
                    </div>
                )}
            </div>
        </div>
    );
}
