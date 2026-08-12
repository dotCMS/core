import { DotExperimentStatus, ExperimentsStatusList } from '@dotcms/dotcms-models';

import { TagSeverity } from './models';

/**
 * Copied from `DotExperimentsUiHeaderComponent` so a status looks identical here and in the
 * UVE header. Duplicated rather than imported: the header is legacy code left untouched.
 */
export const STATUS_SEVERITIES: Record<DotExperimentStatus, TagSeverity> = {
    [DotExperimentStatus.RUNNING]: 'success',
    [DotExperimentStatus.SCHEDULED]: 'info',
    [DotExperimentStatus.DRAFT]: 'warn',
    [DotExperimentStatus.ENDED]: 'info',
    [DotExperimentStatus.ARCHIVED]: 'secondary'
};

/** Existing lowercase i18n keys (`draft`, `running`, …) already declared by `ExperimentsStatusList`. */
export const STATUS_LABEL_KEYS = new Map<string, string>(
    ExperimentsStatusList.map(({ value, label }) => [value, label])
);

/** Lifetime of the success toasts pushed after a row action. */
export const SUCCESS_MESSAGE_LIFE = 5000;

export const ROWS_PER_PAGE_OPTIONS = [10, 25, 50];

/** Placeholder rows drawn while the first page is still loading. */
export const SKELETON_ROW_COUNT = 5;

/** One skeleton cell per table column. */
export const SKELETON_COLUMNS = Array.from({ length: 8 }, (_, index) => index);

/** Placeholder rendered in the Goal column when no goal is configured. */
export const NO_GOAL_PLACEHOLDER = '—';

/** Height of the status filter's option list before it scrolls. */
export const LISTBOX_SCROLL_HEIGHT = '320px';
