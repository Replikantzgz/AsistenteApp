import fs from 'fs';
import path from 'path';

const USAGE_FILE = path.join(process.cwd(), 'usage.json');

interface UsageData {
    [userId: string]: {
        count: number;
        lastReset: string; // ISO date
    };
}

const LIMIT_PER_DAY = 50; // Example limit for fallback

export function checkUsageLimit(userId: string): boolean {
    try {
        let data: UsageData = {};

        if (fs.existsSync(USAGE_FILE)) {
            const content = fs.readFileSync(USAGE_FILE, 'utf-8');
            data = JSON.parse(content);
        }

        const today = new Date().toISOString().split('T')[0];
        const userUsage = data[userId] || { count: 0, lastReset: today };

        if (userUsage.lastReset !== today) {
            userUsage.count = 0;
            userUsage.lastReset = today;
        }

        if (userUsage.count >= LIMIT_PER_DAY) {
            return false; // Limit exceeded
        }

        return true;
    } catch (error) {
        console.error("Error checking usage:", error);
        return true; // Fail open if error
    }
}

export function incrementUsage(userId: string) {
    try {
        let data: UsageData = {};

        if (fs.existsSync(USAGE_FILE)) {
            const content = fs.readFileSync(USAGE_FILE, 'utf-8');
            data = JSON.parse(content);
        }

        const today = new Date().toISOString().split('T')[0];
        const userUsage = data[userId] || { count: 0, lastReset: today };

        if (userUsage.lastReset !== today) {
            userUsage.count = 0;
            userUsage.lastReset = today;
        }

        userUsage.count++;
        data[userId] = userUsage;

        fs.writeFileSync(USAGE_FILE, JSON.stringify(data, null, 2));
    } catch (error) {
        console.error("Error incrementing usage:", error);
    }
}
