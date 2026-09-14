import { Component, computed, inject } from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { InputGroupModule } from 'primeng/inputgroup';
import { InputGroupAddonModule } from 'primeng/inputgroupaddon';
import { InputTextModule } from 'primeng/inputtext';
import { ProgressBarModule } from 'primeng/progressbar';
import { SkeletonModule } from 'primeng/skeleton';

import { DotMessagePipe, DotRelativeDatePipe } from '@dotcms/ui';

import { DotAiEmptyStateComponent } from '../../components/dot-ai-empty-state/dot-ai-empty-state.component';
import { DotAiWorkspaceComponent } from '../../components/dot-ai-workspace/dot-ai-workspace.component';
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
        DotAiEmptyStateComponent,
        ButtonModule,
        InputGroupModule,
        InputGroupAddonModule,
        InputTextModule,
        ProgressBarModule,
        SkeletonModule,
        DotAiWorkspaceComponent,
        DotMessagePipe,
        DotRelativeDatePipe
    ],
    templateUrl: './dot-ai-search.component.html',
    host: { class: 'block h-full' }
})
export default class DotAiSearchComponent {
    protected readonly store = inject(DotAiStore);

    /**
     * Typed rather than `$any($event.target).value`, and a handler rather than `ngModel`:
     * `disabled` is an *input on NgModel*, which applies it a microtask after the binding, so
     * this field would render briefly enabled on an unconfigured instance. Same shape as
     * `DotAiPromptInputComponent.onInput`.
     */
    protected onPromptInput(event: Event): void {
        this.store.setSearchPrompt((event.target as HTMLInputElement).value);
    }

    /** Closeness for the result bar. See `toClosenessPercent` for why it normalises. */
    protected readonly toCloseness = toClosenessPercent;

    /**
     * The threshold the server echoes is a float32 round trip, so `0.01` comes back as
     * `0.009999999776482582`. Printed as given, that is noise rather than information.
     */
    protected formatThreshold(threshold: number): string {
        return Number.isFinite(threshold) ? `${Number(threshold.toFixed(4))}` : '';
    }

    /**
     * The operator that actually produced the results on screen, echoed by the response —
     * not the panel's current selection, which the user may already have changed.
     */
    protected readonly $resultOperator = computed(
        () => this.store.searchResponse()?.operator ?? ''
    );

    protected readonly $canSearch = computed(
        () => this.store.isConfigured() && !!this.store.searchPrompt().trim()
    );

    protected onSearch(): void {
        if (this.$canSearch()) {
            this.store.runSearch();
        }
    }
}
