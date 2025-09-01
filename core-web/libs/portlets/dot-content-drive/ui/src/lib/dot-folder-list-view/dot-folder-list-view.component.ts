import {
    ChangeDetectionStrategy,
    Component,
    computed,
    CUSTOM_ELEMENTS_SCHEMA,
    effect,
    input,
    output,
    signal
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
    $items = input<DotContentDriveItem[]>([], { alias: 'items' });
    $totalItems = input<number>(0, { alias: 'totalItems' });
    $loading = input<boolean>(false, { alias: 'loading' });

    selectionChange = output<DotContentDriveItem[]>();
    paginate = output<LazyLoadEvent>();
    sort = output<SortEvent>();

    selectedItems: DotContentDriveItem[] = [];
    readonly MIN_ROWS_PER_PAGE = 20;
    protected readonly rowsPerPageOptions = [this.MIN_ROWS_PER_PAGE, 40, 60];
    protected readonly HEADER_COLUMNS = HEADER_COLUMNS;
    protected readonly SKELETON_SPAN = HEADER_COLUMNS.length + 1;
    protected readonly $showPagination = computed(
        () => this.$totalItems() > this.MIN_ROWS_PER_PAGE
    );
    protected readonly $styleClass = computed(() =>
        this.$items().length === 0 ? 'dotTable empty-table' : 'dotTable'
    );

    /**
     * Index of the first row to be displayed in the current page.
     * Used by PrimeNG Table for pagination state management.
     */
    protected readonly $currentPageFirstRowIndex = signal<number>(0);
    protected readonly firstEffect = effect(() => {
        const showPagination = this.$showPagination();
        if (showPagination) {
            this.$currentPageFirstRowIndex.set(0);
        }
    });

    onPage(event: LazyLoadEvent) {
        this.$currentPageFirstRowIndex.set(event.first);
        this.paginate.emit(event);
    }

    onSelectionChange() {
        this.selectionChange.emit(this.selectedItems);
    }

    onSort(event: SortEvent) {
        this.sort.emit(event);
    }
}
