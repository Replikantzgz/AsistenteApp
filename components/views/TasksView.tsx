'use client';
import { useStore } from '@/store';
import { clsx } from 'clsx';
import { useState, useEffect } from 'react';
import { Trash2 } from 'lucide-react';

export default function TasksView() {
    const { tasks, addTask, toggleTask, deleteTask, triggerAction, setTriggerAction } = useStore();
    const [isModalOpen, setIsModalOpen] = useState(false);
    const [newTaskTitle, setNewTaskTitle] = useState('');
    const [newTaskPriority, setNewTaskPriority] = useState<'high' | 'medium' | 'low'>('medium');

    useEffect(() => {
        if (triggerAction) {
            setIsModalOpen(true);
            setTriggerAction(null);
        }
    }, [triggerAction, setTriggerAction]);

    const handleAddTask = () => {
        if (!newTaskTitle.trim()) return;
        addTask({
            id: Date.now().toString(),
            title: newTaskTitle,
            priority: newTaskPriority,
            status: 'pending',
        });
        setNewTaskTitle('');
        setNewTaskPriority('medium');
        setIsModalOpen(false);
    };

    const priorityConfig = {
        high: { label: 'Alta', className: 'bg-red-100 text-red-600' },
        medium: { label: 'Media', className: 'bg-blue-100 text-blue-600' },
        low: { label: 'Baja', className: 'bg-slate-100 text-slate-500' },
    };

    const pending = tasks.filter(t => t.status === 'pending');
    const done = tasks.filter(t => t.status === 'done');

    return (
        <div className="h-full bg-slate-50 p-4 overflow-y-auto relative">
            <div className="space-y-2 pb-24">
                {/* Pending tasks */}
                {pending.map((task) => (
                    <div
                        key={task.id}
                        onClick={() => toggleTask(task.id)}
                        className="group cursor-pointer bg-white p-4 rounded-xl shadow-sm border border-slate-200 hover:border-blue-300 transition-all flex items-center gap-3"
                    >
                        <div className="w-6 h-6 rounded-full border-2 border-slate-300 group-hover:border-blue-400 flex items-center justify-center shrink-0 transition-colors" />
                        <div className="flex-1 min-w-0">
                            <p className="text-base font-medium text-slate-800 truncate">{task.title}</p>
                            <span className={clsx(
                                'text-xs px-2 py-0.5 rounded-full font-medium',
                                priorityConfig[task.priority].className
                            )}>
                                {priorityConfig[task.priority].label}
                            </span>
                        </div>
                        <button
                            onClick={(e) => { e.stopPropagation(); deleteTask(task.id); }}
                            className="opacity-0 group-hover:opacity-100 p-2 text-slate-300 hover:text-red-500 rounded-lg transition-all shrink-0"
                        >
                            <Trash2 className="w-4 h-4" />
                        </button>
                    </div>
                ))}

                {/* Completed tasks */}
                {done.length > 0 && (
                    <>
                        <p className="text-xs font-semibold text-slate-400 uppercase tracking-wide pt-2 pb-1 pl-1">
                            Completadas ({done.length})
                        </p>
                        {done.map((task) => (
                            <div
                                key={task.id}
                                onClick={() => toggleTask(task.id)}
                                className="group cursor-pointer bg-white p-4 rounded-xl shadow-sm border border-slate-100 flex items-center gap-3 opacity-60 hover:opacity-80 transition-all"
                            >
                                <div className="w-6 h-6 rounded-full bg-green-500 flex items-center justify-center shrink-0">
                                    <span className="text-white text-xs font-bold">✓</span>
                                </div>
                                <div className="flex-1 min-w-0">
                                    <p className="text-base font-medium text-slate-500 line-through truncate">{task.title}</p>
                                </div>
                                <button
                                    onClick={(e) => { e.stopPropagation(); deleteTask(task.id); }}
                                    className="opacity-0 group-hover:opacity-100 p-2 text-slate-300 hover:text-red-500 rounded-lg transition-all shrink-0"
                                >
                                    <Trash2 className="w-4 h-4" />
                                </button>
                            </div>
                        ))}
                    </>
                )}

                {tasks.length === 0 && (
                    <div className="text-center py-16 text-slate-400">
                        <p className="text-4xl mb-3">✓</p>
                        <p className="font-medium">Sin tareas pendientes</p>
                        <p className="text-sm mt-1">Pulsa + para añadir una nueva tarea</p>
                    </div>
                )}
            </div>

            {/* Modal */}
            {isModalOpen && (
                <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
                    <div className="absolute inset-0 bg-slate-900/50 backdrop-blur-sm" onClick={() => setIsModalOpen(false)} />
                    <div className="bg-white w-full max-w-sm rounded-2xl p-6 relative shadow-xl z-10">
                        <h3 className="text-xl font-bold text-slate-800 mb-5">Nueva Tarea</h3>
                        <input
                            type="text"
                            placeholder="¿Qué tienes que hacer?"
                            className="w-full border-b-2 border-slate-200 py-2 text-lg outline-none focus:border-blue-600 transition-colors mb-4"
                            autoFocus
                            value={newTaskTitle}
                            onChange={(e) => setNewTaskTitle(e.target.value)}
                            onKeyDown={(e) => e.key === 'Enter' && handleAddTask()}
                        />
                        {/* Priority selector */}
                        <div className="flex gap-2 mb-6">
                            {(['high', 'medium', 'low'] as const).map((p) => (
                                <button
                                    key={p}
                                    onClick={() => setNewTaskPriority(p)}
                                    className={clsx(
                                        'flex-1 py-1.5 rounded-lg text-xs font-semibold border-2 transition-all',
                                        newTaskPriority === p
                                            ? priorityConfig[p].className + ' border-current'
                                            : 'border-slate-200 text-slate-400 hover:border-slate-300'
                                    )}
                                >
                                    {priorityConfig[p].label}
                                </button>
                            ))}
                        </div>
                        <div className="flex justify-end gap-3">
                            <button onClick={() => setIsModalOpen(false)} className="px-4 py-2 text-slate-500 hover:bg-slate-100 rounded-xl font-medium">
                                Cancelar
                            </button>
                            <button
                                onClick={handleAddTask}
                                disabled={!newTaskTitle.trim()}
                                className="px-5 py-2 bg-blue-600 text-white rounded-xl font-bold hover:bg-blue-700 disabled:opacity-50 transition-colors"
                            >
                                Crear
                            </button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
}
