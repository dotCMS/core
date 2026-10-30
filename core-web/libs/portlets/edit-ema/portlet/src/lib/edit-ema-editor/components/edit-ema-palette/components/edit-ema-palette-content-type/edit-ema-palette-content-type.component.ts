import { Subject } from 'rxjs';

import { JsonPipe } from '@angular/common';
import {
    ChangeDetectionStrategy,
    Component,
    EventEmitter,
    Input,
    OnDestroy,
    OnInit,
    Output
} from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';

import { InputTextModule } from 'primeng/inputtext';

import { debounceTime, takeUntil } from 'rxjs/operators';

import { DotIconModule, DotMessagePipe, DotSpinnerModule } from '@dotcms/ui';

import { EditEmaPaletteStoreStatus } from '../../store/edit-ema-palette.store';

@Component({
    selector: 'dot-edit-ema-palette-content-type',
    imports: [
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
export class EditEmaPaletteContentTypeComponent implements OnInit, OnDestroy {
    @Input() contentTypes = [];
    @Input() filter = '';
    @Input() paletteStatus: EditEmaPaletteStoreStatus;

    @Output() showContentlets = new EventEmitter<string>();
    @Output() search = new EventEmitter<string>();

    private destroy$ = new Subject<void>();

    searchContentType = new FormControl('');
    EDIT_EMA_PALETTE_STATUS = EditEmaPaletteStoreStatus;

    ngOnInit() {
        this.searchContentType.setValue(this.filter, { emitEvent: false });
        this.searchContentType.valueChanges
            .pipe(takeUntil(this.destroy$), debounceTime(1000))
            .subscribe((value) => {
                this.search.emit(value);
            });
    }

    /**
     *
     *
     * @param {string} contentTypeName
     * @memberof EditEmaPaletteContentTypeComponent
     */
    showContentletsFromContentType(contentTypeName: string) {
        this.showContentlets.emit(contentTypeName);
    }

    ngOnDestroy() {
        this.destroy$.next();
        this.destroy$.complete();
    }
}
