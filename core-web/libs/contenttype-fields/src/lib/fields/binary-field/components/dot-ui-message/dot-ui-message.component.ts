import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, Input } from '@angular/core';

@Component({
    // eslint-disable-next-line @angular-eslint/component-selector
    selector: 'dot-ui-message',
    standalone: true,
    imports: [CommonModule],
    templateUrl: './dot-ui-message.component.html',
    styleUrls: ['./dot-ui-message.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotUiMessageComponent {
    @Input() message: string;
    @Input() icon: string;
    @Input() severity: string;
}
