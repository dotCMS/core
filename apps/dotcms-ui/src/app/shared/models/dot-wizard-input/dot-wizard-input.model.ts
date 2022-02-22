import { DotWizardStep } from '@models/dot-wizard-step/dot-wizard-step.model';

export interface DotWizardInput<T = unknown> {
    steps: DotWizardStep<T>[];
    title: string;
}
