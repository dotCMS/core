import { SpectatorHost, createHostFactory } from '@openng/spectator/vitest';

import { Component } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';

import { Checkbox } from 'primeng/checkbox';

import { DotCMSContentlet, DotCMSContentTypeField } from '@dotcms/dotcms-models';
import { createFakeContentlet } from '@dotcms/utils-testing';

import { DotEditContentCheckboxFieldComponent } from './dot-edit-content-checkbox-field.component';

import { getSingleSelectableFieldOptions } from '../../utils/functions.util';
import { CHECKBOX_FIELD_MOCK } from '../../utils/mocks';

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

describe('DotEditContentCheckboxFieldComponent', () => {
    let spectator: SpectatorHost<DotEditContentCheckboxFieldComponent, MockFormComponent>;

    const createHost = createHostFactory({
        component: DotEditContentCheckboxFieldComponent,
        host: MockFormComponent,
        imports: [ReactiveFormsModule],
        detectChanges: false
    });

    describe('test with value', () => {
        it('should render a checbox selected if the form have value', () => {
            spectator = createHost(
                `<form [formGroup]="formGroup">
                    <dot-edit-content-checkbox-field [field]="field" [contentlet]="contentlet" />
                </form>`,
                {
                    hostProps: {
                        formGroup: new FormGroup({
                            [CHECKBOX_FIELD_MOCK.variable]: new FormControl(['one', 'two'])
                        }),
                        field: CHECKBOX_FIELD_MOCK,
                        contentlet: createFakeContentlet({
                            [CHECKBOX_FIELD_MOCK.variable]: ['one', 'two']
                        })
                    }
                }
            );
            spectator.detectChanges();

            const checkboxChecked = spectator
                .queryAll(Checkbox)
                .filter((checkbox) => checkbox.checked);
            expect(checkboxChecked.length).toBe(2);
        });
    });

    describe('test without value', () => {
        beforeEach(() => {
            spectator = createHost(
                `<form [formGroup]="formGroup">
                    <dot-edit-content-checkbox-field [field]="field" [contentlet]="contentlet" />
                </form>`,
                {
                    hostProps: {
                        formGroup: new FormGroup({
                            [CHECKBOX_FIELD_MOCK.variable]: new FormControl()
                        }),
                        field: CHECKBOX_FIELD_MOCK,
                        contentlet: createFakeContentlet({
                            [CHECKBOX_FIELD_MOCK.variable]: null
                        })
                    }
                }
            );
            spectator.detectChanges();
        });

        it('should render a checkbox list', () => {
            expect(spectator.queryAll(Checkbox).length).toBe(2);
        });

        it('should dont have any checkbox checked if the form value or defaultValue is null', () => {
            const checkboxChecked = spectator
                .queryAll(Checkbox)
                .filter((checkbox) => checkbox.checked);
            expect(checkboxChecked.length).toBe(0);
        });

        it('should have label with for attribute and text equal to checkbox options', () => {
            spectator.detectComponentChanges();

            const options = getSingleSelectableFieldOptions(
                CHECKBOX_FIELD_MOCK.values || '',
                CHECKBOX_FIELD_MOCK.dataType
            );
            spectator.queryAll(Checkbox).forEach((checkbox, index) => {
                const selector = `label[for="${checkbox.inputId}"]`;
                const labelEl = spectator.query<HTMLLabelElement>(selector);
                expect(labelEl).toBeTruthy();
                const expectedLabel = options[index]?.label ?? '';
                expect(labelEl?.textContent?.trim()).toEqual(expectedLabel);
            });
        });
    });

    it('should set the key/value the same when bad formattings options passed', () => {
        const CHECKBOX_FIELD_MOCK_WITHOUT_VALUE_AND_LABEL = {
            ...CHECKBOX_FIELD_MOCK,
            values: 'one\r\ntwo'
        };
        spectator = createHost(
            `<form [formGroup]="formGroup">
                <dot-edit-content-checkbox-field [field]="field" [contentlet]="contentlet" />
            </form>`,
            {
                hostProps: {
                    formGroup: new FormGroup({
                        [CHECKBOX_FIELD_MOCK_WITHOUT_VALUE_AND_LABEL.variable]: new FormControl()
                    }),
                    field: CHECKBOX_FIELD_MOCK_WITHOUT_VALUE_AND_LABEL,
                    contentlet: createFakeContentlet({
                        [CHECKBOX_FIELD_MOCK_WITHOUT_VALUE_AND_LABEL.variable]: null
                    })
                }
            }
        );
        spectator.detectChanges();

        expect(spectator.queryAll(Checkbox).map((checkbox) => checkbox.value)).toEqual([
            'one',
            'two'
        ]);
    });

    describe('test with value (string, pipe, comma, boolean, numeric)', () => {
        it('should render checkboxes for pipe format', () => {
            const field = {
                ...CHECKBOX_FIELD_MOCK,
                values: 'foo|1\r\nbar|2',
                variable: 'check'
            };
            spectator = createHost(
                `<form [formGroup]="formGroup">
                    <dot-edit-content-checkbox-field [field]="field" [contentlet]="contentlet" />
                </form>`,
                {
                    hostProps: {
                        formGroup: new FormGroup({
                            check: new FormControl('1,2')
                        }),
                        field: field,
                        contentlet: createFakeContentlet({
                            [field.variable]: '1,2'
                        })
                    }
                }
            );
            spectator.detectChanges();
            const checkboxes = spectator.queryAll(Checkbox);
            expect(checkboxes.length).toBe(2);
            expect(checkboxes[0].value).toBe('1');
            expect(checkboxes[1].value).toBe('2');
            const options = getSingleSelectableFieldOptions(field.values, field.dataType);
            expect(
                spectator.query(`label[for="${checkboxes[0].inputId}"]`)?.textContent?.trim()
            ).toBe(options[0].label);
            expect(
                spectator.query(`label[for="${checkboxes[1].inputId}"]`)?.textContent?.trim()
            ).toBe(options[1].label);
        });

        it('should render checkboxes for label-only format', () => {
            const field = {
                ...CHECKBOX_FIELD_MOCK,
                values: 'label1\r\nlabel2',
                variable: 'check'
            };
            spectator = createHost(
                `<form [formGroup]="formGroup">
                    <dot-edit-content-checkbox-field [field]="field" [contentlet]="contentlet" />
                </form>`,
                {
                    hostProps: {
                        formGroup: new FormGroup({
                            check: new FormControl('1,2')
                        }),
                        field: field,
                        contentlet: createFakeContentlet({
                            [field.variable]: '1,2'
                        })
                    }
                }
            );
            spectator.detectChanges();
            const checkboxes = spectator.queryAll(Checkbox);
            expect(checkboxes.length).toBe(2);
            expect(checkboxes[0].value).toBe('label1');
            expect(checkboxes[1].value).toBe('label2');
            const options = getSingleSelectableFieldOptions(field.values, field.dataType);
            expect(
                spectator.query(`label[for="${checkboxes[0].inputId}"]`)?.textContent?.trim()
            ).toBe(options[0].label);
            expect(
                spectator.query(`label[for="${checkboxes[1].inputId}"]`)?.textContent?.trim()
            ).toBe(options[1].label);
        });

        it('should render checkboxes for comma format', () => {
            const field = {
                ...CHECKBOX_FIELD_MOCK,
                values: '1,2,3',
                variable: 'check'
            };
            spectator = createHost(
                `<form [formGroup]="formGroup">
                    <dot-edit-content-checkbox-field [field]="field" [contentlet]="contentlet" />
                </form>`,
                {
                    hostProps: {
                        formGroup: new FormGroup({
                            check: new FormControl('1,2')
                        }),
                        field: field,
                        contentlet: createFakeContentlet({
                            [field.variable]: '1,2'
                        })
                    }
                }
            );
            spectator.detectChanges();
            const checkboxes = spectator.queryAll(Checkbox);
            expect(checkboxes.length).toBe(3);
            expect(checkboxes[0].value).toBe('1');
            expect(checkboxes[1].value).toBe('2');
            expect(checkboxes[2].value).toBe('3');
            const options = getSingleSelectableFieldOptions(field.values, field.dataType);
            expect(
                spectator.query(`label[for="${checkboxes[0].inputId}"]`)?.textContent?.trim()
            ).toBe(options[0].label);
            expect(
                spectator.query(`label[for="${checkboxes[1].inputId}"]`)?.textContent?.trim()
            ).toBe(options[1].label);
            expect(
                spectator.query(`label[for="${checkboxes[2].inputId}"]`)?.textContent?.trim()
            ).toBe(options[2].label);
        });

        it('should render checkboxes for boolean values', () => {
            const field = {
                ...CHECKBOX_FIELD_MOCK,
                values: '|true\r\n|false',
                dataType: 'BOOL',
                variable: 'check'
            };
            spectator = createHost(
                `<form [formGroup]="formGroup">
                    <dot-edit-content-checkbox-field [field]="field" [contentlet]="contentlet" />
                </form>`,
                {
                    hostProps: {
                        formGroup: new FormGroup({
                            check: new FormControl('1,2')
                        }),
                        field: field,
                        contentlet: createFakeContentlet({
                            [field.variable]: '1,2'
                        })
                    }
                }
            );
            spectator.detectChanges();
            const checkboxes = spectator.queryAll(Checkbox);
            expect(checkboxes.length).toBe(2);
            expect(checkboxes[0].value).toBe(true);
            expect(checkboxes[1].value).toBe(false);
            // Boolean options have empty labels - no label elements are rendered
            expect(spectator.query(`label[for="${checkboxes[0].inputId}"]`)).toBeFalsy();
            expect(spectator.query(`label[for="${checkboxes[1].inputId}"]`)).toBeFalsy();
        });

        it('should render checkboxes for numeric values', () => {
            const field = {
                ...CHECKBOX_FIELD_MOCK,
                values: '1,2,3',
                dataType: 'INTEGER',
                variable: 'check'
            };
            spectator = createHost(
                `<form [formGroup]="formGroup">
                    <dot-edit-content-checkbox-field [field]="field" [contentlet]="contentlet" />
                </form>`,
                {
                    hostProps: {
                        formGroup: new FormGroup({
                            check: new FormControl('1,2')
                        }),
                        field: field,
                        contentlet: createFakeContentlet({
                            [field.variable]: '1,2'
                        })
                    }
                }
            );
            spectator.detectChanges();
            const checkboxes = spectator.queryAll(Checkbox);
            expect(checkboxes.length).toBe(3);
            expect(checkboxes[0].value).toBe(1);
            expect(checkboxes[1].value).toBe(2);
            expect(checkboxes[2].value).toBe(3);
        });

        it('should render no checkboxes for empty values', () => {
            const field = {
                ...CHECKBOX_FIELD_MOCK,
                values: '',
                variable: 'check'
            };
            spectator = createHost(
                `<form [formGroup]="formGroup">
                    <dot-edit-content-checkbox-field [field]="field" [contentlet]="contentlet" />
                </form>`,
                {
                    hostProps: {
                        formGroup: new FormGroup({
                            check: new FormControl('1,2')
                        }),
                        field: field,
                        contentlet: createFakeContentlet({
                            [field.variable]: '1,2'
                        })
                    }
                }
            );
            spectator.detectChanges();
            expect(spectator.queryAll(Checkbox).length).toBe(0);
        });

        it('should render no checkboxes for null values', () => {
            const field = {
                ...CHECKBOX_FIELD_MOCK,
                values: null,
                variable: 'check'
            };
            spectator = createHost(
                `<form [formGroup]="formGroup">
                    <dot-edit-content-checkbox-field [field]="field" [contentlet]="contentlet" />
                </form>`,
                {
                    hostProps: {
                        formGroup: new FormGroup({
                            check: new FormControl('1,2')
                        }),
                        field: field,
                        contentlet: createFakeContentlet({
                            [field.variable]: '1,2'
                        })
                    }
                }
            );
            spectator.detectChanges();
            expect(spectator.queryAll(Checkbox).length).toBe(0);
        });

        it('should render no checkboxes for undefined values', () => {
            const field = {
                ...CHECKBOX_FIELD_MOCK,
                values: undefined,
                variable: 'check'
            };
            spectator = createHost(
                `<form [formGroup]="formGroup">
                    <dot-edit-content-checkbox-field [field]="field" [contentlet]="contentlet" />
                </form>`,
                {
                    hostProps: {
                        formGroup: new FormGroup({
                            check: new FormControl('1,2')
                        }),
                        field: field,
                        contentlet: createFakeContentlet({
                            [field.variable]: '1,2'
                        })
                    }
                }
            );
            spectator.detectChanges();
            expect(spectator.queryAll(Checkbox).length).toBe(0);
        });

        it('should render no checkboxes for whitespace values', () => {
            const field = {
                ...CHECKBOX_FIELD_MOCK,
                values: '   ',
                variable: 'check'
            };
            spectator = createHost(
                `<form [formGroup]="formGroup">
                    <dot-edit-content-checkbox-field [field]="field" [contentlet]="contentlet" />
                </form>`,
                {
                    hostProps: {
                        formGroup: new FormGroup({
                            check: new FormControl('1,2')
                        }),
                        field: field,
                        contentlet: createFakeContentlet({
                            [field.variable]: '1,2'
                        })
                    }
                }
            );
            spectator.detectChanges();
            expect(spectator.queryAll(Checkbox).length).toBe(0);
        });
    });
});

/**
 * T-03 — option rows use the global `.form-checkbox` (AC-109).
 *
 * `.form .form-checkbox { @apply flex flex-row items-center gap-2 }` computes identically to the
 * hand-rolled `flex items-center gap-2` it replaces — `flex-row` is the default for
 * `display: flex` — so this is a zero-delta swap on the ROW.
 *
 * It is not zero-delta on the option LABELS, and that is the point: `.form .form-checkbox label`
 * additionally applies `text-sm font-normal`, which is a DESCENDANT selector and so reaches them
 * where they already sit, with no restructuring. Option labels go 14px/400 -> 12.25px/400, which
 * is the one accepted delta that #37460's measured-impact table does not list (research R3).
 */
describe('DotEditContentCheckboxFieldComponent — option rows', () => {
    let spectator: SpectatorHost<DotEditContentCheckboxFieldComponent, MockFormComponent>;

    const createHost = createHostFactory({
        component: DotEditContentCheckboxFieldComponent,
        host: MockFormComponent,
        imports: [ReactiveFormsModule],
        detectChanges: false
    });

    beforeEach(() => {
        spectator = createHost(
            `<form [formGroup]="formGroup">
                <dot-edit-content-checkbox-field [field]="field" [contentlet]="contentlet" />
            </form>`,
            {
                hostProps: {
                    formGroup: new FormGroup({
                        [CHECKBOX_FIELD_MOCK.variable]: new FormControl(null)
                    }),
                    field: CHECKBOX_FIELD_MOCK,
                    contentlet: createFakeContentlet({ [CHECKBOX_FIELD_MOCK.variable]: null })
                }
            }
        );
        spectator.detectChanges();
    });

    it('should wrap each option in a .form-checkbox row', () => {
        expect(spectator.queryAll('.form-checkbox').length).toBeGreaterThan(0);
    });

    it('should not hand-roll the layout the global class already provides', () => {
        expect(spectator.query('.flex.items-center.gap-2')).toBeNull();
    });

    it('should not use the .form-radio variant', () => {
        expect(spectator.query('.form-radio')).toBeNull();
    });

    it('should keep each option label inside its row so the descendant rule reaches it', () => {
        const row = spectator.query('.form-checkbox');

        expect(row).toBeTruthy();
        expect(row?.querySelector('label')).toBeTruthy();
    });
});

/**
 * AC-209 — naming and requiredness for an option group.
 *
 * A set of options has no single control to carry `for` / `aria-required`: the field's value is the
 * selection, not any one input. The group itself becomes the named widget, and because a `<div>` is
 * not a labelable element, it is named with `aria-labelledby` rather than the label's `for`.
 */
describe('DotEditContentCheckboxFieldComponent — option group semantics (AC-209)', () => {
    let spectator: SpectatorHost<DotEditContentCheckboxFieldComponent, MockFormComponent>;

    const createHost = createHostFactory({
        component: DotEditContentCheckboxFieldComponent,
        host: MockFormComponent,
        imports: [ReactiveFormsModule],
        detectChanges: false
    });

    const render = (required: boolean) => {
        spectator = createHost(
            `<form [formGroup]="formGroup">
                <dot-edit-content-checkbox-field [field]="field" [contentlet]="contentlet" />
            </form>`,
            {
                hostProps: {
                    formGroup: new FormGroup({
                        [CHECKBOX_FIELD_MOCK.variable]: new FormControl(null)
                    }),
                    field: { ...CHECKBOX_FIELD_MOCK, required },
                    contentlet: createFakeContentlet({ [CHECKBOX_FIELD_MOCK.variable]: null })
                }
            }
        );
        spectator.detectChanges();
    };

    it('should expose the options as a group', () => {
        render(true);

        expect(spectator.query('[role="group"]')).toBeTruthy();
    });

    it('should name the group from the field label', () => {
        render(true);

        const group = spectator.query('[role="group"]');

        expect(group).toBeTruthy();
        expect(group?.getAttribute('aria-labelledby')).toBe(
            'label-' + CHECKBOX_FIELD_MOCK.variable
        );
        expect(spectator.query('label[dotCardFieldLabel]')?.id).toBe(
            'label-' + CHECKBOX_FIELD_MOCK.variable
        );
    });

    // Deliberately no aria-required assertion: ARIA defines that attribute on radiogroup but NOT
    // on the plain `group` role a checkbox set uses, so asserting it would enshrine invalid ARIA.
    it('should not put aria-required on a plain group, which ARIA does not define it on', () => {
        render(true);

        // toBeTruthy first: with `?.` alone a missing group would satisfy toBeNull vacuously.
        const group = spectator.query('[role="group"]');

        expect(group).toBeTruthy();
        expect(group?.getAttribute('aria-required')).toBeNull();
    });
});
