import { createComponentFactory, Spectator } from '@ngneat/spectator/jest';

import { FormsModule } from '@angular/forms';

import { MultiSelect, MultiSelectModule } from 'primeng/multiselect';

import { DotMessageService } from '@dotcms/data-access';
import { DotExperimentStatus, ExperimentsStatusList } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';
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
        imports: [MultiSelectModule, FormsModule, DotMessagePipe, DotDropdownDirective],
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
        const selectedItems = [DotExperimentStatus.DRAFT, DotExperimentStatus.ENDED];

        spectator.setInput({
            options: selectOptions,
            selectedItems
        });

        expect(spectator.component.options).toEqual(spectator.query(MultiSelect).options);
    });
});
