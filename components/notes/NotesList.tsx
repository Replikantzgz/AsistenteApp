'use client';

import { useEffect, useState } from 'react';
import { Note, NotesService } from '@/lib/notes-service';
import { Plus, Loader2, RefreshCw } from 'lucide-react';
import { useRouter } from 'next/navigation';

export default function NotesList({ limit }: { limit?: number }) {
    const [notes, setNotes] = useState<Note[]>([]);
    const [loading, setLoading] = useState(true);
    const router = useRouter();

    const fetchNotes = async () => {
        try {
            setLoading(true);
            const data = await NotesService.getNotes();
            setNotes(data);
        } catch (error) {
            console.error('Error fetching notes:', error);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchNotes();

        // Subscribe to real-time changes could be added here

        return () => { };
    }, []);

    const displayNotes = limit ? notes.slice(0, limit) : notes;

    if (loading) {
        return (
            <div className="flex items-center justify-center p-8">
                <Loader2 className="w-6 h-6 animate-spin text-slate-400" />
            </div>
        );
    }

    if (notes.length === 0) {
        return (
            <div className="text-center p-8 border-2 border-dashed border-slate-200 rounded-xl">
                <p className="text-slate-500 mb-2">No tienes notas aún</p>
                <button
                    onClick={() => { /* Open create modal */ }}
                    className="text-sm text-blue-600 font-medium hover:underline"
                >
                    Pídele al asistente que cree una
                </button>
            </div>
        );
    }

    return (
        <div className="bg-white rounded-xl shadow-sm border border-slate-200 overflow-hidden min-h-[500px] relative">
            {/* Notebook Margin Line */}
            <div className="absolute left-10 md:left-12 top-0 bottom-0 w-px bg-red-200/50 pointer-events-none z-10 hidden md:block" />

            <div className="divide-y divide-slate-100">
                {displayNotes.map((note) => (
                    <div
                        key={note.id}
                        onClick={() => {/* Open detail view */ }}
                        className="group relative p-4 pl-4 md:pl-16 hover:bg-slate-50 transition-colors cursor-pointer flex items-start gap-4"
                    >
                        <div className="flex-1 min-w-0">
                            <h3 className="font-serif font-bold text-slate-800 text-lg mb-1 group-hover:text-blue-600 transition-colors">
                                {note.title}
                            </h3>
                            <p className="text-slate-500 font-serif line-clamp-2 text-sm leading-relaxed">
                                {note.content}
                            </p>

                            {Boolean(note.audio_url) && (
                                <div className="mt-2 inline-flex items-center px-2 py-1 rounded-md bg-stone-100 border border-stone-200 text-xs font-medium text-stone-600">
                                    <span className="w-2 h-2 rounded-full bg-red-400 mr-2 animate-pulse" />
                                    Nota de voz adjunta
                                </div>
                            )}
                        </div>

                        <div className="text-xs font-mono text-slate-400 whitespace-nowrap pt-1">
                            {new Date(note.created_at).toLocaleDateString()}
                        </div>
                    </div>
                ))}
            </div>

            {!limit && notes.length > 0 && (
                <div className="p-8 flex justify-center border-t border-slate-100 bg-slate-50/50">
                    <button
                        onClick={() => {/* Open create modal via parent */ }}
                        className="text-sm font-medium text-slate-400 hover:text-slate-600 flex items-center gap-2 group"
                    >
                        <Plus className="w-4 h-4 group-hover:scale-110 transition-transform" />
                        Nueva entrada
                    </button>
                </div>
            )}
        </div>
    );
}
