import { ValuesPropertyComponent } from './index';
import { ComponentFixture, async } from '@angular/core/testing';
import { DebugElement, Component, Input } from '@angular/core';
import { MockDotMessageService } from '@tests/dot-message-service.mock';
import { DOTTestBed } from '@tests/dot-test-bed';
import { DotMessageService } from '@services/dot-message/dot-messages.service';
import { FormGroup, FormControl, NgControl } from '@angular/forms';
import { By } from '@angular/platform-browser';
import { DotTextareaContentModule } from '@components/_common/dot-textarea-content/dot-textarea-content.module';
import { DotFieldHelperModule } from '@components/dot-field-helper/dot-field-helper.module';
import { dotcmsContentTypeFieldBasicMock } from '@tests/dot-content-types.mock';

@Component({
    selector: 'dot-field-validation-message',
    template: ''
})
class TestFieldValidationMessageComponent {
    @Input() field: NgControl;
    @Input() message: string;
}

describe('ValuesPropertyComponent', () => {
    let comp: ValuesPropertyComponent;
    let fixture: ComponentFixture<ValuesPropertyComponent>;
    let de: DebugElement;
    const messageServiceMock = new MockDotMessageService({
        'Validation-RegEx': 'Validation-RegEx'
    });

    beforeEach(
        async(() => {
            DOTTestBed.configureTestingModule({
                declarations: [TestFieldValidationMessageComponent, ValuesPropertyComponent],
                imports: [DotTextareaContentModule, DotFieldHelperModule],
                providers: [{ provide: DotMessageService, useValue: messageServiceMock }]
            });

            fixture = DOTTestBed.createComponent(ValuesPropertyComponent);
            comp = fixture.componentInstance;
            de = fixture.debugElement;

            comp.group = new FormGroup({
                values: new FormControl('')
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
        const group = new FormGroup({});
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
        expect(comp.value.height).toBe('90px');
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
