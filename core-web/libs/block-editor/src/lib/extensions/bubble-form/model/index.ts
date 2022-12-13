export interface DynamicControl<T> {
    value?: T;
    key?: string;
    label?: string;
    required?: boolean;
    controlType?: string;
    type?: string;
    min?: number;
}
