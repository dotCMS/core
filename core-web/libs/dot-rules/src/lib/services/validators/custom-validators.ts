import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

import { Verify } from '../utils/verify.util';

/**
 * Custom Angular form validators for the rule engine
 */
export class CustomValidators {
    static required(): ValidatorFn {
        return (control: AbstractControl): ValidationErrors | null => {
            const v: string = control.value;

            return Verify.empty(v) ? { required: true } : null;
        };
    }

    static isString(allowEmpty = false): ValidatorFn {
        return (control: AbstractControl): ValidationErrors | null => {
            const v: string = control.value;

            return !Verify.isStringWithEmpty(v, allowEmpty)
                ? { isString: { emptyAllowed: allowEmpty } }
                : null;
        };
    }

    static noQuotes(): ValidatorFn {
        return (control: AbstractControl): ValidationErrors | null => {
            const v: string = control.value;
            let failed = false;
            if (!Verify.empty(v) && (v.indexOf('"') !== -1 || v.indexOf("'") !== -1)) {
                failed = true;
            }

            return failed ? { noQuotes: true } : null;
        };
    }

    static noDoubleQuotes(): ValidatorFn {
        return (control: AbstractControl): ValidationErrors | null => {
            const v: string = control.value;
            let failed = false;
            if (!Verify.empty(v) && v.indexOf('"') !== -1) {
                failed = true;
            }

            return failed ? { noDoubleQuotes: true } : null;
        };
    }

    static maxLength(max: number): ValidatorFn {
        return (control: AbstractControl): ValidationErrors | null => {
            const v: string = control.value;

            return !Verify.maxLength(v, max)
                ? { maxLength: { maximumLength: max, actualLength: v ? v.length : 0 } }
                : null;
        };
    }

    static minLength(min: number): ValidatorFn {
        return (control: AbstractControl): ValidationErrors | null => {
            const v: string = control.value;

            return !Verify.minLength(v, min)
                ? { minLength: { minimumLength: min, actualLength: v ? v.length : 0 } }
                : null;
        };
    }

    static isNumber(): ValidatorFn {
        return (control: AbstractControl): ValidationErrors | null => {
            const v = control.value;

            return !Verify.isNumber(v) ? { isNumber: true } : null;
        };
    }

    static isInteger(): ValidatorFn {
        return (control: AbstractControl): ValidationErrors | null => {
            const v = control.value;

            return !Verify.isInteger(v) ? { isInteger: true } : null;
        };
    }

    static min(min: number): ValidatorFn {
        return (control: AbstractControl): ValidationErrors | null => {
            const v = control.value;

            return !Verify.min(v, min) ? { min: { minimumValue: min, actualValue: v } } : null;
        };
    }

    static max(max: number): ValidatorFn {
        return (control: AbstractControl): ValidationErrors | null => {
            const v = control.value;

            return !Verify.max(v, max) ? { max: { maximumValue: max, actualValue: v } } : null;
        };
    }

    static minSelections(minSelections: number): ValidatorFn {
        return (control: AbstractControl): ValidationErrors | null => {
            let v = control.value;
            let valid: ValidationErrors | null = null;
            if (Verify.isString(v)) {
                v = [v];
            }

            if (minSelections > 0) {
                if (v == null || v.length < minSelections) {
                    valid = {
                        minSelectionCount: minSelections,
                        minSelections: {
                            actualSelectionCount: v ? v.length : 0
                        }
                    };
                }
            }

            return valid;
        };
    }

    static maxSelections(maxSelections: number): ValidatorFn {
        return (control: AbstractControl): ValidationErrors | null => {
            let v = control.value;
            let valid: ValidationErrors | null = null;
            if (Verify.isString(v)) {
                v = [v];
            }

            if (v != null && v.length > maxSelections) {
                valid = {
                    maxSelections: {
                        actualSelectionCount: v ? v.length : 0,
                        maxSelectionCount: maxSelections
                    }
                };
            }

            return valid;
        };
    }
}
