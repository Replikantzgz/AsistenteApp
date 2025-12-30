'use client';

import { useState } from 'react';
import NotesList from '@/components/notes/NotesList';
import NoteEditor from '@/components/notes/NoteEditor';
import { Plus } from 'lucide-react';

export default function NotesPage() {
    const [isEditorOpen, setIsEditorOpen] = useState(false);
    const [refreshKey, setRefreshKey] = useState(0);

    return (
        <div className="p-4 md:p-6 pb-24 max-w-7xl mx-auto space-y-6">
            <div className="flex items-center justify-between">
                <div>
                    <h2 className="text-2xl font-bold text-slate-900">Mis Notas</h2>
                    <p className="text-slate-500">Gestiona tus ideas, tareas y recordatorios</p>
                </div>
                <button
                    onClick={() => setIsEditorOpen(true)}
                    className="bg-blue-600 text-white p-3 rounded-full hover:bg-blue-700 transition-colors shadow-lg shadow-blue-200"
                    title="Nueva Nota"
                >
                    <Plus className="w-6 h-6" />
                </button>
            </div>

            <NotesList key={refreshKey} />

            {isEditorOpen && (
                <NoteEditor
                    onClose={() => setIsEditorOpen(false)}
                    onSave={() => setRefreshKey(prev => prev + 1)}
                />
            )}
        </div>
    );
}
