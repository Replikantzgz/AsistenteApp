const OpenAI = require('openai');

async function testDeepSeek() {
    console.log("Testing DeepSeek API...");
    const apiKey = "sk-e64406b4302f499b9e8f273f08233119";

    console.log("Using Hardcoded Key");

    const client = new OpenAI({
        apiKey: apiKey,
        baseURL: 'https://api.deepseek.com',
    });

    const tools = [
        {
            type: 'function',
            function: {
                name: 'create_contact',
                description: 'Add a new contact to the address book.',
                parameters: {
                    type: 'object',
                    properties: {
                        name: { type: 'string', description: 'Full name' }
                    },
                    required: ['name']
                }
            }
        }
    ];

    try {
        const response = await client.chat.completions.create({
            model: 'deepseek-chat',
            messages: [
                { role: 'user', content: 'Agrega a Santiago a mis contactos' }
            ],
            tools: tools,
            tool_choice: 'auto',
        });

        console.log("✅ Response received!");
        console.log(JSON.stringify(response.choices[0], null, 2));

    } catch (error) {
        console.error("❌ API Error:", error);
    }
}

testDeepSeek();
