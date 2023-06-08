import { createComponentFactory, Spectator } from '@ngneat/spectator';

import { FormsModule } from '@angular/forms';

import { MultiSelect, MultiSelectModule } from 'primeng/multiselect';

import { DotMessageService } from '@dotcms/data-access';
import { DotExperimentStatusList, ExperimentsStatusList } from '@dotcms/dotcms-models';
import { DotMessagePipeModule } from '@dotcms/ui';
import { MockDotMessageService } from '@dotcms/utils-testing';
import { DotDropdownDirective } from '@portlets/shared/directives/dot-dropdown.directive';

import { DotExperimentsStatusFilterComponent } from './dot-experiments-status-filter.component';

const messageServiceMock = new MockDotMessageService({
    'experimentspage.experiment.status.placeholder': 'Select one Filter'
});

const selectOptions = ExperimentsStatusList;
describe('DotExperimentsStatusFilterComponent', () => {
    let spectator: Spectator<DotExperimentsStatusFilterComponent>;

    const createComponent = createComponentFactory({
        imports: [MultiSelectModule, FormsModule, DotMessagePipeModule, DotDropdownDirective],
        component: DotExperimentsStatusFilterComponent,
        providers: [
            {
                provide: DotMessageService,
                useValue: messageServiceMock
            }
        ]
    });

    beforeEach(() => {
        spectator = createComponent();
    });

    it('should get a list of options', () => {
        spectator.setInput('options', selectOptions);

        expect(spectator.query(MultiSelect).options).toEqual(selectOptions);
    });

    it('should get a list of selected options', () => {
        const selectedItems = [DotExperimentStatusList.DRAFT, DotExperimentStatusList.ENDED];

        spectator.setInput({
            options: selectOptions,
            selectedItems
        });

        expect(spectator.component.options).toEqual(spectator.query(MultiSelect).options);
    });
});
