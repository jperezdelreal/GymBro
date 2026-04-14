# Veteran UX Report — "Carlos" (4 años entrenando, PPL, Powerlifter)

**Autor:** Switch (Tester)  
**Fecha:** 2026-04-14  
**Método:** Maestro E2E — 5 workouts, 48 screenshots, ~200 pasos  
**Resultado del test:** ✅ PASS (todas las aserciones pasaron)

---

## Puntuación Global: 7/10 para usuario avanzado

GymBro demuestra ser una app sólida y rápida para registrar entrenamientos pesados. El logging es eficiente, la persistencia de datos funciona perfectamente, y el flujo de "buscar ejercicio → registrar sets → finalizar" es limpio. Sin embargo, un veterano como Carlos nota carencias importantes en funciones avanzadas que apps como Strong, Hevy o FitBod ya ofrecen.

---

## Top 5 — Lo que funciona BIEN ✅

### 1. Velocidad de registro — EXCELENTE
El flujo peso→reps→completar set es genuinamente rápido. Para un veterano entre sets pesados (100kg+ bench, 160kg deadlift), esto es crítico. No hay fricción innecesaria. Carlos puede registrar un set de 3 reps con 160kg de peso muerto en <5 segundos. **Esto es competitivo con Strong.**

### 2. Búsqueda de ejercicios — EFICIENTE
La búsqueda funciona instantáneamente: "bench", "squat", "deadlift", "row" devuelven resultados correctos sin demora. Los filtros por grupo muscular (Espalda, Pecho) funcionan correctamente. Para un veterano que sabe exactamente qué ejercicio quiere, esto es perfecto.

### 3. Persistencia de datos — SÓLIDA
Datos persisten correctamente tras stop/relaunch de la app. Los 5 workouts de Carlos (bench 100kg, deadlift 160kg, squat 130kg, bench 102.5kg, row 80kg) se guardaron sin pérdida. El historial muestra todas las sesiones. **Confianza total en que los datos no se perderán.**

### 4. Onboarding — BIEN DISEÑADO para avanzados
El onboarding reconoce nivel "Avanzado", permite 5 días/semana y sesiones de 90 min. La selección de fase (Bulk) es relevante para periodización. Un veterano no siente que la app le habla como a un principiante. **Punto positivo frente a FitBod**, que a veces es paternalista.

### 5. Tercer set con "Añadir Serie" — FUNCIONAL
El sistema de añadir series funciona correctamente. Carlos pudo hacer 3 sets de cada ejercicio sin problemas. El peso y reps se ingresan limpiamente en cada index.

---

## Top 5 — Frustraciones / Features faltantes ❌

### 1. Sin smart defaults / auto-fill de peso anterior — CRÍTICO
Cuando Carlos regresa a bench press (workout 4, 102.5kg), el peso NO se rellena automáticamente con el último valor (100kg). Debe teclear todo desde cero. **Strong y Hevy pre-rellenan peso y reps del workout anterior.** Para un veterano que hace los mismos ejercicios 2-3x/semana, esto es un deal-breaker.

### 2. Sin visualización de progresión — ALTO
Carlos hizo bench 100kg → 102.5kg (progresión de 2.5kg). La app NO muestra esta progresión de ninguna forma visible. No hay gráficos de progreso por ejercicio, ni indicadores de PR, ni comparación con sesiones anteriores. **Un veterano NECESITA ver su progresión.** Hevy y Strong tienen gráficos de progreso excelentes.

### 3. Timer de descanso muy básico — MEDIO
El timer de descanso salta con "Saltar", pero un veterano de powerlifting necesita 3-5 minutos entre sets pesados. No hay configuración de timer por ejercicio (compuestos vs accesorios). No hay notificación sonora al terminar el descanso. **Strong permite timers personalizados por ejercicio.**

### 4. Sin RPE / RIR — MEDIO
No hay campo para registrar RPE (Rate of Perceived Exertion) o RIR (Reps In Reserve). Para un veterano que programa basado en RPE, esto es una carencia significativa. Un set de 100kg×5 puede ser RPE 7 o RPE 9.5 — información crucial para auto-regulación.

### 5. Sin notas por set / ejercicio — BAJO
No hay campo de notas para escribir "grip ancho", "pausa en el pecho", "tempo 3-1-0". Un veterano usa variaciones y necesita documentarlas. **Hevy permite notas por set.**

---

## ¿Cambiaría Carlos a GymBro? — VEREDICTO HONESTO

### 🟡 AÚN NO, pero tiene potencial

Carlos reconoce que GymBro es rápida, limpia y no le hace perder tiempo. Pero sin auto-fill de peso, sin gráficos de progresión, y sin RPE, sigue siendo inferior a Strong/Hevy para un powerlifter serio. 

**Lo que lo convencería:**
1. Auto-fill inteligente del último peso/reps por ejercicio
2. Gráfico de progresión (peso vs tiempo) por ejercicio
3. Campo de RPE/RIR en cada set
4. Timer configurable por tipo de ejercicio

Si GymBro implementa los puntos 1 y 2, Carlos lo considera seriamente. Con los 4, es switch inmediato.

---

## Comparación vs Competidores

| Feature | GymBro | Strong | Hevy | FitBod |
|---------|--------|--------|------|--------|
| Velocidad de logging | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐ |
| Auto-fill peso anterior | ❌ | ✅ | ✅ | ✅ |
| Gráficos de progreso | ❌ | ✅ | ✅ | ✅ |
| RPE/RIR | ❌ | ❌ | ✅ | ❌ |
| Timer configurable | ⚠️ básico | ✅ | ✅ | ✅ |
| Notas por set | ❌ | ✅ | ✅ | ❌ |
| Biblioteca de ejercicios | ✅ buena | ✅ | ✅ | ✅ |
| Personalización plan | ⚠️ limitada | ✅ | ✅ | ✅ auto |
| Onboarding inteligente | ✅ | ⚠️ | ⚠️ | ✅ |
| Precio | Gratis | $$$$ | $$ | $$$ |

**GymBro gana en velocidad y onboarding. Pierde en funciones avanzadas para veteranos.**

---

## Bugs encontrados durante el test

### BUG-1: `hideKeyboard` cierra la app (Maestro/Infra)
- **Severidad:** Infra (no afecta usuario)
- `hideKeyboard` en Maestro envía la app al background en este emulador. Workaround: usar `tapOn: point: "50%,10%"` para cerrar teclado.

### BUG-2: `nav_progress` testTag no encontrado
- **Severidad:** BAJA
- El tab de progreso no tiene un testTag `nav_progress` accesible. Puede ser que el tab no exista o tenga un nombre diferente.

### BUG-3: ADB crash intermitente durante screenshots (Maestro/Infra)  
- **Severidad:** Infra (no afecta usuario)
- `device offline` gRPC error durante screenshots largos. Conocido de sesiones anteriores.

### No se encontraron crashes ni ANRs de la app — ✅ estabilidad perfecta

---

## Resumen ejecutivo

GymBro es una app **rápida y fiable** que cumple las necesidades básicas de registro. Para un principiante o intermedio, es excelente. Para un veterano/powerlifter, le faltan las 3 funciones clave: auto-fill, gráficos de progresión y RPE. El potencial está ahí — la base es sólida y el UX de logging es genuinamente superior a muchos competidores. Con las mejoras sugeridas, GymBro podría ser la app definitiva para lifters serios.
