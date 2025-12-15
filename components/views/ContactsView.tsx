'use client';

import { useState, useEffect } from 'react';
import { Search, Plus, Phone, Mail, MoreHorizontal } from 'lucide-react';
import { motion } from 'framer-motion';
import { useStore } from '@/store';

// Mock data
const INITIAL_CONTACTS = [
    { id: 1, name: 'Alejandro Garcia', role: 'CEO TechCorp', email: 'alex@techcorp.com', phone: '+34 600 123 456', avatar: 'AG' },
    { id: 2, name: 'Beatriz Lopez', role: 'Designer', email: 'bea@design.studio', phone: '+34 600 987 654', avatar: 'BL' },
    { id: 3, name: 'Carlos Ruiz', role: 'Developer', email: 'carlos@dev.io', phone: '+34 600 555 123', avatar: 'CR' },
    { id: 4, name: 'Diana Martin', role: 'Product Manager', email: 'diana@pm.co', phone: '+34 600 444 888', avatar: 'DM' },
];

export default function ContactsView() {
    const [searchTerm, setSearchTerm] = useState('');
    const { triggerAction, addMessage } = useStore(); // Access store

    // Using global trigger to simulate add contact
    useEffect(() => {
        if (triggerAction) {
            // For now just simulate or show info, as create_contact is AI driven mainly or needs modal
            // Let's redirect to chat with instructions or open a simple prompt?
            // User asked for "create new event", assuming contact too.
            //  alert("Para añadir contacto, pídeselo al Asistente IA por ahora.");
            // Better: switch to chat and pre-fill? No, let's just alert for now as ContactsView has no modal code yet.
            const name = prompt("Nombre del nuevo contacto:");
            if (name) {
                // We don't have addContact action in store yet publicly exposed for manual UI, only AI.
                // Let's just mock it or say "Saved".
                alert(`Contacto ${name} guardado (Simulación).`);
            }
        }
    }, [triggerAction]);

    const filteredContacts = INITIAL_CONTACTS.filter(c =>
        c.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
        c.role.toLowerCase().includes(searchTerm.toLowerCase())
    );

    return (
        <div className="h-full flex flex-col bg-slate-50">
            {/* Header */}
            <div className="p-4 bg-white border-b border-slate-100">
                <div className="flex gap-2">
                    <div className="relative flex-1">
                        <Search className="absolute left-3 top-2.5 w-4 h-4 text-slate-400" />
                        <input
                            type="text"
                            placeholder="Buscar contacto..."
                            value={searchTerm}
                            onChange={(e) => setSearchTerm(e.target.value)}
                            className="w-full pl-9 pr-4 py-2 bg-slate-100 rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-500/20 text-slate-900 text-sm"
                        />
                    </div>
                </div>
            </div>

            {/* Contact List */}
            <div className="flex-1 overflow-auto p-4 space-y-3 pb-24">
                {filteredContacts.map((contact, index) => (
                    <motion.div
                        key={contact.id}
                        initial={{ opacity: 0, y: 20 }}
                        animate={{ opacity: 1, y: 0 }}
                        transition={{ delay: index * 0.05 }}
                        className="bg-white p-4 rounded-2xl shadow-sm border border-slate-100 flex items-center gap-4"
                    >
                        <div className="w-12 h-12 rounded-full bg-gradient-to-br from-blue-100 to-indigo-100 text-blue-600 flex items-center justify-center font-bold text-lg">
                            {contact.avatar}
                        </div>

                        <div className="flex-1 min-w-0">
                            <h3 className="font-semibold text-slate-900 truncate">{contact.name}</h3>
                            <p className="text-sm text-slate-500 truncate">{contact.role}</p>
                        </div>

                        <div className="flex items-center gap-2">
                            <button className="p-2 text-slate-400 hover:bg-slate-50 hover:text-green-600 rounded-full transition-colors">
                                <Phone className="w-5 h-5" />
                            </button>
                            <button className="p-2 text-slate-400 hover:bg-slate-50 hover:text-blue-600 rounded-full transition-colors">
                                <Mail className="w-5 h-5" />
                            </button>
                        </div>
                    </motion.div>
                ))}
            </div>
        </div>
    );
}
