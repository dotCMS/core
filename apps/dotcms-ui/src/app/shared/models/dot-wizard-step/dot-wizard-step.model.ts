export interface DotWizardStep<T> {
    component: T;
    data: Record<string, unknown>;
}
