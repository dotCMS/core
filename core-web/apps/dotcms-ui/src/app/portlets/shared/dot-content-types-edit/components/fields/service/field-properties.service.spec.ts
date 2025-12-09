import { Observable, of as observableOf } from 'rxjs';

import { TestBed } from '@angular/core/testing';
import { Validators } from '@angular/forms';

import { DotCMSContentTypeField, DotRenderModes } from '@dotcms/dotcms-models';

import { FieldPropertyService } from './field-properties.service';
import { FieldService } from './field.service';
import { validateDateDefaultValue } from './validators';

import { FieldType } from '..';
import {
    CategoriesPropertyComponent,
    DataTypePropertyComponent,
    DefaultValuePropertyComponent
} from '../content-type-fields-properties-form/field-properties';

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

    describe('getValue', () => {
        it('should return the value from fieldVariable when propertyName is newRenderMode and fieldVariable exists', () => {
            const field: DotCMSContentTypeField = {
                id: '123',
                name: 'Test Field',
                variable: 'testField',
                clazz: 'com.dotcms.contenttype.model.field.ImmutableCustomField',
                dataType: 'text',
                fieldType: 'CustomField',
                fieldTypeLabel: 'Custom Field',
                fieldVariables: [
                    {
                        id: 'var1',
                        key: 'newRenderMode',
                        value: DotRenderModes.COMPONENT,
                        fieldId: '123',
                        clazz: 'com.dotcms.contenttype.model.field.ImmutableFieldVariable'
                    }
                ],
                contentTypeId: 'contentTypeId',
                fixed: false,
                iDate: 1234567890,
                indexed: false,
                listed: false,
                modDate: 1234567890,
                readOnly: false,
                required: false,
                searchable: false,
                sortOrder: 1,
                unique: false
            };

            const result = fieldPropertiesService.getValue(field, 'newRenderMode');
            expect(result).toEqual(DotRenderModes.COMPONENT);
        });

        it('should return DotRenderModes.IFRAME when propertyName is newRenderMode and fieldVariable does not exist', () => {
            const field: DotCMSContentTypeField = {
                id: '123',
                name: 'Test Field',
                variable: 'testField',
                clazz: 'com.dotcms.contenttype.model.field.ImmutableCustomField',
                dataType: 'text',
                fieldType: 'CustomField',
                fieldTypeLabel: 'Custom Field',
                fieldVariables: [
                    {
                        id: 'var1',
                        key: 'otherVariable',
                        value: 'someValue',
                        fieldId: '123',
                        clazz: 'com.dotcms.contenttype.model.field.ImmutableFieldVariable'
                    }
                ],
                contentTypeId: 'contentTypeId',
                fixed: false,
                iDate: 1234567890,
                indexed: false,
                listed: false,
                modDate: 1234567890,
                readOnly: false,
                required: false,
                searchable: false,
                sortOrder: 1,
                unique: false
            };

            const result = fieldPropertiesService.getValue(field, 'newRenderMode');
            expect(result).toEqual(DotRenderModes.IFRAME);
        });

        it('should return DotRenderModes.IFRAME when propertyName is newRenderMode and fieldVariables is empty', () => {
            const field: DotCMSContentTypeField = {
                id: '123',
                name: 'Test Field',
                variable: 'testField',
                clazz: 'com.dotcms.contenttype.model.field.ImmutableCustomField',
                dataType: 'text',
                fieldType: 'CustomField',
                fieldTypeLabel: 'Custom Field',
                fieldVariables: [],
                contentTypeId: 'contentTypeId',
                fixed: false,
                iDate: 1234567890,
                indexed: false,
                listed: false,
                modDate: 1234567890,
                readOnly: false,
                required: false,
                searchable: false,
                sortOrder: 1,
                unique: false
            };

            const result = fieldPropertiesService.getValue(field, 'newRenderMode');
            expect(result).toEqual(DotRenderModes.IFRAME);
        });

        it('should return DotRenderModes.IFRAME when propertyName is newRenderMode and fieldVariables is undefined', () => {
            const field: DotCMSContentTypeField = {
                id: '123',
                name: 'Test Field',
                variable: 'testField',
                clazz: 'com.dotcms.contenttype.model.field.ImmutableCustomField',
                dataType: 'text',
                fieldType: 'CustomField',
                fieldTypeLabel: 'Custom Field',
                fieldVariables: undefined as unknown as [],
                contentTypeId: 'contentTypeId',
                fixed: false,
                iDate: 1234567890,
                indexed: false,
                listed: false,
                modDate: 1234567890,
                readOnly: false,
                required: false,
                searchable: false,
                sortOrder: 1,
                unique: false
            };

            const result = fieldPropertiesService.getValue(field, 'newRenderMode');
            expect(result).toEqual(DotRenderModes.IFRAME);
        });

        it('should return DotRenderModes.IFRAME when propertyName is newRenderMode and field is null', () => {
            const result = fieldPropertiesService.getValue(
                null as unknown as DotCMSContentTypeField,
                'newRenderMode'
            );
            expect(result).toEqual(DotRenderModes.IFRAME);
        });

        it('should return DotRenderModes.IFRAME when propertyName is newRenderMode and fieldVariable value is empty string', () => {
            const field: DotCMSContentTypeField = {
                id: '123',
                name: 'Test Field',
                variable: 'testField',
                clazz: 'com.dotcms.contenttype.model.field.ImmutableCustomField',
                dataType: 'text',
                fieldType: 'CustomField',
                fieldTypeLabel: 'Custom Field',
                fieldVariables: [
                    {
                        id: 'var1',
                        key: 'newRenderMode',
                        value: '',
                        fieldId: '123',
                        clazz: 'com.dotcms.contenttype.model.field.ImmutableFieldVariable'
                    }
                ],
                contentTypeId: 'contentTypeId',
                fixed: false,
                iDate: 1234567890,
                indexed: false,
                listed: false,
                modDate: 1234567890,
                readOnly: false,
                required: false,
                searchable: false,
                sortOrder: 1,
                unique: false
            };

            const result = fieldPropertiesService.getValue(field, 'newRenderMode');
            expect(result).toEqual(DotRenderModes.IFRAME);
        });

        it('should return field property value when propertyName is not newRenderMode', () => {
            const field: DotCMSContentTypeField = {
                id: '123',
                name: 'Test Field',
                variable: 'testField',
                clazz: 'com.dotcms.contenttype.model.field.ImmutableTextField',
                dataType: 'text',
                fieldType: 'Text',
                fieldTypeLabel: 'Text Field',
                fieldVariables: [],
                contentTypeId: 'contentTypeId',
                fixed: false,
                iDate: 1234567890,
                indexed: false,
                listed: false,
                modDate: 1234567890,
                readOnly: false,
                required: true,
                searchable: false,
                sortOrder: 1,
                unique: false,
                hint: 'Test hint'
            };

            expect(fieldPropertiesService.getValue(field, 'name')).toEqual('Test Field');
            expect(fieldPropertiesService.getValue(field, 'hint')).toEqual('Test hint');
            expect(fieldPropertiesService.getValue(field, 'required')).toEqual(true);
            expect(fieldPropertiesService.getValue(field, 'id')).toEqual('123');
        });

        it('should return undefined when propertyName is not newRenderMode and property does not exist on field', () => {
            const field: DotCMSContentTypeField = {
                id: '123',
                name: 'Test Field',
                variable: 'testField',
                clazz: 'com.dotcms.contenttype.model.field.ImmutableTextField',
                dataType: 'text',
                fieldType: 'Text',
                fieldTypeLabel: 'Text Field',
                fieldVariables: [],
                contentTypeId: 'contentTypeId',
                fixed: false,
                iDate: 1234567890,
                indexed: false,
                listed: false,
                modDate: 1234567890,
                readOnly: false,
                required: false,
                searchable: false,
                sortOrder: 1,
                unique: false
            };

            const result = fieldPropertiesService.getValue(field, 'nonExistentProperty');
            expect(result).toBeUndefined();
        });
    });
});
