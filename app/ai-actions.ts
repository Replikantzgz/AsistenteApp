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
    action?: {
        type: 'create_appointment' | 'create_task' | 'create_email' | 'create_template' | 'switch_view';
        data: any;
    };
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
    }
];

export async function processUserCommand(text: string): Promise<AIResponse> {
    try {
        // --- SMART ROUTING LOGIC ---
        // 1. Detect Intent: If the user uses specific keywords, force DeepSeek (Tools).
        // 2. Detect Plan: (Simulated) If Pro -> OpenAI for conversation. If Eco -> DeepSeek.

        const lowerText = text.toLowerCase();
        const actionKeywords = ['crear', 'nueva', 'nuevo', 'agendar', 'cita', 'reunión', 'tarea', 'recordatorio', 'email', 'correo', 'plantilla'];
        const isAction = actionKeywords.some(kw => lowerText.includes(kw));

        // TODO: Get real user plan from Supabase/Auth
        const userPlan: 'eco' | 'pro' = 'pro';
        // TODO: Check monthly usage limit for Pro

        let activeClient = deepseek; // Default to DeepSeek (Cheaper/Free logic)
        let model = 'deepseek-chat';

        if (isAction) {
            // Actions always go to DeepSeek (Cost optimization)
            activeClient = deepseek;
            model = 'deepseek-chat';
            // console.log('🤖 Routing: Action detected -> Using DeepSeek');
        } else if (userPlan === 'pro') {
            // General questions for Pro -> OpenAI
            activeClient = openai;
            model = 'gpt-4o-mini';
            // console.log('🤖 Routing: Pro Question -> Using GPT-4o-mini');
        } else {
            // General questions for Eco -> DeepSeek
            // console.log('🤖 Routing: Eco Question -> Using DeepSeek');
        }

        const response = await activeClient.chat.completions.create({
            model: model,
            messages: [
                {
                    role: 'system',
                    content: `Eres IATUALCANCE, un asistente personal inteligente y eficiente.
          Tu objetivo es ayudar al usuario a gestionar su agenda, tareas y correos.
          
          Reglas:
          1. Si el usuario pide crear algo (cita, tarea, email), USA LAS TOOLS disponibles.
          2. Sé profesional pero cercano.
          3. Si usas una tool, responde confirmando brevemente la acción.`
                },
                { role: 'user', content: text }
            ],
            tools: tools,
            tool_choice: 'auto',
        });

        const choice = response.choices[0];
        const toolCall = choice.message.tool_calls?.[0];

        // If no tool called, return the conversation message
        if (!toolCall) {
            return {
                message: choice.message.content || 'No entendí eso, ¿puedes repetir?',
            };
        }

        // Handle Tools (DeepSeek & OpenAI outputs are compatible)
        const fnName = (toolCall as any).function.name;
        const args = JSON.parse((toolCall as any).function.arguments);

        if (fnName === 'create_appointment') {
            return {
                message: `Entendido, he agendado: "${args.title}".`,
                action: { type: 'create_appointment', data: args }
            };
        }

        if (fnName === 'create_task') {
            return {
                message: `Anotado en tareas: "${args.title}".`,
                action: { type: 'create_task', data: args }
            };
        }

        if (fnName === 'create_email_draft') {
            return {
                message: `He creado el borrador del correo sobre "${args.subject}".`,
                action: { type: 'create_email', data: args }
            };
        }

        if (fnName === 'create_template') {
            return {
                message: `Plantilla "${args.name}" guardada.`,
                action: { type: 'create_template', data: args }
            };
        }

        return { message: 'Función no reconocida, pero te escuché.' };

    } catch (error) {
        console.error('AI Error:', error);
        return { message: 'Lo siento, tuve un problema conectando con mi cerebro digital. Intenta de nuevo.' };
    }
}
