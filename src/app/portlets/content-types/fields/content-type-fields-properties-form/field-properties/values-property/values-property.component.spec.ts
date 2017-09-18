
import { ValuesPropertyComponent } from './index';
import { ComponentFixture, async } from '@angular/core/testing';
import { DebugElement, Component, Input } from '@angular/core';
import { MockMessageService } from '../../../../../../test/message-service.mock';
import { DOTTestBed } from '../../../../../../test/dot-test-bed';
import { MessageService } from '../../../../../../api/services/messages-service';
import { FormGroup, FormControl, NgControl } from '@angular/forms';
import { By } from '@angular/platform-browser';
import { DotTextareaContentModule } from '../../../../../../view/components/_common/dot-textarea-content/dot-textarea-content.module';

@Component({
    selector: 'field-validation-message',
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
    let el: HTMLElement;
    const messageServiceMock = new MockMessageService({
        'Validation-RegEx': 'Validation-RegEx'
    });

    beforeEach(async(() => {
        DOTTestBed.configureTestingModule({
            declarations: [
                TestFieldValidationMessageComponent,
                ValuesPropertyComponent
            ],
            imports: [
                DotTextareaContentModule
            ],
            providers: [
                { provide: MessageService, useValue: messageServiceMock },
            ]
        });

        fixture = DOTTestBed.createComponent(ValuesPropertyComponent);
        comp = fixture.componentInstance;
        de = fixture.debugElement;
        el = de.nativeElement;

        comp.group = new FormGroup({
            values: new FormControl('')
        });
        comp.property = {
            name: 'values',
            value: 'value',
            field: {}
        };
    }));

    it('should have a form', () => {
        const group = new FormGroup({});
        comp.group = group;
        const divForm: DebugElement = fixture.debugElement.query(By.css('div'));

        expect(divForm).not.toBeNull();
        expect(group).toEqual(divForm.componentInstance.group);
    });

    it('should have a field-message', () => {
        fixture.detectChanges();

        const divForm: DebugElement = fixture.debugElement.query(By.css('div'));
        const fieldValidationmessage: DebugElement = fixture.debugElement.query(By.css('field-validation-message'));

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
});
