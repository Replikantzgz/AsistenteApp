'use client';
import { useStore } from '@/store';

export default function EmailsView() {
    const { emails } = useStore();

    return (
        <div className="h-full bg-slate-50 p-8 overflow-y-auto">
            <h2 className="text-3xl font-bold text-slate-800 mb-6">Buzón de Correo</h2>
            {emails.length === 0 ? (
                <div className="bg-white rounded-xl p-10 text-center border border-dashed border-slate-300 text-slate-400">
                    No hay correos pendientes
                </div>
            ) : (
                <div className="space-y-4">
                    {emails.map((email) => (
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
    );
}
