import { Spectator } from '@ngneat/spectator';
import { createComponentFactory } from '@ngneat/spectator/jest';

import { ControlContainer, FormControl, FormGroup, FormGroupDirective } from '@angular/forms';

import { Checkbox } from 'primeng/checkbox';

import { DotEditContentCheckboxFieldComponent } from './dot-edit-content-checkbox-field.component';

import { CHECKBOX_FIELD_MOCK, createFormGroupDirectiveMock } from '../../utils/mocks';

describe('DotEditContentCheckboxFieldComponent', () => {
    describe('test with value', () => {
        let spectator: Spectator<DotEditContentCheckboxFieldComponent>;

        const FAKE_FORM_GROUP = new FormGroup({
            check: new FormControl(['one', 'two'])
        });

        const createComponent = createComponentFactory({
            component: DotEditContentCheckboxFieldComponent,
            componentViewProviders: [
                {
                    provide: ControlContainer,
                    useValue: createFormGroupDirectiveMock(FAKE_FORM_GROUP)
                }
            ],
            providers: [FormGroupDirective],
            detectChanges: false
        });

        beforeEach(() => {
            spectator = createComponent();
        });

        it('should render a checbox selected if the form have value', () => {
            spectator.setInput('field', CHECKBOX_FIELD_MOCK);
            spectator.detectComponentChanges();

            const checkboxChecked = spectator
                .queryAll(Checkbox)
                .filter((checkbox) => checkbox.checked());
            expect(checkboxChecked.length).toBe(2);
        });
    });

    describe('test without value', () => {
        let spectator: Spectator<DotEditContentCheckboxFieldComponent>;

        const createComponent = createComponentFactory({
            component: DotEditContentCheckboxFieldComponent,
            componentViewProviders: [
                {
                    provide: ControlContainer,
                    useValue: createFormGroupDirectiveMock()
                }
            ],
            providers: [FormGroupDirective],
            detectChanges: false
        });

        beforeEach(() => {
            spectator = createComponent();
        });

        it('should render a checkbox list', () => {
            spectator.setInput('field', CHECKBOX_FIELD_MOCK);
            spectator.detectComponentChanges();

            expect(spectator.queryAll(Checkbox).length).toBe(2);
        });

        it('should dont have any checkbox checked if the form value or defaultValue is null', () => {
            spectator.setInput('field', CHECKBOX_FIELD_MOCK);
            spectator.detectComponentChanges();

            const checkboxChecked = spectator
                .queryAll(Checkbox)
                .filter((checkbox) => checkbox.checked());
            expect(checkboxChecked.length).toBe(0);
        });

        it('should set the key/value the same when bad formattings options passed', () => {
            const CHECKBOX_FIELD_MOCK_WITHOUT_VALUE_AND_LABEL = {
                ...CHECKBOX_FIELD_MOCK,
                values: 'one\r\ntwo'
            };
            spectator.setInput('field', CHECKBOX_FIELD_MOCK_WITHOUT_VALUE_AND_LABEL);
            spectator.detectComponentChanges();

            expect(spectator.queryAll(Checkbox).map((checkbox) => checkbox.value)).toEqual([
                'one',
                'two'
            ]);
        });

        it('should have label with for atritbute and text equal to checkbox options', () => {
            spectator.setInput('field', CHECKBOX_FIELD_MOCK);
            spectator.detectComponentChanges();

            spectator.queryAll(Checkbox).forEach((checkbox) => {
                const selector = `label[for="${checkbox.inputId}"]`;
                expect(spectator.query(selector)).toBeTruthy();
                expect(spectator.query(selector).textContent).toEqual(` ${checkbox.label}`);
            });
        });
    });
});
