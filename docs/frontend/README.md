# Frontend documentation (docs/frontend)

Index for Angular/TypeScript frontend standards in `core-web`. **Cursor** uses `.cursor/rules/frontend-context.mdc`, which points here; use **`@docs/frontend/README.md`** for this index or **`@docs/frontend/<file>`** to load a specific doc.

## Documents

| Doc | When to load |
|-----|----------------|
| [ANGULAR_STANDARDS.md](./ANGULAR_STANDARDS.md) | Components, templates, signals, OnPush, PrimeNG, testing stack |
| [COMPONENT_ARCHITECTURE.md](./COMPONENT_ARCHITECTURE.md) | Component structure, file layout, data flow, parent-child |
| [STATE_MANAGEMENT.md](./STATE_MANAGEMENT.md) | NgRx Signal Store, rxMethod, patchState — **prefer over manual state** |
| [STYLING_STANDARDS.md](./STYLING_STANDARDS.md) | PrimeFlex, PrimeNG, BEM, SCSS variables |
| [TESTING_FRONTEND.md](./TESTING_FRONTEND.md) | Spectator, Jest/Vitest, byTestId, setInput, data-testid |
| [TYPESCRIPT_STANDARDS.md](./TYPESCRIPT_STANDARDS.md) | Strict types, inference, unknown, as const, # private |

## Congruence

- **Signals**: `$` prefix (e.g. `$loading`) — ANGULAR_STANDARDS, COMPONENT_ARCHITECTURE, TESTING_FRONTEND.
- **State**: Use NgRx Signal Store for feature state; avoid manual signal soup — STATE_MANAGEMENT, COMPONENT_ARCHITECTURE.
- **Testing**: Spectator, `byTestId`, `setInput`, `detectChanges`, `click` — TESTING_FRONTEND, ANGULAR_STANDARDS.
- **TypeScript**: Strict, no `any`, `as const`, `#` private — TYPESCRIPT_STANDARDS, referenced from others.

## Split vs merge (Cursor)

Per Cursor’s [rule.md](../rule.md): keep rules **under ~500 lines**, **focused**, and **referenced** instead of duplicated.

- **Recomendación: mantener los 6 archivos.** Cada doc tiene un tema claro; Cursor carga `@docs/frontend/X` solo cuando hace falta. Menos contexto = mejor aplicación de las reglas.
- **No unir** todo en 1–2 archivos: se mezclarían temas y se superaría el límite de líneas; Cursor aplicaría peor las reglas al tener que cargar todo.
- **Docs largos** (COMPONENT_ARCHITECTURE, TESTING_FRONTEND): siguen siendo un solo tema cada uno. Si necesitas menos contexto, carga solo la sección relevante o menciona el doc y haz una pregunta concreta.
