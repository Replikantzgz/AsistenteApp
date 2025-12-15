'use client';
import { useState } from 'react';
import { useStore } from '@/store';
import {
    format,
    addMonths,
    subMonths,
    startOfMonth,
    endOfMonth,
    startOfWeek,
    endOfWeek,
    eachDayOfInterval,
    isSameMonth,
    isSameDay,
    addDays,
    addWeeks,
    subWeeks,
    subDays,
    startOfDay,
    isToday,
    parseISO,
    getHours
} from 'date-fns';
import { es } from 'date-fns/locale';
import { ChevronLeft, ChevronRight, Calendar as CalendarIcon, Clock } from 'lucide-react';
import clsx from 'clsx';
import { twMerge } from 'tailwind-merge';

type ViewType = 'month' | 'week' | 'day';

export default function CalendarView() {
    const { appointments } = useStore();
    const [currentDate, setCurrentDate] = useState(new Date());
    const [view, setView] = useState<ViewType>('month');

    const next = () => {
        if (view === 'month') setCurrentDate(addMonths(currentDate, 1));
        if (view === 'week') setCurrentDate(addWeeks(currentDate, 1));
        if (view === 'day') setCurrentDate(addDays(currentDate, 1));
    };

    const prev = () => {
        if (view === 'month') setCurrentDate(subMonths(currentDate, 1));
        if (view === 'week') setCurrentDate(subWeeks(currentDate, 1));
        if (view === 'day') setCurrentDate(subDays(currentDate, 1));
    };

    const today = () => setCurrentDate(new Date());

    const getEventsForDay = (day: Date) => {
        return appointments.filter(appt => isSameDay(appt.start, day));
    };

    return (
        <div className="h-full flex flex-col bg-white/50 p-6">
            {/* Calendar Header */}
            <div className="flex items-center justify-between mb-6">
                <div className="flex items-center gap-4">
                    <h2 className="text-2xl font-bold text-slate-800 capitalize min-w-[200px]">
                        {format(currentDate, view === 'day' ? "d 'de' MMMM" : 'MMMM yyyy', { locale: es })}
                    </h2>
                    <div className="flex items-center bg-white rounded-lg shadow-sm border border-slate-200 p-1">
                        <button onClick={prev} className="p-1 hover:bg-slate-100 rounded-md text-slate-600">
                            <ChevronLeft className="w-5 h-5" />
                        </button>
                        <button onClick={today} className="px-3 py-1 text-sm font-medium text-slate-600 hover:bg-slate-100 rounded-md mx-1">
                            Hoy
                        </button>
                        <button onClick={next} className="p-1 hover:bg-slate-100 rounded-md text-slate-600">
                            <ChevronRight className="w-5 h-5" />
                        </button>
                    </div>
                </div>

                <div className="flex bg-slate-100 p-1 rounded-lg">
                    {(['month', 'week', 'day'] as ViewType[]).map((v) => (
                        <button
                            key={v}
                            onClick={() => setView(v)}
                            className={clsx(
                                "px-4 py-1.5 rounded-md text-sm font-medium transition-all capitalize",
                                view === v
                                    ? "bg-white text-slate-900 shadow-sm"
                                    : "text-slate-500 hover:text-slate-700"
                            )}
                        >
                            {v === 'month' ? 'Mes' : v === 'week' ? 'Semana' : 'Día'}
                        </button>
                    ))}
                </div>
            </div>

            {/* Calendar Content */}
            <div className="flex-1 bg-white rounded-2xl shadow-sm border border-slate-200 overflow-hidden flex flex-col">
                {view === 'month' && <MonthView currentDate={currentDate} getEvents={getEventsForDay} />}
                {view === 'week' && <WeekView currentDate={currentDate} getEvents={getEventsForDay} />}
                {view === 'day' && <DayView currentDate={currentDate} getEvents={getEventsForDay} />}
            </div>
        </div>
    );
}

// Sub-components for cleaner file structure

function MonthView({ currentDate, getEvents }: { currentDate: Date, getEvents: (d: Date) => any[] }) {
    const monthStart = startOfMonth(currentDate);
    const monthEnd = endOfMonth(monthStart);
    const startDate = startOfWeek(monthStart, { weekStartsOn: 1 }); // Monday start
    const endDate = endOfWeek(monthEnd, { weekStartsOn: 1 });
    const days = eachDayOfInterval({ start: startDate, end: endDate });

    const weekDays = ['Lun', 'Mar', 'Mié', 'Jue', 'Vie', 'Sáb', 'Dom'];

    return (
        <div className="flex flex-col h-full">
            <div className="grid grid-cols-7 border-b border-slate-100">
                {weekDays.map(d => (
                    <div key={d} className="py-3 text-center text-xs font-semibold text-slate-400 uppercase tracking-wider">
                        {d}
                    </div>
                ))}
            </div>
            <div className="grid grid-cols-7 flex-1 auto-rows-fr">
                {days.map((day, dayIdx) => {
                    const events = getEvents(day);
                    return (
                        <div
                            key={day.toString()}
                            className={clsx(
                                "min-h-[100px] border-b border-r border-slate-50 p-2 transition-colors hover:bg-slate-50/50 flex flex-col gap-1 relative",
                                !isSameMonth(day, monthStart) && "bg-slate-50/30 text-slate-400",
                                isToday(day) && "bg-blue-50/30"
                            )}
                        >
                            <span className={clsx(
                                "text-sm font-medium w-7 h-7 flex items-center justify-center rounded-full mb-1",
                                isToday(day) ? "bg-blue-600 text-white" : "text-slate-700"
                            )}>
                                {format(day, 'd')}
                            </span>

                            {/* Events List */}
                            <div className="flex flex-col gap-1 overflow-y-auto max-h-[80px] scrollbar-hide">
                                {events.map((evt: any) => (
                                    <div key={evt.id} className="text-[10px] px-1.5 py-0.5 rounded bg-indigo-100 text-indigo-700 font-medium truncate border-l-2 border-indigo-500">
                                        {format(evt.start, 'HH:mm')} {evt.title}
                                    </div>
                                ))}
                            </div>
                        </div>
                    );
                })}
            </div>
        </div>
    );
}

function WeekView({ currentDate, getEvents }: { currentDate: Date, getEvents: (d: Date) => any[] }) {
    const startDate = startOfWeek(currentDate, { weekStartsOn: 1 });
    const days = Array.from({ length: 7 }).map((_, i) => addDays(startDate, i));
    const hours = Array.from({ length: 24 }).map((_, i) => i);

    return (
        <div className="flex flex-col h-full overflow-hidden">
            <div className="flex border-b border-slate-200 ml-14">
                {days.map(day => (
                    <div key={day.toString()} className={clsx(
                        "flex-1 py-3 text-center border-l border-slate-100",
                        isToday(day) && "bg-blue-50/20"
                    )}>
                        <p className="text-xs text-slate-500 uppercase font-medium">{format(day, 'EEE', { locale: es })}</p>
                        <p className={clsx(
                            "text-lg font-bold mx-auto w-8 h-8 flex items-center justify-center rounded-full mt-1",
                            isToday(day) ? "bg-blue-600 text-white" : "text-slate-800"
                        )}>
                            {format(day, 'd')}
                        </p>
                    </div>
                ))}
            </div>

            <div className="flex-1 overflow-y-auto relative">
                <div className="flex min-h-[1440px]"> {/* 60px per hour */}
                    {/* Time Column */}
                    <div className="w-14 flex flex-col text-xs text-slate-400 font-medium bg-slate-50/50 sticky left-0 z-10 border-r border-slate-200">
                        {hours.map(hour => (
                            <div key={hour} className="h-[60px] flex items-start justify-center pt-2 relative">
                                {hour}:00
                            </div>
                        ))}
                    </div>

                    {/* Days Columns */}
                    <div className="flex-1 flex">
                        {days.map(day => {
                            const events = getEvents(day);
                            return (
                                <div key={day.toString()} className="flex-1 border-l border-slate-100 relative group hover:bg-slate-50/10">
                                    {/* Grid Lines */}
                                    {hours.map(h => (
                                        <div key={h} className="h-[60px] border-b border-slate-50"></div>
                                    ))}

                                    {/* Events Overlay */}
                                    {events.map((evt: any) => {
                                        const start = evt.start;
                                        const end = evt.end || start; // fallback
                                        const startHour = start.getHours() + (start.getMinutes() / 60);
                                        const duration = (end.getTime() - start.getTime()) / (1000 * 60 * 60) || 1; // default 1h

                                        return (
                                            <div
                                                key={evt.id}
                                                className="absolute left-1 right-1 rounded-md bg-indigo-100 border-l-4 border-indigo-500 p-1 text-xs text-indigo-800 overflow-hidden shadow-sm hover:shadow-md hover:z-20 transition-all cursor-pointer"
                                                style={{
                                                    top: `${startHour * 60}px`,
                                                    height: `${duration * 60}px`
                                                }}
                                            >
                                                <div className="font-bold">{evt.title}</div>
                                                <div className="opacity-80">{format(start, 'HH:mm')} - {format(end, 'HH:mm')}</div>
                                            </div>
                                        );
                                    })}
                                </div>
                            );
                        })}
                    </div>
                </div>
            </div>
        </div>
    );
}

function DayView({ currentDate, getEvents }: { currentDate: Date, getEvents: (d: Date) => any[] }) {
    const hours = Array.from({ length: 24 }).map((_, i) => i);
    const events = getEvents(currentDate);

    return (
        <div className="flex flex-col h-full overflow-hidden">
            <div className="p-4 border-b border-slate-200 text-center">
                <p className="text-slate-500 font-medium uppercase">{format(currentDate, 'EEEE', { locale: es })}</p>
                <h3 className={clsx(
                    "text-4xl font-bold mt-2 inline-flex items-center justify-center w-12 h-12 rounded-full",
                    isToday(currentDate) ? "bg-blue-600 text-white shadow-lg shadow-blue-200" : "text-slate-800"
                )}>
                    {format(currentDate, 'd')}
                </h3>
            </div>

            <div className="flex-1 overflow-y-auto relative">
                <div className="min-h-[1440px] relative">
                    {hours.map(hour => (
                        <div key={hour} className="flex h-[60px] border-b border-slate-100">
                            <div className="w-16 text-right pr-4 pt-2 text-xs text-slate-400 font-medium border-r border-slate-100">
                                {hour}:00
                            </div>
                            <div className="flex-1 relative">
                                {/* Place for click-to-create logic later */}
                            </div>
                        </div>
                    ))}

                    {/* Events Overlay */}
                    {events.map((evt: any) => {
                        const start = evt.start;
                        const end = evt.end || start;
                        const startHour = start.getHours() + (start.getMinutes() / 60);
                        const duration = (end.getTime() - start.getTime()) / (1000 * 60 * 60) || 1;

                        return (
                            <div
                                key={evt.id}
                                className="absolute left-20 right-4 rounded-lg bg-gradient-to-l from-indigo-50 to-indigo-100 border-l-4 border-indigo-500 p-3 text-indigo-900 shadow-sm hover:shadow-md transition-all cursor-pointer z-10"
                                style={{
                                    top: `${startHour * 60}px`,
                                    height: `${duration * 60}px`
                                }}
                            >
                                <div className="flex items-center gap-2 mb-1">
                                    <Clock className="w-3 h-3 text-indigo-500" />
                                    <span className="text-xs font-bold">{format(start, 'HH:mm')} - {format(end, 'HH:mm')}</span>
                                </div>
                                <h4 className="font-bold text-sm">{evt.title}</h4>
                                {evt.description && <p className="text-xs text-indigo-700 mt-1 line-clamp-2">{evt.description}</p>}
                            </div>
                        );
                    })}
                </div>
            </div>
        </div>
    );
}
