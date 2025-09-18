import { SpectatorHost, createHostFactory } from '@ngneat/spectator/jest';

import { Component } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';

import { Checkbox } from 'primeng/checkbox';

import { DotCMSContentTypeField } from '@dotcms/dotcms-models';

import { DotEditContentCheckboxFieldComponent } from './dot-edit-content-checkbox-field.component';

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
                    <dot-edit-content-checkbox-field [field]="field" [formControlName]="field.variable" />
                </form>`,
                {
                    hostProps: {
                        formGroup: new FormGroup({
                            [CHECKBOX_FIELD_MOCK.variable]: new FormControl(['one', 'two'])
                        }),
                        field: CHECKBOX_FIELD_MOCK
                    }
                }
            );
            spectator.detectChanges();

            const checkboxChecked = spectator
                .queryAll(Checkbox)
                .filter((checkbox) => checkbox.checked());
            expect(checkboxChecked.length).toBe(2);
        });
    });

    describe('test without value', () => {
        beforeEach(() => {
            spectator = createHost(
                `<form [formGroup]="formGroup">
                    <dot-edit-content-checkbox-field [field]="field" [formControlName]="field.variable" />
                </form>`,
                {
                    hostProps: {
                        formGroup: new FormGroup({
                            [CHECKBOX_FIELD_MOCK.variable]: new FormControl()
                        }),
                        field: CHECKBOX_FIELD_MOCK
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
                .filter((checkbox) => checkbox.checked());
            expect(checkboxChecked.length).toBe(0);
        });

        it('should have label with for atritbute and text equal to checkbox options', () => {
            spectator.detectComponentChanges();

            spectator.queryAll(Checkbox).forEach((checkbox) => {
                const selector = `label[for="${checkbox.inputId}"]`;
                expect(spectator.query(selector)).toBeTruthy();
                expect(spectator.query(selector).textContent).toEqual(` ${checkbox.label}`);
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
                <dot-edit-content-checkbox-field [field]="field" [formControlName]="field.variable" />
            </form>`,
            {
                hostProps: {
                    formGroup: new FormGroup({
                        [CHECKBOX_FIELD_MOCK_WITHOUT_VALUE_AND_LABEL.variable]: new FormControl()
                    }),
                    field: CHECKBOX_FIELD_MOCK_WITHOUT_VALUE_AND_LABEL
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
                    <dot-edit-content-checkbox-field [field]="field" [formControlName]="field.variable" />
                </form>`,
                {
                    hostProps: {
                        formGroup: new FormGroup({
                            check: new FormControl('1,2')
                        }),
                        field: field
                    }
                }
            );
            spectator.detectChanges();
            const checkboxes = spectator.queryAll(Checkbox);
            expect(checkboxes.length).toBe(2);
            expect(checkboxes[0].label).toBe('foo');
            expect(checkboxes[0].value).toBe('1');
            expect(checkboxes[1].label).toBe('bar');
            expect(checkboxes[1].value).toBe('2');
        });

        it('should render checkboxes for label-only format', () => {
            const field = {
                ...CHECKBOX_FIELD_MOCK,
                values: 'label1\r\nlabel2',
                variable: 'check'
            };
            spectator = createHost(
                `<form [formGroup]="formGroup">
                    <dot-edit-content-checkbox-field [field]="field" [formControlName]="field.variable" />
                </form>`,
                {
                    hostProps: {
                        formGroup: new FormGroup({
                            check: new FormControl('1,2')
                        }),
                        field: field
                    }
                }
            );
            spectator.detectChanges();
            const checkboxes = spectator.queryAll(Checkbox);
            expect(checkboxes.length).toBe(2);
            expect(checkboxes[0].label).toBe('label1');
            expect(checkboxes[0].value).toBe('label1');
            expect(checkboxes[1].label).toBe('label2');
            expect(checkboxes[1].value).toBe('label2');
        });

        it('should render checkboxes for comma format', () => {
            const field = {
                ...CHECKBOX_FIELD_MOCK,
                values: '1,2,3',
                variable: 'check'
            };
            spectator = createHost(
                `<form [formGroup]="formGroup">
                    <dot-edit-content-checkbox-field [field]="field" [formControlName]="field.variable" />
                </form>`,
                {
                    hostProps: {
                        formGroup: new FormGroup({
                            check: new FormControl('1,2')
                        }),
                        field: field
                    }
                }
            );
            spectator.detectChanges();
            const checkboxes = spectator.queryAll(Checkbox);
            expect(checkboxes.length).toBe(3);
            expect(checkboxes[0].label).toBe('1');
            expect(checkboxes[0].value).toBe('1');
            expect(checkboxes[1].label).toBe('2');
            expect(checkboxes[1].value).toBe('2');
            expect(checkboxes[2].label).toBe('3');
            expect(checkboxes[2].value).toBe('3');
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
                    <dot-edit-content-checkbox-field [field]="field" [formControlName]="field.variable" />
                </form>`,
                {
                    hostProps: {
                        formGroup: new FormGroup({
                            check: new FormControl('1,2')
                        }),
                        field: field
                    }
                }
            );
            spectator.detectChanges();
            const checkboxes = spectator.queryAll(Checkbox);
            expect(checkboxes.length).toBe(2);
            expect(checkboxes[0].label).toBe(null);
            expect(checkboxes[0].value).toBe(true);
            expect(checkboxes[1].label).toBe(null);
            expect(checkboxes[1].value).toBe(false);
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
                    <dot-edit-content-checkbox-field [field]="field" [formControlName]="field.variable" />
                </form>`,
                {
                    hostProps: {
                        formGroup: new FormGroup({
                            check: new FormControl('1,2')
                        }),
                        field: field
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
                    <dot-edit-content-checkbox-field [field]="field" [formControlName]="field.variable" />
                </form>`,
                {
                    hostProps: {
                        formGroup: new FormGroup({
                            check: new FormControl('1,2')
                        }),
                        field: field
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
                    <dot-edit-content-checkbox-field [field]="field" [formControlName]="field.variable" />
                </form>`,
                {
                    hostProps: {
                        formGroup: new FormGroup({
                            check: new FormControl('1,2')
                        }),
                        field: field
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
                    <dot-edit-content-checkbox-field [field]="field" [formControlName]="field.variable" />
                </form>`,
                {
                    hostProps: {
                        formGroup: new FormGroup({
                            check: new FormControl('1,2')
                        }),
                        field: field
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
                    <dot-edit-content-checkbox-field [field]="field" [formControlName]="field.variable" />
                </form>`,
                {
                    hostProps: {
                        formGroup: new FormGroup({
                            check: new FormControl('1,2')
                        }),
                        field: field
                    }
                }
            );
            spectator.detectChanges();
            expect(spectator.queryAll(Checkbox).length).toBe(0);
        });
    });
});
