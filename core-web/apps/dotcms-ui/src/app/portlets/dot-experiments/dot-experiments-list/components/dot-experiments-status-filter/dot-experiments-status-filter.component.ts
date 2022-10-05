import { ChangeDetectionStrategy, Component, EventEmitter, Input, Output } from '@angular/core';
import { DotExperimentStatusList } from '@portlets/dot-experiments/shared/models/dot-experiments.model';

interface optionItem {
    label: string;
    value: string;
    inactive?: boolean;
}

@Component({
    selector: 'dot-experiments-status-filter',
    templateUrl: './dot-experiments-status-filter.component.html',
    styleUrls: ['./dot-experiments-status-filter.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotExperimentsStatusFilterComponent {
    @Input()
    selectedItems: Array<string>;
    @Input()
    options: optionItem[];

    @Output()
    checkedOptions = new EventEmitter<DotExperimentStatusList[]>();
}
