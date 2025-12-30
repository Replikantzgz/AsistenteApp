'use client';

import { Note } from '@/lib/notes-service';
import { format } from 'date-fns';
import { es } from 'date-fns/locale';
import { Mic, StickyNote, Tag, Pin } from 'lucide-react';

interface NoteCardProps {
    note: Note;
    onClick?: () => void;
}

export default function NoteCard({ note, onClick }: NoteCardProps) {
    return (
        <div
            onClick={onClick}
            className={`p-4 rounded-xl border transition-all cursor-pointer hover:shadow-md group ${note.is_pinned
                    ? 'bg-yellow-50/50 border-yellow-200'
                    : 'bg-white border-slate-100 hover:border-slate-200'
                }`}
        >
            <div className="flex items-start justify-between mb-2">
                <div className="flex items-center gap-2">
                    {note.audio_url ? (
                        <div className="w-8 h-8 rounded-full bg-blue-100 flex items-center justify-center text-blue-600">
                            <Mic className="w-4 h-4" />
                        </div>
                    ) : (
                        <div className="w-8 h-8 rounded-full bg-slate-100 flex items-center justify-center text-slate-500">
                            <StickyNote className="w-4 h-4" />
                        </div>
                    )}
                    <h3 className="font-semibold text-slate-900 line-clamp-1">
                        {note.title || 'Nota sin título'}
                    </h3>
                </div>
                {note.is_pinned && (
                    <Pin className="w-4 h-4 text-yellow-500 transform rotate-45" />
                )}
            </div>

            <p className="text-sm text-slate-600 mb-3 line-clamp-3 min-h-[3rem]">
                {note.content || 'Sin contenido...'}
            </p>

            <div className="flex items-center justify-between mt-auto">
                <div className="flex items-center gap-2 overflow-hidden">
                    {note.tags && note.tags.length > 0 && (
                        <div className="flex gap-1 overflow-x-auto no-scrollbar">
                            {note.tags.map((tag, i) => (
                                <span key={i} className="px-2 py-0.5 bg-slate-100 text-slate-600 text-xs rounded-full whitespace-nowrap">
                                    #{tag}
                                </span>
                            ))}
                        </div>
                    )}
                </div>
                <span className="text-xs text-slate-400 shrink-0 ml-2">
                    {format(new Date(note.created_at), "d MMM", { locale: es })}
                </span>
            </div>
        </div>
    );
}
