package com.iatualcance.alfred.billing

/** Funciones que dependen del plan. */
enum class Feature { VOICE, NOTES, DRIVE, SEARCH }

/**
 * Planes de Alfred. Mientras [BillingConfig.ENABLED] sea false NO se aplica nada (todo desbloqueado y sin topes);
 * el consumo se mide igual para ir calibrando precios.
 *
 * [budgetEur] es el tope de COSTE de IA al mes (no de mensajes): con 6 € + IVA el tope es 2,5 € (queda >50 % de margen)
 * y con 10 € + IVA el tope es 6 €. Al 80 % y al 100 % se avisa.
 */
enum class Plan(val id: String, val label: String, val priceNetEur: Double, val budgetEur: Double, val features: Set<Feature>) {
    BASIC("basic", "Básico", 6.0, 2.5, emptySet()),
    PREMIUM("premium", "Premium", 10.0, 6.0, Feature.entries.toSet());

    fun allows(f: Feature) = f in features
    /** Margen bruto aproximado sobre el precio sin IVA si se gasta todo el tope. */
    val marginEur get() = priceNetEur - budgetEur

    companion object {
        /** El plan de una licencia ("basic" / "premium"); lo desconocido cuenta como Premium por compatibilidad con los códigos ya emitidos. */
        fun fromId(id: String?) = entries.firstOrNull { it.id.equals(id, true) } ?: PREMIUM
    }
}

object Entitlements {
    /** ¿Está permitida [f]? Sin cobros activados, todo lo está. Con cobros, [plan] manda (null = sin licencia = Básico). */
    fun allows(enabled: Boolean, plan: Plan?, f: Feature): Boolean = !enabled || (plan ?: Plan.BASIC).allows(f)

    /** Tope de coste mensual en euros; 0 = sin tope. Sin cobros, el que haya puesto el propietario en Ajustes. */
    fun budgetEur(enabled: Boolean, plan: Plan?, ownerBudget: Double): Double = if (enabled) (plan ?: Plan.BASIC).budgetEur else ownerBudget

    /** Mensaje a mostrar al cruzar un umbral de consumo (80 % y 100 %), o null. */
    fun threshold(before: Double, after: Double, budget: Double): Int? = when {
        budget <= 0 -> null
        before < budget && after >= budget -> 100
        before < budget * 0.8 && after >= budget * 0.8 -> 80
        else -> null
    }

    fun featureName(f: Feature) = when (f) { Feature.VOICE -> "La voz"; Feature.NOTES -> "Las notas y tareas"; Feature.DRIVE -> "Drive"; Feature.SEARCH -> "La búsqueda en internet" }
}
