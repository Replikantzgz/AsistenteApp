'use client';

import { useState, useRef } from 'react';
import { NotesService } from '@/lib/notes-service';
import { Mic, Square, Save, X, Loader2 } from 'lucide-react';

interface NoteEditorProps {
    onClose: () => void;
    onSave: () => void;
}

export default function NoteEditor({ onClose, onSave }: NoteEditorProps) {
    const [title, setTitle] = useState('');
    const [content, setContent] = useState('');
    const [isRecording, setIsRecording] = useState(false);
    const [audioBlob, setAudioBlob] = useState<Blob | null>(null);
    const [loading, setLoading] = useState(false);

    const mediaRecorderRef = useRef<MediaRecorder | null>(null);
    const chunksRef = useRef<Blob[]>([]);

    const startRecording = async () => {
        try {
            const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
            const mediaRecorder = new MediaRecorder(stream);
            mediaRecorderRef.current = mediaRecorder;
            chunksRef.current = [];

            mediaRecorder.ondataavailable = (e) => {
                if (e.data.size > 0) {
                    chunksRef.current.push(e.data);
                }
            };

            mediaRecorder.onstop = () => {
                const blob = new Blob(chunksRef.current, { type: 'audio/webm' });
                setAudioBlob(blob);
                stream.getTracks().forEach(track => track.stop());
            };

            mediaRecorder.start();
            setIsRecording(true);
        } catch (err) {
            console.error('Error accessing microphone:', err);
            alert('No se pudo acceder al micrófono');
        }
    };

    const stopRecording = () => {
        if (mediaRecorderRef.current && isRecording) {
            mediaRecorderRef.current.stop();
            setIsRecording(false);
        }
    };

    const handleSave = async () => {
        if (!title.trim()) return;
        setLoading(true);
        try {
            let audioUrl = null;
            if (audioBlob) {
                audioUrl = await NotesService.uploadAudio(audioBlob);
            }

            await NotesService.createNote({
                title,
                content,
                audio_url: audioUrl,
                tags: []
            });
            onSave();
            onClose();
        } catch (error) {
            console.error('Error saving note:', error);
            alert('Error al guardar la nota');
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="fixed inset-0 bg-black/60 backdrop-blur-sm z-50 flex items-center justify-center p-0 md:p-4 animate-in fade-in duration-200">
            <div className="bg-[#fdfbf7] w-full h-full md:h-auto md:max-w-lg md:rounded-xl shadow-2xl overflow-hidden flex flex-col relative">

                {/* Header pattern mimicking a notebook binding or top tape */}
                <div className="h-2 w-full bg-gradient-to-r from-red-400/80 via-red-500/80 to-red-400/80" />

                <div className="p-4 border-b border-stone-200 flex items-center justify-between bg-[#fdfbf7]">
                    <button onClick={onClose} className="text-stone-400 hover:text-stone-600 p-2 -ml-2">
                        <X className="w-6 h-6" />
                    </button>
                    <span className="font-serif text-stone-500 italic">Nueva Nota</span>
                    <button
                        onClick={handleSave}
                        disabled={loading || !title.trim()}
                        className="text-blue-600 font-bold hover:text-blue-700 disabled:opacity-40 disabled:cursor-not-allowed text-lg"
                    >
                        {loading ? <Loader2 className="w-6 h-6 animate-spin" /> : "Guardar"}
                    </button>
                </div>

                <div className="flex-1 overflow-y-auto bg-[#fdfbf7] relative">
                    {/* Lined paper background effect */}
                    <div className="absolute inset-0 pointer-events-none"
                        style={{
                            backgroundImage: 'linear-gradient(#e5e7eb 1px, transparent 1px)',
                            backgroundSize: '100% 32px',
                            marginTop: '31px'
                        }}
                    />

                    {/* Red margin line */}
                    <div className="absolute left-8 top-0 bottom-0 w-px bg-red-200/50 pointer-events-none h-full" />

                    <div className="p-0 relative min-h-full">
                        <input
                            type="text"
                            placeholder="Título de la nota..."
                            value={title}
                            onChange={(e) => setTitle(e.target.value)}
                            className="w-full text-2xl font-serif font-bold placeholder:text-stone-300 bg-transparent border-none focus:ring-0 px-10 py-4 text-stone-800"
                            autoFocus
                        />

                        <textarea
                            placeholder="Empieza a escribir..."
                            value={content}
                            onChange={(e) => setContent(e.target.value)}
                            className="w-full h-[calc(100vh-250px)] md:h-96 resize-none placeholder:text-stone-300 bg-transparent border-none focus:ring-0 px-10 py-1 text-lg leading-8 text-stone-700 font-serif"
                            style={{ lineHeight: '32px' }}
                        />
                    </div>
                </div>

                {/* Attachments Area */}
                <div className="p-3 bg-white/50 border-t border-stone-200 backdrop-blur-sm">
                    {audioBlob && (
                        <div className="bg-stone-100 p-2 rounded-lg flex items-center gap-3 mb-3 mx-4 border border-stone-200">
                            <div className="w-8 h-8 bg-red-100 rounded-full flex items-center justify-center text-red-500">
                                <Mic className="w-4 h-4" />
                            </div>
                            <div className="flex-1 min-w-0">
                                <p className="text-xs font-semibold text-stone-700 truncate">Nota de voz</p>
                                <p className="text-[10px] text-stone-500">{(audioBlob.size / 1024).toFixed(1)} KB</p>
                            </div>
                            <button
                                onClick={() => setAudioBlob(null)}
                                className="p-1 text-stone-400 hover:text-red-500"
                            >
                                <X className="w-4 h-4" />
                            </button>
                        </div>
                    )}

                    <div className="flex items-center justify-between px-4 pb-safe-bottom">
                        {/* Toolbar */}
                        <div className="flex items-center gap-4">
                            <button
                                onClick={() => {/* Placeholder for image attachment */ alert("Funcionalidad de imagen próximamente") }}
                                className="text-stone-400 hover:text-stone-600 transition-colors"
                                title="Adjuntar imagen"
                            >
                                {/* Paperclip icon for attachment */}
                                <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" className="lucide lucide-paperclip"><path d="m21.44 11.05-9.19 9.19a6 6 0 0 1-8.49-8.49l9.19-9.19a4 4 0 0 1 5.66 5.66l-9.2 9.19a2 2 0 0 1-2.83-2.83l8.49-8.48" /></svg>
                            </button>

                            <button
                                onClick={isRecording ? stopRecording : startRecording}
                                className={`p-2 rounded-full transition-all ${isRecording
                                    ? 'bg-red-500 text-white animate-pulse shadow-lg shadow-red-200'
                                    : 'text-stone-400 hover:text-stone-600'
                                    }`}
                                title="Grabar audio"
                            >
                                {isRecording ? <Square className="w-5 h-5 fill-current" /> : <Mic className="w-6 h-6" />}
                            </button>
                        </div>

                        <div className="text-xs text-stone-300 font-mono">
                            {content.length} caracteres
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
}
