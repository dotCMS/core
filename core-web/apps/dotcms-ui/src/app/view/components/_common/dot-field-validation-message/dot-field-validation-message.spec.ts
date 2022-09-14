/* eslint-disable @typescript-eslint/no-explicit-any */

import { DebugElement, Pipe, PipeTransform } from '@angular/core';

import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { DotFieldValidationMessageComponent } from './dot-field-validation-message';
import { By } from '@angular/platform-browser';
import { UntypedFormControl, Validators } from '@angular/forms';
import { MockDotMessageService } from '@tests/dot-message-service.mock';
import { DotMessageService } from '@services/dot-message/dot-messages.service';

@Pipe({
    name: 'dm'
})
class DotMessageMockPipe implements PipeTransform {
    transform(): string {
        return 'Required';
    }
}

const messageServiceMock = new MockDotMessageService({
    'contentType.errors.input.maxlength': 'Value must be no more than {0} characters and has {1}',
    'contentType.form.variable.placeholder': 'Will be auto-generated if left empty'
});

describe('FieldValidationComponent', () => {
    let de: DebugElement;
    let el: HTMLElement;
    let fixture: ComponentFixture<DotFieldValidationMessageComponent>;
    let component: DotFieldValidationMessageComponent;

    beforeEach(
        waitForAsync(() => {
            TestBed.configureTestingModule({
                declarations: [DotFieldValidationMessageComponent, DotMessageMockPipe],
                providers: [
                    {
                        provide: DotMessageService,
                        useValue: messageServiceMock
                    }
                ]
            }).compileComponents();
        })
    );

    beforeEach(() => {
        fixture = TestBed.createComponent(DotFieldValidationMessageComponent);
        component = fixture.debugElement.componentInstance;
    });

    it('should hide the message by default', () => {
        const control = new UntypedFormControl('', Validators.required);
        component.field = control;
        fixture.detectChanges();
        de = fixture.debugElement.query(By.css('[data-testId="dotErrorMsg"]'));
        expect(de).toBeNull();
    });

    it('should hide the message when field it is valid', () => {
        const control = new UntypedFormControl('valid-content', Validators.required);
        control.markAsDirty();
        control.markAsTouched();

        component.field = control;

        fixture.detectChanges();
        de = fixture.debugElement.query(By.css('[data-testId="dotErrorMsg"]'));

        expect(de).toBeNull();
    });

    it('should show the default message when field it is dirty and invalid', () => {
        const control = new UntypedFormControl('', Validators.required);
        control.markAsDirty();
        control.markAsTouched();
        component.field = control;

        fixture.detectChanges();

        de = fixture.debugElement.query(By.css('[data-testId="dotErrorMsg"]'));
        el = de.nativeElement;
        expect(el).toBeDefined();
        expect(el.textContent).toContain('Required');
    });

    it('should show the message when field it is dirty and invalid', () => {
        const control = new UntypedFormControl('', Validators.required);
        component.defaultMessage = 'Required';

        control.markAsDirty();
        control.markAsTouched();

        component.field = control;
        fixture.detectChanges();

        de = fixture.debugElement.query(By.css('[data-testId="dotErrorMsg"]'));

        el = de.nativeElement;
        expect(el).toBeDefined();
        expect(el.textContent).toContain('Required');
    });
});
