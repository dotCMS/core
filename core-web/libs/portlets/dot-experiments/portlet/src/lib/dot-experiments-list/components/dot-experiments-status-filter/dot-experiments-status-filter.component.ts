import { ChangeDetectionStrategy, Component, EventEmitter, Input, Output } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { MultiSelectModule } from 'primeng/multiselect';

import { DotDropdownSelectOption, DotExperimentStatus } from '@dotcms/dotcms-models';
import { DotDropdownDirective, DotMessagePipe } from '@dotcms/ui';

@Component({
    selector: 'dot-experiments-status-filter',
    imports: [
        FormsModule,
        // dotCMS
        DotMessagePipe,
        DotDropdownDirective,
        // PrimeNG
        MultiSelectModule
    ],
    templateUrl: './dot-experiments-status-filter.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotExperimentsStatusFilterComponent {
    @Input()
    selectedItems: Array<string>;
    @Input()
    options: Array<DotDropdownSelectOption<string>>;

    @Output()
    switch = new EventEmitter<DotExperimentStatus[]>();
}
