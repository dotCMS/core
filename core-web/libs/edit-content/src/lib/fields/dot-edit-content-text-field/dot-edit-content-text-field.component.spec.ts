import { SpectatorHost, createHostFactory, byTestId } from '@openng/spectator';

import { Component, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';

import { InputTextModule } from 'primeng/inputtext';

import { DotCMSContentlet, DotCMSContentTypeField } from '@dotcms/dotcms-models';
import { createFakeTextField, createFakeContentlet } from '@dotcms/utils-testing';

import { DotEditContentTextFieldComponent } from './dot-edit-content-text-field.component';
import { INPUT_TEXT_OPTIONS, INPUT_TYPE } from './utils';

import { DotEditContentStore } from '../../store/edit-content.store';

@Component({
    standalone: false,
    selector: 'dot-custom-host',
    template: ''
})
export class MockFormComponent {
    // Host Props
    formGroup!: FormGroup;
    field!: DotCMSContentTypeField;
    contentlet!: DotCMSContentlet;
}

const TEXT_FIELD_MOCK = createFakeTextField({
    variable: 'text_field'
});

describe('DotEditContentTextFieldComponent', () => {
    let spectator: SpectatorHost<DotEditContentTextFieldComponent, MockFormComponent>;
    let textInput: Element;

    const createHost = createHostFactory({
        component: DotEditContentTextFieldComponent,
        host: MockFormComponent,
        imports: [ReactiveFormsModule, InputTextModule],
        detectChanges: false
    });

    it('should have the variable as id', () => {
        spectator = createHost(
            `<form [formGroup]="formGroup">
                <dot-edit-content-text-field [field]="field" [contentlet]="contentlet" />
            </form>`,
            {
                hostProps: {
                    formGroup: new FormGroup({
                        [TEXT_FIELD_MOCK.variable]: new FormControl('one')
                    }),
                    field: TEXT_FIELD_MOCK,
                    contentlet: createFakeContentlet({
                        [TEXT_FIELD_MOCK.variable]: 'one'
                    })
                }
            }
        );
        spectator.detectChanges();
        textInput = spectator.query(byTestId(TEXT_FIELD_MOCK.variable))!;
        expect(textInput.getAttribute('id')).toBe(TEXT_FIELD_MOCK.variable);
    });

    describe.each([
        {
            dataType: INPUT_TYPE.TEXT
        },
        {
            dataType: INPUT_TYPE.INTEGER
        },
        {
            dataType: INPUT_TYPE.FLOAT
        }
    ])('with dataType as $dataType', ({ dataType }) => {
        const options = INPUT_TEXT_OPTIONS[dataType];

        beforeEach(() => {
            spectator = createHost(
                `<form [formGroup]="formGroup">
                    <dot-edit-content-text-field [field]="field" [contentlet]="contentlet" />
                </form>`,
                {
                    hostProps: {
                        formGroup: new FormGroup({
                            [TEXT_FIELD_MOCK.variable]: new FormControl()
                        }),
                        field: {
                            ...TEXT_FIELD_MOCK,
                            dataType
                        },
                        contentlet: createFakeContentlet({
                            [TEXT_FIELD_MOCK.variable]: null
                        })
                    }
                }
            );
            spectator.detectChanges();

            textInput = spectator.query(byTestId(TEXT_FIELD_MOCK.variable))!;
        });

        it('should have the type as defined in the options', () => {
            expect(textInput.getAttribute('type')).toBe(options.type);
        });

        it('should have the inputMode as defined in the options', () => {
            expect(textInput.getAttribute('inputmode')).toBe(options.inputMode);
        });

        it('should have the step as defined in the options', () => {
            if (options.step === undefined) {
                expect(textInput.getAttribute('step')).toBeNull();

                return;
            }

            expect(textInput.getAttribute('step')).toBe(options.step.toString());
        });
    });

    it('should remove the leading slash from the value if the contentlet is an HTML page and the field is the url', () => {
        const fieldMock = createFakeTextField({
            variable: 'url'
        });
        spectator = createHost(
            `<form [formGroup]="formGroup">
                <dot-edit-content-text-field [field]="field" [contentlet]="contentlet" />
            </form>`,
            {
                hostProps: {
                    formGroup: new FormGroup({
                        [fieldMock.variable]: new FormControl('')
                    }),
                    field: fieldMock,
                    contentlet: createFakeContentlet({
                        baseType: 'HTMLPAGE',
                        [fieldMock.variable]: '/one'
                    })
                }
            }
        );
        spectator.detectChanges();
        expect(spectator.component.$initValue()).toBe('one');
    });

    it('should return the default value', () => {
        const fieldMock = createFakeTextField({
            variable: 'someValuev1',
            defaultValue: 'defaultValue'
        });
        spectator = createHost(
            `<form [formGroup]="formGroup">
                <dot-edit-content-text-field [field]="field" [contentlet]="contentlet" />
            </form>`,
            {
                hostProps: {
                    formGroup: new FormGroup({
                        [fieldMock.variable]: new FormControl('')
                    }),
                    field: fieldMock,
                    contentlet: createFakeContentlet()
                }
            }
        );
        spectator.detectChanges();
        expect(spectator.component.$initValue()).toBe('defaultValue');
    });

    it('should return the value from the contentlet', () => {
        const fieldMock = createFakeTextField({
            variable: 'field'
        });
        spectator = createHost(
            `<form [formGroup]="formGroup">
                <dot-edit-content-text-field [field]="field" [contentlet]="contentlet" />
            </form>`,
            {
                hostProps: {
                    formGroup: new FormGroup({
                        [fieldMock.variable]: new FormControl('')
                    }),
                    field: fieldMock,
                    contentlet: createFakeContentlet({
                        [fieldMock.variable]: 'myValue'
                    })
                }
            }
        );
        spectator.detectChanges();
        expect(spectator.component.$initValue()).toBe('myValue');
    });
});

/**
 * T-05 / T-07 / T-08 — the four presentation states, and the save gate (#37464).
 *
 * The four states a field can be in. Only three are reachable today: every template guards its
 * hint with `!fieldHasError`, so "error AND hint" — the one the author most needs, because the
 * hint is what explains how to fix the error — cannot happen.
 *
 *   1. no hint, no error   -> label + control
 *   2. hint,    no error   -> hint below the control
 *   3. no hint, error      -> required message, red, no icon
 *   4. hint,    error      -> required message FIRST, hint immediately below
 *
 * The gate: `$hasError` moves from `control.invalid && control.touched` to
 * `hasAttemptedSubmit && control.invalid`. Blur marks a control touched, which is why an untouched
 * author tabbing through an empty required field currently turns it red before they have tried to
 * save anything.
 */
describe('DotEditContentTextFieldComponent — hint and required error', () => {
    let spectator: SpectatorHost<DotEditContentTextFieldComponent, MockFormComponent>;

    const REQUIRED_HINTED = createFakeTextField({
        variable: 'text_field',
        required: true,
        hint: 'Use the customer legal name'
    });

    const submitted = signal(false);

    const createHost = createHostFactory({
        component: DotEditContentTextFieldComponent,
        host: MockFormComponent,
        imports: [ReactiveFormsModule, InputTextModule],
        providers: [{ provide: DotEditContentStore, useValue: { hasAttemptedSubmit: submitted } }],
        detectChanges: false
    });

    const render = (field: DotCMSContentTypeField, value = '') => {
        const control = new FormControl(value, field.required ? [Validators.required] : []);
        spectator = createHost(
            `<form [formGroup]="formGroup">
                <dot-edit-content-text-field [field]="field" [contentlet]="contentlet" />
            </form>`,
            {
                hostProps: {
                    formGroup: new FormGroup({ [field.variable]: control }),
                    field,
                    contentlet: createFakeContentlet({ [field.variable]: value })
                }
            }
        );
        spectator.detectChanges();

        return control;
    };

    const error = () => spectator.query('.p-field-error');
    const hint = () => spectator.query('.p-field-hint');

    beforeEach(() => submitted.set(false));

    describe('the four states', () => {
        // `hint: ''` is explicit throughout: createFakeBaseField() defaults hint to a faker
        // sentence, so a field built without it is never actually hint-less.
        it('state 1 — no hint, no error: renders neither', () => {
            render(createFakeTextField({ variable: 'text_field', hint: '' }));

            expect(error()).toBeNull();
            expect(hint()).toBeNull();
        });

        it('state 2 — hint, no error: renders the hint only', () => {
            render(createFakeTextField({ variable: 'text_field', hint: 'Use the legal name' }));

            expect(error()).toBeNull();
            expect(hint()?.textContent?.trim()).toBe('Use the legal name');
        });

        it('state 3 — error, no hint: renders the required message only, with no icon', () => {
            render(createFakeTextField({ variable: 'text_field', required: true, hint: '' }));
            submitted.set(true);
            spectator.detectChanges();

            expect(error()).toBeTruthy();
            expect(hint()).toBeNull();
            expect(spectator.query('.p-field-error i')).toBeNull();
        });

        it('state 4 — error AND hint: renders BOTH, error first', () => {
            render(REQUIRED_HINTED);
            submitted.set(true);
            spectator.detectChanges();

            expect(error()).toBeTruthy();
            expect(hint()).toBeTruthy();

            const order =
                spectator.element.querySelector('dot-card-field-footer')?.textContent ?? '';
            const iError = order.indexOf('dot.edit.content.form.field.required');
            const iHint = order.indexOf('Use the customer legal name');

            expect(iError).toBeGreaterThanOrEqual(0);
            expect(iHint).toBeGreaterThan(iError);
        });
    });

    describe('the save gate (AC-202)', () => {
        it('should NOT show the error on blur alone', () => {
            const control = render(REQUIRED_HINTED);

            control.markAsTouched();
            spectator.detectChanges();

            expect(error()).toBeNull();
        });

        it('should show the error once a save or publish has been attempted', () => {
            render(REQUIRED_HINTED);

            submitted.set(true);
            spectator.detectChanges();

            expect(error()).toBeTruthy();
        });

        it('should keep the hint visible while the field is not in error', () => {
            render(REQUIRED_HINTED);

            expect(hint()).toBeTruthy();
        });
    });

    describe('clearing the error (AC-203)', () => {
        it('should clear as soon as the field holds a valid value, with no second save', () => {
            const control = render(REQUIRED_HINTED);
            submitted.set(true);
            spectator.detectChanges();

            expect(error()).toBeTruthy();

            control.setValue('Acme Corporation');
            spectator.detectChanges();

            expect(error()).toBeNull();
        });

        it('should leave the hint in place when the error clears', () => {
            const control = render(REQUIRED_HINTED);
            submitted.set(true);
            spectator.detectChanges();
            control.setValue('Acme Corporation');
            spectator.detectChanges();

            expect(hint()?.textContent?.trim()).toBe('Use the customer legal name');
        });
    });
});
