# Reporte UX Principiante — 8 Sesiones de Gym con GymBro

**Autor:** Switch (Tester)  
**Fecha:** 2026-04-14  
**Contexto:** Simulación E2E de un principiante absoluto ("Alex") usando GymBro por primera vez durante 8 sesiones de gimnasio.  
**Persona:** Beginner, General Fitness, 3 días/semana, 45 min, Maintenance, KG  

---

## Puntuación General: 7/10 ⭐⭐⭐⭐⭐⭐⭐

GymBro funciona bien mecánicamente — el registro de sets es rápido y los datos persisten correctamente. Sin embargo, la app está claramente diseñada para lifters experimentados y no guía a un principiante de forma proactiva.

---

## ✅ Top 5 — Lo que funciona BIEN

### 1. Registro de sets ultra-rápido
El flujo weight → reps → "Completar serie" es limpio y eficiente. Un principiante puede registrar un set en ~5 segundos. Los inputs numéricos con `pressKey: Enter` funcionan consistentemente.

### 2. Búsqueda de ejercicios excelente
La búsqueda en el picker funciona perfectamente. Buscar "goblet", "lat pulldown", "dumbbell bench" siempre devuelve resultados relevantes al instante. Los filtros por grupo muscular (Pecho, Espalda) funcionan sin problemas.

### 3. Persistencia de datos sólida
Después de 5 workouts, `stopApp` + `launchApp` confirmó que todos los datos se mantienen. No se muestra onboarding de nuevo. El historial muestra todos los entrenamientos previos. Cero pérdida de datos.

### 4. Onboarding completo y bien estructurado
Las 9 páginas del wizard cubren todas las preferencias importantes: objetivo, experiencia, fase, duración, frecuencia, unidades, nombre. Las opciones son claras y bien traducidas al español.

### 5. Navegación fluida entre tabs
Los 4 tabs (Home, History, Programs, Profile) son accesibles consistentemente. Los IDs de navegación (`nav_home`, `nav_history`, `nav_programs`, `nav_profile`) funcionan sin fallo en todas las pruebas.

---

## ❌ Top 5 — Frustraciones y funcionalidad faltante

### 1. CERO guía para principiantes
La app no dice "qué ejercicio hacer" ni "cuánto peso usar". Un principiante llega al Home y ve... nada útil. No hay sugerencias como "Hoy es día de pecho — intenta estos 3 ejercicios". El plan generado en Programs existe pero no guía proactivamente desde Home.

### 2. No hay smart defaults para peso
Cuando un principiante selecciona Dumbbell Bench Press por primera vez, el campo de peso está vacío. La app debería sugerir un peso basado en el perfil (Beginner + General Fitness → "¿5kg?"). En workout 4+, debería pre-llenar el último peso usado.

### 3. Timer de descanso confuso
El rest timer aparece como "Hero" al tope del LazyColumn, empujando el contenido hacia abajo. Después de scrollear para ver ejercicios, el timer queda off-screen y es imposible encontrar el botón "Skip/Saltar". Un principiante no entendería por qué "Finish Workout" desapareció.

### 4. No hay tracking de progresión visible
Después de 8 workouts con progresión clara (DB Bench: 5kg → 7.5kg → 10kg → 12.5kg), la app no celebra ni muestra esta mejora. No hay "¡Nuevo PR!" ni "Has mejorado 150% en press de pecho". Un principiante necesita esta motivación.

### 5. Exercises en inglés — confuso para usuarios en español
Los nombres de ejercicios están en inglés (Dumbbell Bench Press, Goblet Squat, Lat Pulldown). Para un principiante hispanohablante, estos términos son incomprensibles. Necesitan nombres en español o al menos una descripción.

---

## ⚠️ Bugs encontrados

### BUG-1: Rest timer oculta "Finish Workout" (Severidad: MEDIA)
Cuando `isRestTimerActive = true`, el `bottomBar` con "Finalizar Entrenamiento" se oculta. Si el usuario scrolleó abajo, no puede ni ver el timer ni terminar el workout. Tiene que saber que debe scrollear arriba para encontrar el botón "Skip".

### BUG-2: `nav_progress` no existe como testTag (Severidad: BAJA)  
El tab de Progress no tiene `nav_progress` como ID. El tap opcional fue WARNED. Posiblemente el tab no existe o tiene un ID diferente.

### BUG-3: "Seguir entrenando" no existe en el UI (Severidad: INFO)
El texto "Seguir entrenando|Continue Training" nunca se encontró en ningún test. Solo "Skip|Saltar" funciona para saltar el rest timer. Los flows antiguos que usan "Seguir entrenando" están desactualizados.

---

## 💡 Recomendaciones para v1.1

### 1. "Workout del Día" en Home
Para principiantes, mostrar un workout sugerido basado en su plan: "Hoy: Día de Pecho — 3 ejercicios, ~20 min". Un tap para empezar.

### 2. Smart Defaults para peso/reps
Pre-llenar campos con el último peso usado, o sugerir peso basado en experiencia. Principiantes no saben qué poner.

### 3. Celebración de PRs y progresión
Cuando un usuario supera su mejor marca, mostrar un confetti/notificación: "¡Nuevo récord personal! DB Bench Press: 12.5kg × 12 🎉"

### 4. Traducción de nombres de ejercicios
Usar `nameEs` del seed data cuando el dispositivo está en español. "Dumbbell Bench Press" → "Press de Pecho con Mancuernas"

### 5. Rest timer siempre visible
Mover el botón "Skip" a una posición fija (floating action button o bottom sheet) que no dependa del scroll. Un principiante no debería perder el control del workout por scrollear.

---

## Resumen de la Jornada de 8 Workouts

| # | Ejercicio | Peso | Reps | Observación |
|---|-----------|------|------|-------------|
| 1 | DB Bench Press | 5kg | 12/10 | Nervioso, primera vez |
| 2 | Lat Pulldown (Cable) | 20kg | 12/10 | Más confianza, máquina |
| 3 | Goblet Squat | 5kg | 15/12 | Exploró búsqueda y filtros |
| 4 | DB Bench Press | 7.5kg | 12/10 | ¡Progresión! +50% peso |
| 5 | Leg Press | 25kg | 15/12 | Visitó Programs |
| 6 | Dumbbell Curl | 7.5kg | 12/10 | Revisó progreso |
| 7 | Lateral Raise | 7.5kg | 15/12/10 | 3 sets, exploró perfil |
| 8 | DB Bench Press | 12.5kg | 12/10/8 | ¡150% desde workout 1! |

**Test E2E:** ✅ PASÓ (44 screenshots, 8 workouts, 0 crashes)  
**Archivo:** `android/.maestro/beginner-8workout-journey.yaml`
