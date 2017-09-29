import { PROPERTY_INFO } from './field-property-info';
import { DOTTestBed } from '../../../../test/dot-test-bed';
import { FieldPropertyService } from './field-properties.service';
import {
    CategoriesPropertyComponent,
    DataTypePropertyComponent,
    DefaultValuePropertyComponent
} from '../content-type-fields-properties-form/field-properties';
import { FieldService } from './field.service';
import { Validators } from '@angular/forms';
import { validateDateDefaultValue } from './validators';
import { ConnectionBackend, ResponseOptions } from '@angular/http';
import { MockBackend } from '@angular/http/testing';
import { FieldType } from '../';
import { Observable } from 'rxjs/Rx';

class TestFieldService {
    loadFieldTypes(): Observable<FieldType[]> {
        return Observable.of([
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

describe('FieldPropertyService', () => {
    beforeEach(() => {

        this.injector = DOTTestBed.resolveAndCreate([
            FieldPropertyService,
            { provide: FieldService, useClass: TestFieldService },
        ]);

        this.fieldPropertiesService = this.injector.get(FieldPropertyService);
    });

    it('should return ttrue if the property has a component linked', () => {
        expect(true).toEqual(this.fieldPropertiesService.existsComponent('categories'));
        expect(true).toEqual(this.fieldPropertiesService.existsComponent('dataType'));
        expect(true).toEqual(this.fieldPropertiesService.existsComponent('defaultValue'));

        expect(false).toEqual(this.fieldPropertiesService.existsComponent('property'));
    });

    it('should return the right component', () => {
        expect(CategoriesPropertyComponent).toEqual(this.fieldPropertiesService.getComponent('categories'));
        expect(DataTypePropertyComponent).toEqual(this.fieldPropertiesService.getComponent('dataType'));
        expect(DefaultValuePropertyComponent).toEqual(this.fieldPropertiesService.getComponent('defaultValue'));

        expect(this.fieldPropertiesService.getComponent('property')).toBeNull();
    });

    it('should return the right default value', () => {
        expect('').toEqual(this.fieldPropertiesService.getDefaultValue('categories',
                            'com.dotcms.contenttype.model.field.ImmutableRadioField'));
        expect('TEXT').toEqual(this.fieldPropertiesService.getDefaultValue('dataType',
                                                    'com.dotcms.contenttype.model.field.ImmutableRadioField'));
        expect('').toEqual(this.fieldPropertiesService.getDefaultValue('defaultValue',
                            'com.dotcms.contenttype.model.field.ImmutableRadioField'));

        expect(this.fieldPropertiesService.getDefaultValue('property')).toBeNull();
    });

    it('should return the right order', () => {
        expect(2).toEqual(this.fieldPropertiesService.getOrder('categories'));
        expect(1).toEqual(this.fieldPropertiesService.getOrder('dataType'));
        expect(4).toEqual(this.fieldPropertiesService.getOrder('defaultValue'));

        expect(this.fieldPropertiesService.getOrder('property')).toBeNull();
    });

    it('shoukd return the right set of validations', () => {
        let validations = this.fieldPropertiesService.getValidations('categories');
        expect(1).toBe(validations.length);
        expect(Validators.required).toBe(validations[0]);

        expect(0).toBe(this.fieldPropertiesService.getValidations('dataType').length);

        validations = this.fieldPropertiesService.getValidations('defaultValue');
        expect(1).toBe(validations.length);
        expect(validateDateDefaultValue).toBe(validations[0]);

        expect(0).toBe(this.fieldPropertiesService.getValidations('property').length);
    });

    it('should return if the property is editable in EditMode', () => {
        expect(this.fieldPropertiesService.isDisabledInEditMode('categories')).toBeUndefined();
        expect(true).toEqual(this.fieldPropertiesService.isDisabledInEditMode('dataType'));
        expect(this.fieldPropertiesService.isDisabledInEditMode('defaultValue')).toBeUndefined();

        expect(this.fieldPropertiesService.isDisabledInEditMode('property')).toBeNull();
    });

    it('should return the right proeprties for a Field Class', () => {
        expect(['property1', 'property2', 'property3']).toEqual(this.fieldPropertiesService.getProperties('fieldClass'));
        expect(this.fieldPropertiesService.getProperties('fieldClass2')).toBeUndefined();
    });

    it('should return the Field Type Object for a Field Class', () => {
        expect({
            clazz: 'fieldClass',
            helpText: 'help',
            id: '1',
            label: 'label',
            properties: ['property1', 'property2', 'property3']
        }).toEqual(this.fieldPropertiesService.getFieldType('fieldClass'));
        expect(this.fieldPropertiesService.getFieldType('fieldClass2')).toBeUndefined();
    });

});
