import { DotWizardStep } from '@models/dot-wizard-step/dot-wizard-step.model';

export interface DotWizardInput {
    steps: DotWizardStep<any>[];
    title: string;
}
