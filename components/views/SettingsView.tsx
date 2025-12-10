'use client';

export default function SettingsView() {
    return (
        <div className="h-full bg-slate-50 p-8 overflow-y-auto">
            <h2 className="text-3xl font-bold text-slate-800 mb-8">Planes y Suscripción</h2>

            <div className="grid md:grid-cols-2 gap-8 max-w-4xl mx-auto">
                {/* Eco Plan */}
                <div className="bg-white p-8 rounded-2xl shadow-sm border border-slate-200 flex flex-col">
                    <div className="mb-4">
                        <span className="bg-green-100 text-green-700 px-3 py-1 rounded-full text-xs font-bold uppercase tracking-wide">Plan Eco</span>
                    </div>
                    <h3 className="text-4xl font-bold text-slate-900 mb-2">3,99€ <span className="text-lg font-normal text-slate-500">/mes</span></h3>
                    <p className="text-slate-500 mb-6">Ideal para organizar tu día a día.</p>
                    <ul className="space-y-3 mb-8 flex-1">
                        <li className="flex items-center text-slate-600">
                            <span className="text-green-500 mr-2">✓</span> IA Básica
                        </li>
                        <li className="flex items-center text-slate-600">
                            <span className="text-green-500 mr-2">✓</span> 50 comandos/día
                        </li>
                        <li className="flex items-center text-slate-600">
                            <span className="text-green-500 mr-2">✓</span> Calendario y Tareas
                        </li>
                    </ul>
                    <button className="w-full py-3 rounded-xl border-2 border-slate-200 text-slate-700 font-bold hover:border-slate-800 hover:text-slate-900 transition-all">
                        Elegir Eco
                    </button>
                </div>

                {/* Pro Plan */}
                <div className="bg-slate-900 p-8 rounded-2xl shadow-xl flex flex-col relative overflow-hidden">
                    <div className="absolute top-0 right-0 bg-gradient-to-l from-purple-600 to-blue-600 text-white px-4 py-1 rounded-bl-xl text-xs font-bold">
                        RECOMENDADO
                    </div>
                    <div className="mb-4">
                        <span className="bg-blue-500/20 text-blue-300 px-3 py-1 rounded-full text-xs font-bold uppercase tracking-wide">Plan Pro</span>
                    </div>
                    <h3 className="text-4xl font-bold text-white mb-2">8,99€ <span className="text-lg font-normal text-slate-400">/mes</span></h3>
                    <p className="text-slate-400 mb-6">Potencia ilimitada para profesionales.</p>
                    <ul className="space-y-3 mb-8 flex-1">
                        <li className="flex items-center text-slate-300">
                            <span className="text-blue-400 mr-2">✓</span> IA Avanzada (GPT-4)
                        </li>
                        <li className="flex items-center text-slate-300">
                            <span className="text-blue-400 mr-2">✓</span> Comandos Ilimitados
                        </li>
                        <li className="flex items-center text-slate-300">
                            <span className="text-blue-400 mr-2">✓</span> Redacción de Emails
                        </li>
                        <li className="flex items-center text-slate-300">
                            <span className="text-blue-400 mr-2">✓</span> Plantillas Ilimitadas
                        </li>
                    </ul>
                    <button className="w-full py-3 rounded-xl bg-gradient-to-r from-blue-600 to-purple-600 text-white font-bold hover:shadow-lg hover:shadow-blue-900/50 transition-all">
                        Prueba gratis 7 días
                    </button>
                </div>
            </div>
        </div>
    );
}
