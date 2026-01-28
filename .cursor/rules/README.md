# Cursor Project Rules

Reglas del proyecto en `.cursor/rules/`. Cursor las aplica según el tipo de regla.

## Tipos de regla (frontmatter)

| Tipo en Cursor | Frontmatter | Cuándo se aplica |
|----------------|-------------|-------------------|
| **Always Apply** | `alwaysApply: true` | En cada chat |
| **Apply to Specific Files** | `globs: ["..."]` + `alwaysApply: false` | Cuando el archivo abierto/contexto coincide con el patrón |
| **Apply Intelligently** | `description: "..."` + `alwaysApply: false` | Cuando el Agent considera la descripción relevante |
| **Apply Manually** | Sin alwaysApply/globs | Solo si mencionas la regla con @ (ej. `@doc-updates`) |

## Reglas actuales

- **dotcms-guide.mdc** – Always Apply. Navegación y recordatorios críticos.
- **frontend-context.mdc** – Globs: `core-web/**/*.{ts,tsx,html,scss,css}`. Nx monorepo, Angular, SDK, docs/frontend index.
- **java-context.mdc** – Globs: `**/*.java`, `**/pom.xml`, `dotCMS/src/**/*`. Config, Logger, Maven.
- **test-context.mdc** – Globs: `**/*.spec.ts`, `**/*Test.java`, etc. Spectator, data-testid.
- **doc-updates.mdc** – Globs: `**/*.md`, `docs/**/*`. Dónde actualizar docs, DRY.

## Buenas prácticas (Cursor docs)

- Reglas **cortas** (< 500 líneas; idealmente ~50 para recordatorios).
- **Una preocupación** por regla; dividir si crecen.
- **Descripciones** claras y con palabras clave para Apply Intelligently.
- Usar **`.mdc`** (no `.md`) para que Cursor interprete bien `description`, `globs`, `alwaysApply`.
- Detalles largos en **`/docs/`** y referenciar con `@docs/...` en lugar de copiar en la regla.

## Referencia

- [Cursor Rules (rule.md)](../rule.md) – Documentación oficial.
- **CLAUDE.md** – Guía principal del repo; las reglas apuntan a `/docs/` para el detalle.
