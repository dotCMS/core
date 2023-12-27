import { JsonPipe, NgFor, NgIf } from '@angular/common';
import { ChangeDetectionStrategy, Component, EventEmitter, Input, Output } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';

import { InputTextModule } from 'primeng/inputtext';

import { DotIconModule, DotMessagePipe, DotSpinnerModule } from '@dotcms/ui';

import { EditEmaPaletteStoreStatus } from '../../store/edit-ema-palette.store';

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
    @Input() control: FormControl;
    @Input() paletteStatus: EditEmaPaletteStoreStatus;

    @Output() dragStart = new EventEmitter();
    @Output() dragEnd = new EventEmitter();
    @Output() showContentlets = new EventEmitter<string>();

    EDIT_EMA_PALETTE_STATUS = EditEmaPaletteStoreStatus;

    /**
     *
     *
     * @param {string} contentTypeName
     * @memberof EditEmaPaletteContentTypeComponent
     */
    showContentletsFromContentType(contentTypeName: string) {
        this.showContentlets.emit(contentTypeName);
    }
}
