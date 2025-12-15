'use client';
import { useStore } from '@/store';
import { clsx } from 'clsx';
import { useState } from 'react';

export default function TasksView() {
    const { tasks, addTask, toggleTask } = useStore();
    const [isModalOpen, setIsModalOpen] = useState(false);
    const [newTaskTitle, setNewTaskTitle] = useState('');

    const handleAddTask = () => {
        if (!newTaskTitle.trim()) return;
        addTask({
            id: Date.now().toString(),
            title: newTaskTitle,
            priority: 'medium',
            status: 'pending'
        });
        setNewTaskTitle('');
        setIsModalOpen(false);
    };

    return (
        <div className="h-full bg-slate-50 p-4 overflow-y-auto relative">
            {/* Header removed globally */}
            <div className="space-y-3 pb-20">
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

            {/* FAB */}
            <button
                onClick={() => setIsModalOpen(true)}
                className="fixed bottom-6 right-6 w-14 h-14 bg-blue-600 text-white rounded-full shadow-lg shadow-blue-600/30 flex items-center justify-center hover:scale-105 transition-transform z-10"
            >
                <svg className="w-8 h-8" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" />
                </svg>
            </button>

            {/* Modal */}
            {isModalOpen && (
                <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
                    <div className="absolute inset-0 bg-slate-900/50 backdrop-blur-sm" onClick={() => setIsModalOpen(false)} />
                    <div className="bg-white w-full max-w-sm rounded-2xl p-6 relative shadow-xl transform transition-all">
                        <h3 className="text-xl font-bold text-slate-800 mb-4">Nueva Tarea</h3>
                        <input
                            type="text"
                            placeholder="¿Qué tienes que hacer?"
                            className="w-full border-b-2 border-slate-200 py-2 text-lg outline-none focus:border-blue-600 transition-colors mb-6"
                            autoFocus
                            value={newTaskTitle}
                            onChange={(e) => setNewTaskTitle(e.target.value)}
                            onKeyDown={(e) => e.key === 'Enter' && handleAddTask()}
                        />
                        <div className="flex justify-end gap-3">
                            <button
                                onClick={() => setIsModalOpen(false)}
                                className="px-4 py-2 text-slate-500 hover:bg-slate-100 rounded-lg font-medium"
                            >
                                Cancelar
                            </button>
                            <button
                                onClick={handleAddTask}
                                disabled={!newTaskTitle.trim()}
                                className="px-4 py-2 bg-blue-600 text-white rounded-lg font-bold hover:bg-blue-700 disabled:opacity-50"
                            >
                                Crear Tarea
                            </button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
}
