import { validateRelationship } from './dot-relationship-validator';
import { FormControl } from '@angular/forms';

describe('validateRelationship', () => {
    it('should return true if both cardinality and velocityVarare set', () => {
        const formControl: FormControl = new FormControl({
            cardinality: 0,
            velocityVar: 'velocityVar'
        });

        expect(validateRelationship(formControl)).toBeNull();
    });

    it('should return false if cardinality is not set', () => {
        const formControl: FormControl = new FormControl({
            velocityVar: 'velocityVar'
        });

        expect(validateRelationship(formControl)).toEqual({
            valid: false
        });
    });

    it('should return false if velocityVar is not set', () => {
        const formControl: FormControl = new FormControl({
            cardinality: 1
        });

        expect(validateRelationship(formControl)).toEqual({
            valid: false
        });
    });
});
