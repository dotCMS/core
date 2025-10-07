// tslint:disable:typedef
import { NgControl } from '@angular/forms';

import { Verify } from './Verify';

// @dynamic
export class CustomValidators {
    static required() {
        return (control: NgControl): { [key: string]: any } => {
            const v: string = control.value;

            return Verify.empty(v) ? { required: true } : null;
        };
    }
    static isString(allowEmpty = false) {
        return (control: NgControl): { [key: string]: any } => {
            const v: string = control.value;

            return !Verify.isStringWithEmpty(v, allowEmpty)
                ? { isString: { emptyAllowed: allowEmpty } }
                : null;
        };
    }

    static noQuotes() {
        return (control: NgControl): { [key: string]: any } => {
            const v: string = control.value;
            let failed = false;
            if (!Verify.empty(v) && (v.indexOf('"') !== -1 || v.indexOf("'") !== -1)) {
                failed = true;
            }

            return failed ? { noQuotes: true } : null;
        };
    }

    static noDoubleQuotes() {
        return (control: NgControl): { [key: string]: any } => {
            const v: string = control.value;
            let failed = false;
            if (!Verify.empty(v) && v.indexOf('"') !== -1) {
                failed = true;
            }

            return failed ? { noDoubleQuotes: true } : null;
        };
    }

    static maxLength(max) {
        return (control: NgControl): { [key: string]: any } => {
            const v: string = control.value;

            return !Verify.maxLength(v, max)
                ? { maxLength: { maximumLength: max, actualLength: v ? v.length : 0 } }
                : null;
        };
    }

    static minLength(min) {
        return (control: NgControl): { [key: string]: any } => {
            const v: string = control.value;

            return !Verify.minLength(v, min)
                ? { minLength: { minimumLength: min, actualLength: v ? v.length : 0 } }
                : null;
        };
    }

    static isNumber() {
        return (control: NgControl): { [key: string]: any } => {
            const v: string = control.value;

            return !Verify.isNumber(v) ? { isNumber: true } : null;
        };
    }

    static isInteger() {
        return (control: NgControl): { [key: string]: any } => {
            const v: string = control.value;

            return !Verify.isInteger(v) ? { isInteger: true } : null;
        };
    }

    static min(min) {
        return (control: NgControl): { [key: string]: any } => {
            const v: string = control.value;

            return !Verify.min(v, min) ? { min: { minimumValue: min, actualValue: v } } : null;
        };
    }

    static max(max) {
        return (control: NgControl): { [key: string]: any } => {
            const v: string = control.value;

            return !Verify.max(v, max) ? { max: { maximumValue: max, actualValue: v } } : null;
        };
    }

    static minSelections(minSelections) {
        return (control: NgControl): { [key: string]: any } => {
            let v: any = control.value;
            let valid = null;
            if (Verify.isString(v)) {
                v = [v];
            }

            if (minSelections > 0) {
                if (v == null || v.length < minSelections) {
                    valid = {
                        minSelectionCount: this.minSelections,
                        minSelections: {
                            actualSelectionCount: v ? v.length : 0
                        }
                    };
                }
            }

            return valid;
        };
    }

    static maxSelections(maxSelections) {
        return (control: NgControl): { [key: string]: any } => {
            let v = control.value;
            let valid = null;
            if (Verify.isString(v)) {
                v = [v];
            }

            if (v != null && v.length > maxSelections) {
                valid = valid ? valid : {};
                valid['maxSelections'] = {
                    actualSelectionCount: v ? v.length : 0,
                    maxSelectionCount: this.maxSelections
                };
            }

            return valid;
        };
    }
}
