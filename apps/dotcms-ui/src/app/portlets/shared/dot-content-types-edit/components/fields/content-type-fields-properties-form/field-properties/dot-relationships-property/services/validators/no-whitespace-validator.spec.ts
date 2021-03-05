import { FormControl } from '@angular/forms';
import { noWhitespaceValidator } from './no-whitespace-validator';

describe('validateRelationship', () => {
    it('should return null if the value is set', () => {
        const formControl: FormControl = new FormControl('name');

        expect(noWhitespaceValidator(formControl)).toBeNull();
    });

    it('should return false if value is not set', () => {
        const formControl: FormControl = new FormControl(null);

        expect(noWhitespaceValidator(formControl)).toEqual({
            valid: false
        });
    });

    it('should return false if velocityVar is just white spaces', () => {
        const formControl: FormControl = new FormControl('      ');

        expect(noWhitespaceValidator(formControl)).toEqual({
            valid: false
        });
    });
});
