export interface PropValidationInfo<T> {
    field: {
        type: string;
        name: string;
    };
    name: string;
    value: T;
}
