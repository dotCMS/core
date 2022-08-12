import { UntypedFormGroup, UntypedFormControl } from '@angular/forms';
import { validateDateDefaultValue } from './date.validator';

describe('validateDateDefaultValue', () => {
    const invalidResponse = {
        validateDate: {
            valid: false
        }
    };

    it('should be valid (not a dete field time)', () => {
        const group: UntypedFormGroup = new UntypedFormGroup({
            clazz: new UntypedFormControl('not.date.class'),
            defaultValue: new UntypedFormControl('2017-08-27')
        });

        const valid = validateDateDefaultValue(<UntypedFormControl>group.controls['defaultValue']);
        expect(valid).toBeNull();
    });

    it('should be valid (DateField)', () => {
        const group: UntypedFormGroup = new UntypedFormGroup({
            clazz: new UntypedFormControl('com.dotcms.contenttype.model.field.ImmutableDateField'),
            defaultValue: new UntypedFormControl('2017-08-27')
        });

        const valid = validateDateDefaultValue(<UntypedFormControl>group.controls['defaultValue']);
        expect(valid).toBeNull();
    });

    it('should be invalid (DateField)', () => {
        const group: UntypedFormGroup = new UntypedFormGroup({
            clazz: new UntypedFormControl('com.dotcms.contenttype.model.field.ImmutableDateField'),
            defaultValue: new UntypedFormControl('2017/08/27')
        });

        const valid = validateDateDefaultValue(<UntypedFormControl>group.controls['defaultValue']);
        expect(valid).toEqual(invalidResponse);
    });

    it('should be valid (DateField)', () => {
        const group: UntypedFormGroup = new UntypedFormGroup({
            clazz: new UntypedFormControl('com.dotcms.contenttype.model.field.ImmutableDateTimeField'),
            defaultValue: new UntypedFormControl('2017-08-27 14:06:45')
        });

        const valid = validateDateDefaultValue(<UntypedFormControl>group.controls['defaultValue']);
        expect(valid).toBeNull();
    });

    it('should be invalid (DateField)', () => {
        const group: UntypedFormGroup = new UntypedFormGroup({
            clazz: new UntypedFormControl('com.dotcms.contenttype.model.field.ImmutableDateTimeField'),
            defaultValue: new UntypedFormControl('2017-08-27 140645')
        });

        const valid = validateDateDefaultValue(<UntypedFormControl>group.controls['defaultValue']);
        expect(valid).toEqual(invalidResponse);
    });

    it('should be valid (DateField)', () => {
        const group: UntypedFormGroup = new UntypedFormGroup({
            clazz: new UntypedFormControl('com.dotcms.contenttype.model.field.ImmutableTimeField'),
            defaultValue: new UntypedFormControl('14:06:45')
        });

        const valid = validateDateDefaultValue(<UntypedFormControl>group.controls['defaultValue']);
        expect(valid).toBeNull();
    });

    it('should be invalid (DateField)', () => {
        const group: UntypedFormGroup = new UntypedFormGroup({
            clazz: new UntypedFormControl('com.dotcms.contenttype.model.field.ImmutableTimeField'),
            defaultValue: new UntypedFormControl('140645')
        });

        const valid = validateDateDefaultValue(<UntypedFormControl>group.controls['defaultValue']);
        expect(valid).toEqual(invalidResponse);
    });

    it('should be valid because is not mandatory', () => {
        const group: UntypedFormGroup = new UntypedFormGroup({
            clazz: new UntypedFormControl('com.dotcms.contenttype.model.field.ImmutableDateField'),
            defaultValue: new UntypedFormControl('')
        });

        const valid = validateDateDefaultValue(<UntypedFormControl>group.controls['defaultValue']);
        expect(valid).toBeNull();
    });
    it('should be valid (DateField) when clazz equal com.dotcms.contenttype.model.field.ImmutableDateField', () => {
        const group: UntypedFormGroup = new UntypedFormGroup({
            clazz: new UntypedFormControl('com.dotcms.contenttype.model.field.ImmutableDateField'),
            defaultValue: new UntypedFormControl('now')
        });

        const valid = validateDateDefaultValue(<UntypedFormControl>group.controls['defaultValue']);
        expect(valid).toBeNull();
    });
    it('should be valid (DateField) when clazz equal com.dotcms.contenttype.model.field.ImmutableDateTimeField', () => {
        const group: UntypedFormGroup = new UntypedFormGroup({
            clazz: new UntypedFormControl('com.dotcms.contenttype.model.field.ImmutableDateTimeField'),
            defaultValue: new UntypedFormControl('now')
        });

        const valid = validateDateDefaultValue(<UntypedFormControl>group.controls['defaultValue']);
        expect(valid).toBeNull();
    });
    it('should be valid (DateField) when clazz equal com.dotcms.contenttype.model.field.ImmutableTimeField', () => {
        const group: UntypedFormGroup = new UntypedFormGroup({
            clazz: new UntypedFormControl('com.dotcms.contenttype.model.field.ImmutableTimeField'),
            defaultValue: new UntypedFormControl('now')
        });

        const valid = validateDateDefaultValue(<UntypedFormControl>group.controls['defaultValue']);
        expect(valid).toBeNull();
    });
});
