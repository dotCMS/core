import { DatePipe } from '@angular/common';
import {
    ChangeDetectionStrategy,
    Component,
    CUSTOM_ELEMENTS_SCHEMA,
    input,
    model,
    output,
    signal
} from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { IconFieldModule } from 'primeng/iconfield';
import { InputIconModule } from 'primeng/inputicon';
import { InputTextModule } from 'primeng/inputtext';
import { SkeletonModule } from 'primeng/skeleton';
import { TableModule } from 'primeng/table';

import { DotCMSContentlet } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';

@Component({
    selector: 'dot-dataview',
    imports: [
        ButtonModule,
        TableModule,
        IconFieldModule,
        InputIconModule,
        InputTextModule,
        SkeletonModule,
        DatePipe,
        DotMessagePipe
    ],
    templateUrl: './dot-dataview.component.html',
    styleUrls: ['./dot-dataview.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    schemas: [CUSTOM_ELEMENTS_SCHEMA]
})
export class DotDataViewComponent {
    /**
     * Represents an observable stream of content data.
     *
     * @type {Observable<DotCMSContentlet[]>}
     * @alias data
     * @required
     */
    $data = input.required<DotCMSContentlet[]>({ alias: 'data' });

    /**
     * Error message to display in the table.
     *
     * @type {string}
     */
    $errorMessage = input<string>('', { alias: 'error' });

    /**
     * A boolean observable that indicates the loading state.
     * This is typically used to show or hide a loading indicator in the UI.
     *
     * @type {boolean}
     */
    $loading = input.required<boolean>({ alias: 'loading' });

    /**
     * Signal representing the number of rows per page in the data view.
     *
     * @type {number}
     */
    $rowsPerPage = signal<number>(9);

    /**
     * Reactive model holding the currently selected product.
     * Can be a `DotCMSContentlet` or `null`.
     */
    $selectedProduct = model<DotCMSContentlet | null>(null);

    /**
     * Emits the selected `DotCMSContentlet` when a row is selected.
     */
    onRowSelect = output<DotCMSContentlet>();
}
