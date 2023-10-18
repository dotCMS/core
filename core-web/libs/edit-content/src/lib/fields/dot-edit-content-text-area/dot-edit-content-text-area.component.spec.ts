import { expect, it, test } from '@jest/globals';
import { Spectator, byTestId, createComponentFactory } from '@ngneat/spectator';

import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, ControlContainer, FormGroupDirective } from '@angular/forms';

import { InputTextareaModule } from 'primeng/inputtextarea';

import { DotFieldRequiredDirective } from '@dotcms/ui';

import { DotEditContentTextAreaComponent } from './dot-edit-content-text-area.component';

import { createFormGroupDirectiveMock, FIELD_MOCK } from '../../utils/mocks';

describe('DotEditContentTextAreaComponent', () => {
    let spectator: Spectator<DotEditContentTextAreaComponent>;
    let textArea: Element;

    const createComponent = createComponentFactory({
        component: DotEditContentTextAreaComponent,
        imports: [
            CommonModule,
            ReactiveFormsModule,
            InputTextareaModule,
            DotFieldRequiredDirective
        ],
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

        textArea = spectator.query(byTestId(FIELD_MOCK.variable));
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
        expect(textArea.getAttribute(attribute)).toBe(variable);
    });

    it('should have min height as 18.75rem and resize as vertical', () => {
        expect(textArea.getAttribute('style')).toBe('min-height: 18.75rem; resize: vertical;');
    });
});
