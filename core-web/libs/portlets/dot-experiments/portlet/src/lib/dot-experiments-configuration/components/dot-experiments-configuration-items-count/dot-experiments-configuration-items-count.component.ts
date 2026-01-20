import { ChangeDetectionStrategy, Component, Input } from '@angular/core';

@Component({
    selector: 'dot-experiments-configuration-items-count',
    imports: [],
    templateUrl: './dot-experiments-configuration-items-count.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    host: {
        class: 'text-sm text-[var(--gray-800)] pr-2 font-bold'
    }
})
export class DotExperimentsConfigurationItemsCountComponent {
    @Input()
    maxLength: number;

    @Input()
    count: number;
}
