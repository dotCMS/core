import { SpectatorHost, createHostFactory } from '@ngneat/spectator';

import { Component } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';

import { Dropdown } from 'primeng/dropdown';

import { DotCMSContentTypeField } from '@dotcms/dotcms-models';

import { DotEditContentSelectFieldComponent } from './dot-edit-content-select-field.component';

import {
    SELECT_FIELD_BOOLEAN_MOCK,
    SELECT_FIELD_TEXT_MOCK,
    SELECT_FIELD_INTEGER_MOCK,
    SELECT_FIELD_FLOAT_MOCK
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
}

describe('DotEditContentSelectFieldComponent', () => {
    let spectator: SpectatorHost<DotEditContentSelectFieldComponent, MockFormComponent>;

    const createHost = createHostFactory({
        component: DotEditContentSelectFieldComponent,
        host: MockFormComponent,
        imports: [ReactiveFormsModule],
        detectChanges: false
    });

    it('should set the key/value the same when bad formatting options passed', () => {
        const SELECT_FIELD_INTEGER_MOCK_WITHOUT_VALUE_AND_LABEL = {
            ...SELECT_FIELD_INTEGER_MOCK,
            values: '1000'
        };

        spectator = createHost(
            `<form [formGroup]="formGroup">
                <dot-edit-content-select-field [field]="field" [formControlName]="field.variable" />
            </form>`,
            {
                hostProps: {
                    formGroup: new FormGroup({
                        [SELECT_FIELD_INTEGER_MOCK_WITHOUT_VALUE_AND_LABEL.variable]:
                            new FormControl()
                    }),
                    field: SELECT_FIELD_INTEGER_MOCK_WITHOUT_VALUE_AND_LABEL
                }
            }
        );
        spectator.detectChanges();

        const expectedList = [
            {
                label: '1000',
                value: 1000
            }
        ];
        expect(spectator.query(Dropdown).options).toEqual(expectedList);
    });

    describe('test DataType', () => {
        it('should have options array as select with Text', () => {
            const expectedList = [
                {
                    label: 'Option 1',
                    value: 'Test,1'
                },
                {
                    label: 'Option 2',
                    value: '2'
                },
                {
                    label: 'Option 3',
                    value: '3'
                },
                {
                    label: '123-ad',
                    value: '123-ad'
                },
                {
                    label: 'rules and weird code',
                    value: 'rules and weird code'
                }
            ];

            spectator = createHost(
                `<form [formGroup]="formGroup">
                    <dot-edit-content-select-field [field]="field" [formControlName]="field.variable" />
                </form>`,
                {
                    hostProps: {
                        formGroup: new FormGroup({
                            [SELECT_FIELD_TEXT_MOCK.variable]: new FormControl()
                        }),
                        field: SELECT_FIELD_TEXT_MOCK
                    }
                }
            );
            spectator.detectChanges();
            expect(spectator.query(Dropdown).options).toEqual(expectedList);
        });

        it('should have options array as select with Bool', () => {
            const expectedList = [
                {
                    label: 'Truthy',
                    value: true
                },
                {
                    label: 'Falsy',
                    value: false
                }
            ];
            spectator = createHost(
                `<form [formGroup]="formGroup">
                    <dot-edit-content-select-field [field]="field" [formControlName]="field.variable" />
                </form>`,
                {
                    hostProps: {
                        formGroup: new FormGroup({
                            [SELECT_FIELD_BOOLEAN_MOCK.variable]: new FormControl()
                        }),
                        field: SELECT_FIELD_BOOLEAN_MOCK
                    }
                }
            );
            spectator.detectChanges();
            expect(spectator.query(Dropdown).options).toEqual(expectedList);
        });

        it('should have options array as select with Float', () => {
            const expectedList = [
                {
                    label: 'One hundred point five',
                    value: 100.5
                },
                {
                    label: 'Three point five',
                    value: 10.3
                }
            ];
            spectator = createHost(
                `<form [formGroup]="formGroup">
                    <dot-edit-content-select-field [field]="field" [formControlName]="field.variable" />
                </form>`,
                {
                    hostProps: {
                        formGroup: new FormGroup({
                            [SELECT_FIELD_FLOAT_MOCK.variable]: new FormControl()
                        }),
                        field: SELECT_FIELD_FLOAT_MOCK
                    }
                }
            );
            spectator.detectChanges();
            expect(spectator.query(Dropdown).options).toEqual(expectedList);
        });

        it('should have options array as select with Integer', () => {
            const expectedList = [
                {
                    label: 'One hundred',
                    value: 100
                },
                {
                    label: 'One thousand',
                    value: 1000
                },
                {
                    label: 'Ten thousand',
                    value: 10000
                }
            ];
            spectator = createHost(
                `<form [formGroup]="formGroup">
                    <dot-edit-content-select-field [field]="field" [formControlName]="field.variable" />
                </form>`,
                {
                    hostProps: {
                        formGroup: new FormGroup({
                            [SELECT_FIELD_INTEGER_MOCK.variable]: new FormControl()
                        }),
                        field: SELECT_FIELD_INTEGER_MOCK
                    }
                }
            );
            spectator.detectChanges();
            expect(spectator.query(Dropdown).options).toEqual(expectedList);
        });
    });
});
