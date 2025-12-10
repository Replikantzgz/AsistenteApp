'use client';
import { useStore } from '@/store';

export default function TemplatesView() {
    const { templates } = useStore();

    return (
        <div className="h-full bg-slate-50 p-8 overflow-y-auto">
            <div className="flex justify-between items-center mb-6">
                <h2 className="text-3xl font-bold text-slate-800">Plantillas</h2>
                <button className="bg-indigo-600 text-white px-4 py-2 rounded-lg hover:bg-indigo-700 transition">
                    + Crear
                </button>
            </div>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                {templates.map((tpl) => (
                    <div key={tpl.id} className="bg-white p-6 rounded-xl shadow-sm border border-slate-200 hover:border-indigo-300 transition cursor-pointer">
                        <h3 className="font-semibold text-slate-800 mb-2">{tpl.name}</h3>
                        <p className="text-slate-500 text-sm line-clamp-3">{tpl.content}</p>
                    </div>
                ))}
            </div>
        </div>
    );
}
