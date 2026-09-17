import { SpectatorHost, createHostFactory } from '@openng/spectator';

import { Component } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';

import { RadioButton } from 'primeng/radiobutton';

import { DotCMSContentlet, DotCMSContentTypeField } from '@dotcms/dotcms-models';
import { createFakeContentlet } from '@dotcms/utils-testing';

import { DotEditContentRadioFieldComponent } from './dot-edit-content-radio-field.component';

import {
    RADIO_FIELD_BOOLEAN_MOCK,
    RADIO_FIELD_FLOAT_MOCK,
    RADIO_FIELD_INTEGER_MOCK,
    RADIO_FIELD_TEXT_MOCK
} from '../../utils/mocks';

@Component({
    standalone: false,
    selector: 'dot-custom-host',
    template: ''
})
export class MockFormComponent {
    // Host Props
    formGroup: FormGroup;
    field: DotCMSContentTypeField;
    contentlet: DotCMSContentlet;
}

describe('DotEditContentRadioFieldComponent', () => {
    let spectator: SpectatorHost<DotEditContentRadioFieldComponent, MockFormComponent>;

    const createHost = createHostFactory({
        component: DotEditContentRadioFieldComponent,
        host: MockFormComponent,
        imports: [ReactiveFormsModule],
        detectChanges: false
    });

    describe('test with value', () => {
        it('should render radio selected if the form have value', () => {
            spectator = createHost(
                `<form [formGroup]="formGroup">
                    <dot-edit-content-radio-field [field]="field" [contentlet]="contentlet" />
                </form>`,
                {
                    hostProps: {
                        formGroup: new FormGroup({
                            [RADIO_FIELD_TEXT_MOCK.variable]: new FormControl('one')
                        }),
                        field: RADIO_FIELD_TEXT_MOCK,
                        contentlet: createFakeContentlet({
                            [RADIO_FIELD_TEXT_MOCK.variable]: 'one'
                        })
                    }
                }
            );
            spectator.detectChanges();

            const inputChecked = spectator.queryAll(RadioButton).filter((radio) => radio.checked);
            expect(inputChecked.length).toBe(1);
        });
    });

    describe('test without value', () => {
        it('should dont have any value if the form value or defaultValue is null', () => {
            spectator = createHost(
                `<form [formGroup]="formGroup">
                    <dot-edit-content-radio-field [field]="field" [contentlet]="contentlet" />
                </form>`,
                {
                    hostProps: {
                        formGroup: new FormGroup({
                            [RADIO_FIELD_TEXT_MOCK.variable]: new FormControl()
                        }),
                        field: RADIO_FIELD_TEXT_MOCK,
                        contentlet: createFakeContentlet({
                            [RADIO_FIELD_TEXT_MOCK.variable]: null
                        })
                    }
                }
            );
            spectator.detectChanges();
            const inputChecked = spectator.queryAll(RadioButton).filter((radio) => radio.checked);
            expect(inputChecked.length).toBe(0);
        });

        it('should set the key/value the same when bad formatting options passed', () => {
            const RADIO_FIELD_TEXT_MOCK_WITHOUT_VALUE_AND_LABEL = {
                ...RADIO_FIELD_TEXT_MOCK,
                values: 'one\r\ntwo'
            };
            spectator = createHost(
                `<form [formGroup]="formGroup">
                    <dot-edit-content-radio-field [field]="field" [contentlet]="contentlet" />
                </form>`,
                {
                    hostProps: {
                        formGroup: new FormGroup({
                            [RADIO_FIELD_TEXT_MOCK_WITHOUT_VALUE_AND_LABEL.variable]:
                                new FormControl()
                        }),
                        field: RADIO_FIELD_TEXT_MOCK_WITHOUT_VALUE_AND_LABEL,
                        contentlet: createFakeContentlet({
                            [RADIO_FIELD_TEXT_MOCK_WITHOUT_VALUE_AND_LABEL.variable]: null
                        })
                    }
                }
            );
            spectator.detectChanges();

            expect(spectator.queryAll(RadioButton).map((radio) => radio.value)).toEqual([
                'one',
                'two'
            ]);
        });

        it('should have label with for attribute and text equal to radio options', () => {
            const expectedOptions = [
                { label: 'One', value: 'one' },
                { label: 'Two', value: 'two' }
            ];
            spectator = createHost(
                `<form [formGroup]="formGroup">
                    <dot-edit-content-radio-field [field]="field" [contentlet]="contentlet" />
                </form>`,
                {
                    hostProps: {
                        formGroup: new FormGroup({
                            [RADIO_FIELD_TEXT_MOCK.variable]: new FormControl()
                        }),
                        field: RADIO_FIELD_TEXT_MOCK,
                        contentlet: createFakeContentlet({
                            [RADIO_FIELD_TEXT_MOCK.variable]: null
                        })
                    }
                }
            );
            spectator.detectChanges();

            spectator.queryAll(RadioButton).forEach((radio) => {
                const expectedOption = expectedOptions.find((opt) => opt.value === radio.value);
                expect(expectedOption).toBeTruthy();
                const labelEl = spectator.query(`label[for="${radio.inputId}"]`);
                expect(labelEl).toBeTruthy();
                expect(labelEl?.textContent?.trim()).toEqual(expectedOption?.label);
            });
        });

        it('should set the key/value the same when bad formatting options passed', () => {
            const RADIO_FIELD_FLOAT_MOCK_WITHOUT_VALUE_AND_LABEL = {
                ...RADIO_FIELD_FLOAT_MOCK,
                values: '100.5'
            };
            spectator = createHost(
                `<form [formGroup]="formGroup">
                    <dot-edit-content-radio-field [field]="field" [contentlet]="contentlet" />
                </form>`,
                {
                    hostProps: {
                        formGroup: new FormGroup({
                            [RADIO_FIELD_FLOAT_MOCK_WITHOUT_VALUE_AND_LABEL.variable]:
                                new FormControl()
                        }),
                        field: RADIO_FIELD_FLOAT_MOCK_WITHOUT_VALUE_AND_LABEL,
                        contentlet: createFakeContentlet({
                            [RADIO_FIELD_FLOAT_MOCK_WITHOUT_VALUE_AND_LABEL.variable]: null
                        })
                    }
                }
            );
            spectator.detectChanges();

            const expectedList = [
                {
                    label: '100.5',
                    value: 100.5
                }
            ];
            expect(
                spectator
                    .queryAll(RadioButton)
                    .every((radioOption) => typeof radioOption.value === 'number')
            ).toBeTruthy();

            expectedList.forEach((option) => {
                expect(
                    spectator
                        .queryAll(RadioButton)
                        .find((radioOption) => radioOption.value === option.value)
                ).toBeTruthy();
            });
        });
    });

    describe('test DataType', () => {
        it('should have a options array as radio with Text dataType', () => {
            const expectedList = [
                {
                    label: 'One',
                    value: 'one'
                },
                {
                    label: 'Two',
                    value: 'two'
                }
            ];
            spectator = createHost(
                `<form [formGroup]="formGroup">
                    <dot-edit-content-radio-field [field]="field" [contentlet]="contentlet" />
                </form>`,
                {
                    hostProps: {
                        formGroup: new FormGroup({
                            [RADIO_FIELD_TEXT_MOCK.variable]: new FormControl()
                        }),
                        field: RADIO_FIELD_TEXT_MOCK,
                        contentlet: createFakeContentlet({
                            [RADIO_FIELD_TEXT_MOCK.variable]: null
                        })
                    }
                }
            );
            spectator.detectChanges();

            expect(
                spectator
                    .queryAll(RadioButton)
                    .every((radioOption) => typeof radioOption.value === 'string')
            ).toBeTruthy();

            expectedList.forEach((option) => {
                expect(
                    spectator
                        .queryAll(RadioButton)
                        .find((radioOption) => radioOption.value === option.value)
                ).toBeTruthy();
            });
        });

        it('should have a options array as radio with Boolean dataType', () => {
            const expectedList = [
                {
                    label: 'Falsy',
                    value: false
                },
                {
                    label: 'Truthy',
                    value: true
                }
            ];
            spectator = createHost(
                `<form [formGroup]="formGroup">
                    <dot-edit-content-radio-field [field]="field" [contentlet]="contentlet" />
                </form>`,
                {
                    hostProps: {
                        formGroup: new FormGroup({
                            [RADIO_FIELD_BOOLEAN_MOCK.variable]: new FormControl()
                        }),
                        field: RADIO_FIELD_BOOLEAN_MOCK,
                        contentlet: createFakeContentlet({
                            [RADIO_FIELD_BOOLEAN_MOCK.variable]: null
                        })
                    }
                }
            );
            spectator.detectChanges();

            expect(
                spectator
                    .queryAll(RadioButton)
                    .every((radioOption) => typeof radioOption.value === 'boolean')
            ).toBeTruthy();

            expectedList.forEach((option) => {
                expect(
                    spectator
                        .queryAll(RadioButton)
                        .find((radioOption) => radioOption.value === option.value)
                ).toBeTruthy();
            });
        });
        it('should have a options array as radio with Integer dataType', () => {
            const expectedList = [
                {
                    label: 'Twelve',
                    value: 12
                },
                {
                    label: 'Twenty',
                    value: 20
                },
                {
                    label: 'Thirty',
                    value: 30
                }
            ];
            spectator = createHost(
                `<form [formGroup]="formGroup">
                    <dot-edit-content-radio-field [field]="field" [contentlet]="contentlet" />
                </form>`,
                {
                    hostProps: {
                        formGroup: new FormGroup({
                            [RADIO_FIELD_INTEGER_MOCK.variable]: new FormControl()
                        }),
                        field: RADIO_FIELD_INTEGER_MOCK,
                        contentlet: createFakeContentlet({
                            [RADIO_FIELD_INTEGER_MOCK.variable]: null
                        })
                    }
                }
            );
            spectator.detectChanges();

            expect(
                spectator
                    .queryAll(RadioButton)
                    .every((radioOption) => typeof radioOption.value === 'number')
            ).toBeTruthy();

            expectedList.forEach((option) => {
                expect(
                    spectator
                        .queryAll(RadioButton)
                        .find((radioOption) => radioOption.value === option.value)
                ).toBeTruthy();
            });
        });

        it('should have a options array as radio with Float dataType', () => {
            const expectedList = [
                {
                    label: 'Five point two',
                    value: 5.2
                },
                {
                    label: 'Nine point three',
                    value: 9.3
                }
            ];
            spectator = createHost(
                `<form [formGroup]="formGroup">
                    <dot-edit-content-radio-field [field]="field" [contentlet]="contentlet" />
                </form>`,
                {
                    hostProps: {
                        formGroup: new FormGroup({
                            [RADIO_FIELD_FLOAT_MOCK.variable]: new FormControl()
                        }),
                        field: RADIO_FIELD_FLOAT_MOCK,
                        contentlet: createFakeContentlet({
                            [RADIO_FIELD_FLOAT_MOCK.variable]: null
                        })
                    }
                }
            );
            spectator.detectChanges();

            expect(
                spectator
                    .queryAll(RadioButton)
                    .every((radioOption) => typeof radioOption.value === 'number')
            ).toBeTruthy();

            expectedList.forEach((option) => {
                expect(
                    spectator
                        .queryAll(RadioButton)
                        .find((radioOption) => radioOption.value === option.value)
                ).toBeTruthy();
            });
        });
    });
});

/**
 * T-03 — option rows use the global `.form-radio` (AC-109).
 *
 * `.form .form-radio { @apply flex flex-row items-center gap-2 }` computes identically to the
 * hand-rolled `flex items-center gap-2` it replaces — `flex-row` is the default for
 * `display: flex` — so this is a zero-delta swap on the ROW.
 *
 * It is not zero-delta on the option LABELS, and that is the point: `.form .form-radio label`
 * additionally applies `text-sm font-normal`, which is a DESCENDANT selector and so reaches them
 * where they already sit, with no restructuring. Option labels go 14px/400 -> 12.25px/400, which
 * is the one accepted delta that #37460's measured-impact table does not list (research R3).
 */
describe('DotEditContentRadioFieldComponent — option rows', () => {
    let spectator: SpectatorHost<DotEditContentRadioFieldComponent, MockFormComponent>;

    const createHost = createHostFactory({
        component: DotEditContentRadioFieldComponent,
        host: MockFormComponent,
        imports: [ReactiveFormsModule],
        detectChanges: false
    });

    beforeEach(() => {
        spectator = createHost(
            `<form [formGroup]="formGroup">
                <dot-edit-content-radio-field [field]="field" [contentlet]="contentlet" />
            </form>`,
            {
                hostProps: {
                    formGroup: new FormGroup({
                        [RADIO_FIELD_TEXT_MOCK.variable]: new FormControl(null)
                    }),
                    field: RADIO_FIELD_TEXT_MOCK,
                    contentlet: createFakeContentlet({ [RADIO_FIELD_TEXT_MOCK.variable]: null })
                }
            }
        );
        spectator.detectChanges();
    });

    it('should wrap each option in a .form-radio row', () => {
        expect(spectator.queryAll('.form-radio').length).toBeGreaterThan(0);
    });

    it('should not hand-roll the layout the global class already provides', () => {
        expect(spectator.query('.flex.items-center.gap-2')).toBeNull();
    });

    it('should not use the .form-checkbox variant', () => {
        expect(spectator.query('.form-checkbox')).toBeNull();
    });

    it('should keep each option label inside its row so the descendant rule reaches it', () => {
        const row = spectator.query('.form-radio');

        expect(row.querySelector('label')).toBeTruthy();
    });
});

/**
 * AC-209 — naming and requiredness for an option group.
 *
 * A set of options has no single control to carry `for` / `aria-required`: the field's value is the
 * selection, not any one input. The group itself becomes the named widget, and because a `<div>` is
 * not a labelable element, it is named with `aria-labelledby` rather than the label's `for`.
 */
describe('DotEditContentRadioFieldComponent — option group semantics (AC-209)', () => {
    let spectator: SpectatorHost<DotEditContentRadioFieldComponent, MockFormComponent>;

    const createHost = createHostFactory({
        component: DotEditContentRadioFieldComponent,
        host: MockFormComponent,
        imports: [ReactiveFormsModule],
        detectChanges: false
    });

    const render = (required: boolean) => {
        spectator = createHost(
            `<form [formGroup]="formGroup">
                <dot-edit-content-radio-field [field]="field" [contentlet]="contentlet" />
            </form>`,
            {
                hostProps: {
                    formGroup: new FormGroup({
                        [RADIO_FIELD_TEXT_MOCK.variable]: new FormControl(null)
                    }),
                    field: { ...RADIO_FIELD_TEXT_MOCK, required },
                    contentlet: createFakeContentlet({ [RADIO_FIELD_TEXT_MOCK.variable]: null })
                }
            }
        );
        spectator.detectChanges();
    };

    it('should expose the options as a radiogroup', () => {
        render(true);

        expect(spectator.query('[role="radiogroup"]')).toBeTruthy();
    });

    it('should name the group from the field label', () => {
        render(true);

        const group = spectator.query('[role="radiogroup"]');

        expect(group.getAttribute('aria-labelledby')).toBe(
            'label-' + RADIO_FIELD_TEXT_MOCK.variable
        );
        expect(spectator.query('label[dotCardFieldLabel]')?.id).toBe(
            'label-' + RADIO_FIELD_TEXT_MOCK.variable
        );
    });

    it('should mark the group required, a role ARIA defines aria-required on', () => {
        render(true);

        expect(spectator.query('[role="radiogroup"]').getAttribute('aria-required')).toBe('true');
    });

    it('should not mark the group when the field is not required', () => {
        render(false);

        expect(spectator.query('[role="radiogroup"]').getAttribute('aria-required')).toBeNull();
    });
});
