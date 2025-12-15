import { google } from 'googleapis';
import { OAuth2Client } from 'google-auth-library';

export class GoogleService {
    private auth: OAuth2Client;

    constructor(accessToken: string) {
        this.auth = new google.auth.OAuth2();
        this.auth.setCredentials({ access_token: accessToken });
    }

    // --- CALENDAR ---
    async createEvent(title: string, start: Date, end: Date) {
        const calendar = google.calendar({ version: 'v3', auth: this.auth });
        const event = {
            summary: title,
            start: { dateTime: start.toISOString() },
            end: { dateTime: end.toISOString() },
        };
        const res = await calendar.events.insert({
            calendarId: 'primary',
            requestBody: event,
        });
        return res.data;
    }

    // --- TASKS ---
    async createTask(title: string) {
        const tasks = google.tasks({ version: 'v1', auth: this.auth });
        // First get the default list
        const lists = await tasks.tasklists.list();
        const taskListId = lists.data.items?.[0]?.id || '@default';

        const res = await tasks.tasks.insert({
            tasklist: taskListId,
            requestBody: { title: title },
        });
        return res.data;
    }

    // --- GMAIL (Draft) ---
    async createDraft(recipient: string, subject: string, body: string) {
        const gmail = google.gmail({ version: 'v1', auth: this.auth });

        // Construct email in RFC 822 format
        const str = [
            `To: ${recipient}`,
            `Subject: ${subject}`,
            'Content-Type: text/html; charset=utf-8',
            'MIME-Version: 1.0',
            '',
            body
        ].join('\n');

        const encodedMessage = Buffer.from(str)
            .toString('base64')
            .replace(/\+/g, '-')
            .replace(/\//g, '_')
            .replace(/=+$/, '');

        const res = await gmail.users.drafts.create({
            userId: 'me',
            requestBody: {
                message: {
                    raw: encodedMessage
                }
            }
        });
        return res.data;
    }

    // --- CONTACTS ---
    async createContact(name: string, email?: string, phone?: string) {
        const people = google.people({ version: 'v1', auth: this.auth });

        const contactBody: any = {
            names: [{ givenName: name }],
        };

        if (email) contactBody.emailAddresses = [{ value: email }];
        if (phone) contactBody.phoneNumbers = [{ value: phone }];

        const res = await people.people.createContact({
            requestBody: contactBody
        });
        return res.data;
    }
}
