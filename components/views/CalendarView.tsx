'use client';

import { useState, useEffect } from 'react';
import { ChevronLeft, ChevronRight, Plus } from 'lucide-react';
import { useSession } from 'next-auth/react';
import { format, startOfMonth, endOfMonth, eachDayOfInterval, isSameMonth, isSameDay, addMonths, subMonths, startOfWeek, endOfWeek } from 'date-fns';
import { es } from 'date-fns/locale';

type ViewMode = 'month' | 'week' | 'day';

interface CalendarEvent {
    id: string;
    title: string;
    start: Date;
    end: Date;
    description?: string;
}

export default function CalendarView() {
    const { data: session } = useSession();
    const [currentDate, setCurrentDate] = useState(new Date());
    const [viewMode, setViewMode] = useState<ViewMode>('month');
    const [events, setEvents] = useState<CalendarEvent[]>([]);
    const [loading, setLoading] = useState(false);

    useEffect(() => {
        if (session) {
            fetchEvents();
        }
    }, [session, currentDate]);

    const fetchEvents = async () => {
        setLoading(true);
        try {
            // TODO: Fetch real events from Google Calendar API
            // const res = await fetch('/api/calendar/events');
            // const data = await res.json();
            // setEvents(data);

            // Mock data for now
            setEvents([
                {
                    id: '1',
                    title: 'Reunión de Equipo',
                    start: new Date(),
                    end: new Date(Date.now() + 3600000),
                    description: 'Reunión semanal del equipo'
                }
            ]);
        } catch (error) {
            console.error('Error fetching events:', error);
        } finally {
            setLoading(false);
        }
    };

    const renderMonthView = () => {
        const monthStart = startOfMonth(currentDate);
        const monthEnd = endOfMonth(currentDate);
        const calendarStart = startOfWeek(monthStart, { weekStartsOn: 1 });
        const calendarEnd = endOfWeek(monthEnd, { weekStartsOn: 1 });
        const days = eachDayOfInterval({ start: calendarStart, end: calendarEnd });

        const weekDays = ['Lun', 'Mar', 'Mié', 'Jue', 'Vie', 'Sáb', 'Dom'];

        return (
            <div className="bg-white rounded-2xl shadow-sm border border-slate-200 overflow-hidden">
                {/* Week day headers */}
                <div className="grid grid-cols-7 border-b border-slate-200 bg-slate-50">
                    {weekDays.map((day) => (
                        <div key={day} className="p-3 text-center text-sm font-semibold text-slate-600">
                            {day}
                        </div>
                    ))}
                </div>

                {/* Calendar grid */}
                <div className="grid grid-cols-7">
                    {days.map((day, idx) => {
                        const isCurrentMonth = isSameMonth(day, currentDate);
                        const isToday = isSameDay(day, new Date());
                        const dayEvents = events.filter(event =>
                            isSameDay(new Date(event.start), day)
                        );

                        return (
                            <div
                                key={idx}
                                className={`min-h-[100px] p-2 border-b border-r border-slate-100 ${!isCurrentMonth ? 'bg-slate-50/50' : 'bg-white'
                                    } hover:bg-blue-50/30 transition-colors cursor-pointer`}
                            >
                                <div className={`text-sm font-medium mb-1 ${isToday
                                        ? 'w-7 h-7 bg-blue-600 text-white rounded-full flex items-center justify-center'
                                        : isCurrentMonth
                                            ? 'text-slate-900'
                                            : 'text-slate-400'
                                    }`}>
                                    {format(day, 'd')}
                                </div>
                                <div className="space-y-1">
                                    {dayEvents.map((event) => (
                                        <div
                                            key={event.id}
                                            className="text-xs bg-blue-100 text-blue-700 px-2 py-1 rounded truncate"
                                        >
                                            {event.title}
                                        </div>
                                    ))}
                                </div>
                            </div>
                        );
                    })}
                </div>
            </div>
        );
    };

    const renderWeekView = () => {
        return (
            <div className="bg-white rounded-2xl shadow-sm border border-slate-200 p-6">
                <p className="text-slate-500 text-center">Vista semanal - Próximamente</p>
            </div>
        );
    };

    const renderDayView = () => {
        return (
            <div className="bg-white rounded-2xl shadow-sm border border-slate-200 p-6">
                <p className="text-slate-500 text-center">Vista diaria - Próximamente</p>
            </div>
        );
    };

    return (
        <div className="h-full bg-slate-50 p-6 overflow-y-auto">
            {/* Header */}
            <div className="mb-6">
                <div className="flex items-center justify-between mb-4">
                    <h2 className="text-3xl font-bold text-slate-900">
                        {format(currentDate, 'MMMM yyyy', { locale: es })}
                    </h2>
                    <button className="flex items-center gap-2 px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors">
                        <Plus className="w-5 h-5" />
                        <span className="hidden sm:inline">Nuevo Evento</span>
                    </button>
                </div>

                {/* Navigation and view controls */}
                <div className="flex items-center justify-between">
                    <div className="flex items-center gap-2">
                        <button
                            onClick={() => setCurrentDate(subMonths(currentDate, 1))}
                            className="p-2 hover:bg-slate-100 rounded-lg transition-colors"
                        >
                            <ChevronLeft className="w-5 h-5 text-slate-600" />
                        </button>
                        <button
                            onClick={() => setCurrentDate(new Date())}
                            className="px-4 py-2 text-sm font-medium text-slate-700 hover:bg-slate-100 rounded-lg transition-colors"
                        >
                            Hoy
                        </button>
                        <button
                            onClick={() => setCurrentDate(addMonths(currentDate, 1))}
                            className="p-2 hover:bg-slate-100 rounded-lg transition-colors"
                        >
                            <ChevronRight className="w-5 h-5 text-slate-600" />
                        </button>
                    </div>

                    {/* View mode selector */}
                    <div className="flex gap-1 bg-slate-100 p-1 rounded-lg">
                        <button
                            onClick={() => setViewMode('month')}
                            className={`px-4 py-2 text-sm font-medium rounded-md transition-colors ${viewMode === 'month'
                                    ? 'bg-white text-slate-900 shadow-sm'
                                    : 'text-slate-600 hover:text-slate-900'
                                }`}
                        >
                            Mes
                        </button>
                        <button
                            onClick={() => setViewMode('week')}
                            className={`px-4 py-2 text-sm font-medium rounded-md transition-colors ${viewMode === 'week'
                                    ? 'bg-white text-slate-900 shadow-sm'
                                    : 'text-slate-600 hover:text-slate-900'
                                }`}
                        >
                            Semana
                        </button>
                        <button
                            onClick={() => setViewMode('day')}
                            className={`px-4 py-2 text-sm font-medium rounded-md transition-colors ${viewMode === 'day'
                                    ? 'bg-white text-slate-900 shadow-sm'
                                    : 'text-slate-600 hover:text-slate-900'
                                }`}
                        >
                            Día
                        </button>
                    </div>
                </div>
            </div>

            {/* Calendar content */}
            {loading ? (
                <div className="flex items-center justify-center h-64">
                    <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600"></div>
                </div>
            ) : (
                <>
                    {viewMode === 'month' && renderMonthView()}
                    {viewMode === 'week' && renderWeekView()}
                    {viewMode === 'day' && renderDayView()}
                </>
            )}
        </div>
    );
}
