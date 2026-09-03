import { Route } from '@angular/router';

import { DOT_AGENTS } from './agent-registry';
import { DotAgentsShellComponent } from './agents-shell/dot-agents-shell.component';

/**
 * Lazy child route for every agent that has a route loader, e.g.
 * `agents/a11y`. `coming-soon` agents (no `loadChildren`) produce no route.
 */
const agentRoutes: Route[] = DOT_AGENTS.flatMap((agent) =>
    agent.loadChildren
        ? [{ path: agent.id, data: { reuseRoute: false }, loadChildren: agent.loadChildren }]
        : []
);

/**
 * Routes for the agents shell. The gallery landing renders at the base path;
 * each available agent is lazy-loaded full-screen at `agents/{id}`. Registered
 * in `app.routes.ts` under the `agents` path.
 */
export const dotAgentsRoutes: Route[] = [
    {
        path: '',
        component: DotAgentsShellComponent,
        children: [
            {
                path: '',
                loadComponent: () =>
                    import('./agents-landing/dot-agents-landing.component').then(
                        (m) => m.DotAgentsLandingComponent
                    )
            },
            ...agentRoutes
        ]
    }
];
