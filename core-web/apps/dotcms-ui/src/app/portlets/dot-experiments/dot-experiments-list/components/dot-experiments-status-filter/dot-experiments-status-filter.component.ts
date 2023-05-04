import { ChangeDetectionStrategy, Component, EventEmitter, Input, Output } from '@angular/core';

import { DotDropdownSelectOption, DotExperimentStatusList } from '@dotcms/dotcms-models';

@Component({
    selector: 'dot-experiments-status-filter',
    templateUrl: './dot-experiments-status-filter.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotExperimentsStatusFilterComponent {
    @Input()
    selectedItems: Array<string>;
    @Input()
    options: Array<DotDropdownSelectOption<string>>;

    @Output()
    switch = new EventEmitter<DotExperimentStatusList[]>();
}
