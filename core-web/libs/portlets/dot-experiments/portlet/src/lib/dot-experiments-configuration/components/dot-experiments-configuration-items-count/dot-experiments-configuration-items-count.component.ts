import { ChangeDetectionStrategy, Component, Input } from '@angular/core';

@Component({
    selector: 'dot-experiments-configuration-items-count',
    imports: [],
    templateUrl: './dot-experiments-configuration-items-count.component.html',
    styleUrls: ['./dot-experiments-configuration-items-count.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotExperimentsConfigurationItemsCountComponent {
    @Input()
    maxLength: number;

    @Input()
    count: number;
}
