import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, input } from '@angular/core';

import { DotMessagePipe } from '@dotcms/ui';

import { UiMessageI } from '../../interfaces';

@Component({
    selector: 'dot-binary-field-ui-message',
    imports: [CommonModule, DotMessagePipe],
    templateUrl: './dot-binary-field-ui-message.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotBinaryFieldUiMessageComponent {
    $uiMessage = input<UiMessageI>(undefined, { alias: 'uiMessage' });

    /**
     * Whether the component is disabled.
     *
     * @memberof DotBinaryFieldUiMessageComponent
     */
    $disabled = input<boolean>(false, { alias: 'disabled' });
}
