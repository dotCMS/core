import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { RouterOutlet } from '@angular/router';

import { DotPageScannerService } from '@dotcms/portlets/dot-ema/ui';

import { DotA11yAgentService } from '../services/dot-a11y-agent.service';
import { AccessibilityStudioStore } from '../store/accessibility-studio.store';

/**
 * Root component of the Accessibility Studio agent. Provides the store — so the
 * selected page + run state survive the picker↔run switch — and hosts the child
 * routes via `router-outlet`:
 *   - `''`      → the page picker
 *   - `:pageId` → the run screen (the selected page lives in the URL, so runs are
 *                 deep-linkable and shareable; see {@link dotAccessibilityStudioRoutes}).
 */
@Component({
    selector: 'dot-a11y',
    standalone: true,
    imports: [RouterOutlet],
    template: `<router-outlet />`,
    providers: [AccessibilityStudioStore, DotPageScannerService, DotA11yAgentService],
    changeDetection: ChangeDetectionStrategy.OnPush,
    host: { class: 'flex flex-col h-full min-h-0 block bg-surface-100' }
})
export class DotA11yRootComponent {
    readonly store = inject(AccessibilityStudioStore);
}
