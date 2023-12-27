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

import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { PaginatorModule } from 'primeng/paginator';

import { DotIconModule, DotMessagePipe, DotSpinnerModule } from '@dotcms/ui';

import { EditEmaPaletteStoreStatus } from '../../shared/edit-ema-palette.enums';

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
        DotSpinnerModule,
        ButtonModule
    ],
    templateUrl: './edit-ema-palette-contentlets.component.html',
    styleUrls: ['./edit-ema-palette-contentlets.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    schemas: [CUSTOM_ELEMENTS_SCHEMA]
})
export class EditEmaPaletteContentletsComponent {
    @Input() contentlets;
    @Input() control: FormControl;
    @Input() paletteStatus: EditEmaPaletteStoreStatus;

    @Output() dragStart = new EventEmitter();
    @Output() dragEnd = new EventEmitter();
    @Output() paginate = new EventEmitter();
    @Output() showContentTypes = new EventEmitter();

    EDIT_EMA_PALETTE_STATUS = EditEmaPaletteStoreStatus;

    /**
     * Handles the pagination event and emits the paginated data.
     * @param event The pagination event.
     * @param filter The filter object containing the query and contentTypeVarName.
     */
    onPaginate(event, filter: { query: string; contentTypeVarName: string }) {
        this.paginate.emit({ ...event, ...filter });
    }
}
