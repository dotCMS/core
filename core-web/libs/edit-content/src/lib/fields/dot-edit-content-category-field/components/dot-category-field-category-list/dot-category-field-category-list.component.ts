import { CommonModule } from '@angular/common';
import {
    AfterViewInit,
    ChangeDetectionStrategy,
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

import { DotCategory } from '@dotcms/dotcms-models';

import { DotCategoryFieldCategory } from '../../models/dot-category-field.models';

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
    categories = input.required<DotCategoryFieldCategory[][]>();

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
    @Output() itemClicked = new EventEmitter<{ index: number; item: DotCategory }>();

    /**
     * Emit the item checked or selected to the parent component
     */
    @Output() itemChecked = new EventEmitter<{ selected: string[]; item: DotCategory }>();

    /**
     * Model of the items selected
     */
    itemsSelected: string[];

    readonly #destroyRef = inject(DestroyRef);

    readonly #effectRef = effect(() => {
        // Todo: find a better way to update this
        // Initial selected items from the contentlet
        this.itemsSelected = this.selected();
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

            if (
                columnsArray[MINIMUM_CATEGORY_WITHOUT_SCROLLING - 1] &&
                columnsArray[MINIMUM_CATEGORY_WITHOUT_SCROLLING - 1].nativeElement.children.length >
                    0
            ) {
                columnsArray[columnsArray.length - 1].nativeElement.scrollIntoView({
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
