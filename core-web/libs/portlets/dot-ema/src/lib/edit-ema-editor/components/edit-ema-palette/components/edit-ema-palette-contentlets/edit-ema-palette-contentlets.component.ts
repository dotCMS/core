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

import { DotIconModule, DotMessagePipe } from '@dotcms/ui';

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
        InputTextModule
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

    @Output() dragStart = new EventEmitter();
    @Output() dragEnd = new EventEmitter();
    @Output() paginate = new EventEmitter();
    @Output() showContentTypes = new EventEmitter();

    onDragStart(event: DragEvent) {
        this.dragStart.emit(event);
    }

    onDragEnd(event: DragEvent) {
        this.dragEnd.emit(event);
    }

    backToContentTypes() {
        this.showContentTypes.emit();
    }

    onPaginate(event, filter: { query: string; contentTypeVarName: string }) {
        this.paginate.emit({ ...event, ...filter });
    }
}
