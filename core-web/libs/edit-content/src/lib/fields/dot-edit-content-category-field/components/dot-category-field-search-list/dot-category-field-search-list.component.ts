import { CommonModule } from '@angular/common';
import {
    AfterViewInit,
    ChangeDetectionStrategy,
    Component,
    DestroyRef,
    ElementRef,
    EventEmitter,
    inject,
    input,
    Output,
    signal,
    ViewChild
} from '@angular/core';

import { SkeletonModule } from 'primeng/skeleton';
import { TableModule } from 'primeng/table';

import { DotMessagePipe } from '@dotcms/ui';

import {
    DotCategoryFieldCategory,
    DotCategoryFieldCategorySearchedItems
} from '../../models/dot-category-field.models';
import { DotTableSkeletonComponent } from '../dot-table-skeleton/dot-table-skeleton.component';

@Component({
    selector: 'dot-category-field-search-list',
    standalone: true,
    imports: [CommonModule, TableModule, SkeletonModule, DotTableSkeletonComponent, DotMessagePipe],
    templateUrl: './dot-category-field-search-list.component.html',
    styleUrl: './dot-category-field-search-list.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotCategoryFieldSearchListComponent implements AfterViewInit {
    @ViewChild('tableContainer', { static: false }) tableContainer!: ElementRef;
    scrollHeight = signal<string>('0px');

    /**
     * Represents the required categories input for DotCategoryFieldCategory.
     *
     * @typedef {DotCategoryFieldCategory[]} RequiredCategories
     */
    categories = input.required<DotCategoryFieldCategorySearchedItems[]>();

    @Output() selected = new EventEmitter<DotCategoryFieldCategorySearchedItems[]>();

    /**
     * Represents the selected categories in the DotCategoryFieldCategory class.
     */
    selectedCategories: DotCategoryFieldCategory;
    isLoading = input.required<boolean>();
    #destroyRef = inject(DestroyRef);

    ngAfterViewInit(): void {
        this.calculateScrollHeight();
        window.addEventListener('resize', this.calculateScrollHeight.bind(this));

        this.#destroyRef.onDestroy(() => {
            window.removeEventListener('resize', this.calculateScrollHeight.bind(this));
        });
    }

    private calculateScrollHeight(): void {
        const containerHeight = this.tableContainer.nativeElement.offsetHeight;
        this.scrollHeight.set(`${containerHeight}px`);
    }
}
