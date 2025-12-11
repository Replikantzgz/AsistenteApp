'use client';

import { useState, useEffect } from 'react';
import { useStore } from '@/store';
import Sidebar from '@/components/Sidebar';
import BottomNav from '@/components/BottomNav';
import OnboardingWizard from '@/components/OnboardingWizard';
import ChatView from '@/components/views/ChatView'; // This will be our "Home"
import CalendarView from '@/components/views/CalendarView';
import TasksView from '@/components/views/TasksView';
import EmailsView from '@/components/views/EmailsView';
import TemplatesView from '@/components/views/TemplatesView';
import ContactsView from '@/components/views/ContactsView';
import SettingsView from '@/components/views/SettingsView';

export default function Home() {
    const { currentView } = useStore();
    const [onboardingDone, setOnboardingDone] = useState(true); // Default true to avoid flash, check effect
    const [mounted, setMounted] = useState(false);

    useEffect(() => {
        setMounted(true);
        const done = localStorage.getItem('propel_onboarding_completed');
        if (!done) setOnboardingDone(false);
    }, []);

    const handleOnboardingComplete = () => {
        localStorage.setItem('propel_onboarding_completed', 'true');
        setOnboardingDone(true);
    };

    const renderView = () => {
        switch (currentView) {
            case 'chat': return <ChatView />;
            case 'calendar': return <CalendarView />;
            case 'tasks': return <TasksView />;
            case 'emails': return <EmailsView />;
            case 'templates': return <TemplatesView />;
            case 'contacts': return <ContactsView />;
            case 'settings': return <SettingsView />;
            default: return <ChatView />;
        }
    };

    if (!mounted) return null;

    return (
        <div className="flex h-full w-full bg-slate-50">
            {!onboardingDone && <OnboardingWizard onComplete={handleOnboardingComplete} />}

            {/* Desktop Sidebar (Optional/Hidden on mobile) */}
            <div className="hidden lg:block h-full">
                <Sidebar />
            </div>

            <main className="flex-1 h-full overflow-hidden relative pb-20 lg:pb-0">
                {renderView()}
            </main>

            {/* Mobile Bottom Nav */}
            <BottomNav />
        </div>
    );
}
