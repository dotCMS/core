import { AllowedActionsByExperimentStatus, DotExperiment } from '@dotcms/dotcms-models';

/** Every action of the list gated by `AllowedActionsByExperimentStatus`. */
export type ExperimentListAction = keyof typeof AllowedActionsByExperimentStatus;

/** `warn` (not `warning`) is PrimeNG's spelling — anything else yields no `p-tag-*` class. */
export type TagSeverity = 'success' | 'info' | 'warn' | 'secondary';

/** A table row: the experiment plus everything the template would otherwise have to derive. */
export interface ExperimentRow {
    experiment: DotExperiment;
    pagePath: string;
    /** i18n key of the primary goal type, or `null` when the experiment has no goal. */
    goalLabelKey: string | null;
    variants: number;
    schedule: string;
    statusSeverity: TagSeverity;
    statusLabelKey: string;
}
