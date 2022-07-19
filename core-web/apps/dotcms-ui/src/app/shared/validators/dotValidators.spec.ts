import { DotValidators } from './dotValidators';
import { UntypedFormControl } from '@angular/forms';

describe('DotValidators', () => {
    it('should return hasError false when input value is only alphanumeric', () => {
        const control = new UntypedFormControl('input', DotValidators.alphaNumeric);
        control.setValue('asdfgasdasdh');

        expect(control.hasError('alphaNumericError')).toBeFalsy();
    });
    it('should return hasError true when the input value has an hyphen', () => {
        const control = new UntypedFormControl('input', DotValidators.alphaNumeric);
        control.setValue('asdfga-sdasdh');

        expect(control.hasError('alphaNumericError')).toBeTruthy();
    });
    it('should return hasError true when the input value has an space', () => {
        const control = new UntypedFormControl('input', DotValidators.alphaNumeric);
        control.setValue('asdfga sdasdh');

        expect(control.hasError('alphaNumericError')).toBeTruthy();
    });
});
