import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

import { TagModule } from 'primeng/tag';

import {
    DotExperimentStatus,
    ExperimentsStatusIcons,
    ExperimentsStatusList
} from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';

/** `warn` (not `warning`) is PrimeNG's spelling — any other value produces no
 * `p-tag-*` class and the tag falls back to the solid primary fill. */
export type TagSeverity = 'success' | 'info' | 'warn' | 'secondary';

/** Mirrors `DotExperimentsUiHeaderComponent.statusSeverityMap` so an experiment status
 * looks identical in the experiments list and in the UVE header. Duplicated (not imported)
 * on purpose: the header is existing UVE code that must not be touched here. */
const SEVERITIES: Record<DotExperimentStatus, TagSeverity> = {
    [DotExperimentStatus.RUNNING]: 'success',
    [DotExperimentStatus.SCHEDULED]: 'info',
    [DotExperimentStatus.DRAFT]: 'warn',
    [DotExperimentStatus.ENDED]: 'info',
    [DotExperimentStatus.ARCHIVED]: 'secondary'
};

/** Existing i18n keys (`draft`, `scheduled`, `running`, `ended`, `archived`) already
 * declared by `ExperimentsStatusList`; indexed by status for O(1) template lookups. */
const LABEL_KEYS = new Map<string, string>(
    ExperimentsStatusList.map(({ value, label }) => [value, label])
);

/** Pure mapping function — exported for direct testing without component instantiation. */
export function experimentStatusSeverity(status: DotExperimentStatus): TagSeverity {
    return SEVERITIES[status] ?? 'secondary';
}

/**
 * Renders the canonical status tag for a `DotExperimentStatus`: a `p-tag` with the
 * shared status icon, severity and label, so the experiments list matches the experiment
 * header presentation without duplicating the mapping at every call site.
 */
@Component({
    selector: 'dot-experiment-status-tag',
    imports: [TagModule, DotMessagePipe],
    templateUrl: './dot-experiment-status-tag.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotExperimentStatusTagComponent {
    /** Experiment status to render; `null` renders nothing. */
    readonly $status = input<DotExperimentStatus | null>(null, { alias: 'status' });

    readonly $severity = computed<TagSeverity | null>(() => {
        const status = this.$status();

        return status ? experimentStatusSeverity(status) : null;
    });

    readonly $icon = computed<string>(() => {
        const status = this.$status();

        return status ? (ExperimentsStatusIcons[status] ?? '') : '';
    });

    readonly $labelKey = computed<string>(() => {
        const status = this.$status();

        return status ? (LABEL_KEYS.get(status) ?? '') : '';
    });
}
