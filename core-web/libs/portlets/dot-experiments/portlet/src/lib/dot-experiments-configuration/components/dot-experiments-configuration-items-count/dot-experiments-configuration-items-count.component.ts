import { ChangeDetectionStrategy, Component, input } from '@angular/core';

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
    maxLength = input.required<number>();
    count = input.required<number>();
}
