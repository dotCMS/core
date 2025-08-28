import { NgClass } from '@angular/common';
import { ChangeDetectionStrategy, Component, HostBinding, input, Input } from '@angular/core';

import { DotMessagePipe } from '@dotcms/ui';

import { UIMessage } from '../../../../models/dot-edit-content-file.model';

@Component({
    selector: 'dot-file-field-ui-message',
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

    /**
     * Whether the component is disabled.
     *
     * @memberof DotBinaryFieldUiMessageComponent
     */
    @Input()
    @HostBinding('class.disabled')
    disabled = false;
}
