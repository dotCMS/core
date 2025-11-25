import { NgClass } from '@angular/common';
import { ChangeDetectionStrategy, Component, input } from '@angular/core';

import { DotMessagePipe } from '@dotcms/ui';

import { UIMessage } from '../../models';

@Component({
    selector: 'dot-file-field-ui-message',
    standalone: true,
    imports: [NgClass, DotMessagePipe],
    templateUrl: './dot-file-field-ui-message.component.html',
    styleUrls: ['./dot-file-field-ui-message.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotFileFieldUiMessageComponent {
    /**
     * The UI message.
     *
     * @memberof DotFileFieldPreviewComponent
     */
    $uiMessage = input.required<UIMessage>({ alias: 'uiMessage' });
}
