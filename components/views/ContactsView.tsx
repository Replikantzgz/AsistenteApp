'use client';

import { useState, useEffect } from 'react';
import { Search, Phone, Mail, X, User } from 'lucide-react';
import { motion } from 'framer-motion';
import { useStore } from '@/store';

export default function ContactsView() {
    const { contacts, addContact, triggerAction, setTriggerAction } = useStore();
    const [searchTerm, setSearchTerm] = useState('');
    const [isModalOpen, setIsModalOpen] = useState(false);
    const [newContact, setNewContact] = useState({ name: '', role: '', email: '', phone: '' });

    useEffect(() => {
        if (triggerAction) {
            setIsModalOpen(true);
            setTriggerAction(null);
        }
    }, [triggerAction, setTriggerAction]);

    const filteredContacts = contacts.filter((c) =>
        c.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
        c.role.toLowerCase().includes(searchTerm.toLowerCase()) ||
        c.email.toLowerCase().includes(searchTerm.toLowerCase())
    );

    const handleAddContact = () => {
        if (!newContact.name.trim()) return;
        const initials = newContact.name
            .split(' ')
            .map((w) => w[0])
            .slice(0, 2)
            .join('')
            .toUpperCase();
        addContact({
            id: Date.now().toString(),
            name: newContact.name,
            role: newContact.role,
            email: newContact.email,
            phone: newContact.phone,
            avatar: initials,
        });
        setNewContact({ name: '', role: '', email: '', phone: '' });
        setIsModalOpen(false);
    };

    const avatarColors = [
        'from-blue-100 to-indigo-100 text-blue-600',
        'from-purple-100 to-pink-100 text-purple-600',
        'from-green-100 to-teal-100 text-green-600',
        'from-orange-100 to-yellow-100 text-orange-600',
        'from-rose-100 to-red-100 text-rose-600',
    ];

    return (
        <div className="h-full flex flex-col bg-slate-50">
            {/* Search */}
            <div className="p-4 bg-white border-b border-slate-100">
                <div className="relative">
                    <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-400" />
                    <input
                        type="text"
                        placeholder="Buscar contacto..."
                        value={searchTerm}
                        onChange={(e) => setSearchTerm(e.target.value)}
                        className="w-full pl-9 pr-4 py-2.5 bg-slate-100 rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-500/20 text-slate-900 text-sm"
                    />
                </div>
            </div>

            {/* Contact list */}
            <div className="flex-1 overflow-auto p-4 space-y-3 pb-24">
                {filteredContacts.length === 0 && (
                    <div className="text-center py-16 text-slate-400">
                        <User className="w-12 h-12 mx-auto mb-3 text-slate-300" />
                        <p className="font-medium">{searchTerm ? 'No encontrado' : 'Sin contactos'}</p>
                        <p className="text-sm mt-1">Pulsa + para añadir un contacto</p>
                    </div>
                )}
                {filteredContacts.map((contact, index) => (
                    <motion.div
                        key={contact.id}
                        initial={{ opacity: 0, y: 16 }}
                        animate={{ opacity: 1, y: 0 }}
                        transition={{ delay: index * 0.04 }}
                        className="bg-white p-4 rounded-2xl shadow-sm border border-slate-100 flex items-center gap-4"
                    >
                        <div className={`w-12 h-12 rounded-full bg-gradient-to-br ${avatarColors[index % avatarColors.length]} flex items-center justify-center font-bold text-base shrink-0`}>
                            {contact.avatar}
                        </div>
                        <div className="flex-1 min-w-0">
                            <h3 className="font-semibold text-slate-900 truncate">{contact.name}</h3>
                            <p className="text-sm text-slate-500 truncate">{contact.role}</p>
                        </div>
                        <div className="flex items-center gap-1">
                            {contact.phone && (
                                <a
                                    href={`tel:${contact.phone}`}
                                    onClick={(e) => e.stopPropagation()}
                                    className="p-2 text-slate-400 hover:bg-slate-50 hover:text-green-600 rounded-full transition-colors"
                                >
                                    <Phone className="w-4 h-4" />
                                </a>
                            )}
                            {contact.email && (
                                <a
                                    href={`mailto:${contact.email}`}
                                    onClick={(e) => e.stopPropagation()}
                                    className="p-2 text-slate-400 hover:bg-slate-50 hover:text-blue-600 rounded-full transition-colors"
                                >
                                    <Mail className="w-4 h-4" />
                                </a>
                            )}
                        </div>
                    </motion.div>
                ))}
            </div>

            {/* Add Contact Modal */}
            {isModalOpen && (
                <div className="fixed inset-0 z-50 flex items-end sm:items-center justify-center p-0 sm:p-4">
                    <div className="absolute inset-0 bg-slate-900/50 backdrop-blur-sm" onClick={() => setIsModalOpen(false)} />
                    <div className="bg-white w-full sm:max-w-sm rounded-t-2xl sm:rounded-2xl shadow-2xl relative z-10">
                        <div className="flex items-center justify-between p-5 border-b border-slate-100">
                            <h3 className="text-lg font-bold text-slate-800">Nuevo Contacto</h3>
                            <button onClick={() => setIsModalOpen(false)} className="p-1.5 text-slate-400 hover:bg-slate-100 rounded-lg">
                                <X className="w-5 h-5" />
                            </button>
                        </div>
                        <div className="p-5 space-y-3">
                            {[
                                { key: 'name', label: 'Nombre *', placeholder: 'Nombre completo', type: 'text' },
                                { key: 'role', label: 'Cargo / Rol', placeholder: 'Ej: CEO, Diseñador...', type: 'text' },
                                { key: 'email', label: 'Email', placeholder: 'email@ejemplo.com', type: 'email' },
                                { key: 'phone', label: 'Teléfono', placeholder: '+34 600 000 000', type: 'tel' },
                            ].map(({ key, label, placeholder, type }) => (
                                <div key={key}>
                                    <label className="text-xs font-semibold text-slate-500 uppercase tracking-wide mb-1 block">{label}</label>
                                    <input
                                        type={type}
                                        placeholder={placeholder}
                                        value={newContact[key as keyof typeof newContact]}
                                        onChange={(e) => setNewContact(d => ({ ...d, [key]: e.target.value }))}
                                        autoFocus={key === 'name'}
                                        className="w-full border border-slate-200 rounded-xl px-4 py-2.5 text-sm outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500 transition-all"
                                        onKeyDown={(e) => e.key === 'Enter' && key === 'phone' && handleAddContact()}
                                    />
                                </div>
                            ))}
                        </div>
                        <div className="flex justify-end gap-3 p-5 border-t border-slate-100">
                            <button onClick={() => setIsModalOpen(false)} className="px-4 py-2 text-slate-500 hover:bg-slate-100 rounded-xl font-medium text-sm">
                                Cancelar
                            </button>
                            <button
                                onClick={handleAddContact}
                                disabled={!newContact.name.trim()}
                                className="px-5 py-2 bg-blue-600 text-white rounded-xl font-bold hover:bg-blue-700 disabled:opacity-50 transition-colors text-sm"
                            >
                                Añadir Contacto
                            </button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
}
