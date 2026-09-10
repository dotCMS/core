import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { DrawerModule } from 'primeng/drawer';

import { DotExperimentsPanelStore } from '@dotcms/portlets/dot-experiments/data-access';
import { DotMessagePipe } from '@dotcms/ui';

import { PANEL_WIDTH } from '../shared/constants';

/**
 * The page's experiments, beside the page (#37478).
 *
 * A `p-drawer`, like the Edit Content side panel — but **not modal and with no mask**, which is
 * the one place the two deliberately differ. Edit Content is a full editing surface and blocking
 * what is behind it is correct; this panel must leave the editor usable, because the editor is
 * allowed to change page while it is open and the panel re-scopes when they do (D10, FR-034).
 *
 * The drawer teleports to `body`, so nothing here participates in the shell's layout: the page
 * behind is not reflowed, only overlaid (FR-038, SC-007).
 *
 * One surface at a time — list, creation, configuration or results, never two (spec Assumptions).
 * Only the list is wired so far; the other three arrive with their own phases, and results behind
 * a nested `@defer` so the charting dependency loads only when results are opened (FR-025f).
 */
@Component({
    selector: 'dot-experiments-panel',
    imports: [DrawerModule, ButtonModule, DotMessagePipe],
    templateUrl: './dot-experiments-panel.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotExperimentsPanelComponent {
    protected readonly store = inject(DotExperimentsPanelStore);

    protected readonly PANEL_WIDTH = PANEL_WIDTH;

    /**
     * The heading, which names the view rather than restating "Experiments" four times.
     *
     * The list's heading is the panel's own title; the deeper views name themselves once they
     * exist.
     */
    protected readonly $titleKey = computed<string>(() => {
        switch (this.store.view()) {
            case 'create':
                return 'experiments.configure.header.new-experiment';
            case 'configure':
                return 'experiment.container.configuration.title';
            case 'results':
                return 'experiment.container.report.title';
            default:
                return 'experiments.panel.title';
        }
    });

    /**
     * Dismissal, as opposed to the panel closing because the editor left to see a variant.
     *
     * Routed through the store's `close()` so the two stay distinguishable: `close()` discards
     * the view state, `suspendForVariant()` keeps it (FR-039 vs FR-023).
     */
    protected onClose(): void {
        this.store.close();
    }
}
