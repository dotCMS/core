import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
    selector: 'dot-experiments-configuration-items-count',
    standalone: true,
    imports: [CommonModule],
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
