import { DefaultValuePropertyComponent } from './index';
import { ComponentFixture, async } from '@angular/core/testing';
import { DebugElement, Component, Input } from '@angular/core';
import { MockMessageService } from '../../../../../../test/message-service.mock';
import { DOTTestBed } from '../../../../../../test/dot-test-bed';
import { MessageService } from '../../../../../../api/services/messages-service';
import { FormGroup, FormControl, NgControl } from '@angular/forms';
import { By } from '@angular/platform-browser';

@Component({
    selector: 'field-validation-message',
    template: ''
  })
 class TestFieldValidationMessageComponent {
    @Input() field: NgControl;
    @Input() message: string;
}

describe('DefaultValuePropertyComponent', () => {
    let comp: DefaultValuePropertyComponent;
    let fixture: ComponentFixture<DefaultValuePropertyComponent>;
    let de: DebugElement;
    let el: HTMLElement;
    const messageServiceMock = new MockMessageService({
        'Default-Value': 'Default-Value'
    });

    beforeEach(async(() => {
        DOTTestBed.configureTestingModule({
            declarations: [
                DefaultValuePropertyComponent,
                TestFieldValidationMessageComponent
            ],
            imports: [
            ],
            providers: [
                { provide: MessageService, useValue: messageServiceMock },
            ]
        });

        fixture = DOTTestBed.createComponent(DefaultValuePropertyComponent);
        comp = fixture.componentInstance;
        de = fixture.debugElement;
        el = de.nativeElement;
    }));

    it('should have a form', () => {
        const group = new FormGroup({});
        comp.group = group;
        const divForm: DebugElement = fixture.debugElement.query(By.css('div'));

        expect(divForm).not.toBeNull();
        expect(group).toEqual(divForm.componentInstance.group);
    });

    it('should have a input', () => {
        comp.group = new FormGroup({
            name: new FormControl('')
        });
        comp.property = {
            name: 'name',
            value: 'value',
            field: {}
        };

        fixture.detectChanges();

        const divForm: DebugElement = fixture.debugElement.query(By.css('div'));
        const pInput: DebugElement = fixture.debugElement.query(By.css('input[type="text"]'));

        expect(pInput).not.toBeNull();
    });


    it('should have a field-message', () => {
        comp.group = new FormGroup({
            name: new FormControl('')
        });
        comp.property = {
            name: 'name',
            value: 'value',
            field: {}
        };

        fixture.detectChanges();

        const divForm: DebugElement = fixture.debugElement.query(By.css('div'));
        const fieldValidationmessage: DebugElement = fixture.debugElement.query(By.css('field-validation-message'));

        expect(fieldValidationmessage).not.toBeNull();
        expect(comp.group.controls['name']).toBe(fieldValidationmessage.componentInstance.field);
    });
});
