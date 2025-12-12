/* eslint-disable @typescript-eslint/no-explicit-any */

import {
    Component,
    ComponentFactoryResolver,
    DebugElement,
    Directive,
    Injectable,
    Input
} from '@angular/core';
import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import {
    ReactiveFormsModule,
    UntypedFormBuilder,
    UntypedFormGroup,
    ValidationErrors,
    Validators
} from '@angular/forms';
import { By } from '@angular/platform-browser';

import { DotMessageService } from '@dotcms/data-access';
import {
    DotCMSClazzes,
    DotCMSContentTypeField,
    NEW_RENDER_MODE_VARIABLE_KEY
} from '@dotcms/dotcms-models';
import { DotMessagePipe, DotSafeHtmlPipe } from '@dotcms/ui';
import { dotcmsContentTypeFieldBasicMock, MockDotMessageService } from '@dotcms/utils-testing';

import { ContentTypeFieldsPropertiesFormComponent } from './content-type-fields-properties-form.component';

import { FieldPropertyService } from '../service';

const mockDFormFieldData = {
    ...dotcmsContentTypeFieldBasicMock,
    clazz: DotCMSClazzes.TEXT,
    name: 'fieldName',
    id: '123'
};

@Component({
    selector: 'dot-host-tester',
    template:
        '<dot-content-type-fields-properties-form [formFieldData]="mockDFormFieldData"></dot-content-type-fields-properties-form>',
    standalone: false
})
class DotHostTesterComponent {
    mockDFormFieldData: DotCMSContentTypeField = {
        ...dotcmsContentTypeFieldBasicMock
    };
}

@Directive({
    selector: '[dotDynamicFieldProperty]',
    standalone: false
})
class TestDynamicFieldPropertyDirective {
    @Input()
    propertyName: string;
    @Input()
    field: DotCMSContentTypeField;
    @Input()
    group: UntypedFormGroup;
}

@Injectable()
class TestFieldPropertiesService {
    getProperties(): string[] {
        return ['property1', 'property2', 'property3'];
    }

    existsComponent(propertyName: string): boolean {
        return propertyName === 'property1' || propertyName === 'property2';
    }

    getValue(field: DotCMSContentTypeField, propertyName: string): any {
        return field[propertyName];
    }

    getDefaultValue(propertyName: string): any {
        return propertyName === 'property1' ? '' : true;
    }

    getValidations(propertyName: string): ValidationErrors[] {
        return propertyName === 'property1' ? [Validators.required] : [];
    }

    isDisabledInEditMode(propertyName: string): boolean {
        return propertyName === 'property1';
    }

    getOrder(propertyName: string): any {
        return propertyName === 'property1' ? 0 : 1;
    }
}

describe('ContentTypeFieldsPropertiesFormComponent', () => {
    let hostComp: DotHostTesterComponent;
    let hostFixture: ComponentFixture<DotHostTesterComponent>;

    let comp: ContentTypeFieldsPropertiesFormComponent;
    let fixture: DebugElement;
    let de: DebugElement;

    let mockFieldPropertyService: FieldPropertyService;

    const messageServiceMock = new MockDotMessageService({
        name: 'name',
        Label: 'Label',
        'message.field.fieldType': 'message.field.fieldType',
        categories: 'categories',
        'Data-Type': 'Data-Type',
        required: 'required',
        'User-Searchable': 'User-Searchable',
        'System-Indexed': 'System-Indexed',
        listed: 'listed',
        Unique: 'Unique',
        'Default-Value': 'Default-Value',
        Hint: 'Hint',
        'Validation-RegEx': 'Validation-RegEx',
        Value: 'Value',
        Binary: 'Binary',
        Text: 'Text',
        'True-False': 'True-False',
        Date: 'Date',
        Decimal: 'Decimal',
        'Whole-Number': 'Whole-Number',
        'Large-Block-of-Text': 'Large-Block-of-Text',
        'System-Field': 'System-Field'
    });

    const startHostComponent = () => {
        hostComp.mockDFormFieldData = {
            ...mockDFormFieldData
        };

        hostFixture.detectChanges();

        return new Promise((resolve) => {
            setTimeout(() => resolve(true), 1);
        });
    };

    beforeEach(waitForAsync(() => {
        TestBed.configureTestingModule({
            declarations: [
                ContentTypeFieldsPropertiesFormComponent,
                DotHostTesterComponent,
                TestDynamicFieldPropertyDirective
            ],
            imports: [ReactiveFormsModule, DotSafeHtmlPipe, DotMessagePipe],
            providers: [
                UntypedFormBuilder,
                ComponentFactoryResolver,
                { provide: FieldPropertyService, useClass: TestFieldPropertiesService },
                { provide: DotMessageService, useValue: messageServiceMock }
            ]
        }).compileComponents();

        hostFixture = TestBed.createComponent(DotHostTesterComponent);
        hostComp = hostFixture.componentInstance;
        de = hostFixture.debugElement;

        fixture = de.query(By.css('dot-content-type-fields-properties-form'));
        comp = fixture.componentInstance;

        mockFieldPropertyService = fixture.injector.get(FieldPropertyService);
    }));

    describe('should init component', () => {
        beforeEach(() => {
            jest.spyOn(mockFieldPropertyService, 'getProperties').mockReturnValue([
                'property1',
                'property2',
                'property3',
                'id'
            ]);
        });

        beforeEach(async () => await startHostComponent());

        it('should init form', () => {
            expect(mockFieldPropertyService.getProperties).toHaveBeenCalledWith(DotCMSClazzes.TEXT);
            expect(mockFieldPropertyService.getProperties).toHaveBeenCalledTimes(1);
            expect(comp.form.get('clazz').value).toBe(DotCMSClazzes.TEXT);

            expect(comp.form.get('id').value).toBe('123');
            expect(comp.form.get('property1').value).toBe('');
            expect(comp.form.get('property2').value).toBe(true);
            expect(comp.form.get('property3')).toBeNull();
        });

        it('should init field properties', () => {
            expect(comp.fieldProperties[0]).toBe('property1');
            expect(comp.fieldProperties[1]).toBe('property2');
        });

        it('should emit false to valid when saveFieldProperties is called', () => {
            jest.spyOn(comp.valid, 'emit');
            comp.saveFieldProperties();

            expect(comp.valid.emit).toHaveBeenCalledWith(false);
            expect(comp.valid.emit).toHaveBeenCalledTimes(1);
        });
    });

    describe('checkboxes interactions', () => {
        beforeEach(() => {
            jest.spyOn(mockFieldPropertyService, 'getProperties').mockReturnValue([
                'searchable',
                'required',
                'unique',
                'indexed',
                'listed'
            ]);
            jest.spyOn(mockFieldPropertyService, 'existsComponent').mockReturnValue(true);
        });

        beforeEach(async () => await startHostComponent());

        it('should set system indexed true when select user searchable', () => {
            comp.form.get('indexed').setValue(false);
            comp.form.get('searchable').setValue(true);

            expect(comp.form.get('indexed').value).toBe(true);
            expect(comp.form.get('indexed').disabled).toBe(true);
        });

        it('should set system indexed true when you select show in list', () => {
            comp.form.get('indexed').setValue(false);
            comp.form.get('listed').setValue(true);

            expect(comp.form.get('indexed').value).toBe(true);
            expect(comp.form.get('indexed').disabled).toBe(true);
        });

        // TODO: fix because is failing intermittently
        xit('should set system indexed and required true when you select unique', () => {
            comp.form.get('indexed').setValue(false);
            comp.form.get('required').setValue(false);

            comp.form.get('unique').setValue(true);

            expect(comp.form.get('indexed').value).toBe(true);
            expect(comp.form.get('required').value).toBe(true);

            expect(comp.form.get('indexed').disabled).toBe(true);
            expect(comp.form.get('required').disabled).toBe(true);
        });
    });

    describe('checkboxes interactions with undefined fields', () => {
        beforeEach(() => {
            jest.spyOn(mockFieldPropertyService, 'getProperties').mockReturnValue([
                'searchable',
                'unique',
                'listed'
            ]);
            jest.spyOn(mockFieldPropertyService, 'existsComponent').mockReturnValue(true);
        });

        beforeEach(async () => await startHostComponent());

        it("should set unique and no break when indexed and required doesn't exist", () => {
            comp.form.get('unique').setValue(true);

            expect(comp.form.get('indexed')).toBe(null);
            expect(comp.form.get('required')).toBe(null);
        });
    });

    describe('form fields', () => {
        beforeEach(() => {
            jest.spyOn(mockFieldPropertyService, 'getProperties').mockReturnValue([
                'property1',
                'searchable',
                'unique',
                'listed'
            ]);
            jest.spyOn(mockFieldPropertyService, 'existsComponent').mockReturnValue(true);
        });

        beforeEach(async () => await startHostComponent());

        it('should only be disabled when isDisabledInEditMode is true', () => {
            const formProperties = Object.keys(comp.form.controls);
            formProperties.forEach((property) => {
                if (
                    comp.form.controls[property].disabled &&
                    !mockFieldPropertyService.isDisabledInEditMode(property)
                ) {
                    fail();
                }
            });
        });
    });

    describe('transformFormValue', () => {
        beforeEach(() => {
            jest.spyOn(mockFieldPropertyService, 'getProperties').mockReturnValue([
                'property1',
                'property2'
            ]);
        });

        describe('when field clazz is NOT CUSTOM_FIELD', () => {
            beforeEach(async () => await startHostComponent());

            it('should return the value as-is', () => {
                const formValue = {
                    name: 'testField',
                    label: 'Test Field',
                    clazz: DotCMSClazzes.TEXT
                };
                const result = comp.transformFormValue(formValue);

                expect(result).toEqual(formValue);
                expect(result).toBe(formValue);
            });
        });

        describe('when field clazz is CUSTOM_FIELD', () => {
            beforeEach(() => {
                comp.formFieldData = {
                    ...mockDFormFieldData,
                    clazz: DotCMSClazzes.CUSTOM_FIELD
                };
            });

            it('should create fieldVariables array with newRenderMode when fieldVariables is undefined', () => {
                comp.formFieldData.fieldVariables = undefined;
                const formValue = { newRenderMode: 'editable', name: 'customField' };
                const result = comp.transformFormValue(formValue);

                expect(result.fieldVariables).toBeDefined();
                expect(result.fieldVariables.length).toBe(1);
                expect(result.fieldVariables[0]).toEqual({
                    clazz: DotCMSClazzes.FIELD_VARIABLE,
                    key: NEW_RENDER_MODE_VARIABLE_KEY,
                    value: 'editable'
                });
                expect(result.newRenderMode).toBe('editable');
                expect(result.name).toBe('customField');
            });

            it('should create fieldVariables array with newRenderMode when fieldVariables is empty array', () => {
                comp.formFieldData.fieldVariables = [];
                const formValue = { newRenderMode: 'readonly', label: 'Custom Field' };
                const result = comp.transformFormValue(formValue);

                expect(result.fieldVariables).toBeDefined();
                expect(result.fieldVariables.length).toBe(1);
                expect(result.fieldVariables[0]).toEqual({
                    clazz: DotCMSClazzes.FIELD_VARIABLE,
                    key: NEW_RENDER_MODE_VARIABLE_KEY,
                    value: 'readonly'
                });
                expect(result.newRenderMode).toBe('readonly');
                expect(result.label).toBe('Custom Field');
            });

            it('should preserve existing fieldVariables and add newRenderMode', () => {
                comp.formFieldData.fieldVariables = [
                    {
                        key: 'existingVar1',
                        value: 'value1',
                        clazz: DotCMSClazzes.FIELD_VARIABLE,
                        id: '1',
                        fieldId: 'field123'
                    },
                    {
                        key: 'existingVar2',
                        value: 'value2',
                        clazz: DotCMSClazzes.FIELD_VARIABLE,
                        id: '2',
                        fieldId: 'field123'
                    }
                ];
                const formValue = { newRenderMode: 'editable', name: 'customField' };
                const result = comp.transformFormValue(formValue);

                expect(result.fieldVariables.length).toBe(3);
                expect(result.fieldVariables[0]).toEqual({
                    key: 'existingVar1',
                    value: 'value1',
                    clazz: DotCMSClazzes.FIELD_VARIABLE,
                    id: '1',
                    fieldId: 'field123'
                });
                expect(result.fieldVariables[1]).toEqual({
                    key: 'existingVar2',
                    value: 'value2',
                    clazz: DotCMSClazzes.FIELD_VARIABLE,
                    id: '2',
                    fieldId: 'field123'
                });
                expect(result.fieldVariables[2]).toEqual({
                    clazz: DotCMSClazzes.FIELD_VARIABLE,
                    key: NEW_RENDER_MODE_VARIABLE_KEY,
                    value: 'editable'
                });
            });

            it('should update existing newRenderMode variable and preserve other variables', () => {
                comp.formFieldData.fieldVariables = [
                    {
                        key: 'existingVar1',
                        value: 'value1',
                        clazz: DotCMSClazzes.FIELD_VARIABLE,
                        id: '1',
                        fieldId: 'field123'
                    },
                    {
                        key: NEW_RENDER_MODE_VARIABLE_KEY,
                        value: 'oldRenderMode',
                        clazz: DotCMSClazzes.FIELD_VARIABLE,
                        id: 'renderModeId',
                        fieldId: 'field123'
                    },
                    {
                        key: 'existingVar2',
                        value: 'value2',
                        clazz: DotCMSClazzes.FIELD_VARIABLE,
                        id: '2',
                        fieldId: 'field123'
                    }
                ];
                const formValue = { newRenderMode: 'newRenderMode', name: 'customField' };
                const result = comp.transformFormValue(formValue);

                expect(result.fieldVariables.length).toBe(3);
                expect(result.fieldVariables[0]).toEqual({
                    key: 'existingVar1',
                    value: 'value1',
                    clazz: DotCMSClazzes.FIELD_VARIABLE,
                    id: '1',
                    fieldId: 'field123'
                });
                expect(result.fieldVariables[1]).toEqual({
                    key: 'existingVar2',
                    value: 'value2',
                    clazz: DotCMSClazzes.FIELD_VARIABLE,
                    id: '2',
                    fieldId: 'field123'
                });
                expect(result.fieldVariables[2]).toEqual({
                    id: 'renderModeId',
                    fieldId: 'field123',
                    clazz: DotCMSClazzes.FIELD_VARIABLE,
                    key: NEW_RENDER_MODE_VARIABLE_KEY,
                    value: 'newRenderMode'
                });
            });

            it('should handle newRenderMode value being undefined', () => {
                comp.formFieldData.fieldVariables = [
                    {
                        key: 'existingVar1',
                        value: 'value1',
                        clazz: DotCMSClazzes.FIELD_VARIABLE,
                        id: '1',
                        fieldId: 'field123'
                    }
                ];
                const formValue = { newRenderMode: undefined, name: 'customField' };
                const result = comp.transformFormValue(formValue);

                expect(result.fieldVariables.length).toBe(2);
                expect(result.fieldVariables[1]).toEqual({
                    clazz: DotCMSClazzes.FIELD_VARIABLE,
                    key: NEW_RENDER_MODE_VARIABLE_KEY,
                    value: undefined
                });
            });

            it('should handle newRenderMode value being null', () => {
                comp.formFieldData.fieldVariables = [];
                const formValue = { newRenderMode: null, name: 'customField' };
                const result = comp.transformFormValue(formValue);

                expect(result.fieldVariables.length).toBe(1);
                expect(result.fieldVariables[0]).toEqual({
                    clazz: DotCMSClazzes.FIELD_VARIABLE,
                    key: NEW_RENDER_MODE_VARIABLE_KEY,
                    value: null
                });
            });

            it('should preserve existing newRenderMode variable properties when updating', () => {
                comp.formFieldData.fieldVariables = [
                    {
                        key: NEW_RENDER_MODE_VARIABLE_KEY,
                        value: 'oldValue',
                        clazz: DotCMSClazzes.FIELD_VARIABLE,
                        id: 'existingId',
                        fieldId: 'fieldId123'
                    }
                ];
                const formValue = { newRenderMode: 'newValue', name: 'customField' };
                const result = comp.transformFormValue(formValue);

                expect(result.fieldVariables.length).toBe(1);
                expect(result.fieldVariables[0]).toEqual({
                    id: 'existingId',
                    fieldId: 'fieldId123',
                    clazz: DotCMSClazzes.FIELD_VARIABLE,
                    key: NEW_RENDER_MODE_VARIABLE_KEY,
                    value: 'newValue'
                });
            });

            it('should preserve all form value properties along with fieldVariables', () => {
                comp.formFieldData.fieldVariables = [];
                const formValue = {
                    newRenderMode: 'editable',
                    name: 'customField',
                    label: 'Custom Field Label',
                    required: true,
                    indexed: false
                };
                const result = comp.transformFormValue(formValue);

                expect(result.name).toBe('customField');
                expect(result.label).toBe('Custom Field Label');
                expect(result.required).toBe(true);
                expect(result.indexed).toBe(false);
                expect(result.newRenderMode).toBe('editable');
                expect(result.fieldVariables).toBeDefined();
                expect(result.fieldVariables.length).toBe(1);
            });
        });
    });
});
