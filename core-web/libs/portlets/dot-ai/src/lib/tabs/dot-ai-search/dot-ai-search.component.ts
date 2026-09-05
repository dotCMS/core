import { Component, computed, inject } from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { InputGroupModule } from 'primeng/inputgroup';
import { InputGroupAddonModule } from 'primeng/inputgroupaddon';
import { InputTextModule } from 'primeng/inputtext';
import { ProgressBarModule } from 'primeng/progressbar';
import { SkeletonModule } from 'primeng/skeleton';

import { DotMessagePipe, DotRelativeDatePipe } from '@dotcms/ui';

import { DotAiStore } from '../../store/dot-ai.store';
import { toClosenessPercent } from '../../utils/dot-ai-distance.utils';

/**
 * Search tab: a hero query field over a ranked result list, with the shared retrieval-settings
 * panel beside it.
 *
 * Handles all four states explicitly (FR-054) — the first-run empty state, in-flight, the
 * "nothing passed the threshold" state, and results — because on this screen "no results" and
 * "you have not searched yet" mean very different things.
 */
@Component({
    selector: 'dot-ai-search',
    imports: [
        ButtonModule,
        InputGroupModule,
        InputGroupAddonModule,
        InputTextModule,
        ProgressBarModule,
        SkeletonModule,
        DotMessagePipe,
        DotRelativeDatePipe
    ],
    templateUrl: './dot-ai-search.component.html',
    host: { class: 'block h-full' }
})
export default class DotAiSearchComponent {
    protected readonly store = inject(DotAiStore);

    /** Closeness for the result bar. See `toClosenessPercent` for why it normalises. */
    protected readonly toCloseness = toClosenessPercent;

    protected readonly $canSearch = computed(
        () => this.store.isConfigured() && !!this.store.searchPrompt().trim()
    );

    protected onSearch(): void {
        if (this.$canSearch()) {
            this.store.runSearch();
        }
    }
}
