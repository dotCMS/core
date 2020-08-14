export interface DotWizardStep<T> {
    component: T;
    data: { [key: string]: any };
}
