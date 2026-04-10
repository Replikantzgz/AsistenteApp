import { create } from 'zustand';
import { persist, createJSONStorage } from 'zustand/middleware';
import { addDays } from 'date-fns';

export type ViewType = 'chat' | 'calendar' | 'notes' | 'emails' | 'templates' | 'contacts' | 'settings';

export interface Appointment {
    id: string;
    title: string;
    start: Date;
    end: Date;
}

export interface Task {
    id: string;
    title: string;
    priority: 'high' | 'medium' | 'low';
    status: 'pending' | 'done';
}

export interface Email {
    id: string;
    subject: string;
    body: string;
    recipient: string;
    type: 'draft' | 'sent' | 'inbox';
}

export interface Template {
    id: string;
    name: string;
    content: string;
}

export interface Contact {
    id: string;
    name: string;
    role: string;
    email: string;
    phone: string;
    avatar: string;
}

export interface Message {
    role: 'user' | 'assistant';
    content: string;
}

interface AppState {
    currentView: ViewType;
    setView: (view: ViewType) => void;

    appointments: Appointment[];
    addAppointment: (appt: Appointment) => void;
    updateAppointment: (id: string, updates: Partial<Appointment>) => void;

    tasks: Task[];
    addTask: (task: Task) => void;
    toggleTask: (id: string) => void;
    deleteTask: (id: string) => void;

    emails: Email[];
    addEmail: (email: Email) => void;

    templates: Template[];
    addTemplate: (template: Template) => void;
    deleteTemplate: (id: string) => void;

    contacts: Contact[];
    addContact: (contact: Contact) => void;

    toggleSidebar: boolean;
    setToggleSidebar: (v: boolean) => void;

    triggerAction: number | null;
    setTriggerAction: (action: number | null) => void;

    messages: Message[];
    addMessage: (msg: Message) => void;
    setMessages: (msgs: Message[]) => void;

    userName: string;
    setUserName: (name: string) => void;
}

export const useStore = create<AppState>()(
    persist(
        (set) => ({
            currentView: 'chat',
            setView: (view) => set({ currentView: view }),

            userName: 'Usuario',
            setUserName: (name) => set({ userName: name }),

            appointments: [
                { id: '1', title: 'Reunión inicial', start: new Date(), end: addDays(new Date(), 0) },
            ],
            addAppointment: (appt) => set((state) => ({ appointments: [...state.appointments, appt] })),
            updateAppointment: (id, updates) => set((state) => ({
                appointments: state.appointments.map((a) => (a.id === id ? { ...a, ...updates } : a))
            })),

            tasks: [
                { id: '1', title: 'Revisar presupuesto', priority: 'high', status: 'pending' }
            ],
            addTask: (task) => set((state) => ({ tasks: [...state.tasks, task] })),
            toggleTask: (id) => set((state) => ({
                tasks: state.tasks.map((t) => (t.id === id ? { ...t, status: t.status === 'pending' ? 'done' : 'pending' } : t))
            })),
            deleteTask: (id) => set((state) => ({ tasks: state.tasks.filter((t) => t.id !== id) })),

            emails: [],
            addEmail: (email) => set((state) => ({ emails: [...state.emails, email] })),

            templates: [
                { id: '1', name: 'Respuesta rápida', content: 'Gracias por tu mensaje. Lo revisaré pronto.' }
            ],
            addTemplate: (template) => set((state) => ({ templates: [...state.templates, template] })),
            deleteTemplate: (id) => set((state) => ({ templates: state.templates.filter((t) => t.id !== id) })),

            contacts: [
                { id: '1', name: 'Alejandro Garcia', role: 'CEO TechCorp', email: 'alex@techcorp.com', phone: '+34 600 123 456', avatar: 'AG' },
                { id: '2', name: 'Beatriz Lopez', role: 'Diseñadora', email: 'bea@design.studio', phone: '+34 600 987 654', avatar: 'BL' },
                { id: '3', name: 'Carlos Ruiz', role: 'Desarrollador', email: 'carlos@dev.io', phone: '+34 600 555 123', avatar: 'CR' },
                { id: '4', name: 'Diana Martin', role: 'Product Manager', email: 'diana@pm.co', phone: '+34 600 444 888', avatar: 'DM' },
            ],
            addContact: (contact) => set((state) => ({ contacts: [...state.contacts, contact] })),

            toggleSidebar: true,
            setToggleSidebar: (v) => set({ toggleSidebar: v }),

            triggerAction: null,
            setTriggerAction: (action) => set({ triggerAction: action }),

            messages: [],
            addMessage: (msg) => set((state) => ({ messages: [...state.messages, msg] })),
            setMessages: (msgs) => set({ messages: msgs }),
        }),
        {
            name: 'alfred-store',
            storage: createJSONStorage(() => localStorage),
            // Persist data but not transient UI state
            partialize: (state) => ({
                tasks: state.tasks,
                emails: state.emails,
                templates: state.templates,
                contacts: state.contacts,
                appointments: state.appointments,
                messages: state.messages,
                userName: state.userName,
            }),
            // Revive Date objects after JSON parse (dates become strings)
            onRehydrateStorage: () => (state) => {
                if (state) {
                    state.appointments = state.appointments.map((a) => ({
                        ...a,
                        start: new Date(a.start),
                        end: new Date(a.end),
                    }));
                }
            },
        }
    )
);
