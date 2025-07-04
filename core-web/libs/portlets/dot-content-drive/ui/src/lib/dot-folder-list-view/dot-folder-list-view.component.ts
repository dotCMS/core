import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';

import { LazyLoadEvent, SortEvent } from 'primeng/api';
import { SkeletonModule } from 'primeng/skeleton';
import { TableModule } from 'primeng/table';

import { DotContentDriveItem } from '@dotcms/dotcms-models';
import { DotMessagePipe, DotRelativeDatePipe } from '@dotcms/ui';

@Component({
    selector: 'dot-folder-list-view',
    standalone: true,
    imports: [TableModule, DotRelativeDatePipe, SkeletonModule, DotMessagePipe],
    templateUrl: './dot-folder-list-view.component.html',
    styleUrl: './dot-folder-list-view.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotFolderListViewComponent {
    items = input<DotContentDriveItem[]>([]);
    totalItems = input<number>(0);
    loading = input<boolean>(false);

    selectedItems: DotContentDriveItem[] = [];

    selectionChange = output<DotContentDriveItem[]>();
    paginate = output<LazyLoadEvent>();
    sort = output<SortEvent>();

    readonly headerColumns = [
        { field: 'title', header: 'title', width: '45%' },
        { field: 'live', header: 'status', width: '10%' },
        { field: 'baseType', header: 'type', sortable: true, width: '10%' },
        { field: 'modUserName', header: 'Edited-By', width: '15%' },
        { field: 'modDate', header: 'Last-Edited', sortable: true, width: '15%' }
    ];

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
