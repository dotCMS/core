export enum DotWizardComponentEnum {
    commentAndAssign = 'commentAndAssign',
    pushPublish = 'pushPublish'
}

export interface DotWizardStep {
    component: string;
    data: Record<string, unknown>;
}

export interface DotWizardInput {
    steps: DotWizardStep[];
    title: string;
}
