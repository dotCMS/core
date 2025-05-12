import { test, describe } from '@jest/globals';
import { Spectator, byTestId, createComponentFactory } from '@ngneat/spectator';

import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, ControlContainer, FormGroupDirective } from '@angular/forms';

import { InputTextModule } from 'primeng/inputtext';

import { DotCMSDataTypes } from '@dotcms/dotcms-models';
import { DotFieldRequiredDirective } from '@dotcms/ui';

import { DotEditContentTextFieldComponent } from './dot-edit-content-text-field.component';
import { INPUT_TEXT_OPTIONS } from './utils';

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
            } as unknown
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
            dataType: DotCMSDataTypes.TEXT
        },
        {
            dataType: DotCMSDataTypes.INTEGER
        },
        {
            dataType: DotCMSDataTypes.FLOAT
        }
    ])('with dataType as $dataType', ({ dataType }) => {
        const options = INPUT_TEXT_OPTIONS[dataType];

        beforeEach(() => {
            spectator = createComponent({
                props: {
                    field: { ...TEXT_FIELD_MOCK, dataType }
                } as unknown
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
