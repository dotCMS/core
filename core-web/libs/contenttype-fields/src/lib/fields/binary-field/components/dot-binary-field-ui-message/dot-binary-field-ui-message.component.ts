import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, Input } from '@angular/core';

@Component({
    selector: 'dot-binary-field-ui-message',
    standalone: true,
    imports: [CommonModule],
    templateUrl: './dot-binary-field-ui-message.component.html',
    styleUrls: ['./dot-binary-field-ui-message.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotBinaryFieldUiMessageComponent {
    @Input() message: string;
    @Input() icon: string;
    @Input() severity: string;
}
