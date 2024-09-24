import { NgClass } from '@angular/common';
import { ChangeDetectionStrategy, Component, input } from '@angular/core';

import { UIMessage } from '../../models';

@Component({
    selector: 'dot-file-field-ui-message',
    standalone: true,
    imports: [NgClass],
    templateUrl: './dot-file-field-ui-message.component.html',
    styleUrls: ['./dot-file-field-ui-message.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotFileFieldUiMessageComponent {
    $uiMessage = input.required<UIMessage>({ alias: 'uiMessage' });
}
