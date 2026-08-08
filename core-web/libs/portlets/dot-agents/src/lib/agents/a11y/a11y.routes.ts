import { Route } from '@angular/router';

import { DotA11yPageListComponent } from './a11y-page-list/a11y-page-list.component';
import { DotA11yRootComponent } from './a11y-root/a11y-root.component';
import { DotA11yRunComponent } from './a11y-run/a11y-run.component';

/**
 * Accessibility Studio routes. The root is a thin outlet host; each child route
 * provides its OWN store (page list vs run) so the two screens are fully independent:
 *   - `''`   → the page list
 *   - `**`   → the run screen for one page
 */
export const dotAccessibilityStudioRoutes: Route[] = [
    {
        path: '',
        component: DotA11yRootComponent,
        children: [
            { path: '', component: DotA11yPageListComponent },
            // Wildcard, not `:id`: the run URL carries the page's human-readable
            // path (e.g. `blog/post/hello`), which is multi-segment and so can't be
            // a single Angular route param. `**` captures the whole path; the run
            // screen reads it back and rehydrates the page. Must come after `''`.
            { path: '**', component: DotA11yRunComponent }
        ]
    }
];
