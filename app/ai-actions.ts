'use server';

import { GoogleGenerativeAI, SchemaType } from '@google/generative-ai';
import { NotesService } from '@/lib/notes-service';
import { createServerSupabaseClient } from '@/lib/supabase/server';
import { GoogleService } from '@/lib/google-service';

// Google Gemini — tier gratuito: 15 RPM, 1M tokens/día con gemini-2.0-flash
const genAI = new GoogleGenerativeAI(process.env.GEMINI_API_KEY!);

const toolDeclarations = [
    {
        name: 'create_appointment',
        description: 'Schedule a new meeting or appointment in the calendar.',
        parameters: {
            type: SchemaType.OBJECT,
            properties: {
                title: { type: SchemaType.STRING, description: 'Title of the meeting' },
                startOffsetDays: { type: SchemaType.NUMBER, description: 'Number of days from today to start (e.g., 1 for tomorrow)' },
                durationHours: { type: SchemaType.NUMBER, description: 'Duration in hours' },
            },
            required: ['title', 'startOffsetDays'],
        },
    },
    {
        name: 'create_note',
        description: 'Save a note, task, or meeting summary.',
        parameters: {
            type: SchemaType.OBJECT,
            properties: {
                title: { type: SchemaType.STRING, description: 'Title of the note' },
                content: { type: SchemaType.STRING, description: 'Content or details of the note' },
                tags: {
                    type: SchemaType.ARRAY,
                    items: { type: SchemaType.STRING },
                    description: 'Tags for organization',
                },
            },
            required: ['title'],
        },
    },
    {
        name: 'create_email_draft',
        description: 'Draft a new email.',
        parameters: {
            type: SchemaType.OBJECT,
            properties: {
                recipient: { type: SchemaType.STRING, description: 'Email address of recipient' },
                subject: { type: SchemaType.STRING, description: 'Subject line' },
                body: { type: SchemaType.STRING, description: 'Body content of the email' },
            },
            required: ['subject', 'body'],
        },
    },
    {
        name: 'create_template',
        description: 'Save a text template for future use.',
        parameters: {
            type: SchemaType.OBJECT,
            properties: {
                name: { type: SchemaType.STRING, description: 'Name of the template' },
                content: { type: SchemaType.STRING, description: 'Content of the template' },
            },
            required: ['name', 'content'],
        },
    },
    {
        name: 'create_contact',
        description: 'Add a new contact to the address book.',
        parameters: {
            type: SchemaType.OBJECT,
            properties: {
                name: { type: SchemaType.STRING, description: 'Full name of the contact' },
                phone: { type: SchemaType.STRING, description: 'Phone number' },
                email: { type: SchemaType.STRING, description: 'Email address' },
            },
            required: ['name'],
        },
    },
];

export interface AIResponse {
    message: string;
    action?: {
        type: 'create_appointment' | 'create_note' | 'create_email' | 'create_template' | 'switch_view';
        data: any;
    };
    actions?: {
        type: 'create_appointment' | 'create_note' | 'create_email' | 'create_template' | 'switch_view' | 'create_contact';
        data: any;
    }[];
}

export async function processUserCommand(text: string): Promise<AIResponse> {
    try {
        const supabase = await createServerSupabaseClient();
        const { data: { session } } = await supabase.auth.getSession();
        const accessToken = (session as any)?.provider_token as string | undefined;

        let googleService: GoogleService | null = null;
        if (accessToken) {
            googleService = new GoogleService(accessToken);
        }

        const model = genAI.getGenerativeModel({
            model: 'gemini-2.0-flash',
            tools: [{ functionDeclarations: toolDeclarations as any }],
            systemInstruction: `Eres Alfred, un asistente personal inteligente, sofisticado y eficiente, al estilo de un mayordomo moderno.
Tu objetivo es ayudar al usuario a gestionar su agenda, notas y correos de manera impecable.

Reglas:
1. Si el usuario pide crear algo (cita, nota, email, contacto), USA LAS TOOLS disponibles.
2. PUEDES Y DEBES USAR MÚLTIPLES TOOLS en una sola respuesta si el usuario pide varias cosas.
3. Sé profesional, servicial y elegante en tu trato.
4. Si usas tools, SIEMPRE responde confirmando TODAS las acciones realizadas.
5. Si el usuario pide recordar algo, crear una tarea o guardar un resumen, crea una NOTA (create_note).
6. Responde siempre en español.`,
        });

        const result = await model.generateContent({
            contents: [{ role: 'user', parts: [{ text }] }],
        });

        const response = result.response;
        const functionCalls = response.functionCalls();

        if (!functionCalls || functionCalls.length === 0) {
            const textResponse = response.text();
            return { message: textResponse || 'No entendí eso, ¿puedes repetir?' };
        }

        const actions: AIResponse['actions'] = [];
        let feedbackMessage = '';

        for (const call of functionCalls) {
            const fnName = call.name;
            const args = call.args as any;

            try {
                if (fnName === 'create_appointment') {
                    if (googleService) {
                        const start = new Date();
                        start.setDate(start.getDate() + (args.startOffsetDays || 1));
                        start.setHours(10, 0, 0, 0);
                        const end = new Date(start.getTime() + (args.durationHours || 1) * 3600000);
                        await googleService.createEvent(args.title, start, end);
                        feedbackMessage += `✓ Agendada en Google Calendar: "${args.title}". `;
                    } else {
                        feedbackMessage += `✓ Cita creada: "${args.title}". `;
                    }
                    actions.push({ type: 'create_appointment', data: args });
                }

                if (fnName === 'create_note') {
                    await NotesService.createNote({
                        title: args.title,
                        content: args.content || '',
                        tags: args.tags || [],
                    });
                    feedbackMessage += `✓ Nota guardada: "${args.title}". `;
                    actions.push({ type: 'create_note', data: args });
                }

                if (fnName === 'create_email_draft') {
                    if (googleService) {
                        await googleService.createDraft(args.recipient || '', args.subject, args.body);
                        feedbackMessage += `✓ Borrador creado en Gmail: "${args.subject}". `;
                    } else {
                        feedbackMessage += `✓ Borrador de email: "${args.subject}". `;
                    }
                    actions.push({ type: 'create_email', data: args });
                }

                if (fnName === 'create_contact') {
                    if (googleService) {
                        await googleService.createContact(args.name, args.email, args.phone);
                        feedbackMessage += `✓ Contacto añadido a Google: ${args.name}. `;
                    } else {
                        feedbackMessage += `✓ Contacto guardado: ${args.name}. `;
                    }
                    actions.push({ type: 'create_contact', data: args });
                }

                if (fnName === 'create_template') {
                    feedbackMessage += `✓ Plantilla "${args.name}" guardada. `;
                    actions.push({ type: 'create_template', data: args });
                }
            } catch (err: any) {
                console.error(`Error executing tool ${fnName}:`, err);
                feedbackMessage += `Error al ejecutar "${fnName}": ${err.message}. `;
            }
        }

        return {
            message: feedbackMessage || '✓ Acciones realizadas.',
            actions,
        };

    } catch (error: any) {
        console.error('AI Error:', error);
        return { message: `Lo siento, hubo un error técnico: ${error.message || 'Error desconocido'}` };
    }
}
