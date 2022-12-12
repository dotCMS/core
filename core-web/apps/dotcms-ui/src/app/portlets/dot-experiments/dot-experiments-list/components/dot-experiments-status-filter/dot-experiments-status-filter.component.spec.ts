import { DotExperimentsStatusFilterComponent } from './dot-experiments-status-filter.component';
import { createComponentFactory, Spectator } from '@ngneat/spectator';

import { MultiSelect, MultiSelectModule } from 'primeng/multiselect';
import { DotMessageService } from '@dotcms/data-access';
import { FormsModule } from '@angular/forms';
import { ExperimentsStatusList, DotExperimentStatusList } from '@dotcms/dotcms-models';
import { DotMessagePipeModule } from '@pipes/dot-message/dot-message-pipe.module';
import { MockDotMessageService } from '@dotcms/utils-testing';

const messageServiceMock = new MockDotMessageService({
    'experimentspage.experiment.status.placeholder': 'Select one Filter'
});

const selectOptions = ExperimentsStatusList;
describe('DotExperimentsStatusFilterComponent', () => {
    let spectator: Spectator<DotExperimentsStatusFilterComponent>;
    let multiSelect: MultiSelect;

    const createComponent = createComponentFactory({
        imports: [MultiSelectModule, FormsModule, DotMessagePipeModule],
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

        multiSelect = spectator.query(MultiSelect);
        expect(spectator.component.options).toBe(multiSelect.options);
    });
    it('should get a list of selected options', () => {
        const selectedItems = [DotExperimentStatusList.DRAFT, DotExperimentStatusList.ENDED];

        spectator.setInput({
            options: selectOptions,
            selectedItems
        });

        multiSelect = spectator.query(MultiSelect);

        expect(spectator.component.options).toBe(multiSelect.options);
    });
});
