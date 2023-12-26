import { JsonPipe, NgFor, NgIf } from '@angular/common';
import {
    CUSTOM_ELEMENTS_SCHEMA,
    ChangeDetectionStrategy,
    Component,
    EventEmitter,
    Input,
    Output
} from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';

import { InputTextModule } from 'primeng/inputtext';
import { PaginatorModule } from 'primeng/paginator';

import { DotIconModule, DotMessagePipe, DotSpinnerModule } from '@dotcms/ui';

@Component({
    selector: 'dot-edit-ema-palette-contentlets',
    standalone: true,
    imports: [
        NgIf,
        NgFor,
        ReactiveFormsModule,
        DotIconModule,
        PaginatorModule,
        DotMessagePipe,
        JsonPipe,
        InputTextModule,
        DotSpinnerModule
    ],
    templateUrl: './edit-ema-palette-contentlets.component.html',
    styleUrls: ['./edit-ema-palette-contentlets.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    schemas: [CUSTOM_ELEMENTS_SCHEMA]
})
export class EditEmaPaletteContentletsComponent {
    @Input() contentlets;
    @Input() control: FormControl;
    @Input() itemsPerPage: number;
    @Input() isLoading: boolean;

    @Output() dragStart = new EventEmitter();
    @Output() dragEnd = new EventEmitter();
    @Output() paginate = new EventEmitter();
    @Output() showContentTypes = new EventEmitter();

    /**
     * Event handler for the drag start event.
     * @param event The drag event.
     */
    onDragStart(event: DragEvent) {
        this.dragStart.emit(event);
    }

    /**
     * Handles the drag end event.
     * @param event The drag event.
     */
    onDragEnd(event: DragEvent) {
        this.dragEnd.emit(event);
    }

    /**
     * Navigates back to the content types view.
     */
    backToContentTypes() {
        this.showContentTypes.emit();
    }

    /**
     * Handles the pagination event and emits the paginated data.
     * @param event The pagination event.
     * @param filter The filter object containing the query and contentTypeVarName.
     */
    onPaginate(event, filter: { query: string; contentTypeVarName: string }) {
        this.paginate.emit({ ...event, ...filter });
    }
}
