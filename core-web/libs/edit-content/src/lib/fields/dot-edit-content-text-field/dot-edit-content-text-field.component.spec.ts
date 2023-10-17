import { test, describe } from '@jest/globals';
import { Spectator, byTestId, createComponentFactory } from '@ngneat/spectator';

import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, ControlContainer, FormGroupDirective } from '@angular/forms';

import { InputTextModule } from 'primeng/inputtext';

import { DotFieldRequiredDirective } from '@dotcms/ui';

import { DotEditContentTextFieldComponent } from './dot-edit-content-text-field.component';

import { FIELD_MOCK, createFormGroupDirectiveMock } from '../../utils/mocks';
import { INPUT_TYPE } from '../models';

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
                field: FIELD_MOCK
            }
        });

        textInput = spectator.query(byTestId('input-' + FIELD_MOCK.variable));
    });

    it('should render', () => {
        expect(textInput).toBeTruthy();
    });

    test.each([
        {
            variable: FIELD_MOCK.variable,
            attribute: 'id'
        },
        {
            variable: FIELD_MOCK.variable,
            attribute: 'ng-reflect-name'
        }
    ])('should have the $variable as $attribute', ({ variable, attribute }) => {
        expect(textInput.getAttribute(attribute)).toBe(variable);
    });

    describe.each([
        {
            dataType: INPUT_TYPE.TEXT,
            options: {
                type: 'text',
                inputMode: 'text'
            }
        },
        {
            dataType: INPUT_TYPE.INTEGER,
            options: {
                type: 'number',
                inputMode: 'numeric',
                step: 1
            }
        },
        {
            dataType: INPUT_TYPE.FLOAT,
            options: {
                type: 'number',
                inputMode: 'decimal',
                step: 0.1
            }
        }
    ])('with dataType as $dataType', ({ dataType, options }) => {
        beforeEach(() => {
            spectator = createComponent({
                props: {
                    field: { ...FIELD_MOCK, dataType }
                }
            });

            textInput = spectator.query(byTestId('input-' + FIELD_MOCK.variable));
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
