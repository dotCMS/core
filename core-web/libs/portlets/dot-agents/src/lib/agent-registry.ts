import { Route } from '@angular/router';

/**
 * Whether an agent is ready to use or still on the roadmap. `coming-soon`
 * agents render a disabled card in the gallery and register no route.
 */
export type AgentStatus = 'available' | 'coming-soon';

/**
 * One AI agent that plugs into the agents shell. Adding an agent = one entry
 * in {@link DOT_AGENTS}: give it an id/label/icon, point `loadChildren` at the
 * agent's own routes, and it appears in the gallery and gets a child route
 * under `agents/{id}` automatically. No edits to the shell, landing, or the
 * shared streaming kernel (`@dotcms/ai-ui`, `DotAgentRunService`).
 */
export interface AgentDefinition {
    /** URL segment + i18n key stem, e.g. `a11y` → route `agents/a11y`. */
    readonly id: string;
    /** i18n key for the gallery card title. */
    readonly labelKey: string;
    /** i18n key for the gallery card description. */
    readonly descriptionKey: string;
    /**
     * Material Symbols ligature name for the card icon, e.g. `accessibility_new`.
     * Rendered inside a `dot-color-icon` — see the gallery template.
     */
    readonly icon: string;
    /**
     * Icon accent, passed straight to `dot-color-icon`'s `color`. A PrimeNG palette
     * token (e.g. `blue`) or a hex value.
     */
    readonly iconColor: string;
    /** Availability. `coming-soon` disables the card and skips routing. */
    readonly status: AgentStatus;
    /**
     * Lazy loader for the agent's own routes. Required for `available` agents,
     * omitted for `coming-soon`. The returned routes render full-screen inside
     * the shell's router outlet.
     */
    readonly loadChildren?: () => Promise<Route[]>;
}

/**
 * The catalog of agents. This is the single extension point of the agents
 * shell — everything else (gallery cards, child routes) is derived from it.
 *
 * To add an agent:
 *   1. Build its UI under `src/lib/agents/{id}/` with its own `{id}.routes.ts`.
 *   2. Add an entry here with `status: 'available'` and a `loadChildren`.
 */
export const DOT_AGENTS: readonly AgentDefinition[] = [
    {
        id: 'a11y',
        labelKey: 'agents.a11y.label',
        descriptionKey: 'agents.a11y.description',
        icon: 'accessibility_new',
        iconColor: 'blue',
        status: 'available',
        loadChildren: () =>
            import('./agents/a11y/a11y.routes').then((m) => m.dotAccessibilityStudioRoutes)
    },
    {
        id: 'geo-fixer',
        labelKey: 'agents.geo-fixer.label',
        descriptionKey: 'agents.geo-fixer.description',
        icon: 'location_on',
        iconColor: 'green',
        status: 'coming-soon'
    },
    {
        id: 'page-builder',
        labelKey: 'agents.page-builder.label',
        descriptionKey: 'agents.page-builder.description',
        icon: 'dashboard_customize',
        iconColor: 'purple',
        status: 'coming-soon'
    }
];
