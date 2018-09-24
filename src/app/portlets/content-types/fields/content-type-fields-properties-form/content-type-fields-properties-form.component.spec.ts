import { DebugElement, ComponentFactoryResolver, Directive, Input, Injectable, Component } from '@angular/core';
import { ContentTypeFieldsPropertiesFormComponent } from './content-type-fields-properties-form.component';
import { ComponentFixture, async } from '@angular/core/testing';
import { MockDotMessageService } from '../../../../test/dot-message-service.mock';
import { DOTTestBed } from '../../../../test/dot-test-bed';
import { FormBuilder, FormGroup, ValidationErrors, Validators } from '@angular/forms';
import { FieldPropertyService } from '../service';
import { DotMessageService } from '@services/dot-messages-service';
import { ContentTypeField } from '../index';
import { By } from '@angular/platform-browser';

const mockDFormFieldData = {
    clazz: 'field.class',
    name: 'fieldName'
};

@Component({
    selector: 'dot-host-tester',
    template: '<dot-content-type-fields-properties-form [formFieldData]="mockDFormFieldData"></dot-content-type-fields-properties-form>'
})
class DotHostTesterComponent {
    mockDFormFieldData: ContentTypeField = {};

    constructor() {}
}

@Directive({
    selector: '[dotDynamicFieldProperty]'
})
class TestDynamicFieldPropertyDirective {
    @Input()
    propertyName: string;
    @Input()
    field: ContentTypeField;
    @Input()
    group: FormGroup;
}

@Injectable()
class TestFieldPropertiesService {
    getProperties(): string[] {
        return ['property1', 'property2', 'property3'];
    }

    existsComponent(propertyName: string): boolean {
        return propertyName === 'property1' || propertyName === 'property2';
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
        hostFixture.detectChanges();

        /*
            This is the way it work in the real life, it triggers the ngOnChange twice, when is added to the DOM
            and when is passed data, so I'm recreating this.

            TODO: it's should NOT be in the DOM until data is passed, need to refactor that because we're triggering
            a whole lifecycle events just because.
        */

        hostComp.mockDFormFieldData = {
            ...mockDFormFieldData
        };

        hostFixture.detectChanges();
    };

    beforeEach(async(() => {
        DOTTestBed.configureTestingModule({
            declarations: [ContentTypeFieldsPropertiesFormComponent, DotHostTesterComponent, TestDynamicFieldPropertyDirective],
            imports: [],
            providers: [
                FormBuilder,
                ComponentFactoryResolver,
                FieldPropertyService,
                { provide: FieldPropertyService, useClass: TestFieldPropertiesService },
                { provide: DotMessageService, useValue: messageServiceMock }
            ]
        });

        hostFixture = DOTTestBed.createComponent(DotHostTesterComponent);
        hostComp = hostFixture.componentInstance;
        de = hostFixture.debugElement;

        fixture = de.query(By.css('dot-content-type-fields-properties-form'));
        comp = fixture.componentInstance;

        mockFieldPropertyService = fixture.injector.get(FieldPropertyService);
    }));

    describe('should init component', () => {
        beforeEach(() => {
            spyOn(mockFieldPropertyService, 'getProperties').and.returnValue(['property1', 'property2', 'property3']);
            startHostComponent();
        });

        it('init form', () => {
            expect(mockFieldPropertyService.getProperties).toHaveBeenCalledWith('field.class');
            expect(comp.form.get('clazz').value).toBe('field.class');

            expect(comp.form.get('property1').value).toBe('');
            expect(comp.form.get('property2').value).toBe(true);
            expect(comp.form.get('property3')).toBeNull();
        });

        it('init field proeprties', () => {
            expect(comp.fieldProperties[0]).toBe('property1');
            expect(comp.fieldProperties[1]).toBe('property2');
        });
    });

    describe('checkboxes interactions', () => {
        beforeEach(() => {
            spyOn(mockFieldPropertyService, 'getProperties').and.returnValue(['searchable', 'required', 'unique', 'indexed', 'listed']);
            spyOn(mockFieldPropertyService, 'existsComponent').and.returnValue(true);
            startHostComponent();
        });

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

        it('should set system indexed and required true when you select unique', () => {
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
            spyOn(mockFieldPropertyService, 'getProperties').and.returnValue(['searchable', 'unique', 'listed']);
            spyOn(mockFieldPropertyService, 'existsComponent').and.returnValue(true);
            startHostComponent();
        });

        it("should set unique and no break when indexed and required doesn't exist", () => {
            comp.form.get('unique').setValue(true);

            expect(comp.form.get('indexed')).toBe(null);
            expect(comp.form.get('required')).toBe(null);
        });
    });
});
