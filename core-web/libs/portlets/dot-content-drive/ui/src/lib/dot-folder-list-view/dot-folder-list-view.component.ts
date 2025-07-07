import {
    ChangeDetectionStrategy,
    Component,
    CUSTOM_ELEMENTS_SCHEMA,
    input,
    output
} from '@angular/core';

import { LazyLoadEvent, SortEvent } from 'primeng/api';
import { ChipModule } from 'primeng/chip';
import { SkeletonModule } from 'primeng/skeleton';
import { TableModule } from 'primeng/table';

import { DotContentDriveItem } from '@dotcms/dotcms-models';
import { DotContentletStatusPipe, DotMessagePipe, DotRelativeDatePipe } from '@dotcms/ui';

import { HEADER_COLUMNS } from '../shared/constants';

@Component({
    selector: 'dot-folder-list-view',
    standalone: true,
    imports: [
        TableModule,
        DotRelativeDatePipe,
        SkeletonModule,
        DotMessagePipe,
        DotContentletStatusPipe,
        ChipModule
    ],
    schemas: [CUSTOM_ELEMENTS_SCHEMA],
    templateUrl: './dot-folder-list-view.component.html',
    styleUrl: './dot-folder-list-view.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotFolderListViewComponent {
    items = input<DotContentDriveItem[]>([]);
    totalItems = input<number>(0);
    loading = input<boolean>(false);

    selectionChange = output<DotContentDriveItem[]>();
    paginate = output<LazyLoadEvent>();
    sort = output<SortEvent>();

    readonly headerColumns = HEADER_COLUMNS;
    readonly selectedItems: DotContentDriveItem[] = [];

    onPage(event: LazyLoadEvent) {
        this.paginate.emit(event);
    }

    onSelectionChange() {
        this.selectionChange.emit(this.selectedItems);
    }

    onSort(event: SortEvent) {
        this.sort.emit(event);
    }
}
