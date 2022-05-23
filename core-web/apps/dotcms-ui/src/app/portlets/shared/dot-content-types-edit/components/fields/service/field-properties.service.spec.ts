import { of as observableOf, Observable } from 'rxjs';
import { FieldPropertyService } from './field-properties.service';
import {
    CategoriesPropertyComponent,
    DataTypePropertyComponent,
    DefaultValuePropertyComponent
} from '../content-type-fields-properties-form/field-properties';
import { FieldService } from './field.service';
import { Validators } from '@angular/forms';
import { validateDateDefaultValue } from './validators';
import { FieldType } from '..';
import { TestBed } from '@angular/core/testing';

class TestFieldService {
    loadFieldTypes(): Observable<FieldType[]> {
        return observableOf([
            {
                clazz: 'fieldClass',
                helpText: 'help',
                id: '1',
                label: 'label',
                properties: ['property1', 'property2', 'property3']
            }
        ]);
    }
}

let fieldPropertiesService;

describe('FieldPropertyService', () => {
    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [FieldPropertyService, { provide: FieldService, useClass: TestFieldService }]
        });

        fieldPropertiesService = TestBed.inject(FieldPropertyService);
    });

    it('should return true if the property has a component linked', () => {
        expect(true).toEqual(fieldPropertiesService.existsComponent('categories'));
        expect(true).toEqual(fieldPropertiesService.existsComponent('dataType'));
        expect(true).toEqual(fieldPropertiesService.existsComponent('defaultValue'));

        expect(false).toEqual(fieldPropertiesService.existsComponent('property'));
    });

    it('should return the right component', () => {
        expect(CategoriesPropertyComponent).toEqual(
            fieldPropertiesService.getComponent('categories')
        );
        expect(DataTypePropertyComponent).toEqual(fieldPropertiesService.getComponent('dataType'));
        expect(DefaultValuePropertyComponent).toEqual(
            fieldPropertiesService.getComponent('defaultValue')
        );

        expect(fieldPropertiesService.getComponent('property')).toBeNull();
    });

    it('should return the right default value', () => {
        expect('').toEqual(
            fieldPropertiesService.getDefaultValue(
                'categories',
                'com.dotcms.contenttype.model.field.ImmutableRadioField'
            )
        );
        expect('TEXT').toEqual(
            fieldPropertiesService.getDefaultValue(
                'dataType',
                'com.dotcms.contenttype.model.field.ImmutableRadioField'
            )
        );
        expect('').toEqual(
            fieldPropertiesService.getDefaultValue(
                'defaultValue',
                'com.dotcms.contenttype.model.field.ImmutableRadioField'
            )
        );

        expect(fieldPropertiesService.getDefaultValue('property')).toBeNull();
    });

    it('should return the right order', () => {
        expect(2).toEqual(fieldPropertiesService.getOrder('categories'));
        expect(1).toEqual(fieldPropertiesService.getOrder('dataType'));
        expect(4).toEqual(fieldPropertiesService.getOrder('defaultValue'));

        expect(fieldPropertiesService.getOrder('property')).toBeNull();
    });

    it('shoukd return the right set of validations', () => {
        let validations = fieldPropertiesService.getValidations('categories');
        expect(1).toBe(validations.length);
        expect(Validators.required).toBe(validations[0]);

        expect(0).toBe(fieldPropertiesService.getValidations('dataType').length);

        validations = fieldPropertiesService.getValidations('defaultValue');
        expect(1).toBe(validations.length);
        expect(validateDateDefaultValue).toBe(validations[0]);

        expect(0).toBe(fieldPropertiesService.getValidations('property').length);
    });

    it('should return if the property is editable in EditMode', () => {
        expect(fieldPropertiesService.isDisabledInEditMode('categories')).toBeUndefined();
        expect(true).toEqual(fieldPropertiesService.isDisabledInEditMode('dataType'));
        expect(fieldPropertiesService.isDisabledInEditMode('defaultValue')).toBeUndefined();

        expect(fieldPropertiesService.isDisabledInEditMode('property')).toBeNull();
    });

    it('should return if the property is editable when field is fixed', () => {
        expect(true).toEqual(fieldPropertiesService.isDisabledInFixed('name'));
        expect(fieldPropertiesService.isDisabledInFixed('dataType')).toBeUndefined();
    });

    it('should return the right proeprties for a Field Class', () => {
        expect(['property1', 'property2', 'property3']).toEqual(
            fieldPropertiesService.getProperties('fieldClass')
        );
        expect(fieldPropertiesService.getProperties('fieldClass2')).toBeUndefined();
    });

    it('should return the Field Type Object for a Field Class', () => {
        expect({
            clazz: 'fieldClass',
            helpText: 'help',
            id: '1',
            label: 'label',
            properties: ['property1', 'property2', 'property3']
        }).toEqual(fieldPropertiesService.getFieldType('fieldClass'));
        expect(fieldPropertiesService.getFieldType('fieldClass2')).toBeUndefined();
    });
});
