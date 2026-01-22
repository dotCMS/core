import { ChangeDetectionStrategy, Component, effect, input, output } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { MultiSelectModule } from 'primeng/multiselect';

import { DotDropdownSelectOption, DotExperimentStatus } from '@dotcms/dotcms-models';
import { DotDropdownDirective, DotMessagePipe } from '@dotcms/ui';

@Component({
    selector: 'dot-experiments-status-filter',
    imports: [FormsModule, DotMessagePipe, DotDropdownDirective, MultiSelectModule],
    templateUrl: './dot-experiments-status-filter.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotExperimentsStatusFilterComponent {
    $selectedItems = input.required<Array<string>>({ alias: 'selectedItems' });
    $options = input.required<Array<DotDropdownSelectOption<string>>>({ alias: 'options' });

    $switch = output<DotExperimentStatus[]>({ alias: 'switch' });

    // Regular property for ngModel two-way binding
    selectedItems: Array<string> = [];

    constructor() {
        effect(() => {
            this.selectedItems = [...this.$selectedItems()];
        });
    }
}
