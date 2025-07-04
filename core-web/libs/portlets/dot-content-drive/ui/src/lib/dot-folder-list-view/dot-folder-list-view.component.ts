import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';

import { LazyLoadEvent } from 'primeng/api';
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
    paginate = output<LazyLoadEvent>();

    selectionChange = output<DotContentDriveItem[]>();

    readonly headerColumns = [
        { field: 'title', header: 'title', sortable: true },
        { field: 'live', header: 'status' },
        { field: 'baseType', header: 'type', sortable: true },
        { field: 'modUserName', header: 'Edited-By', sortable: true },
        { field: 'modDate', header: 'Last-Edited', sortable: true }
    ];

    onPage(event: LazyLoadEvent) {
        this.paginate.emit(event);
    }

    onSelectionChange() {
        this.selectionChange.emit(this.selectedItems);
    }
}
