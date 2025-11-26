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
import { DotCMSClazzes, DotCMSContentTypeField } from '@dotcms/dotcms-models';
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
});
