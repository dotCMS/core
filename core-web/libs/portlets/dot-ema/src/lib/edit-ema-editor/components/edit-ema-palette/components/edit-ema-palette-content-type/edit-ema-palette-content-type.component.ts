import { JsonPipe, NgFor } from '@angular/common';
import { ChangeDetectionStrategy, Component, EventEmitter, Input, Output } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';

import { InputTextModule } from 'primeng/inputtext';

import { DotIconModule, DotMessagePipe } from '@dotcms/ui';

@Component({
    selector: 'dot-edit-ema-palette-content-type',
    standalone: true,
    imports: [NgFor, ReactiveFormsModule, DotIconModule, JsonPipe, DotMessagePipe, InputTextModule],
    templateUrl: './edit-ema-palette-content-type.component.html',
    styleUrls: ['./edit-ema-palette-content-type.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class EditEmaPaletteContentTypeComponent {
    searchContenttype = new FormControl('');
    @Input() contentTypes = [];
    @Input() filter = '';
    @Input() control;

    @Output() dragStart = new EventEmitter();
    @Output() dragEnd = new EventEmitter();
    @Output() showContentlets = new EventEmitter<string>();

    onDragStart(event: DragEvent) {
        this.dragStart.emit(event);
    }

    onDragEnd(event: DragEvent) {
        this.dragEnd.emit(event);
    }

    showContentletsFromContentType(contentTypeName: string) {
        this.showContentlets.emit(contentTypeName);
    }
}
