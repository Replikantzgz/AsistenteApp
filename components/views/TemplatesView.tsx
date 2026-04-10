'use client';
import { useStore } from '@/store';
import { useState, useEffect } from 'react';
import { Copy, Trash2, X } from 'lucide-react';

export default function TemplatesView() {
    const { templates, addTemplate, deleteTemplate, triggerAction, setTriggerAction } = useStore();
    const [isModalOpen, setIsModalOpen] = useState(false);
    const [newName, setNewName] = useState('');
    const [newContent, setNewContent] = useState('');
    const [copiedId, setCopiedId] = useState<string | null>(null);

    useEffect(() => {
        if (triggerAction) {
            setIsModalOpen(true);
            setTriggerAction(null);
        }
    }, [triggerAction, setTriggerAction]);

    const handleCreate = () => {
        if (!newName.trim()) return;
        addTemplate({
            id: Date.now().toString(),
            name: newName,
            content: newContent,
        });
        setNewName('');
        setNewContent('');
        setIsModalOpen(false);
    };

    const handleCopy = (tpl: typeof templates[0]) => {
        navigator.clipboard.writeText(tpl.content);
        setCopiedId(tpl.id);
        setTimeout(() => setCopiedId(null), 2000);
    };

    return (
        <div className="h-full bg-slate-50 p-4 md:p-6 overflow-y-auto relative">
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4 pb-24">
                {templates.map((tpl) => (
                    <div key={tpl.id} className="group bg-white p-5 rounded-2xl shadow-sm border border-slate-200 hover:border-indigo-300 transition-all flex flex-col">
                        <div className="flex items-start justify-between mb-2 gap-2">
                            <h3 className="font-bold text-slate-800 text-base">{tpl.name}</h3>
                            <div className="flex gap-1 shrink-0 opacity-0 group-hover:opacity-100 transition-opacity">
                                <button
                                    onClick={() => handleCopy(tpl)}
                                    className="p-1.5 text-slate-400 hover:text-indigo-600 hover:bg-indigo-50 rounded-lg transition-colors"
                                    title="Copiar"
                                >
                                    {copiedId === tpl.id ? (
                                        <span className="text-xs font-bold text-green-600">✓</span>
                                    ) : (
                                        <Copy className="w-4 h-4" />
                                    )}
                                </button>
                                <button
                                    onClick={() => deleteTemplate(tpl.id)}
                                    className="p-1.5 text-slate-400 hover:text-red-500 hover:bg-red-50 rounded-lg transition-colors"
                                    title="Eliminar"
                                >
                                    <Trash2 className="w-4 h-4" />
                                </button>
                            </div>
                        </div>
                        <p className="text-slate-500 text-sm line-clamp-4 flex-1">{tpl.content}</p>
                    </div>
                ))}

                {templates.length === 0 && (
                    <div className="col-span-2 text-center py-16 text-slate-400">
                        <p className="text-4xl mb-3">📝</p>
                        <p className="font-medium">Sin plantillas todavía</p>
                        <p className="text-sm mt-1">Pulsa + para crear tu primera plantilla</p>
                    </div>
                )}
            </div>

            {/* Create Modal */}
            {isModalOpen && (
                <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
                    <div className="absolute inset-0 bg-slate-900/50 backdrop-blur-sm" onClick={() => setIsModalOpen(false)} />
                    <div className="bg-white w-full max-w-md rounded-2xl shadow-xl relative z-10">
                        <div className="flex items-center justify-between p-5 border-b border-slate-100">
                            <h3 className="text-lg font-bold text-slate-800">Nueva Plantilla</h3>
                            <button onClick={() => setIsModalOpen(false)} className="p-1.5 text-slate-400 hover:bg-slate-100 rounded-lg">
                                <X className="w-5 h-5" />
                            </button>
                        </div>
                        <div className="p-5 space-y-4">
                            <div>
                                <label className="text-xs font-semibold text-slate-500 uppercase tracking-wide mb-1 block">Nombre *</label>
                                <input
                                    type="text"
                                    placeholder="Ej: Respuesta a cliente"
                                    value={newName}
                                    onChange={(e) => setNewName(e.target.value)}
                                    autoFocus
                                    className="w-full border border-slate-200 rounded-xl px-4 py-2.5 text-sm outline-none focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500 transition-all"
                                    onKeyDown={(e) => e.key === 'Enter' && handleCreate()}
                                />
                            </div>
                            <div>
                                <label className="text-xs font-semibold text-slate-500 uppercase tracking-wide mb-1 block">Contenido</label>
                                <textarea
                                    placeholder="Escribe el contenido de la plantilla..."
                                    value={newContent}
                                    onChange={(e) => setNewContent(e.target.value)}
                                    rows={5}
                                    className="w-full border border-slate-200 rounded-xl px-4 py-2.5 text-sm outline-none focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500 transition-all resize-none"
                                />
                            </div>
                        </div>
                        <div className="flex justify-end gap-3 p-5 border-t border-slate-100">
                            <button onClick={() => setIsModalOpen(false)} className="px-4 py-2 text-slate-500 hover:bg-slate-100 rounded-xl font-medium text-sm">
                                Cancelar
                            </button>
                            <button
                                onClick={handleCreate}
                                disabled={!newName.trim()}
                                className="px-5 py-2 bg-indigo-600 text-white rounded-xl font-bold hover:bg-indigo-700 disabled:opacity-50 transition-colors text-sm"
                            >
                                Crear Plantilla
                            </button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
}
