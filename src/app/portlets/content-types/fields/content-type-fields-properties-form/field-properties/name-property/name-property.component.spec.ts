import { NamePropertyComponent } from './index';
import { ComponentFixture, async } from '@angular/core/testing';
import { DebugElement, Component, Input } from '@angular/core';
import { MockDotMessageService } from '../../../../../../test/dot-message-service.mock';
import { DOTTestBed } from '../../../../../../test/dot-test-bed';
import { DotMessageService } from '../../../../../../api/services/dot-messages-service';
import { FormGroup, FormControl, NgControl } from '@angular/forms';
import { By } from '@angular/platform-browser';

@Component({
    selector: 'dot-field-validation-message',
    template: ''
})
class TestFieldValidationMessageComponent {
    @Input() field: NgControl;
    @Input() message: string;
}

describe('NamePropertyComponent', () => {
    let comp: NamePropertyComponent;
    let fixture: ComponentFixture<NamePropertyComponent>;
    let de: DebugElement;

    const messageServiceMock = new MockDotMessageService({
        'Default-Value': 'Default-Value'
    });

    beforeEach(
        async(() => {
            DOTTestBed.configureTestingModule({
                declarations: [NamePropertyComponent, TestFieldValidationMessageComponent],
                imports: [],
                providers: [{ provide: DotMessageService, useValue: messageServiceMock }]
            });

            fixture = DOTTestBed.createComponent(NamePropertyComponent);
            comp = fixture.componentInstance;
            de = fixture.debugElement;

            comp.property = {
                name: 'name',
                value: 'value',
                field: {}
            };
        })
    );

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

        fixture.detectChanges();

        const pInput: DebugElement = fixture.debugElement.query(By.css('input[type="text"]'));

        expect(pInput).not.toBeNull();
    });

    it('should have a field-message', () => {
        comp.group = new FormGroup({
            name: new FormControl('')
        });

        fixture.detectChanges();

        const fieldValidationmessage: DebugElement = fixture.debugElement.query(By.css('dot-field-validation-message'));

        expect(fieldValidationmessage).not.toBeNull();
        expect(comp.group.controls['name']).toBe(fieldValidationmessage.componentInstance.field);
    });

    it('should focus on input on load', () => {
        spyOn(comp.name.nativeElement, 'focus');

        comp.group = new FormGroup({
            name: new FormControl('')
        });

        fixture.detectChanges();

        expect(comp.name.nativeElement.focus).toHaveBeenCalledTimes(1);
    });

    it('should NOT focus on input on load', () => {
        spyOn(comp.name.nativeElement, 'focus');

        comp.group = new FormGroup({
            name: new FormControl({value: '', disabled: true})
        });

        fixture.detectChanges();

        expect(comp.name.nativeElement.focus).not.toHaveBeenCalled();
    });
});
