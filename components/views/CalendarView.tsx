'use client';
import { useStore } from '@/store';
import { format } from 'date-fns';
import { es } from 'date-fns/locale';

export default function CalendarView() {
    const { appointments } = useStore();

    return (
        <div className="h-full bg-slate-50 p-8 overflow-y-auto">
            <h2 className="text-3xl font-bold text-slate-800 mb-6">Mi Agenda</h2>
            {appointments.length === 0 ? (
                <div className="text-center py-20 text-slate-400">No hay citas programadas</div>
            ) : (
                <div className="grid gap-4">
                    {appointments.map((appt) => (
                        <div key={appt.id} className="bg-white p-6 rounded-xl shadow-sm border border-slate-200 flex items-center justify-between hover:shadow-md transition-all">
                            <div>
                                <h3 className="text-lg font-semibold text-slate-800">{appt.title}</h3>
                                <p className="text-slate-500 text-sm">
                                    {format(new Date(appt.start), "EEEE d 'de' MMMM, HH:mm", { locale: es })}
                                </p>
                            </div>
                            <div className="h-10 w-1 bg-blue-500 rounded-full"></div>
                        </div>
                    ))}
                </div>
            )}
        </div>
    );
}
