import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, HostBinding, Input } from '@angular/core';

import { DotMessagePipe } from '@dotcms/ui';

import { UiMessageI } from '../../interfaces';

@Component({
    selector: 'dot-binary-field-ui-message',
    imports: [CommonModule, DotMessagePipe],
    templateUrl: './dot-binary-field-ui-message.component.html',
    styleUrls: ['./dot-binary-field-ui-message.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotBinaryFieldUiMessageComponent {
    @Input() uiMessage: UiMessageI;

    /**
     * Whether the component is disabled.
     *
     * @memberof DotBinaryFieldUiMessageComponent
     */
    @Input()
    @HostBinding('class.disabled')
    disabled = false;
}
