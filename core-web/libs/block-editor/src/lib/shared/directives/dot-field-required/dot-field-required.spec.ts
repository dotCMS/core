/* tslint:disable:no-unused-variable */

import { Component, DebugElement } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import {
    ReactiveFormsModule,
    UntypedFormBuilder,
    UntypedFormGroup,
    Validators
} from '@angular/forms';
import { By } from '@angular/platform-browser';

import { DotFieldRequiredDirective } from './dot-field-required.directive';
@Component({
    template: `
        <form [formGroup]="form">
            <label data-testid="nameLabel" dotFieldRequired for="name">Name</label>
            <input type="text" formControlName="name" />
        </form>
    `
})
class TestHostComponent {
    constructor(private fb: UntypedFormBuilder) {}
    form: UntypedFormGroup = this.fb.group({
        name: ['', Validators.required]
    });

    clearValidator() {
        this.form.get('name').clearValidators();
    }
}

describe('Directive: dotFieldRequired', () => {
    let fixture: ComponentFixture<TestHostComponent>;
    let component: TestHostComponent;
    let labelEl: DebugElement;

    beforeEach(() => {
        TestBed.configureTestingModule({
            declarations: [TestHostComponent, DotFieldRequiredDirective],
            imports: [ReactiveFormsModule]
        }).compileComponents();
        fixture = TestBed.createComponent(TestHostComponent);
        component = fixture.componentInstance;
    });

    it('should required field', async () => {
        fixture.detectChanges();
        labelEl = fixture.debugElement.query(By.css('[data-testid="nameLabel"]'));

        expect(labelEl.nativeElement.classList.contains('p-label-input-required')).toBeTruthy();
    });

    it('should not required field', async () => {
        component.clearValidator();
        fixture.detectChanges();
        labelEl = fixture.debugElement.query(By.css('[data-testid="nameLabel"]'));

        expect(labelEl.nativeElement.classList.contains('p-label-input-required')).toBeFalsy();
    });
});
