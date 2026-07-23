import { Route } from '@angular/router';

import { DotA11yPickerComponent } from './a11y-picker/a11y-picker.component';
import { DotA11yRootComponent } from './a11y-root/a11y-root.component';
import { DotA11yRunComponent } from './a11y-run/a11y-run.component';

/**
 * Accessibility Studio routes. The root component provides the store (so state
 * survives the picker↔run switch) and hosts a `router-outlet`; the child routes
 * pick the screen — and, crucially, put the selected page's URI in the URL:
 *   - `''`   → the page picker
 *   - `**`   → the run screen; the wildcard captures the page path verbatim
 *             (e.g. `/agents/a11y/blog/post/hello`) so runs are deep-linkable and
 *             shareable with a human-readable URL. Must come after `''` — the
 *             wildcard would otherwise swallow the picker route too.
 */
export const dotAccessibilityStudioRoutes: Route[] = [
    {
        path: '',
        component: DotA11yRootComponent,
        children: [
            { path: '', component: DotA11yPickerComponent },
            { path: '**', component: DotA11yRunComponent }
        ]
    }
];
