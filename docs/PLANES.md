# Planes y consumo de IA (oculto)

Todo está construido pero **apagado**: con `BillingConfig.ENABLED = false` no se bloquea nada y no hay topes. El consumo
se mide siempre (Ajustes → IA → «Consumo de IA de este mes») para calibrar precios con datos reales.

| | Básico | Premium |
|---|---|---|
| Precio | 6 € + IVA | 10 € + IVA |
| Tope de coste de IA al mes | 2,5 € | 6 € |
| Margen bruto si se gasta todo el tope | 3,5 € (58 %) | 4 € (40 %) |
| Correo y agenda | Sí | Sí |
| Voz | No | Sí |
| Notas y tareas | No | Sí |
| Drive | No | Sí |
| Búsqueda en internet / consulta avanzada | No | Sí |
| Avisos | 80 % y 100 % del tope | 80 % y 100 % del tope |

Los planes viven en `billing/Plans.kt`. El plan sale del campo `p` de la licencia (`basic` / `premium`); los códigos ya emitidos
sin ese dato cuentan como Premium. El coste se estima en el móvil con precios prudentes (ver `Settings.Usage`).

## Antes de activar los cobros
1. **Clave de pago para los clientes.** La capa gratuita de Gemini no debe usarse con clientes: según las condiciones de Google,
   no es para usuarios del EEE/UK/Suiza y los datos pueden usarse para mejorar sus productos. La clave gratuita queda solo para el propietario.
2. **Servidor intermedio.** El contador del móvil se puede saltar; el control real (clave de empresa, tope por usuario, avisos) debe estar en un servidor.
3. **Pasarela.** Crear en Stripe un producto con dos precios mensuales (6 € y 10 €, IVA aparte o incluido según se decida), ponerlos en
   `BillingConfig.CHECKOUT_*` y emitir códigos con `tools/make_license.py` (campo plan `basic` o `premium`).
4. **Privacidad.** Política de privacidad y condiciones antes de abrir al público.
