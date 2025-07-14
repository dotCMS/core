import { NgClass } from '@angular/common';
import { ChangeDetectionStrategy, Component, HostBinding, input, Input } from '@angular/core';

import { UIMessage } from '@dotcms/edit-content/models/dot-edit-content-file.model';
import { DotMessagePipe } from '@dotcms/ui';

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

    /**
     * Whether the component is disabled.
     *
     * @memberof DotBinaryFieldUiMessageComponent
     */
    @Input()
    @HostBinding('class.disabled')
    disabled = false;
}
