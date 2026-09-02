import { Component, DebugElement, inject, signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import {
    ReactiveFormsModule,
    UntypedFormBuilder,
    UntypedFormControl,
    UntypedFormGroup,
    Validators
} from '@angular/forms';
import { form, required } from '@angular/forms/signals';
import { By } from '@angular/platform-browser';

import { DotFieldRequiredDirective } from './dot-field-required.directive';

const REQUIRED_CLASS = 'p-label-input-required';

const hasMarker = (el: DebugElement): boolean =>
    el.nativeElement.classList.contains(REQUIRED_CLASS);

@Component({
    standalone: false,
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
    private fb = inject(UntypedFormBuilder);

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

        expect(hasMarker(labelEl)).toBeTruthy();
    });

    it('should not required field', async () => {
        labelEl = fixture.debugElement.query(By.css('[data-testid="textLabel"]'));

        expect(hasMarker(labelEl)).toBeFalsy();
    });
});

/**
 * The bare attribute has to stand on its own: the screens that need it most are the ones with no
 * `[formGroup]` anywhere above the label.
 */
@Component({
    standalone: true,
    imports: [DotFieldRequiredDirective],
    template: `<label data-testid="plainLabel" dotFieldRequired for="plain">Plain</label>`
})
class NoFormHostComponent {}

describe('Directive: dotFieldRequired (outside any form)', () => {
    it('should mark the label without a FormGroup above it', () => {
        const fixture = TestBed.createComponent(NoFormHostComponent);
        fixture.detectChanges();

        expect(hasMarker(fixture.debugElement.query(By.css('[data-testid="plainLabel"]')))).toBe(
            true
        );
    });
});

@Component({
    standalone: true,
    imports: [DotFieldRequiredDirective],
    template: `
        <label [dotFieldRequired]="tree.name" data-testid="signalRequired">Name</label>
        <label [dotFieldRequired]="tree.notes" data-testid="signalOptional">Notes</label>
    `
})
class SignalFormHostComponent {
    readonly model = signal({ name: '', notes: '' });
    readonly tree = form(this.model, (path) => {
        required(path.name);
    });
}

describe('Directive: dotFieldRequired (signal forms)', () => {
    let fixture: ComponentFixture<SignalFormHostComponent>;

    beforeEach(() => {
        fixture = TestBed.createComponent(SignalFormHostComponent);
        fixture.detectChanges();
    });

    it('should mark a field the schema declares required', () => {
        expect(hasMarker(fixture.debugElement.query(By.css('[data-testid="signalRequired"]')))).toBe(
            true
        );
    });

    it('should not mark a field the schema leaves optional', () => {
        expect(hasMarker(fixture.debugElement.query(By.css('[data-testid="signalOptional"]')))).toBe(
            false
        );
    });
});
