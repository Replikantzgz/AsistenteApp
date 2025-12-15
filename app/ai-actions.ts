'use server';

import OpenAI from 'openai';

// Client 1: OpenAI (GPT-4o-mini) - For "Pro" questions
const openai = new OpenAI({
    apiKey: process.env.OPENAI_API_KEY,
});

// Client 2: DeepSeek (V3) - For Tools & "Eco" questions
// DeepSeek uses an OpenAI-compatible API
const deepseek = new OpenAI({
    apiKey: process.env.DEEPSEEK_API_KEY,
    baseURL: 'https://api.deepseek.com',
});

export interface AIResponse {
    message: string;
    action?: { // Deprecated, kept for backward compatibility if needed, but we should use actions
        type: 'create_appointment' | 'create_task' | 'create_email' | 'create_template' | 'switch_view';
        data: any;
    };
    actions?: {
        type: 'create_appointment' | 'create_task' | 'create_email' | 'create_template' | 'switch_view' | 'create_contact'; // Added create_contact
        data: any;
    }[];
}

// Tool Definitions are shared
const tools: OpenAI.Chat.Completions.ChatCompletionTool[] = [
    {
        type: 'function',
        function: {
            name: 'create_appointment',
            description: 'Schedule a new meeting or appointment in the calendar.',
            parameters: {
                type: 'object',
                properties: {
                    title: { type: 'string', description: 'Title of the meeting' },
                    startOffsetDays: { type: 'number', description: 'Number of days from today to start (e.g., 1 for tomorrow)' },
                    durationHours: { type: 'number', description: 'Duration in hours', default: 1 }
                },
                required: ['title', 'startOffsetDays']
            }
        }
    },
    {
        type: 'function',
        function: {
            name: 'create_task',
            description: 'Add a new task or reminder to the list.',
            parameters: {
                type: 'object',
                properties: {
                    title: { type: 'string', description: 'Description of the task' },
                    priority: { type: 'string', enum: ['low', 'medium', 'high'], default: 'medium' }
                },
                required: ['title']
            }
        }
    },
    {
        type: 'function',
        function: {
            name: 'create_email_draft',
            description: 'Draft a new email.',
            parameters: {
                type: 'object',
                properties: {
                    recipient: { type: 'string', description: 'Email address of recipient' },
                    subject: { type: 'string', description: 'Subject line' },
                    body: { type: 'string', description: 'Body content of the email' }
                },
                required: ['subject', 'body']
            }
        }
    },
    {
        type: 'function',
        function: {
            name: 'create_template',
            description: 'Save a text template for future use.',
            parameters: {
                type: 'object',
                properties: {
                    name: { type: 'string', description: 'Name of the template' },
                    content: { type: 'string', description: 'Content of the template' }
                },
                required: ['name', 'content']
            }
        }
    },
    {
        type: 'function',
        function: {
            name: 'create_contact',
            description: 'Add a new contact to the address book.',
            parameters: {
                type: 'object',
                properties: {
                    name: { type: 'string', description: 'Full name of the contact' },
                    phone: { type: 'string', description: 'Phone number' },
                    email: { type: 'string', description: 'Email address' }
                },
                required: ['name']
            }
        }
    }
];

import { getServerSession } from 'next-auth';
import { authOptions } from '@/lib/auth-options';
import { GoogleService } from '@/lib/google-service';

export async function processUserCommand(text: string): Promise<AIResponse> {
    try {
        // --- AUTH & GOOGLE SERVICE ---
        const session: any = await getServerSession(authOptions);
        const accessToken = session?.accessToken;
        let googleService: GoogleService | null = null;

        if (accessToken) {
            googleService = new GoogleService(accessToken);
        } else {
            console.warn('⚠️ No access token found. Using simulation mode.');
        }

        // --- SMART ROUTING LOGIC ---
        // 1. Detect Intent: If the user uses specific keywords, force DeepSeek (Tools).
        // 2. Detect Plan: (Simulated) If Pro -> OpenAI for conversation. If Eco -> DeepSeek.

        const lowerText = text.toLowerCase();
        const actionKeywords = ['crear', 'nueva', 'nuevo', 'agendar', 'cita', 'reunión', 'tarea', 'recordatorio', 'email', 'correo', 'plantilla', 'agrega', 'contacto'];
        const isAction = actionKeywords.some(kw => lowerText.includes(kw));

        // TODO: Get real user plan from Supabase/Auth
        const userPlan: 'eco' | 'pro' = 'pro';
        // TODO: Check monthly usage limit for Pro

        let activeClient = openai; // Default to OpenAI (User request to use only GPT-4o-mini)
        let model = 'gpt-4o-mini';

        if (isAction) {
            // Actions now go to OpenAI too (DeepSeek failing)
            activeClient = openai;
            model = 'gpt-4o-mini';
            // console.log('🤖 Routing: Action detected -> Using GPT-4o-mini');
        } else if (userPlan === 'pro') {
            // General questions for Pro -> OpenAI
            activeClient = openai;
            model = 'gpt-4o-mini';
            // console.log('🤖 Routing: Pro Question -> Using GPT-4o-mini');
        } else {
            // General questions for Eco -> OpenAI (fallback for now)
            activeClient = openai;
            model = 'gpt-4o-mini';
        }

        const response = await activeClient.chat.completions.create({
            model: model,
            messages: [
                {
                    role: 'system',
                    content: `Eres Propel, un asistente personal inteligente y eficiente.
          Tu objetivo es ayudar al usuario a gestionar su agenda, tareas y correos.
          
          Reglas:
          1. Si el usuario pide crear algo (cita, tarea, email, contacto), USA LAS TOOLS disponibles.
          2. PUEDES Y DEBES USAR MÚLTIPLES TOOLS en una sola respuesta si el usuario pide varias cosas.
          3. Sé profesional pero cercano.
          4. Si usas tools, SIEMPRE responde confirmando TODAS las acciones realizadas.`
                },
                { role: 'user', content: text }
            ],
            tools: tools,
            tool_choice: 'auto',
        });

        const choice = response.choices[0];
        const toolCalls = choice.message.tool_calls;

        // If no tool called, return the conversation message
        if (!toolCalls || toolCalls.length === 0) {
            return {
                message: choice.message.content || 'No entendí eso, ¿puedes repetir?',
            };
        }

        // Handle Multiple Tools
        const actions: AIResponse['actions'] = [];
        let feedbackMessage = '';

        for (const toolCall of toolCalls) {
            const fnName = (toolCall as any).function.name;
            const args = JSON.parse((toolCall as any).function.arguments);

            try {
                if (fnName === 'create_appointment') {
                    if (googleService) {
                        const start = new Date();
                        start.setDate(start.getDate() + (args.startOffsetDays || 1));
                        start.setHours(10, 0, 0, 0); // Default to 10AM if time not specified (simplified)
                        // Ideally args should have exact time, but for now we use offset

                        const end = new Date(start.getTime() + (args.durationHours || 1) * 3600000);

                        await googleService.createEvent(args.title, start, end);
                        feedbackMessage += `Agendada en Google Calendar: "${args.title}". `;
                    } else {
                        feedbackMessage += `(Simulado) Agendada: "${args.title}". `;
                    }
                    actions.push({ type: 'create_appointment', data: args });
                }

                if (fnName === 'create_task') {
                    if (googleService) {
                        await googleService.createTask(args.title);
                        feedbackMessage += `Tarea creada en Google Tasks: "${args.title}". `;
                    } else {
                        feedbackMessage += `(Simulado) Tarea creada: "${args.title}". `;
                    }
                    actions.push({ type: 'create_task', data: args });
                }

                if (fnName === 'create_email_draft') {
                    if (googleService) {
                        await googleService.createDraft(args.recipient, args.subject, args.body);
                        feedbackMessage += `Borrador creado en Gmail: "${args.subject}". `;
                    } else {
                        feedbackMessage += `(Simulado) Borrador de email: "${args.subject}". `;
                    }
                    actions.push({ type: 'create_email', data: args });
                }

                if (fnName === 'create_contact') {
                    if (googleService) {
                        await googleService.createContact(args.name, args.email, args.phone);
                        feedbackMessage += `Contacto añadido a Google: ${args.name}. `;
                    } else {
                        feedbackMessage += `(Simulado) Contacto añadido: ${args.name}. `;
                    }
                    actions.push({ type: 'create_contact', data: args });
                }

                if (fnName === 'create_template') {
                    // Templates are internal only, no Google API
                    feedbackMessage += `Plantilla "${args.name}" guardada. `;
                    actions.push({ type: 'create_template', data: args });
                }

            } catch (err: any) {
                console.error(`Error executing tool ${fnName}:`, err);
                feedbackMessage += `Error al ejecutar ${fnName}: ${err.message}. `;
            }
        }

        return {
            message: feedbackMessage || choice.message.content || 'Acciones realizadas.',
            actions: actions
        };

    } catch (error: any) {
        console.error('AI Error:', error);
        // Return the actual error to the UI for better debugging
        return { message: `Lo siento, hubo un error técnico: ${error.message || JSON.stringify(error)}` };
    }
}
