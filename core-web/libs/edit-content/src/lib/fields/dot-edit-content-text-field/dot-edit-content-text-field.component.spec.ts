import { SpectatorHost, createHostFactory, byTestId } from '@ngneat/spectator';

import { Component } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';

import { InputTextModule } from 'primeng/inputtext';

import { DotCMSContentlet, DotCMSContentTypeField } from '@dotcms/dotcms-models';
import { createFakeTextField, createFakeContentlet } from '@dotcms/utils-testing';

import { DotEditContentTextFieldComponent } from './dot-edit-content-text-field.component';
import { INPUT_TEXT_OPTIONS, INPUT_TYPE } from './utils';

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
