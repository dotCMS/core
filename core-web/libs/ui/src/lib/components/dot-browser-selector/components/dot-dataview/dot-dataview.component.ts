import { DatePipe } from '@angular/common';
import {
    ChangeDetectionStrategy,
    Component,
    ElementRef,
    input,
    model,
    output,
    signal,
    viewChild
} from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { IconFieldModule } from 'primeng/iconfield';
import { InputIconModule } from 'primeng/inputicon';
import { InputTextModule } from 'primeng/inputtext';
import { MessageModule } from 'primeng/message';
import { SkeletonModule } from 'primeng/skeleton';
import { TableModule } from 'primeng/table';

import { DotCMSContentlet } from '@dotcms/dotcms-models';

import { DotMessagePipe } from '../../../../dot-message/dot-message.pipe';
import { DotContentThumbnailComponent } from '../../../dot-content-thumbnail/dot-content-thumbnail.component';

@Component({
    selector: 'dot-dataview',
    imports: [
        ButtonModule,
        TableModule,
        IconFieldModule,
        InputIconModule,
        InputTextModule,
        MessageModule,
        SkeletonModule,
        DatePipe,
        DotMessagePipe,
        DotContentThumbnailComponent
    ],
    templateUrl: './dot-dataview.component.html',
    styleUrls: ['./dot-dataview.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
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
     * Reactive model holding the currently selected content row.
     * Can be a `DotCMSContentlet` or `null`.
     */
    $selectedContent = model<DotCMSContentlet | null>(null, { alias: 'selectedContent' });

    /**
     * Emits the selected `DotCMSContentlet` when a row is selected.
     */
    onRowSelect = output<DotCMSContentlet>();

    /**
     * Controls the accepted file types for the OS file picker.
     * Defaults to '*' (all files). Pass 'image/*' to restrict to images only.
     */
    $accept = input<string>('*', { alias: 'accept' });

    /**
     * When true, the upload button is disabled (e.g. no site/folder selected yet).
     */
    $uploadDisabled = input<boolean>(false, { alias: 'uploadDisabled' });

    /**
     * Emits the `File` selected by the user via the OS file picker.
     */
    onUploadFile = output<File>();

    $fileInput = viewChild.required<ElementRef<HTMLInputElement>>('fileInput');

    onFileSelected(event: Event): void {
        const input = event.target as HTMLInputElement;
        const file = input.files?.[0];

        if (file) {
            this.onUploadFile.emit(file);
        }

        // Reset via ViewChild so the same file can be selected again.
        this.$fileInput().nativeElement.value = '';
    }
}
