import { Component, DebugElement } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import {
    ReactiveFormsModule,
    UntypedFormBuilder,
    UntypedFormControl,
    UntypedFormGroup,
    Validators
} from '@angular/forms';
import { By } from '@angular/platform-browser';

import { DotFieldRequiredDirective } from './dot-field-required.directive';

@Component({
    template: `
        <form [formGroup]="form">
            <label data-testid="nameLabel" dotFieldRequired for="name">Name</label>
            <input id="name" type="text" formControlName="name" />
            <br />
            <label
                data-testid="textLabel"
                checkIsRequiredControl="text"
                dotFieldRequired
                for="text">
                Text
            </label>
            <input id="text" type="text" formControlName="text" />
        </form>
    `
})
class TestHostComponent {
    constructor(private fb: UntypedFormBuilder) {}
    form: UntypedFormGroup = this.fb.group({
        name: new UntypedFormControl('', Validators.required),
        text: new UntypedFormControl('')
    });
}

describe('Directive: dotFieldRequired', () => {
    let fixture: ComponentFixture<TestHostComponent>;
    let labelEl: DebugElement;

    beforeEach(() => {
        TestBed.configureTestingModule({
            declarations: [TestHostComponent],
            imports: [ReactiveFormsModule, DotFieldRequiredDirective]
        }).compileComponents();
        fixture = TestBed.createComponent(TestHostComponent);
        fixture.detectChanges();
    });

    it('should required field', async () => {
        labelEl = fixture.debugElement.query(By.css('[data-testid="nameLabel"]'));

        expect(labelEl.nativeElement.classList.contains('p-label-input-required')).toBeTruthy();
    });

    it('should not required field', async () => {
        labelEl = fixture.debugElement.query(By.css('[data-testid="textLabel"]'));

        expect(labelEl.nativeElement.classList.contains('p-label-input-required')).toBeFalsy();
    });
});
