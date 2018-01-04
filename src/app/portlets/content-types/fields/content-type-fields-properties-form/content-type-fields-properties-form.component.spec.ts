import { DebugElement, ComponentFactoryResolver, SimpleChange, Directive, Input, Injectable } from '@angular/core';
import { ContentTypeFieldsPropertiesFormComponent } from './content-type-fields-properties-form.component';
import { ComponentFixture, async } from '@angular/core/testing';
import { MockDotMessageService } from '../../../../test/dot-message-service.mock';
import { DOTTestBed } from '../../../../test/dot-test-bed';
import { FormBuilder, FormGroup, ValidationErrors, Validators } from '@angular/forms';
import { FieldPropertyService } from '../service';
import { DotMessageService } from '../../../../api/services/dot-messages-service';
import { Field } from '../index';

@Directive({
    selector: '[dynamicFieldProperty]',
  })
class TestDynamicFieldPropertyDirective {
    @Input() propertyName: string;
    @Input() field: Field;
    @Input() group: FormGroup;
}

@Injectable()
class TestFieldPropertiesService {
    getProperties(fieldTypeClass: string): string[] {
        return ['property1', 'property2', 'property3'];
    }

    existsComponent(propertyName: string): boolean {
        return propertyName === 'property1' || propertyName === 'property2';
    }

    getDefaultValue(propertyName: string, fieldTypeClass?: string): any {
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
    let comp: ContentTypeFieldsPropertiesFormComponent;
    let fixture: ComponentFixture<ContentTypeFieldsPropertiesFormComponent>;
    let de: DebugElement;
    let el: HTMLElement;
    const messageServiceMock = new MockDotMessageService({
        'name': 'name',
        'Label': 'Label',
        'message.field.fieldType': 'message.field.fieldType',
        'categories': 'categories',
        'Data-Type': 'Data-Type',
        'required': 'required',
        'User-Searchable': 'User-Searchable',
        'System-Indexed': 'System-Indexed',
        'listed': 'listed',
        'Unique': 'Unique',
        'Default-Value': 'Default-Value',
        'Hint': 'Hint',
        'Validation-RegEx': 'Validation-RegEx',
        'Value': 'Value',
        'Binary': 'Binary',
        'Text': 'Text',
        'True-False': 'True-False',
        'Date': 'Date',
        'Decimal': 'Decimal',
        'Whole-Number': 'Whole-Number',
        'Large-Block-of-Text': 'Large-Block-of-Text',
        'System-Field': 'System-Field',
    });

    beforeEach(async(() => {
        DOTTestBed.configureTestingModule({
            declarations: [
                ContentTypeFieldsPropertiesFormComponent,
                TestDynamicFieldPropertyDirective
            ],
            imports: [
            ],
            providers: [
                FormBuilder,
                ComponentFactoryResolver,
                FieldPropertyService,
                { provide: FieldPropertyService, useClass: TestFieldPropertiesService },
                { provide: DotMessageService, useValue: messageServiceMock },
            ]
        });

        fixture = DOTTestBed.createComponent(ContentTypeFieldsPropertiesFormComponent);
        comp = fixture.componentInstance;
        de = fixture.debugElement;
        el = de.nativeElement;
    }));

    it('should call submit function', () => {

    });

    describe('should init component', () => {
        beforeEach(async(() => {
            this.field = {
                clazz: 'field.class',
                name: 'fieldName',
            };

            comp.formFieldData = this.field;
        }));

        it('should init form right', () => {
            const mockFieldPropertyService = fixture.debugElement.injector.get(FieldPropertyService);
            const spyMethod = spyOn(mockFieldPropertyService, 'getProperties').and.returnValue(['property1', 'property2', 'property3']);

            comp.ngOnChanges({
                formFieldData: new SimpleChange(null, this.field, true),
            });

            expect(spyMethod).toHaveBeenCalledWith(this.field.clazz);
            expect(this.field.clazz).toBe(comp.form.get('clazz').value);

            expect('').toBe(comp.form.get('property1').value);
            expect(true).toBe(comp.form.get('property2').value);
            expect(comp.form.get('property3')).toBeNull();
        });

        it('should init field proeprties', () => {
            comp.ngOnChanges({
                formFieldData: new SimpleChange(null, this.field, true),
            });

            expect('property1').toBe(comp.fieldProperties[0]);
            expect('property2').toBe(comp.fieldProperties[1]);
        });

        xit('should auto select and disable indexed checkbox', () => {
            // TODO: It needs a real mock of FieldPropertyService
        });

        xit('should auto select and disable require checkbox', () => {
            // TODO: It needs a real mock of FieldPropertyService
        });

        xit('should save checked and auto checked checkbox', () => {
        // TODO: It needs a real mock of FieldPropertyService
        });

        xit('should not unchecked indexed checkbox if unique checkbox is checked', () => {
        // TODO: It needs a real mock of FieldPropertyService
        });
    });
});
