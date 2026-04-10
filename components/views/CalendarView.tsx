'use client';

import { useState, useEffect, useRef } from 'react';
import { ChevronLeft, ChevronRight, Plus } from 'lucide-react';
import { useAuth } from '@/hooks/useAuth';
import {
    format, startOfMonth, endOfMonth, eachDayOfInterval, isSameMonth, isSameDay,
    addMonths, subMonths, startOfWeek, endOfWeek, addDays, addWeeks, subWeeks,
    getHours, getMinutes, differenceInMinutes
} from 'date-fns';
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
    const { session } = useAuth();
    const [currentDate, setCurrentDate] = useState(new Date());
    const [viewMode, setViewMode] = useState<ViewMode>('month');
    const [events, setEvents] = useState<CalendarEvent[]>([]);
    const [loading, setLoading] = useState(false);
    const timeGridRef = useRef<HTMLDivElement>(null);

    useEffect(() => {
        if (session) fetchEvents();
    }, [session, currentDate]);

    // Auto-scroll to current hour in week/day view
    useEffect(() => {
        if ((viewMode === 'week' || viewMode === 'day') && timeGridRef.current) {
            const HOUR_HEIGHT = viewMode === 'day' ? 64 : 56;
            timeGridRef.current.scrollTop = Math.max(0, (new Date().getHours() - 2) * HOUR_HEIGHT);
        }
    }, [viewMode]);

    const fetchEvents = async () => {
        setLoading(true);
        try {
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

    const navigatePrev = () => {
        if (viewMode === 'month') setCurrentDate(subMonths(currentDate, 1));
        else if (viewMode === 'week') setCurrentDate(subWeeks(currentDate, 1));
        else setCurrentDate(addDays(currentDate, -1));
    };

    const navigateNext = () => {
        if (viewMode === 'month') setCurrentDate(addMonths(currentDate, 1));
        else if (viewMode === 'week') setCurrentDate(addWeeks(currentDate, 1));
        else setCurrentDate(addDays(currentDate, 1));
    };

    const headerTitle = () => {
        if (viewMode === 'month') return format(currentDate, 'MMMM yyyy', { locale: es });
        if (viewMode === 'week') {
            const start = startOfWeek(currentDate, { weekStartsOn: 1 });
            const end = endOfWeek(currentDate, { weekStartsOn: 1 });
            return `${format(start, 'd MMM', { locale: es })} – ${format(end, 'd MMM yyyy', { locale: es })}`;
        }
        return format(currentDate, "EEEE, d 'de' MMMM yyyy", { locale: es });
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
                <div className="grid grid-cols-7 border-b border-slate-200 bg-slate-50">
                    {weekDays.map((day) => (
                        <div key={day} className="p-3 text-center text-xs font-semibold text-slate-500 uppercase tracking-wide">
                            {day}
                        </div>
                    ))}
                </div>
                <div className="grid grid-cols-7">
                    {days.map((day, idx) => {
                        const isCurrentMonth = isSameMonth(day, currentDate);
                        const isToday = isSameDay(day, new Date());
                        const dayEvents = events.filter(e => isSameDay(new Date(e.start), day));
                        return (
                            <div
                                key={idx}
                                className={`min-h-[90px] p-2 border-b border-r border-slate-100 hover:bg-blue-50/30 transition-colors cursor-pointer ${!isCurrentMonth ? 'bg-slate-50/60' : 'bg-white'}`}
                            >
                                <div className={`text-sm font-medium mb-1 w-7 h-7 flex items-center justify-center rounded-full ${
                                    isToday ? 'bg-blue-600 text-white' : isCurrentMonth ? 'text-slate-900' : 'text-slate-400'
                                }`}>
                                    {format(day, 'd')}
                                </div>
                                <div className="space-y-0.5">
                                    {dayEvents.slice(0, 3).map((event) => (
                                        <div key={event.id} className="text-xs bg-blue-100 text-blue-700 px-1.5 py-0.5 rounded truncate font-medium">
                                            {event.title}
                                        </div>
                                    ))}
                                    {dayEvents.length > 3 && (
                                        <div className="text-xs text-slate-400 pl-1">+{dayEvents.length - 3} más</div>
                                    )}
                                </div>
                            </div>
                        );
                    })}
                </div>
            </div>
        );
    };

    const renderWeekView = () => {
        const weekStart = startOfWeek(currentDate, { weekStartsOn: 1 });
        const weekDays = Array.from({ length: 7 }, (_, i) => addDays(weekStart, i));
        const hours = Array.from({ length: 24 }, (_, i) => i);
        const HOUR_HEIGHT = 56;
        const weekDayNames = ['Lun', 'Mar', 'Mié', 'Jue', 'Vie', 'Sáb', 'Dom'];

        return (
            <div className="bg-white rounded-2xl shadow-sm border border-slate-200 overflow-hidden">
                {/* Day headers */}
                <div className="grid border-b border-slate-200 bg-slate-50 sticky top-0 z-10" style={{ gridTemplateColumns: '56px repeat(7, 1fr)' }}>
                    <div className="border-r border-slate-200" />
                    {weekDays.map((day, i) => {
                        const isToday = isSameDay(day, new Date());
                        return (
                            <div key={i} className="p-2 text-center border-r border-slate-100 last:border-0">
                                <p className="text-xs font-semibold text-slate-500 uppercase tracking-wide">{weekDayNames[i]}</p>
                                <div className={`text-lg font-bold mx-auto w-9 h-9 flex items-center justify-center rounded-full mt-0.5 ${
                                    isToday ? 'bg-blue-600 text-white' : 'text-slate-800'
                                }`}>
                                    {format(day, 'd')}
                                </div>
                            </div>
                        );
                    })}
                </div>

                {/* Time grid */}
                <div className="overflow-y-auto" style={{ maxHeight: '580px' }} ref={timeGridRef}>
                    <div className="relative" style={{ display: 'grid', gridTemplateColumns: '56px repeat(7, 1fr)' }}>
                        {/* Hour labels */}
                        <div>
                            {hours.map((hour) => (
                                <div key={hour} style={{ height: HOUR_HEIGHT }} className="border-b border-slate-100 pr-2 pt-1 text-right">
                                    <span className="text-xs text-slate-400 font-mono">
                                        {hour === 0 ? '' : `${String(hour).padStart(2, '0')}:00`}
                                    </span>
                                </div>
                            ))}
                        </div>

                        {/* Day columns */}
                        {weekDays.map((day, dayIdx) => {
                            const dayEvents = events.filter(e => isSameDay(new Date(e.start), day));
                            return (
                                <div key={dayIdx} className="relative border-l border-slate-100">
                                    {hours.map((hour) => (
                                        <div key={hour} style={{ height: HOUR_HEIGHT }} className="border-b border-slate-100" />
                                    ))}
                                    {dayEvents.map((event) => {
                                        const startMins = getHours(new Date(event.start)) * 60 + getMinutes(new Date(event.start));
                                        const duration = Math.max(differenceInMinutes(new Date(event.end), new Date(event.start)), 30);
                                        const top = (startMins / 60) * HOUR_HEIGHT;
                                        const height = Math.max((duration / 60) * HOUR_HEIGHT, 22);
                                        return (
                                            <div
                                                key={event.id}
                                                className="absolute left-0.5 right-0.5 bg-blue-100 border-l-2 border-blue-500 text-blue-800 rounded px-1.5 py-0.5 text-xs overflow-hidden cursor-pointer hover:bg-blue-200 transition-colors"
                                                style={{ top, height }}
                                            >
                                                <p className="font-semibold truncate leading-tight">{event.title}</p>
                                                <p className="text-blue-600 opacity-80">{format(new Date(event.start), 'HH:mm')}</p>
                                            </div>
                                        );
                                    })}
                                </div>
                            );
                        })}
                    </div>
                </div>
            </div>
        );
    };

    const renderDayView = () => {
        const hours = Array.from({ length: 24 }, (_, i) => i);
        const HOUR_HEIGHT = 64;
        const dayEvents = events.filter(e => isSameDay(new Date(e.start), currentDate));
        const isToday = isSameDay(currentDate, new Date());

        return (
            <div className="bg-white rounded-2xl shadow-sm border border-slate-200 overflow-hidden">
                {/* Day header */}
                <div className="p-4 border-b border-slate-200 bg-slate-50 text-center">
                    <p className="text-xs font-semibold text-slate-500 uppercase tracking-wide mb-1">
                        {format(currentDate, 'EEEE', { locale: es })}
                    </p>
                    <div className={`text-3xl font-bold inline-flex items-center justify-center w-12 h-12 rounded-full ${
                        isToday ? 'bg-blue-600 text-white' : 'text-slate-900'
                    }`}>
                        {format(currentDate, 'd')}
                    </div>
                    {dayEvents.length > 0 && (
                        <p className="text-xs text-slate-500 mt-1">{dayEvents.length} evento{dayEvents.length !== 1 ? 's' : ''}</p>
                    )}
                </div>

                {/* Hourly timeline */}
                <div className="overflow-y-auto" style={{ maxHeight: '580px' }} ref={timeGridRef}>
                    <div className="relative grid" style={{ gridTemplateColumns: '64px 1fr' }}>
                        {/* Hour gutter */}
                        <div>
                            {hours.map((hour) => (
                                <div key={hour} style={{ height: HOUR_HEIGHT }} className="border-b border-slate-100 pr-3 pt-1 text-right">
                                    <span className="text-xs text-slate-400 font-mono">
                                        {hour === 0 ? '' : `${String(hour).padStart(2, '0')}:00`}
                                    </span>
                                </div>
                            ))}
                        </div>

                        {/* Events column */}
                        <div className="relative border-l border-slate-100">
                            {hours.map((hour) => (
                                <div key={hour} style={{ height: HOUR_HEIGHT }} className="border-b border-slate-100" />
                            ))}
                            {dayEvents.map((event) => {
                                const startMins = getHours(new Date(event.start)) * 60 + getMinutes(new Date(event.start));
                                const duration = Math.max(differenceInMinutes(new Date(event.end), new Date(event.start)), 30);
                                const top = (startMins / 60) * HOUR_HEIGHT;
                                const height = Math.max((duration / 60) * HOUR_HEIGHT, 30);
                                return (
                                    <div
                                        key={event.id}
                                        className="absolute left-2 right-2 bg-blue-100 border-l-4 border-blue-500 text-blue-800 rounded-r-xl px-3 py-2 overflow-hidden cursor-pointer hover:bg-blue-200 transition-colors"
                                        style={{ top, height }}
                                    >
                                        <p className="font-bold text-sm truncate">{event.title}</p>
                                        <p className="text-xs text-blue-600">
                                            {format(new Date(event.start), 'HH:mm')} – {format(new Date(event.end), 'HH:mm')}
                                        </p>
                                        {event.description && height > 50 && (
                                            <p className="text-xs text-slate-500 mt-1 line-clamp-2">{event.description}</p>
                                        )}
                                    </div>
                                );
                            })}
                        </div>
                    </div>
                </div>
            </div>
        );
    };

    return (
        <div className="h-full bg-slate-50 p-4 md:p-6 overflow-y-auto">
            {/* Header */}
            <div className="mb-5">
                <div className="flex items-center justify-between mb-4 gap-3">
                    <h2 className="text-2xl font-bold text-slate-900 capitalize truncate">
                        {headerTitle()}
                    </h2>
                    <button className="flex items-center gap-2 px-4 py-2 bg-blue-600 text-white rounded-xl hover:bg-blue-700 transition-colors text-sm font-medium shrink-0 shadow-sm">
                        <Plus className="w-4 h-4" />
                        <span className="hidden sm:inline">Nuevo Evento</span>
                    </button>
                </div>

                <div className="flex items-center justify-between gap-3">
                    <div className="flex items-center gap-1">
                        <button onClick={navigatePrev} className="p-2 hover:bg-slate-100 rounded-lg transition-colors">
                            <ChevronLeft className="w-5 h-5 text-slate-600" />
                        </button>
                        <button
                            onClick={() => setCurrentDate(new Date())}
                            className="px-3 py-1.5 text-sm font-medium text-slate-700 hover:bg-slate-100 rounded-lg transition-colors"
                        >
                            Hoy
                        </button>
                        <button onClick={navigateNext} className="p-2 hover:bg-slate-100 rounded-lg transition-colors">
                            <ChevronRight className="w-5 h-5 text-slate-600" />
                        </button>
                    </div>

                    {/* View mode selector */}
                    <div className="flex gap-1 bg-slate-100 p-1 rounded-xl">
                        {(['month', 'week', 'day'] as ViewMode[]).map((mode) => (
                            <button
                                key={mode}
                                onClick={() => setViewMode(mode)}
                                className={`px-3 py-1.5 text-xs font-semibold rounded-lg transition-colors ${
                                    viewMode === mode
                                        ? 'bg-white text-slate-900 shadow-sm'
                                        : 'text-slate-500 hover:text-slate-800'
                                }`}
                            >
                                {mode === 'month' ? 'Mes' : mode === 'week' ? 'Semana' : 'Día'}
                            </button>
                        ))}
                    </div>
                </div>
            </div>

            {/* Calendar content */}
            {loading ? (
                <div className="flex items-center justify-center h-64">
                    <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600" />
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
