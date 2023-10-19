import { test, describe } from '@jest/globals';
import { Spectator, byTestId, createComponentFactory } from '@ngneat/spectator';

import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, ControlContainer, FormGroupDirective } from '@angular/forms';

import { InputTextModule } from 'primeng/inputtext';

import { DotFieldRequiredDirective } from '@dotcms/ui';

import { DotEditContentTextFieldComponent } from './dot-edit-content-text-field.component';
import { INPUT_TEXT_OPTIONS, INPUT_TYPE } from './utils';

import { TEXT_FIELD_MOCK, createFormGroupDirectiveMock } from '../../utils/mocks';

describe('DotEditContentTextFieldComponent', () => {
    let spectator: Spectator<DotEditContentTextFieldComponent>;
    let textInput: Element;

    const createComponent = createComponentFactory({
        component: DotEditContentTextFieldComponent,
        imports: [CommonModule, ReactiveFormsModule, InputTextModule, DotFieldRequiredDirective],
        componentViewProviders: [
            {
                provide: ControlContainer,
                useValue: createFormGroupDirectiveMock()
            }
        ],
        providers: [FormGroupDirective]
    });
    beforeEach(() => {
        spectator = createComponent({
            props: {
                field: TEXT_FIELD_MOCK
            }
        });

        textInput = spectator.query(byTestId(TEXT_FIELD_MOCK.variable));
    });

    test.each([
        {
            variable: TEXT_FIELD_MOCK.variable,
            attribute: 'id'
        },
        {
            variable: TEXT_FIELD_MOCK.variable,
            attribute: 'ng-reflect-name'
        }
    ])('should have the $variable as $attribute', ({ variable, attribute }) => {
        expect(textInput.getAttribute(attribute)).toBe(variable);
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
            spectator = createComponent({
                props: {
                    field: { ...TEXT_FIELD_MOCK, dataType }
                }
            });

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
