export interface DotFieldStatus {
    dotTouched: boolean;
    dotValid: boolean;
    dotPristine: boolean;
}
export interface DotFieldStatusClasses {
    'dot-valid': boolean;
    'dot-invalid': boolean;
    'dot-pristine': boolean;
    'dot-dirty': boolean;
    'dot-touched': boolean;
    'dot-untouched': boolean;
    'dot-required'?: boolean;
}
