import { ChangeDetectionStrategy, Component, EventEmitter, Input, Output } from '@angular/core';
import { DotIconModule } from '@dotcms/ui';

@Component({
    standalone: true,
    selector: 'dot-experiments-header',
    templateUrl: './experiments-ui-header.component.html',
    styleUrls: ['./experiments-ui-header.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    imports: [DotIconModule]
})
export class ExperimentsUiHeaderComponent {
    @Input()
    title?: string;

    @Output()
    goBack = new EventEmitter<true>();
}
