import { ChangeDetectionStrategy, Component, EventEmitter, Input, Output } from '@angular/core';
import { DotExperimentStatusList } from '@dotcms/dotcms-models';

interface optionItem {
    label: string;
    value: string;
    inactive?: boolean;
}

@Component({
    selector: 'dot-experiments-status-filter',
    templateUrl: './dot-experiments-status-filter.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotExperimentsStatusFilterComponent {
    @Input()
    selectedItems: Array<string>;
    @Input()
    options: optionItem[];

    @Output()
    switch = new EventEmitter<DotExperimentStatusList[]>();
}
