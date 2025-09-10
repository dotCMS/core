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

    describe('test with value (string, pipe, comma, boolean, numeric)', () => {
        let spectator: Spectator<DotEditContentCheckboxFieldComponent>;

        const createComponent = createComponentFactory({
            component: DotEditContentCheckboxFieldComponent,
            componentViewProviders: [
                {
                    provide: ControlContainer,
                    useValue: createFormGroupDirectiveMock(
                        new FormGroup({ check: new FormControl('1,2') })
                    )
                }
            ],
            providers: [FormGroupDirective],
            detectChanges: false
        });

        beforeEach(() => {
            spectator = createComponent();
        });

        it('should render checkboxes for pipe format', () => {
            const field = {
                ...CHECKBOX_FIELD_MOCK,
                values: 'foo|1\r\nbar|2',
                variable: 'check'
            };
            spectator.setInput('field', field);
            spectator.detectComponentChanges();
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
            spectator.setInput('field', field);
            spectator.detectComponentChanges();
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
            spectator.setInput('field', field);
            spectator.detectComponentChanges();
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
            spectator.setInput('field', field);
            spectator.detectComponentChanges();
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
            spectator.setInput('field', field);
            spectator.detectComponentChanges();
            const checkboxes = spectator.queryAll(Checkbox);
            expect(checkboxes.length).toBe(3);
            expect(checkboxes[0].value).toBe(1);
            expect(checkboxes[1].value).toBe(2);
            expect(checkboxes[2].value).toBe(3);
        });

        it('should render no checkboxes for empty, null or whitespace values', () => {
            const cases = ['', null, undefined, '   '];
            for (const val of cases) {
                const field = {
                    ...CHECKBOX_FIELD_MOCK,
                    values: val,
                    variable: 'check'
                };
                spectator.setInput('field', field);
                spectator.detectComponentChanges();
                expect(spectator.queryAll(Checkbox).length).toBe(0);
            }
        });
    });
});
