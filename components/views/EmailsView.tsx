'use client';
import { useStore } from '@/store';
import { useState, useEffect } from 'react';
import { Mail, Send, X } from 'lucide-react';

type InboxType = 'inbox' | 'sent' | 'drafts' | 'spam';

export default function EmailsView() {
    const { emails, addEmail, triggerAction, setTriggerAction } = useStore();
    const [selectedInbox, setSelectedInbox] = useState<InboxType>('drafts');
    const [isComposeOpen, setIsComposeOpen] = useState(false);
    const [composeData, setComposeData] = useState({ to: '', subject: '', body: '' });

    useEffect(() => {
        if (triggerAction) {
            setIsComposeOpen(true);
            setTriggerAction(null);
        }
    }, [triggerAction, setTriggerAction]);

    const inboxLabels: Record<InboxType, string> = {
        inbox: 'Bandeja de Entrada',
        sent: 'Enviados',
        drafts: 'Borradores',
        spam: 'Spam',
    };

    // Real filtering by email type
    const filteredEmails = emails.filter((email) => {
        if (selectedInbox === 'inbox') return email.type === 'inbox';
        if (selectedInbox === 'sent') return email.type === 'sent';
        if (selectedInbox === 'drafts') return email.type === 'draft';
        return false; // spam: no spam type yet
    });

    const handleCompose = () => {
        if (!composeData.subject.trim()) return;
        addEmail({
            id: Date.now().toString(),
            subject: composeData.subject,
            body: composeData.body,
            recipient: composeData.to,
            type: 'draft',
        });
        setComposeData({ to: '', subject: '', body: '' });
        setIsComposeOpen(false);
    };

    const typeBadgeColor: Record<string, string> = {
        draft: 'bg-yellow-100 text-yellow-700',
        sent: 'bg-green-100 text-green-700',
        inbox: 'bg-blue-100 text-blue-700',
    };

    return (
        <div className="h-full bg-slate-50 flex flex-col">
            {/* Inbox tabs */}
            <div className="p-4 bg-white border-b border-slate-200">
                <div className="flex gap-1 bg-slate-100 p-1 rounded-xl overflow-x-auto">
                    {(Object.entries(inboxLabels) as [InboxType, string][]).map(([value, label]) => (
                        <button
                            key={value}
                            onClick={() => setSelectedInbox(value)}
                            className={`px-3 py-1.5 text-xs font-semibold rounded-lg whitespace-nowrap transition-colors flex-1 ${
                                selectedInbox === value
                                    ? 'bg-white text-slate-900 shadow-sm'
                                    : 'text-slate-500 hover:text-slate-700'
                            }`}
                        >
                            {label}
                            {value === 'drafts' && emails.filter(e => e.type === 'draft').length > 0 && (
                                <span className="ml-1 bg-yellow-200 text-yellow-700 rounded-full px-1.5 py-0.5 text-[10px] font-bold">
                                    {emails.filter(e => e.type === 'draft').length}
                                </span>
                            )}
                        </button>
                    ))}
                </div>
            </div>

            {/* Email list */}
            <div className="flex-1 p-4 overflow-y-auto pb-24">
                {filteredEmails.length === 0 ? (
                    <div className="bg-white rounded-2xl p-10 text-center border border-dashed border-slate-200 text-slate-400 mt-4">
                        <Mail className="w-10 h-10 mx-auto mb-3 text-slate-300" />
                        <p className="font-medium">No hay correos aquí</p>
                        {selectedInbox === 'drafts' && (
                            <p className="text-sm mt-1">Pulsa + para redactar un nuevo borrador</p>
                        )}
                    </div>
                ) : (
                    <div className="space-y-3">
                        {filteredEmails.map((email) => (
                            <div key={email.id} className="bg-white p-5 rounded-2xl shadow-sm border border-slate-200 hover:border-slate-300 transition-colors">
                                <div className="flex justify-between items-start mb-2 gap-3">
                                    <h3 className="font-bold text-slate-800 truncate flex-1">{email.subject}</h3>
                                    <span className={`text-xs px-2 py-0.5 rounded-full font-medium shrink-0 ${typeBadgeColor[email.type] || 'bg-slate-100 text-slate-500'}`}>
                                        {email.type === 'draft' ? 'Borrador' : email.type === 'sent' ? 'Enviado' : 'Recibido'}
                                    </span>
                                </div>
                                {email.recipient && (
                                    <p className="text-slate-500 text-xs mb-3">Para: {email.recipient}</p>
                                )}
                                <div className="bg-slate-50 p-3 rounded-xl text-slate-600 text-sm whitespace-pre-wrap line-clamp-4 font-mono">
                                    {email.body}
                                </div>
                            </div>
                        ))}
                    </div>
                )}
            </div>

            {/* Compose Modal */}
            {isComposeOpen && (
                <div className="fixed inset-0 z-50 flex items-end sm:items-center justify-center p-0 sm:p-4">
                    <div className="absolute inset-0 bg-slate-900/50 backdrop-blur-sm" onClick={() => setIsComposeOpen(false)} />
                    <div className="bg-white w-full sm:max-w-lg rounded-t-2xl sm:rounded-2xl shadow-2xl relative z-10 flex flex-col max-h-[90vh]">
                        {/* Compose header */}
                        <div className="flex items-center justify-between p-5 border-b border-slate-100">
                            <h3 className="text-lg font-bold text-slate-800">Nuevo Borrador</h3>
                            <button onClick={() => setIsComposeOpen(false)} className="p-1.5 text-slate-400 hover:bg-slate-100 rounded-lg">
                                <X className="w-5 h-5" />
                            </button>
                        </div>

                        <div className="p-5 space-y-4 flex-1 overflow-y-auto">
                            <div>
                                <label className="text-xs font-semibold text-slate-500 uppercase tracking-wide mb-1 block">Para</label>
                                <input
                                    type="email"
                                    placeholder="destinatario@email.com"
                                    value={composeData.to}
                                    onChange={(e) => setComposeData(d => ({ ...d, to: e.target.value }))}
                                    className="w-full border border-slate-200 rounded-xl px-4 py-2.5 text-sm outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500 transition-all"
                                />
                            </div>
                            <div>
                                <label className="text-xs font-semibold text-slate-500 uppercase tracking-wide mb-1 block">Asunto *</label>
                                <input
                                    type="text"
                                    placeholder="Asunto del correo"
                                    value={composeData.subject}
                                    onChange={(e) => setComposeData(d => ({ ...d, subject: e.target.value }))}
                                    className="w-full border border-slate-200 rounded-xl px-4 py-2.5 text-sm outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500 transition-all"
                                    autoFocus
                                />
                            </div>
                            <div>
                                <label className="text-xs font-semibold text-slate-500 uppercase tracking-wide mb-1 block">Mensaje</label>
                                <textarea
                                    placeholder="Escribe tu mensaje aquí..."
                                    value={composeData.body}
                                    onChange={(e) => setComposeData(d => ({ ...d, body: e.target.value }))}
                                    rows={6}
                                    className="w-full border border-slate-200 rounded-xl px-4 py-2.5 text-sm outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500 transition-all resize-none"
                                />
                            </div>
                        </div>

                        <div className="p-5 border-t border-slate-100 flex justify-end gap-3">
                            <button onClick={() => setIsComposeOpen(false)} className="px-4 py-2 text-slate-500 hover:bg-slate-100 rounded-xl font-medium text-sm">
                                Descartar
                            </button>
                            <button
                                onClick={handleCompose}
                                disabled={!composeData.subject.trim()}
                                className="px-5 py-2 bg-blue-600 text-white rounded-xl font-bold hover:bg-blue-700 disabled:opacity-50 transition-colors flex items-center gap-2 text-sm"
                            >
                                <Send className="w-4 h-4" />
                                Guardar Borrador
                            </button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
}
