import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, Input } from '@angular/core';

import { DotMessagePipe } from '@dotcms/ui';

import { UiMessageI } from '../../interfaces';

@Component({
    selector: 'dot-binary-field-ui-message',
    standalone: true,
    imports: [CommonModule, DotMessagePipe],
    templateUrl: './dot-binary-field-ui-message.component.html',
    styleUrls: ['./dot-binary-field-ui-message.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotBinaryFieldUiMessageComponent {
    @Input() uiMessage: UiMessageI;
}
