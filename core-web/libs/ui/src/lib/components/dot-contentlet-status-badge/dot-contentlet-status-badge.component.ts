import { ChangeDetectionStrategy, Component, computed, inject, input } from '@angular/core';

import { TagModule } from 'primeng/tag';

import { DotMessageService } from '@dotcms/data-access';
import { DotContentState } from '@dotcms/dotcms-models';

/** Maps each content status to its PrimeNG Tag severity — the single source of truth. */
const STATUS_SEVERITY = {
    Published: 'success',
    Archived: 'danger',
    Revision: 'info',
    Draft: 'warn',
    New: 'info'
} as const;

/** The set of publish statuses a contentlet can be rendered with, inferred from the map. */
type DotContentletStatus = keyof typeof STATUS_SEVERITY;

/** PrimeNG Tag severities used to render content status badges, inferred from the map. */
type DotContentletStatusSeverity = (typeof STATUS_SEVERITY)[DotContentletStatus];

/**
 * Renders the publish status of a contentlet as a single PrimeNG Tag.
 *
 * The status is derived from the `state` input through `status()`. `severity()`
 * maps that status to a Tag severity, and `label()` resolves the displayed text
 * through `DotMessageService` (each status name is an i18n key; the service
 * falls back to the key itself when no translation exists).
 */
@Component({
    selector: 'dot-contentlet-status-badge',
    imports: [TagModule],
    template: `
        <p-tag [severity]="severity()" [value]="label()" data-testid="status-tag" />
    `,
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotContentletStatusBadgeComponent {
    /** The contentlet publish state; null renders the "New" badge. */
    readonly state = input<DotContentState | null>(null);

    readonly #dotMessageService = inject(DotMessageService);

    /** The publish status derived from the current state. */
    protected readonly status = computed<DotContentletStatus>(() => {
        const state = this.state();

        if (!state) {
            return 'New';
        }

        const { live, working, archived, deleted, hasLiveVersion } = state;

        if (deleted || archived) {
            return 'Archived';
        }

        if (live && hasLiveVersion && working) {
            return 'Published';
        }

        if (!live && hasLiveVersion) {
            return 'Revision';
        }

        return 'Draft';
    });

    /** The PrimeNG Tag severity matching the current status. */
    protected readonly severity = computed<DotContentletStatusSeverity>(
        () => STATUS_SEVERITY[this.status()]
    );

    /** The translated Tag label for the current status. */
    protected readonly label = computed(() => this.#dotMessageService.get(this.status()));
}
