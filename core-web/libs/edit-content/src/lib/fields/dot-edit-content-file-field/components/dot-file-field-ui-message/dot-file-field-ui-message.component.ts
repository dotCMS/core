import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, input } from '@angular/core';

import { DotMessagePipe } from '@dotcms/ui';

import { UIMessage } from '../../../../models/dot-edit-content-file.model';

@Component({
    selector: 'dot-file-field-ui-message',
    imports: [CommonModule, DotMessagePipe],
    templateUrl: './dot-file-field-ui-message.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotFileFieldUiMessageComponent {
    /**
     * The UI message.
     *
     * @memberof DotFileFieldPreviewComponent
     */
    $uiMessage = input.required<UIMessage>({ alias: 'uiMessage' });

    /**
     * Whether the component is disabled.
     *
     * @memberof DotBinaryFieldUiMessageComponent
     */
    $disabled = input<boolean>(false, { alias: 'disabled' });
}
