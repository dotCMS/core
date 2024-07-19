import { CommonModule } from '@angular/common';
import {
    AfterViewInit,
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    computed,
    DestroyRef,
    effect,
    ElementRef,
    EventEmitter,
    inject,
    input,
    Output,
    QueryList,
    ViewChildren
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { CheckboxModule } from 'primeng/checkbox';
import { TreeModule } from 'primeng/tree';

import { DotCategoryFieldKeyValueObj } from '../../models/dot-category-field.models';

export const MINIMUM_CATEGORY_COLUMNS = 4;

const MINIMUM_CATEGORY_WITHOUT_SCROLLING = 3;

/**
 * Represents the Dot Category Field Category List component.
 * @class
 * @implements {AfterViewInit}
 */
@Component({
    selector: 'dot-category-field-category-list',
    standalone: true,
    imports: [CommonModule, TreeModule, CheckboxModule, ButtonModule, FormsModule],
    templateUrl: './dot-category-field-category-list.component.html',
    styleUrl: './dot-category-field-category-list.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush,
    host: {
        class: 'category-list__wrapper'
    }
})
export class DotCategoryFieldCategoryListComponent implements AfterViewInit {
    /**
     *  Represent the columns of categories
     */
    @ViewChildren('categoryColumn') categoryColumns: QueryList<ElementRef>;

    /**
     * Represents the variable 'categories' which is of type 'DotCategoryFieldCategory[][]'.
     */
    categories = input.required<DotCategoryFieldKeyValueObj[][]>();

    /**
     * Represent the selected item saved in the contentlet
     */
    selected = input.required<string[]>();

    /**
     * Generate the empty columns
     */
    emptyColumns = computed(() => {
        const numberOfEmptyColumnsNeeded = Math.max(
            MINIMUM_CATEGORY_COLUMNS - this.categories().length,
            0
        );

        return Array(numberOfEmptyColumnsNeeded).fill(null);
    });

    /**
     * Emit the item clicked to the parent component
     */
    @Output() rowClicked = new EventEmitter<{ index: number; item: DotCategoryFieldKeyValueObj }>();

    /**
     * Emit the item checked or selected to the parent component
     */
    @Output() itemChecked = new EventEmitter<{
        selected: string[];
        item: DotCategoryFieldKeyValueObj;
    }>();

    /**
     * Model of the items selected
     */
    itemsSelected: string[];

    #cdr = inject(ChangeDetectorRef);

    readonly #destroyRef = inject(DestroyRef);

    readonly #effectRef = effect(() => {
        // Todo: change itemsSelected to use model when update Angular to >17.3
        // Initial selected items from the contentlet
        this.itemsSelected = this.selected();
        this.#cdr.markForCheck(); // force refresh
    });

    ngAfterViewInit() {
        // Handle the horizontal scroll to make visible the last column
        this.categoryColumns.changes.pipe(takeUntilDestroyed(this.#destroyRef)).subscribe(() => {
            this.scrollHandler();
        });

        this.#destroyRef.onDestroy(() => {
            this.#effectRef.destroy();
        });
    }

    private scrollHandler() {
        try {
            const columnsArray = this.categoryColumns.toArray();

            if (columnsArray.length === 0) {
                return;
            }

            const lastColumnIndex = columnsArray.length - 1;

            if (
                columnsArray[MINIMUM_CATEGORY_WITHOUT_SCROLLING - 1] &&
                columnsArray[MINIMUM_CATEGORY_WITHOUT_SCROLLING - 1].nativeElement.children.length >
                    0
            ) {
                columnsArray[lastColumnIndex].nativeElement.scrollIntoView({
                    behavior: 'smooth',
                    block: 'end',
                    inline: 'end'
                });
            } else {
                // scroll to the first column
                columnsArray[0].nativeElement.scrollIntoView({
                    behavior: 'smooth',
                    block: 'nearest',
                    inline: 'start'
                });
            }
        } catch (error) {
            console.error('Error during scrollHandler execution:', error);
        }
    }
}
