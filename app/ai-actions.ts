import { useStore } from '@/store';
import { addDays } from 'date-fns';

// This is a simulated AI service. In a real app, this would call an LLM API.
export async function processUserCommand(text: string): Promise<string> {
    const store = useStore.getState();
    const lowerText = text.toLowerCase();

    // Artificial delay to simulate "thinking"
    await new Promise(resolve => setTimeout(resolve, 1000));

    if (lowerText.includes('cita') || lowerText.includes('reunión')) {
        store.setView('calendar');
        store.addAppointment({
            id: Date.now().toString(),
            title: 'Nueva Cita (IA Created)',
            start: addDays(new Date(), 1),
            end: addDays(new Date(), 1)
        });
        return 'He agendado una nueva cita para mañana en tu calendario.';
    }

    if (lowerText.includes('tarea') || lowerText.includes('recordatorio')) {
        store.setView('tasks');
        store.addTask({
            id: Date.now().toString(),
            title: text.replace('crear tarea', '').replace('nueva tarea', '').trim() || 'Nueva tarea sin título',
            priority: 'medium',
            status: 'pending'
        });
        return 'Tarea guardada en tu lista.';
    }

    if (lowerText.includes('email') || lowerText.includes('correo')) {
        store.setView('emails');
        store.addEmail({
            id: Date.now().toString(),
            subject: 'Nuevo Borrador',
            body: 'Contenido generado por IA...',
            recipient: 'destinatario@ejemplo.com',
            type: 'draft'
        });
        return 'He empezado un borrador de correo para ti.';
    }

    if (lowerText.includes('plantilla')) {
        store.setView('templates');
        store.addTemplate({
            id: Date.now().toString(),
            name: 'Nueva Plantilla',
            content: text
        });
        return 'Plantilla guardada.';
    }

    if (lowerText.includes('hola') || lowerText.includes('buenos días')) {
        return '¡Hola! Soy tu asistente IATUALCANCE. ¿En qué puedo ayudarte hoy?';
    }

    return 'Entendido. He procesado tu solicitud "' + text + '". (Simulación)';
}
