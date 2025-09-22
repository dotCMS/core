import { SpectatorHost, createHostFactory, byTestId } from '@ngneat/spectator';

import { Component } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';

import { InputTextModule } from 'primeng/inputtext';

import { DotCMSContentTypeField } from '@dotcms/dotcms-models';

import { DotEditContentTextFieldComponent } from './dot-edit-content-text-field.component';
import { INPUT_TEXT_OPTIONS, INPUT_TYPE } from './utils';

import { TEXT_FIELD_MOCK } from '../../utils/mocks';

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
                <dot-edit-content-text-field [field]="field" [formControlName]="field.variable" />
            </form>`,
            {
                hostProps: {
                    formGroup: new FormGroup({
                        [TEXT_FIELD_MOCK.variable]: new FormControl('one')
                    }),
                    field: TEXT_FIELD_MOCK
                }
            }
        );
        spectator.detectChanges();
        textInput = spectator.query(byTestId(TEXT_FIELD_MOCK.variable));
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
                    <dot-edit-content-text-field [field]="field" [formControlName]="field.variable" />
                </form>`,
                {
                    hostProps: {
                        formGroup: new FormGroup({
                            [TEXT_FIELD_MOCK.variable]: new FormControl()
                        }),
                        field: {
                            ...TEXT_FIELD_MOCK,
                            dataType
                        }
                    }
                }
            );
            spectator.detectChanges();

            textInput = spectator.query(byTestId(TEXT_FIELD_MOCK.variable));
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
});
