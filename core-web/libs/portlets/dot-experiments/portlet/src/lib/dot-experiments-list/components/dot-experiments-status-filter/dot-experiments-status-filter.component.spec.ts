import { createComponentFactory, Spectator } from '@ngneat/spectator/jest';

import { MultiSelect, MultiSelectModule } from 'primeng/multiselect';

import { DotMessageService } from '@dotcms/data-access';
import { DotDropdownSelectOption, DotExperimentStatus } from '@dotcms/dotcms-models';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotExperimentsStatusFilterComponent } from './dot-experiments-status-filter.component';

const messageServiceMock = new MockDotMessageService({
    'experimentspage.experiment.status.placeholder': 'Select Status',
    running: 'Running',
    draft: 'Draft',
    ended: 'Ended',
    scheduled: 'Scheduled'
});

const OPTIONS_MOCK: Array<DotDropdownSelectOption<string>> = [
    {
        label: 'Running',
        value: DotExperimentStatus.RUNNING
    },
    {
        label: 'Draft',
        value: DotExperimentStatus.DRAFT
    },
    {
        label: 'Ended',
        value: DotExperimentStatus.ENDED
    },
    {
        label: 'Scheduled',
        value: DotExperimentStatus.SCHEDULED
    }
];

const SELECTED_ITEMS_MOCK = [DotExperimentStatus.RUNNING, DotExperimentStatus.DRAFT];

describe('DotExperimentsStatusFilterComponent', () => {
    let spectator: Spectator<DotExperimentsStatusFilterComponent>;

    const createComponent = createComponentFactory({
        component: DotExperimentsStatusFilterComponent,
        imports: [MultiSelectModule],
        providers: [
            {
                provide: DotMessageService,
                useValue: messageServiceMock
            }
        ]
    });

    beforeEach(() => {
        spectator = createComponent({
            props: {
                options: OPTIONS_MOCK,
                selectedItems: SELECTED_ITEMS_MOCK
            } as unknown
        });
    });

    it('should create', () => {
        expect(spectator.component).toBeTruthy();
    });

    it('should get a list of options', () => {
        const multiSelect = spectator.query(MultiSelect);

        expect(multiSelect).toExist();
        expect(multiSelect.options).toEqual(OPTIONS_MOCK);
    });

    it('should get a list of selected options', () => {
        const multiSelect = spectator.query(MultiSelect);

        expect(multiSelect).toExist();
        expect(multiSelect.value).toEqual(SELECTED_ITEMS_MOCK);
        expect(spectator.component.selectedItems).toEqual(SELECTED_ITEMS_MOCK);
    });
});
