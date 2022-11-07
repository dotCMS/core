import { UntypedFormControl } from '@angular/forms';
import { noWhitespaceValidator } from './no-whitespace-validator';

describe('validateRelationship', () => {
    it('should return null if the value is set', () => {
        const formControl: UntypedFormControl = new UntypedFormControl('name');

        expect(noWhitespaceValidator(formControl)).toBeNull();
    });

    it('should return false if value is not set', () => {
        const formControl: UntypedFormControl = new UntypedFormControl(null);

        expect(noWhitespaceValidator(formControl)).toEqual({
            valid: false
        });
    });

    it('should return false if velocityVar is just white spaces', () => {
        const formControl: UntypedFormControl = new UntypedFormControl('      ');

        expect(noWhitespaceValidator(formControl)).toEqual({
            valid: false
        });
    });
});
