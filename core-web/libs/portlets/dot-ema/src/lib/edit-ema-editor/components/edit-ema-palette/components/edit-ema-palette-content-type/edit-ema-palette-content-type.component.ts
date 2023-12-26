import { JsonPipe, NgFor, NgIf } from '@angular/common';
import { ChangeDetectionStrategy, Component, EventEmitter, Input, Output } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';

import { InputTextModule } from 'primeng/inputtext';

import { DotIconModule, DotMessagePipe, DotSpinnerModule } from '@dotcms/ui';

@Component({
    selector: 'dot-edit-ema-palette-content-type',
    standalone: true,
    imports: [
        NgFor,
        NgIf,
        ReactiveFormsModule,
        DotIconModule,
        JsonPipe,
        DotMessagePipe,
        InputTextModule,
        DotSpinnerModule
    ],
    templateUrl: './edit-ema-palette-content-type.component.html',
    styleUrls: ['./edit-ema-palette-content-type.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class EditEmaPaletteContentTypeComponent {
    searchContenttype = new FormControl('');
    @Input() contentTypes = [];
    @Input() filter = '';
    @Input() control;
    @Input() isLoading: boolean;

    @Output() dragStart = new EventEmitter();
    @Output() dragEnd = new EventEmitter();
    @Output() showContentlets = new EventEmitter<string>();

    /**
     * Event handler for the drag start event.
     * @param event The DragEvent object.
     */
    onDragStart(event: DragEvent) {
        this.dragStart.emit(event);
    }

    /**
     * Event handler for the drag end event.
     * @param event The DragEvent object.
     */
    onDragEnd(event: DragEvent) {
        this.dragEnd.emit(event);
    }

    /**
     * Shows the contentlets from a specific content type.
     * @param contentTypeName - The name of the content type.
     */
    showContentletsFromContentType(contentTypeName: string) {
        this.showContentlets.emit(contentTypeName);
    }
}
