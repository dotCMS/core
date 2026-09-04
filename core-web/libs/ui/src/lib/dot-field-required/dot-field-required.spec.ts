import {
    byTestId,
    createComponentFactory,
    createDirectiveFactory,
    Spectator,
    SpectatorDirective
} from '@openng/spectator/jest';

import { Component, signal } from '@angular/core';
import {
    ReactiveFormsModule,
    UntypedFormBuilder,
    UntypedFormControl,
    UntypedFormGroup,
    Validators
} from '@angular/forms';
import { form, required } from '@angular/forms/signals';

import { DotFieldRequiredDirective } from './dot-field-required.directive';

const REQUIRED_CLASS = 'p-label-input-required';

const isMarked = (element: Element | null): boolean =>
    !!element?.classList.contains(REQUIRED_CLASS);

/**
 * The bare attribute has to stand on its own: the screens that need it most are the ones with no
 * `[formGroup]` anywhere above the label.
 */
describe('DotFieldRequiredDirective (bare)', () => {
    let spectator: SpectatorDirective<DotFieldRequiredDirective>;

    const createDirective = createDirectiveFactory(DotFieldRequiredDirective);

    it('should mark the label with no form of any kind above it', () => {
        spectator = createDirective(
            `<label dotFieldRequired data-testid="plainLabel" for="plain">Plain</label>`
        );

        expect(isMarked(spectator.query(byTestId('plainLabel')))).toBe(true);
    });
});

@Component({
    template: `
        <form [formGroup]="form">
            <label data-testid="nameLabel" dotFieldRequired for="name">Name</label>
            <input id="name" type="text" formControlName="name" />
            <label
                data-testid="textLabel"
                checkIsRequiredControl="text"
                dotFieldRequired
                for="text">
                Text
            </label>
            <input id="text" type="text" formControlName="text" />
        </form>
    `,
    imports: [ReactiveFormsModule, DotFieldRequiredDirective]
})
class ReactiveHostComponent {
    private fb = new UntypedFormBuilder();

    form: UntypedFormGroup = this.fb.group({
        name: new UntypedFormControl('', Validators.required),
        text: new UntypedFormControl('')
    });
}

describe('DotFieldRequiredDirective (reactive forms)', () => {
    let spectator: Spectator<ReactiveHostComponent>;

    const createComponent = createComponentFactory({
        component: ReactiveHostComponent,
        imports: [ReactiveFormsModule]
    });

    beforeEach(() => {
        spectator = createComponent();
    });

    it('should mark a control carrying Validators.required', () => {
        expect(isMarked(spectator.query(byTestId('nameLabel')))).toBe(true);
    });

    it('should leave an optional control unmarked', () => {
        expect(isMarked(spectator.query(byTestId('textLabel')))).toBe(false);
    });
});

/** A host is the right shape here: the template needs a real `form()` to hand the directive. */
@Component({
    template: `
        <label [dotFieldRequired]="tree.name" data-testid="signalRequired">Name</label>
        <label [dotFieldRequired]="tree.notes" data-testid="signalOptional">Notes</label>
    `,
    imports: [DotFieldRequiredDirective]
})
class SignalFormHostComponent {
    readonly model = signal({ name: '', notes: '' });
    readonly tree = form(this.model, (path) => {
        required(path.name);
    });
}

describe('DotFieldRequiredDirective (signal forms)', () => {
    let spectator: Spectator<SignalFormHostComponent>;

    const createComponent = createComponentFactory(SignalFormHostComponent);

    beforeEach(() => {
        spectator = createComponent();
    });

    it('should mark a field the schema declares required', () => {
        expect(isMarked(spectator.query(byTestId('signalRequired')))).toBe(true);
    });

    it('should not mark a field the schema leaves optional', () => {
        expect(isMarked(spectator.query(byTestId('signalOptional')))).toBe(false);
    });
});
