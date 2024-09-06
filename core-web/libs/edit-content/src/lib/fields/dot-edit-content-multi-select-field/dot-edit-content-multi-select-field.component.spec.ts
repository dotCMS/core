import { Spectator } from '@ngneat/spectator';
import { createComponentFactory } from '@ngneat/spectator/jest';

import { ControlContainer, FormControl, FormGroup, FormGroupDirective } from '@angular/forms';

import { MultiSelect, MultiSelectItem } from 'primeng/multiselect';

import { DotEditContentMultiSelectFieldComponent } from './dot-edit-content-multi-select-field.component';

import { MULTI_SELECT_FIELD_MOCK, createFormGroupDirectiveMock } from '../../utils/mocks';

// https://jestjs.io/docs/manual-mocks#mocking-methods-which-are-not-implemented-in-jsdom
Object.defineProperty(window, 'matchMedia', {
    writable: true,
    value: jest.fn().mockImplementation((query) => ({
        matches: false,
        media: query,
        onchange: null,
        addListener: jest.fn(), // deprecated
        removeListener: jest.fn(), // deprecated
        addEventListener: jest.fn(),
        removeEventListener: jest.fn(),
        dispatchEvent: jest.fn()
    }))
});

describe('DotEditContentMultiselectFieldComponent', () => {
    describe('test with value', () => {
        let spectator: Spectator<DotEditContentMultiSelectFieldComponent>;

        const FAKE_FORM_GROUP = new FormGroup({
            multiSelect: new FormControl(['one', 'two'])
        });

        const createComponent = createComponentFactory({
            component: DotEditContentMultiSelectFieldComponent,
            componentViewProviders: [
                {
                    provide: ControlContainer,
                    useValue: createFormGroupDirectiveMock(FAKE_FORM_GROUP)
                }
            ],
            providers: [FormGroupDirective],
            imports: [],
            detectChanges: false
        });

        beforeEach(() => {
            spectator = createComponent();
        });

        it('should render the options selected if the form have value', () => {
            spectator.setInput('field', MULTI_SELECT_FIELD_MOCK);
            spectator.detectComponentChanges();

            spectator.query(MultiSelect).show();
            spectator.detectChanges();

            const options = spectator.component.$options();

            spectator.queryAll('.p-multiselect-item').forEach((item, index) => {
                expect(item.textContent).toBe(options[index].label);
            });
        });
    });

    describe('test without value', () => {
        let spectator: Spectator<DotEditContentMultiSelectFieldComponent>;

        const createComponent = createComponentFactory({
            component: DotEditContentMultiSelectFieldComponent,
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

        it('should render no options selected', () => {
            spectator.setInput('field', MULTI_SELECT_FIELD_MOCK);
            spectator.detectComponentChanges();

            expect(spectator.query(MultiSelect).valuesAsString).toEqual(undefined);
        });

        it('should render options', () => {
            spectator.setInput('field', MULTI_SELECT_FIELD_MOCK);
            spectator.query(MultiSelect).show();
            spectator.detectComponentChanges();

            const multiSelectItems = spectator.queryAll(MultiSelectItem);
            expect(multiSelectItems.length).toBe(2);
            expect(multiSelectItems[0].label).toBe('one');
        });

        it('should set the key/value the same when bad formatting options passed', () => {
            const MULTI_SELECT_FIELD_MOCK_WITHOUT_VALUE_AND_LABEL = {
                ...MULTI_SELECT_FIELD_MOCK,
                values: 'one'
            };
            spectator.setInput('field', MULTI_SELECT_FIELD_MOCK_WITHOUT_VALUE_AND_LABEL);
            spectator.detectComponentChanges();

            const expectedList = [
                {
                    label: 'one',
                    value: 'one'
                }
            ];
            expect(spectator.query(MultiSelect).options).toEqual(expectedList);
        });
    });
});
