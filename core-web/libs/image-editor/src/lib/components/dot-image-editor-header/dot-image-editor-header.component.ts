import { ChangeDetectionStrategy, Component, output } from '@angular/core';

import { ButtonModule } from 'primeng/button';

import { DotMessagePipe } from '@dotcms/ui';

/**
 * Header bar of the image editor dialog. Renders the editor title on the left and
 * a close icon button on the right that emits {@link DotImageEditorHeaderComponent.close}.
 */
@Component({
    selector: 'dot-image-editor-header',
    changeDetection: ChangeDetectionStrategy.OnPush,
    imports: [ButtonModule, DotMessagePipe],
    templateUrl: './dot-image-editor-header.component.html'
})
export class DotImageEditorHeaderComponent {
    /** Emitted when the user clicks the close (✕) button. */
    close = output<void>();
}
