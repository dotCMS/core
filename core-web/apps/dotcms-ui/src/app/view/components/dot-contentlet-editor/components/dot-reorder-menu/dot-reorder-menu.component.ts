import { Component, EventEmitter, Input, Output } from '@angular/core';

import { DotMessagePipe } from '@dotcms/ui';

import { DotIframeDialogComponent } from '../../../dot-iframe-dialog/dot-iframe-dialog.component';

@Component({
    selector: 'dot-reorder-menu',
    templateUrl: './dot-reorder-menu.component.html',
    imports: [DotMessagePipe, DotIframeDialogComponent]
})
export class DotReorderMenuComponent {
    @Input() url: string;
    @Output() shutdown: EventEmitter<unknown> = new EventEmitter();

    /**
     * Handle close event from the iframe
     *
     * @memberof DotContentletWrapperComponent
     */
    onClose(): void {
        this.shutdown.emit();
    }
}
