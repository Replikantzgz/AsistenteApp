'use client';
import { useStore } from '@/store';
import { clsx } from 'clsx';

export default function TasksView() {
    const { tasks, toggleTask } = useStore();

    return (
        <div className="h-full bg-slate-50 p-8 overflow-y-auto">
            <h2 className="text-3xl font-bold text-slate-800 mb-6">Tareas Pendientes</h2>
            <div className="space-y-3">
                {tasks.map((task) => (
                    <div
                        key={task.id}
                        onClick={() => toggleTask(task.id)}
                        className="group cursor-pointer bg-white p-4 rounded-xl shadow-sm border border-slate-200 hover:border-blue-300 transition-all flex items-center gap-4"
                    >
                        <div className={clsx(
                            "w-6 h-6 rounded-full border-2 flex items-center justify-center transition-colors",
                            task.status === 'done' ? "bg-green-500 border-green-500" : "border-slate-300 group-hover:border-blue-400"
                        )}>
                            {task.status === 'done' && <span className="text-white text-xs">✓</span>}
                        </div>
                        <div className="flex-1">
                            <p className={clsx(
                                "text-lg font-medium transition-all",
                                task.status === 'done' ? "text-slate-400 line-through" : "text-slate-800"
                            )}>{task.title}</p>
                            <span className={clsx(
                                "text-xs px-2 py-0.5 rounded-full font-medium uppercase tracking-wider",
                                task.priority === 'high' ? "bg-red-100 text-red-600" : "bg-blue-100 text-blue-600"
                            )}>
                                {task.priority}
                            </span>
                        </div>
                    </div>
                ))}
            </div>
        </div>
    );
}
