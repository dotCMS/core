import { Spectator } from '@ngneat/spectator';
import { createComponentFactory } from '@ngneat/spectator/jest';

import { ControlContainer, FormControl, FormGroup, FormGroupDirective } from '@angular/forms';

import { MultiSelect, MultiSelectModule } from 'primeng/multiselect';

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

        // Only test if Multiselect has the options, but not if they are rendered
        // https://github.com/primefaces/primeng/blob/e3f717d67600186c1e0b419e764c682668ab3313/src/app/components/multiselect/multiselect.spec.ts#L159

        // it('should render options', () => {
        //     spectator.setInput('field', MULTI_SELECT_FIELD_MOCK);
        //     spectator.detectChanges();
        //
        //     spectator.query(MultiSelect).show();
        //     spectator.detectComponentChanges();
        //
        //     expect(spectator.queryAll(MultiSelectItem).length).toBe(2);
        // });
    });
});
