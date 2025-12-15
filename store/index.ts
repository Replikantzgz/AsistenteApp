import { create } from 'zustand';
import { addDays, format } from 'date-fns';

export type ViewType = 'chat' | 'calendar' | 'tasks' | 'emails' | 'templates' | 'contacts' | 'settings';

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

interface AppState {
    currentView: ViewType;
    setView: (view: ViewType) => void;

    appointments: Appointment[];
    addAppointment: (appt: Appointment) => void;
    updateAppointment: (id: string, updates: Partial<Appointment>) => void;

    tasks: Task[];
    addTask: (task: Task) => void;
    toggleTask: (id: string) => void;

    emails: Email[];
    addEmail: (email: Email) => void;

    templates: Template[];
    addTemplate: (template: Template) => void;

    toggleSidebar: boolean;
    setToggleSidebar: (v: boolean) => void;

    messages: Message[];
    addMessage: (msg: Message) => void;
    setMessages: (msgs: Message[]) => void;

    // User Profile
    userName: string;
    setUserName: (name: string) => void;
}

export interface Message {
    role: 'user' | 'assistant';
    content: string;
}

export const useStore = create<AppState>((set) => ({
    currentView: 'chat',
    setView: (view) => set({ currentView: view }),

    userName: 'Usuario', // Default
    setUserName: (name) => set({ userName: name }),

    appointments: [
        { id: '1', title: 'Reunión inicial', start: new Date(), end: addDays(new Date(), 0) } // Example
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

    emails: [],
    addEmail: (email) => set((state) => ({ emails: [...state.emails, email] })),

    templates: [
        { id: '1', name: 'Respuesta rápida', content: 'Gracias por tu mensaje. Lo revisaré pronto.' }
    ],
    addTemplate: (template) => set((state) => ({ templates: [...state.templates, template] })),

    toggleSidebar: true,
    setToggleSidebar: (v) => set({ toggleSidebar: v }),

    // Chats
    messages: [],
    addMessage: (msg) => set((state) => ({ messages: [...state.messages, msg] })),
    setMessages: (msgs) => set({ messages: msgs }),
}));
