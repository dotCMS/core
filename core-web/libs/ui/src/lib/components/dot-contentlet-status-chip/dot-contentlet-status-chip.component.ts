import { ChangeDetectionStrategy, Component, computed, inject, input } from '@angular/core';

import { TagModule } from 'primeng/tag';

import { DotMessageService } from '@dotcms/data-access';
import { DotContentState } from '@dotcms/dotcms-models';

/** The set of publish statuses a contentlet can be rendered with. */
type DotContentletStatus = 'Published' | 'Archived' | 'Revision' | 'Draft' | 'New';

/** PrimeNG Tag severities used to render content status badges. */
type DotContentletStatusSeverity = 'success' | 'danger' | 'info' | 'warn';

/** Maps each content status to its PrimeNG Tag severity. Exhaustiveness is compiler-enforced. */
const STATUS_SEVERITY: Record<DotContentletStatus, DotContentletStatusSeverity> = {
    Published: 'success',
    Archived: 'danger',
    Revision: 'info',
    Draft: 'warn',
    New: 'info'
};

/**
 * Renders the publish status of a contentlet as a single PrimeNG Tag.
 *
 * The status is derived from the `state` input through `status()`, which mirrors
 * the logic of `DotContentletStatusPipe`. `severity()` maps that status to a Tag
 * severity, and `label()` resolves the displayed text — translating only the
 * "New" badge shown when `state` is null.
 */
@Component({
    selector: 'dot-contentlet-status-chip',
    imports: [TagModule],
    templateUrl: './dot-contentlet-status-chip.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotContentletStatusChipComponent {
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

    /** The Tag label; "New" is translated, other statuses use their English label. */
    protected readonly label = computed(() => {
        const status = this.status();

        return status === 'New' ? this.#dotMessageService.get('New') : status;
    });
}
