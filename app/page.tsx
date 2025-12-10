'use client';
import { useStore } from '@/store';
import Sidebar from '@/components/Sidebar';
import ChatView from '@/components/views/ChatView';
import CalendarView from '@/components/views/CalendarView';
import TasksView from '@/components/views/TasksView';
import EmailsView from '@/components/views/EmailsView';
import TemplatesView from '@/components/views/TemplatesView';
import SettingsView from '@/components/views/SettingsView';

export default function Home() {
    const { currentView } = useStore();

    const renderView = () => {
        switch (currentView) {
            case 'chat': return <ChatView />;
            case 'calendar': return <CalendarView />;
            case 'tasks': return <TasksView />;
            case 'emails': return <EmailsView />;
            case 'templates': return <TemplatesView />;
            case 'settings': return <SettingsView />;
            default: return <ChatView />;
        }
    };

    return (
        <div className="flex h-full w-full">
            <Sidebar />
            <main className="flex-1 h-full overflow-hidden relative">
                {renderView()}
            </main>
        </div>
    );
}
