# core-web — Frontend Development Guide

Angular/Nx monorepo for the dotCMS admin UI, SDKs, and shared libraries. Check `package.json` for current framework versions.

## Commands

```bash
# Frontend dev server lifecycle (preferred over running nx serve directly)
just dev-start-frontend                   # start nx serve in background → :4200/dotAdmin (PID-managed)
just dev-stop-frontend                    # stop cleanly via PID file
just dev-frontend-logs                    # tail the dev server log (blocking — Ctrl-C to exit)
just dev-frontend-status                  # show last 40 lines of dev server log (non-blocking)

# Or run directly in the foreground (Ctrl+C to stop)
yarn nx serve dotcms-ui                    # dev server (:4200), proxies API to :8080

yarn nx test dotcms-ui                     # unit tests
yarn nx lint dotcms-ui                     # lint
yarn nx test dotcms-ui --testPathPattern=dot-edit-content  # single test file
yarn run test:dotcms                      # all project tests
yarn run lint:dotcms                      # all project lint
yarn nx affected -t test --exclude='tag:skip:test'         # changed projects only
```

## Conventions (required)

- **Component prefix**: `dot-` (e.g., `dot-edit-content`)
- **Modern Angular syntax**: `@if`/`@for`, `input()`/`output()`, `inject()`, OnPush, standalone — never `*ngIf`, `@Input()`, constructor injection
- **Testing**: Spectator + Jest. Use `data-testid` for selectors, `spectator.setInput()` for inputs, `byTestId()` for queries. Use `@dotcms/utils-testing` createFake functions for mocks.
- **State**: NgRx Signal Store — avoid manual signal soup in components
- **Styling**: Tailwind and PrimeNG components first; BEM only when custom styles needed
- **TypeScript**: Strict mode, no `any` (use `unknown`), `as const` over enums, `#` for private

## Gotchas

- **`:4200` vs `:8080`**: `nx serve` serves Angular from disk at `:4200/dotAdmin` with live reload. `:8080/dotAdmin` serves the Angular WAR compiled into the Docker image — changes to `core-web/` files are invisible there until a full rebuild. Always develop against `:4200`.
- **Proxy errors**: Always start the frontend via `just dev-start-frontend` — it auto-discovers the backend port from Docker and injects it into the proxy. Running `yarn nx serve` directly falls back to `:8080` only; if your backend is on a different port, API calls will fail.
- **Circular dependencies**: Check TypeScript paths in `tsconfig.base.json` if builds fail unexpectedly.
- **Test inputs**: Never set component inputs directly (`component.prop = x`); always use `spectator.setInput()` or tests will silently not trigger change detection.
- **NX caching**: If builds behave strangely, reset with `yarn nx reset`.

## Structure

- `apps/` — dotcms-ui (main admin), dotcms-block-editor, dotcms-binary-field-builder, mcp-server
- `libs/sdk/` — client, react, angular, analytics, experiments, uve (external-facing SDKs)
- `libs/data-access/` — Angular services for API communication
- `libs/ui/` — shared UI presentational components
- `libs/portlets/` — feature portlets (analytics, experiments, locales, etc.)
- `libs/block-editor/` — TipTap-based rich text editor

## Detailed patterns (load on demand)

| Topic | File |
|---|---|
| Angular standards | `docs/frontend/ANGULAR_STANDARDS.md` |
| Component architecture | `docs/frontend/COMPONENT_ARCHITECTURE.md` |
| Testing (Spectator/Jest) | `docs/frontend/TESTING_FRONTEND.md` |
| State management | `docs/frontend/STATE_MANAGEMENT.md` |
| TypeScript standards | `docs/frontend/TYPESCRIPT_STANDARDS.md` |
| Styling (SCSS/BEM) | `docs/frontend/STYLING_STANDARDS.md` |

For backend/Java: see root `AGENTS.md`.

<!-- nx configuration start-->
<!-- Leave the start & end comments to automatically receive updates. -->

# Nx Guidelines

- Run tasks through `nx` (`yarn nx run`, `yarn nx run-many`, `yarn nx affected`) instead of underlying tooling directly
- Prefix nx commands with `yarn` (this repo's package manager): `yarn nx build`, `yarn nx test`
- NEVER guess CLI flags -- check `--help` first when unsure

<!-- nx configuration end-->
