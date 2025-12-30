import { createClient } from '@/lib/supabase/client';

export type Note = {
    id: string;
    user_id: string;
    title: string | null;
    content: string | null;
    audio_url: string | null;
    tags: string[];
    is_pinned: boolean;
    created_at: string;
    updated_at: string;
};

export const NotesService = {
    async getNotes() {
        const supabase = createClient();

        // Check session first
        const { data: { session } } = await supabase.auth.getSession();
        if (!session) return [];

        const { data, error } = await supabase
            .from('notes')
            .select('*')
            .order('is_pinned', { ascending: false })
            .order('created_at', { ascending: false });

        if (error) {
            console.error('Error fetching notes:', error);
            throw error;
        }
        return data as Note[];
    },

    async createNote(note: Partial<Note>) {
        const supabase = createClient();
        const { data: { user } } = await supabase.auth.getUser();

        if (!user) throw new Error('Usuario no autenticado');

        const { data, error } = await supabase
            .from('notes')
            .insert({
                ...note,
                user_id: user.id,
                // Ensure tags is an array if not provided
                tags: note.tags || []
            })
            .select()
            .single();

        if (error) {
            console.error('Error creating note:', error);
            throw error;
        }
        return data as Note;
    },

    async updateNote(id: string, updates: Partial<Note>) {
        const supabase = createClient();
        const { data, error } = await supabase
            .from('notes')
            .update({ ...updates, updated_at: new Date().toISOString() })
            .eq('id', id)
            .select()
            .single();

        if (error) throw error;
        return data as Note;
    },

    async deleteNote(id: string) {
        const supabase = createClient();
        const { error } = await supabase
            .from('notes')
            .delete()
            .eq('id', id);

        if (error) throw error;
    },

    async uploadAudio(file: Blob): Promise<string> {
        const supabase = createClient();
        const filename = `${Date.now()}-${Math.random().toString(36).substring(7)}.webm`;

        const { data, error } = await supabase
            .storage
            .from('note-attachments')
            .upload(filename, file, {
                cacheControl: '3600',
                upsert: false
            });

        if (error) throw error;

        const { data: { publicUrl } } = supabase
            .storage
            .from('note-attachments')
            .getPublicUrl(filename);

        return publicUrl;
    }
};
