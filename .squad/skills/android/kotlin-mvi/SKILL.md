---
name: kotlin-mvi
license: MIT
description: >
  Jetpack Compose and Compose Multiplatform (KMP/CMP) architecture skill.
  Only use when the user explicitly mentions "compose-skill", "@compose-skill",
  or "use compose skill" in their message. Do NOT auto-activate based on
  keyword matching — this skill should only be triggered by direct user request.
---

# Jetpack Compose & Compose Multiplatform — MVI Architecture

> **Source:** [Meet-Miyani/compose-skill](https://github.com/Meet-Miyani/compose-skill) — MIT License

This skill covers the full Compose app development lifecycle — from architecture and state management through UI, networking, persistence, performance, accessibility, cross-platform sharing, build configuration, and distribution. Jetpack Compose and Compose Multiplatform share the same core APIs and mental model. **Not all Jetpack libraries work in `commonMain`** — many remain Android-only. A subset of AndroidX libraries now publish multiplatform artifacts (e.g., `lifecycle-viewmodel`, `lifecycle-runtime-compose`, `datastore-preferences`), but availability and API surface vary by version. **Before adding any Jetpack/AndroidX dependency to `commonMain`, verify the artifact is published for all required targets by checking Maven Central or the library's official documentation.** CMP uses `expect/actual` or interfaces for platform-specific code. MVI (Model-View-Intent) is the recommended architecture, but the skill adapts to existing project conventions.

## Existing Project Policy

**Do not force migration.** If a project already follows MVI with its own conventions (different base class, different naming, different file layout), respect that. Adapt to the project's existing patterns. The architecture pattern — unidirectional data flow with Event, State, and Effect — is what matters, not a specific base class or framework. Only suggest structural changes when the user asks for them or when the existing code has clear architectural violations (business logic in composables, scattered state mutations, etc.).

## Workflow

When helping with Jetpack Compose or Compose Multiplatform code, follow this process:

1. **Read the existing code first for context** — check conventions, base classes, and layout. For small UI or logic asks, restrict your reading to the immediately relevant files to save time. Do not map out the entire project architecture unless a structural refactor is requested.
2. **Identify the concern** — is this architecture, state modeling, performance, navigation, DI, animation, cross-platform, or testing?
3. **Apply the core rules below** — the decision heuristics and defaults in this file cover most cases.
4. **Consult the right reference** — load the relevant file from `references/` only when deeper guidance is needed. Use the Quick Routing section to pick the right file.
5. **Verify dependencies before recommending** — before adding or upgrading any dependency, verify coordinates, target support, and API shape via a documentation MCP tool or official docs.
6. **Flag anti-patterns contextually** — if the user's code violates best practices, call it out for production code. For quick prototypes or minor UI tweaks, prioritize answering their specific question over lecturing them on strict rules.
7. **Write the minimal correct solution** — do not over-engineer. Prefer feature-specific code over generic frameworks.

## Core Architecture: MVI

MVI uses **unidirectional data flow**: UI renders state → user acts → ViewModel updates state → UI re-renders.

- **MVI**: `sealed interface Event` + single `onEvent()` entry point

MVI uses:
- **State** — immutable data class that fully describes the screen, owned via `StateFlow`
- **Effect** — one-shot commands (navigate, snackbar, share) delivered via `Channel`

### UI Rendering Boundary

- **Route** composable: obtains ViewModel, collects state via `collectAsStateWithLifecycle()`, collects effects via `CollectEffect`, binds navigation/snackbar/platform APIs
- **Screen** composable: stateless renderer — receives state and `onEvent` callback, renders the screen, adapts callbacks for leaf composables
- **Leaf** composables: render sub-state, emit specific callbacks, keep only tiny visual-local state (focus, scroll, animation)

## Decision Heuristics

- Composable functions render state and emit events, never decide business rules
- If a value can be derived from state, do not store it redundantly unless async/persistence/performance justifies it
- Event handling in the ViewModel owns state transitions; composables do not mutate state
- UI-local state is acceptable only for ephemeral visual concerns: focus, scroll, animation progress, expansion toggles
- Do not push animation-only flags into global screen state unless business logic depends on them
- Pass the narrowest possible state to leaf composables
- MVI: implement `onEvent()` as the single entry point
- Do not introduce a use case for every repository call
- Least recomposition is achieved by state shape and read boundaries first, Compose APIs second
- When a project has an existing MVI base class or pattern, use it — don't introduce a competing abstraction

## State Modeling

For calculator/form screens, split state into four buckets:

1. **Editable input** — raw text and choice values as the user edits them
2. **Derived display/business** — parsed, validated, calculated values
3. **Persisted domain snapshot** — saved entity for dirty tracking or reset
4. **Transient UI-only** — purely visual, not business-significant

| Concern | Where | Example |
|---|---|---|
| Raw field text | `state` fields | `"12"`, `"12."`, `""` |
| Parsed/derived | `state` computed props or fields | `val hasRequiredFields: Boolean` |
| Validation | `state.validationErrors` or similar | `mapOf("name" to "Required")` |
| Loading/refresh | `state` flags | `isSaving = true` |
| One-off UI commands | `Effect` via Channel | snackbar, navigate, share |
| Scroll/focus/animation | local Compose state | `LazyListState`, focus requester |

## Recommended Defaults

Apply these unless the project already follows a different coherent pattern.

| Concern | Default |
|---|---|
| ViewModel | One ViewModel per screen (`commonMain` for CMP, feature package for Android-only). MVI: `onEvent(Event)` entry point |
| State source of truth | `StateFlow<FeatureState>` owned by the ViewModel |
| Event handling | `onEvent(event)` with `when` expression mapping user actions to state updates, effect emissions, and async launches |
| Side effects | `Effect` sent via `Channel<Effect>(Channel.BUFFERED)` for UI-consumed one-shots (navigate, snackbar). Async work launched in `viewModelScope` |
| Async loading | Keep previous content, flip loading flag, cancel outdated jobs, update state on completion |
| Dumb UI contract | Render props, emit explicit callbacks, keep only ephemeral visual state local |
| Platform separation | CMP: share in `commonMain`, `expect/actual` or interfaces, Koin DI by default. Android-only: standard package, Hilt or Koin DI |
| Navigation | ViewModel emits semantic navigation effect; route/navigation layer executes it |
| Persistence (settings) | DataStore Preferences for key-value settings; Room for relational/queried data |
| Testing | ViewModel event→state→effect tests via Turbine in `commonTest`; validators/calculators tested as pure functions |

## Do / Don't Quick Reference

### Do

- Model raw editable text separately from parsed values
- Keep state immutable and equality-friendly
- Reuse unchanged nested objects when possible
- Emit semantic effects instead of making platform calls from event handling
- Preserve old content during refresh
- Map domain data to UI state close to the presentation boundary
- Use feature-specific ViewModel names
- Key list items by stable domain ID
- Import all types and functions at the top of the file
- Guard no-op state emissions (don't update state if nothing changed)
- Respect the project's existing MVI conventions

### Don't

- Parse numbers in composable bodies
- Run network requests from composables
- Store `MutableState`, controllers, lambdas, or platform objects in screen state
- Encode snackbar/navigation as "consume once" booleans in state — use effects
- Keep every minor visual toggle in the ViewModel state
- Pass entire state to every child composable
- Wrap every repository call in a use case class
- Wipe the screen with a full-screen spinner during refresh
- Force-migrate a working codebase to a different architecture or base class

## Quick Routing (Reference Files — in source repo)

Full reference files are available at [Meet-Miyani/compose-skill/references/](https://github.com/Meet-Miyani/compose-skill/tree/main/references):

- **Recomposition, stability, or Compose Compiler Metrics** → `performance.md`
- **Channel vs SharedFlow, Flow operators, structured concurrency** → `coroutines-flow.md`
- **Backpressure, callbackFlow, Mutex/Semaphore, Turbine testing** → `coroutines-flow-advanced.md`
- **Nav 3 routes, tabs, scenes, deep links, or back stack patterns** → `navigation-3.md`
- **Nav 2 NavHost, tabs, deep links, nested graphs, or animations** → `navigation-2.md`
- **Paging 3 setup, PagingSource, filters, LoadState** → `paging.md`
- **Offline-first paging with Room and RemoteMediator** → `paging-offline.md`
- **Ktor client setup, plugins, DTOs, API service** → `networking-ktor.md`
- **Auth (bearer), WebSockets, or SSE** → `networking-ktor-auth.md`
- **Accessibility audit, semantics, touch targets, WCAG contrast** → `accessibility.md`
- **Animation API selection** → `animations.md`
- **Code review or anti-pattern detection** → `anti-patterns.md`
- **ViewModel pipeline, state modeling, domain layer** → `architecture.md`
- **MVI pipeline, Event/State/Effect, onEvent pattern** → `mvi.md`
- **Room entities, DAOs, migrations, relationships** → `room-database.md`
- **Hilt Android setup, @HiltViewModel, scopes** → `hilt.md`
- **Koin CMP setup, Nav 3 Koin integration** → `koin.md`
- **AGP 9+, version catalog, convention plugins** → `gradle-build.md`
- **GitHub Actions CI/CD, desktop packaging, signing** → `ci-cd-distribution.md`

## GymBro-Specific Notes

GymBro's iOS app uses MVVM. For the Android target, MVI is the recommended pattern:
- **WorkoutLoggingEvent** → sealed interface covering set logging, exercise selection, rest timer actions
- **WorkoutLoggingState** → immutable data class with current exercise, sets logged, rest timer state, loading flags
- **WorkoutLoggingEffect** → navigate to history, show PR celebration snackbar, vibrate on timer complete
- The workout logging flow is the highest-frequency interaction — MVI's single entry point keeps state predictable under rapid user input.
