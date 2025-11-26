import { SpectatorHost, createHostFactory } from '@ngneat/spectator';

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
                expect(spectator.query(`label[for="${radio.inputId}"]`)).toBeTruthy();
                expect(spectator.query(`label[for="${radio.inputId}"]`).textContent).toEqual(
                    radio.label
                );
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
