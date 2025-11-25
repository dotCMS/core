import { Subject } from 'rxjs';

import { JsonPipe } from '@angular/common';
import {
    CUSTOM_ELEMENTS_SCHEMA,
    ChangeDetectionStrategy,
    Component,
    EventEmitter,
    Input,
    OnDestroy,
    OnInit,
    Output
} from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { PaginatorModule, PaginatorState } from 'primeng/paginator';

import { debounceTime, takeUntil } from 'rxjs/operators';

import { DotIconModule, DotMessagePipe, DotSpinnerModule } from '@dotcms/ui';

import { EditEmaPaletteStoreStatus } from '../../store/edit-ema-palette.store';

@Component({
    selector: 'dot-edit-ema-palette-contentlets',
    standalone: true,
    imports: [
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
export class EditEmaPaletteContentletsComponent implements OnInit, OnDestroy {
    @Input() contentlets;
    @Input() paletteStatus: EditEmaPaletteStoreStatus;

    @Output() paginate = new EventEmitter();
    @Output() showContentTypes = new EventEmitter();
    @Output() search = new EventEmitter<string>();

    private destroy$ = new Subject<void>();

    control = new FormControl('');
    EDIT_EMA_PALETTE_STATUS = EditEmaPaletteStoreStatus;

    ngOnInit() {
        this.control.valueChanges
            .pipe(takeUntil(this.destroy$), debounceTime(1000))
            .subscribe((value) => {
                this.search.emit(value);
            });
    }

    /**
     *
     *
     * @param {*} event
     * @param {{ query: string; contentTypeVarName: string }} filter
     * @memberof EditEmaPaletteContentletsComponent
     */
    onPaginate(event: PaginatorState, filter: { query: string; contentTypeVarName: string }) {
        this.paginate.emit({ ...event, ...filter });
    }

    ngOnDestroy() {
        this.destroy$.next();
        this.destroy$.complete();
    }
}
