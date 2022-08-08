import { ValuesPropertyComponent } from './index';
import { ComponentFixture, waitForAsync, TestBed } from '@angular/core/testing';
import { DebugElement, Component, Input, forwardRef } from '@angular/core';
import { MockDotMessageService } from '@tests/dot-message-service.mock';
import { DotMessageService } from '@services/dot-message/dot-messages.service';
import {
    UntypedFormGroup,
    UntypedFormControl,
    NgControl,
    ReactiveFormsModule,
    ControlValueAccessor,
    NG_VALUE_ACCESSOR
} from '@angular/forms';
import { By } from '@angular/platform-browser';
import { DotFieldHelperModule } from '@components/dot-field-helper/dot-field-helper.module';
import { dotcmsContentTypeFieldBasicMock } from '@tests/dot-content-types.mock';
import { DotPipesModule } from '@pipes/dot-pipes.module';

@Component({
    selector: 'dot-field-validation-message',
    template: ''
})
class TestFieldValidationMessageComponent {
    @Input() field: NgControl;
    @Input() message: string;
}

@Component({
    selector: 'dot-textarea-content',
    template: '',
    providers: [
        {
            multi: true,
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => DotTextareaContentMockComponent)
        }
    ]
})
export class DotTextareaContentMockComponent implements ControlValueAccessor {
    @Input() show;
    @Input() height;

    propagateChange = (_: unknown) => {
        //
    };
    registerOnChange(fn): void {
        this.propagateChange = fn;
    }
    registerOnTouched(): void {
        //
    }
    writeValue(): void {
        // no-op
    }
}

describe('ValuesPropertyComponent', () => {
    let comp: ValuesPropertyComponent;
    let fixture: ComponentFixture<ValuesPropertyComponent>;
    let de: DebugElement;
    const messageServiceMock = new MockDotMessageService({
        'Validation-RegEx': 'Validation-RegEx'
    });

    beforeEach(
        waitForAsync(() => {
            TestBed.configureTestingModule({
                declarations: [
                    TestFieldValidationMessageComponent,
                    ValuesPropertyComponent,
                    DotTextareaContentMockComponent
                ],
                imports: [DotFieldHelperModule, ReactiveFormsModule, DotPipesModule],
                providers: [{ provide: DotMessageService, useValue: messageServiceMock }]
            }).compileComponents();

            fixture = TestBed.createComponent(ValuesPropertyComponent);
            comp = fixture.componentInstance;
            de = fixture.debugElement;

            comp.group = new UntypedFormGroup({
                values: new UntypedFormControl('')
            });
            comp.property = {
                name: 'values',
                value: 'value',
                field: {
                    ...dotcmsContentTypeFieldBasicMock
                }
            };
            comp.helpText = 'Helper Text';
        })
    );

    it('should have a form', () => {
        const group = new UntypedFormGroup({});
        comp.group = group;
        const divForm: DebugElement = fixture.debugElement.query(By.css('div'));

        expect(divForm).not.toBeNull();
        expect(group).toEqual(divForm.componentInstance.group);
    });

    it('should have a field-message', () => {
        fixture.detectChanges();

        const fieldValidationmessage: DebugElement = fixture.debugElement.query(
            By.css('dot-field-validation-message')
        );

        expect(fieldValidationmessage).not.toBeNull();
        expect(comp.group.controls['values']).toBe(fieldValidationmessage.componentInstance.field);
    });

    it('should have value field', () => {
        const valueField = de.query(By.css('dot-textarea-content'));
        expect(valueField).toBeTruthy();
    });

    it('should have value component with the right options', () => {
        fixture.detectChanges();
        expect(comp.value.show).toEqual(['code']);
        expect(comp.value.height).toBe('15.7rem');
    });

    it('should show dot-helper for required clazz', () => {
        comp.property.field.clazz = 'com.dotcms.contenttype.model.field.ImmutableRadioField';
        fixture.detectChanges();
        const fieldHelper: DebugElement = fixture.debugElement.query(By.css('dot-field-helper'));
        expect(fieldHelper).not.toBeNull();
    });

    it('should hide dot-helper except for required', () => {
        comp.property.field.clazz = 'random';
        fixture.detectChanges();
        const fieldHelper: DebugElement = fixture.debugElement.query(By.css('dot-field-helper'));

        expect(fieldHelper).toBeNull();
    });
});
