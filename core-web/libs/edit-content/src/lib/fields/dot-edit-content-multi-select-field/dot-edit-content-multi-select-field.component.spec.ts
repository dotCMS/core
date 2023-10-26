import { Spectator } from '@ngneat/spectator';
import { createComponentFactory } from '@ngneat/spectator/jest';

import { ControlContainer, FormControl, FormGroup, FormGroupDirective } from '@angular/forms';

import { MultiSelect, MultiSelectItem, MultiSelectModule } from 'primeng/multiselect';

import { DotEditContentMultiSelectFieldComponent } from './dot-edit-content-multi-select-field.component';

import { createFormGroupDirectiveMock, MULTI_SELECT_FIELD_MOCK } from '../../utils/mocks';

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
            providers: [FormGroupDirective]
        });

        beforeEach(() => {
            spectator = createComponent({ detectChanges: false });
        });

        it('should render a options selected if the form have value', () => {
            spectator.setInput('field', MULTI_SELECT_FIELD_MOCK);
            spectator.detectChanges();
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
            imports: [MultiSelectModule],
            providers: [FormGroupDirective]
        });

        // Mock de matchMedia
        // @ts-ignore
        window.matchMedia =
            window.matchMedia ||
            function () {
                return {
                    matches: false
                };
            };

        beforeEach(() => {
            spectator = createComponent({
                detectChanges: false
            });
        });

        it('should render no options selected', () => {
            spectator.setInput('field', MULTI_SELECT_FIELD_MOCK);
            spectator.detectChanges();

            expect(spectator.query(MultiSelect).valuesAsString).toEqual(undefined);
        });

        it('should render options', () => {
            spectator.setInput('field', MULTI_SELECT_FIELD_MOCK);
            spectator.detectChanges();

            spectator.query(MultiSelect).show();
            spectator.detectComponentChanges();

            expect(spectator.queryAll(MultiSelectItem).length).toBe(2);
        });
    });
});
