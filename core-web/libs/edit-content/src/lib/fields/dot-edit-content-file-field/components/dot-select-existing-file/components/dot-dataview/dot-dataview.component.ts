import { DatePipe, NgOptimizedImage } from '@angular/common';
import { ChangeDetectionStrategy, Component, input } from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { DataViewModule } from 'primeng/dataview';
import { IconFieldModule } from 'primeng/iconfield';
import { ImageModule } from 'primeng/image';
import { InputIconModule } from 'primeng/inputicon';
import { InputTextModule } from 'primeng/inputtext';
import { SkeletonModule } from 'primeng/skeleton';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';

import { DotMessagePipe } from '@dotcms/ui';

import { Content } from '../../store/select-existing-file.store';

@Component({
    selector: 'dot-dataview',
    standalone: true,
    imports: [
        DataViewModule,
        TagModule,
        ButtonModule,
        TableModule,
        IconFieldModule,
        InputIconModule,
        InputTextModule,
        SkeletonModule,
        ImageModule,
        NgOptimizedImage,
        DatePipe,
        DotMessagePipe
    ],
    templateUrl: './dot-dataview.component.html',
    styleUrls: ['./dot-dataview.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotDataViewComponent {
    /**
     * Represents an observable stream of content data.
     *
     * @type {Observable<Content[]>}
     * @alias data
     * @required
     */
    $data = input.required<Content[]>({ alias: 'data' });
    /**
     * A boolean observable that indicates the loading state.
     * This is typically used to show or hide a loading indicator in the UI.
     *
     * @type {boolean}
     */
    $loading = input.required<boolean>({ alias: 'loading' });
}
