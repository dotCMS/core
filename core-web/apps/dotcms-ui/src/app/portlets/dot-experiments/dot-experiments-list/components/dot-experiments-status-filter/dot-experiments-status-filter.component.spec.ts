import { DotExperimentsStatusFilterComponent } from './dot-experiments-status-filter.component';
import { createComponentFactory, mockProvider, Spectator } from '@ngneat/spectator';
import {
    DotExperimentStatusList,
    ExperimentsStatusList
} from '@portlets/dot-experiments/shared/models/dot-experiments.model';
import { MultiSelect, MultiSelectModule } from 'primeng/multiselect';
import { DotMessageService } from '@services/dot-message/dot-messages.service';
import { Pipe, PipeTransform } from '@angular/core';
import { FormsModule } from '@angular/forms';

@Pipe({ name: 'dm' })
class MockDmPipe implements PipeTransform {
    transform(value: string): string {
        return value;
    }
}

const selectOptions = ExperimentsStatusList;
fdescribe('DotExperimentsStatusFilterComponent', () => {
    let spectator: Spectator<DotExperimentsStatusFilterComponent>;
    let multiSelect: MultiSelect;

    const createComponent = createComponentFactory({
        imports: [MultiSelectModule, FormsModule],
        component: DotExperimentsStatusFilterComponent,
        declarations: [MockDmPipe],
        providers: [mockProvider(DotMessageService)]
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
