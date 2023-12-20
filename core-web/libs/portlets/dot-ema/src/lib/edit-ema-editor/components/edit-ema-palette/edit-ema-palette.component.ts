import { JsonPipe, NgFor, NgIf } from '@angular/common';
import { ChangeDetectionStrategy, Component, EventEmitter, Output } from '@angular/core';

import { InputTextModule } from 'primeng/inputtext';
import { PaginatorModule } from 'primeng/paginator';

import { DotIconModule, DotMessagePipe } from '@dotcms/ui';

import { PALETTE_TYPE } from '../../../shared/enums';
@Component({
    selector: 'dot-edit-ema-palette',
    standalone: true,
    templateUrl: './edit-ema-palette.component.html',
    styleUrls: ['./edit-ema-palette.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    imports: [
        JsonPipe,
        DotMessagePipe,
        InputTextModule,
        DotIconModule,
        PaginatorModule,
        NgIf,
        NgFor
    ]
})
export class EditEmaPaletteComponent {
    @Output() dragStart = new EventEmitter();
    @Output() dragEnd = new EventEmitter();

    first = 0;
    rows = 10;

    PALETTETYPE = PALETTE_TYPE;
    currentPaletteType = PALETTE_TYPE.CONTENTTYPE;

    onDragStart(event: DragEvent) {
        this.dragStart.emit(event);
    }

    onDragEnd(event: DragEvent) {
        this.dragEnd.emit(event);
    }

    showContentletsFromContentType() {
        this.currentPaletteType = PALETTE_TYPE.CONTENTLET;
    }

    showContentTypes() {
        this.currentPaletteType = PALETTE_TYPE.CONTENTTYPE;
    }

    onPageChange(event) {
        this.first = event.first;
        this.rows = event.rows;
    }
}
