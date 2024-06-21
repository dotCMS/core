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

const MINIMUM_CATEGORY_COLUMNS = 4;
const MINIMUM_CATEGORY_WITHOUT_SCROLLING = 3;

/**
 * Represents the Dot Category Field Category List component.
 * @class
 * @implements {AfterViewInit}
 */
@Component({
    selector: 'dot-category-field-category-list',
    standalone: true,
    imports: [CommonModule, TreeModule, CheckboxModule, FormsModule, ButtonModule],
    templateUrl: './dot-category-field-category-list.component.html',
    styleUrl: './dot-category-field-category-list.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush,
    // eslint-disable-next-line @angular-eslint/no-host-metadata-property
    host: {
        class: 'category-list__wrapper'
    }
})
export class DotCategoryFieldCategoryListComponent implements AfterViewInit {
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
        const minimumCategoryColumns = MINIMUM_CATEGORY_COLUMNS;
        const currentCategoriesLength = this.categories().length;
        const numberOfEmptyColumns = Math.max(minimumCategoryColumns - currentCategoriesLength, 0);

        return Array(numberOfEmptyColumns).fill(null);
    });

    /**
     * Emit the item clicked to the parent component
     */
    @Output() itemClicked = new EventEmitter<{ index: number; item: DotCategory }>();

    /**
     * Model of the items selected
     */
    itemsSelected: string[];

    readonly #destroyRef = inject(DestroyRef);

    constructor() {
        effect(() => {
            this.itemsSelected = this.selected();
        });
    }

    ngAfterViewInit() {
        // Handle the horizontal scroll to make visible the last column
        this.categoryColumns.changes.pipe(takeUntilDestroyed(this.#destroyRef)).subscribe(() => {
            this.scrollHandler();
        });
    }

    private scrollHandler() {
        const columnsArray = this.categoryColumns.toArray();
        if (
            columnsArray[MINIMUM_CATEGORY_WITHOUT_SCROLLING] &&
            columnsArray[MINIMUM_CATEGORY_WITHOUT_SCROLLING].nativeElement.children.length > 0
        ) {
            columnsArray[columnsArray.length - 1].nativeElement.scrollIntoView({
                behavior: 'smooth',
                block: 'start',
                inline: 'start'
            });
        }
    }
}
