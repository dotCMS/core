import { FormGroup, FormControl } from '@angular/forms';
import { validateDateDefaultValue } from './date.validator';

describe('validateDateDefaultValue', () => {
    const invalidResponse = {
        validateDate: {
            valid: false
        }
    };

    it('should be valid (not a dete field time)', () => {
        const group: FormGroup = new FormGroup({
            clazz: new FormControl('not.date.class'),
            defaultValue: new FormControl('2017-08-27')
        });

        const valid = validateDateDefaultValue(<FormControl>group.controls['defaultValue']);
        expect(valid).toBeNull();
    });

    it('should be valid (DateField)', () => {
        const group: FormGroup = new FormGroup({
            clazz: new FormControl('com.dotcms.contenttype.model.field.ImmutableDateField'),
            defaultValue: new FormControl('2017-08-27')
        });

        const valid = validateDateDefaultValue(<FormControl>group.controls['defaultValue']);
        expect(valid).toBeNull();
    });

    it('should be invalid (DateField)', () => {
        const group: FormGroup = new FormGroup({
            clazz: new FormControl('com.dotcms.contenttype.model.field.ImmutableDateField'),
            defaultValue: new FormControl('2017/08/27')
        });

        const valid = validateDateDefaultValue(<FormControl>group.controls['defaultValue']);
        expect(valid).toEqual(invalidResponse);
    });

    it('should be valid (DateField)', () => {
        const group: FormGroup = new FormGroup({
            clazz: new FormControl('com.dotcms.contenttype.model.field.ImmutableDateTimeField'),
            defaultValue: new FormControl('2017-08-27 14:06:45')
        });

        const valid = validateDateDefaultValue(<FormControl>group.controls['defaultValue']);
        expect(valid).toBeNull();
    });

    it('should be invalid (DateField)', () => {
        const group: FormGroup = new FormGroup({
            clazz: new FormControl('com.dotcms.contenttype.model.field.ImmutableDateTimeField'),
            defaultValue: new FormControl('2017-08-27 140645')
        });

        const valid = validateDateDefaultValue(<FormControl>group.controls['defaultValue']);
        expect(valid).toEqual(invalidResponse);
    });

    it('should be valid (DateField)', () => {
        const group: FormGroup = new FormGroup({
            clazz: new FormControl('com.dotcms.contenttype.model.field.ImmutableTimeField'),
            defaultValue: new FormControl('14:06:45')
        });

        const valid = validateDateDefaultValue(<FormControl>group.controls['defaultValue']);
        expect(valid).toBeNull();
    });

    it('should be invalid (DateField)', () => {
        const group: FormGroup = new FormGroup({
            clazz: new FormControl('com.dotcms.contenttype.model.field.ImmutableTimeField'),
            defaultValue: new FormControl('140645')
        });

        const valid = validateDateDefaultValue(<FormControl>group.controls['defaultValue']);
        expect(valid).toEqual(invalidResponse);
    });

    it('should be valid because is not mandatory', () => {
        const group: FormGroup = new FormGroup({
            clazz: new FormControl('com.dotcms.contenttype.model.field.ImmutableDateField'),
            defaultValue: new FormControl('')
        });

        const valid = validateDateDefaultValue(<FormControl>group.controls['defaultValue']);
        expect(valid).toBeNull();
    });
    it('should be valid (DateField) when clazz equal com.dotcms.contenttype.model.field.ImmutableDateField', () => {
        const group: FormGroup = new FormGroup({
            clazz: new FormControl('com.dotcms.contenttype.model.field.ImmutableDateField'),
            defaultValue: new FormControl('now')
        });

        const valid = validateDateDefaultValue(<FormControl>group.controls['defaultValue']);
        expect(valid).toBeNull();
    });
    it('should be valid (DateField) when clazz equal com.dotcms.contenttype.model.field.ImmutableDateTimeField', () => {
        const group: FormGroup = new FormGroup({
            clazz: new FormControl('com.dotcms.contenttype.model.field.ImmutableDateTimeField'),
            defaultValue: new FormControl('now')
        });

        const valid = validateDateDefaultValue(<FormControl>group.controls['defaultValue']);
        expect(valid).toBeNull();
    });
    it('should be valid (DateField) when clazz equal com.dotcms.contenttype.model.field.ImmutableTimeField', () => {
        const group: FormGroup = new FormGroup({
            clazz: new FormControl('com.dotcms.contenttype.model.field.ImmutableTimeField'),
            defaultValue: new FormControl('now')
        });

        const valid = validateDateDefaultValue(<FormControl>group.controls['defaultValue']);
        expect(valid).toBeNull();
    });
});
