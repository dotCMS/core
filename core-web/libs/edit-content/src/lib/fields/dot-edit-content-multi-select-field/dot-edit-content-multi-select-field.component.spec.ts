import { Spectator } from '@ngneat/spectator';
import { createComponentFactory } from '@ngneat/spectator/jest';

import { ControlContainer, FormControl, FormGroup, FormGroupDirective } from '@angular/forms';

import { MultiSelect, MultiSelectItem } from 'primeng/multiselect';

import { DotEditContentMultiSelectFieldComponent } from './dot-edit-content-multi-select-field.component';

import { MULTI_SELECT_FIELD_MOCK, createFormGroupDirectiveMock } from '../../utils/mocks';

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
            detectChanges: false
        });

        beforeEach(() => {
            spectator = createComponent();
        });

        it('should render a options selected if the form have value', () => {
            spectator.setInput('field', MULTI_SELECT_FIELD_MOCK);
            spectator.detectComponentChanges();

            expect(spectator.query(MultiSelect).valuesAsString).toEqual('one, two');
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
            //Working on this test.
            spectator.setInput('field', MULTI_SELECT_FIELD_MOCK);
            spectator.detectComponentChanges();
            spectator.query(MultiSelect).show();

            const multiSelectItems = spectator.queryAll(MultiSelectItem);
            expect(multiSelectItems.length).toBe(2);
        });
    });
});
