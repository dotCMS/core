import { CommonModule } from '@angular/common';
import {
    ChangeDetectionStrategy,
    Component,
    computed,
    effect,
    ElementRef,
    inject,
    input,
    model,
    output,
    signal,
    viewChildren
} from '@angular/core';
import { FormsModule } from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { CheckboxModule } from 'primeng/checkbox';
import { TreeModule } from 'primeng/tree';

import { DotMessageService } from '@dotcms/data-access';
import { ComponentStatus } from '@dotcms/dotcms-models';
import {
    DotEmptyContainerComponent,
    PrincipalConfiguration,
    DotCollapseBreadcrumbComponent
} from '@dotcms/ui';

import { CATEGORY_FIELD_EMPTY_MESSAGES } from '../../../../models/dot-edit-content-field.constant';
import { ROOT_CATEGORY_KEY } from '../../dot-edit-content-category-field.const';
import {
    DotCategoryFieldItem,
    DotCategoryFieldKeyValueObj
} from '../../models/dot-category-field.models';
import { DotCategoryFieldListSkeletonComponent } from '../dot-category-field-list-skeleton/dot-category-field-list-skeleton.component';

export const MINIMUM_CATEGORY_COLUMNS = 3;

const MINIMUM_CATEGORY_WITHOUT_SCROLLING = 3;

/**
 * Represents the Dot Category Field Category List component.
 * @class
 */
@Component({
    selector: 'dot-category-field-category-list',
    imports: [
        CommonModule,
        TreeModule,
        CheckboxModule,
        ButtonModule,
        FormsModule,
        DotCategoryFieldListSkeletonComponent,
        DotCollapseBreadcrumbComponent,
        DotEmptyContainerComponent
    ],
    templateUrl: './dot-category-field-category-list.component.html',
    styleUrl: './dot-category-field-category-list.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush,
    host: {
        class: 'category-list__wrapper'
    }
})
export class DotCategoryFieldCategoryListComponent {
    /**
     * Represents the DotMessageService instance.
     */
    readonly #dotMessageService = inject(DotMessageService);
    /**
     *  Represent the columns of categories
     */
    $categoryColumns = viewChildren<ElementRef<HTMLDivElement>>('categoryColumn');

    /**
     * Represents the variable 'categories' which is of type 'DotCategoryFieldCategory[][]'.
     */
    $categories = input<DotCategoryFieldKeyValueObj[][]>([], { alias: 'categories' });

    /**
     * Represent the selected item saved in the contentlet
     */
    $selected = model<string[]>([], { alias: 'selected' });

    /**
     * Generate the empty columns
     */
    $emptyColumns = computed(() => {
        const numberOfEmptyColumnsNeeded = Math.max(
            MINIMUM_CATEGORY_COLUMNS - this.$categories().length,
            0
        );

        return Array(numberOfEmptyColumnsNeeded).fill(null);
    });

    /**
     * Represents the breadcrumbs to display
     */
    $breadcrumbs = input<DotCategoryFieldKeyValueObj[]>([], { alias: 'breadcrumbs' });

    /**
     * Represents the current state of the component.
     */
    $state = input<ComponentStatus>(ComponentStatus.INIT, { alias: 'state' });

    stateList = ComponentStatus;

    $showMainSkeleton = computed(() => {
        const isInitialLoadingState = [ComponentStatus.INIT, ComponentStatus.LOADING].includes(
            this.$state()
        );
        const categoriesEmpty = this.$categories().length === 0;

        return isInitialLoadingState && categoriesEmpty;
    });

    /**
     * Represents the breadcrumbs menu to display
     *
     * @memberof DotCategoryFieldCategoryListComponent
     */
    $breadcrumbsMenu = computed(() => {
        const currentItems = this.$breadcrumbs().map((item, index) => {
            return {
                label: item.value,
                command: () => {
                    this.rowClicked.emit({ index, item });
                }
            };
        });

        return [
            {
                label: this.#dotMessageService.get(
                    'edit.content.category-field.category.root-name'
                ),
                command: () => {
                    this.rowClicked.emit({
                        index: 0,
                        item: { key: ROOT_CATEGORY_KEY, value: ROOT_CATEGORY_KEY }
                    });
                }
            },
            ...currentItems
        ];
    });

    /**
     * Emit the item clicked to the parent component
     */
    rowClicked = output<DotCategoryFieldItem>();

    /**
     * Emit the item checked or selected to the parent component
     */
    itemChecked = output<{
        selected: string[];
        item: DotCategoryFieldKeyValueObj;
    }>();

    $emptyState = signal<PrincipalConfiguration>({
        title: this.#dotMessageService.get(CATEGORY_FIELD_EMPTY_MESSAGES.empty.title),
        icon: CATEGORY_FIELD_EMPTY_MESSAGES.empty.icon,
        subtitle: this.#dotMessageService.get(CATEGORY_FIELD_EMPTY_MESSAGES.empty.subtitle)
    });

    constructor() {
        effect(() => {
            const columnsArray = this.$categoryColumns();
            this.scrollHandler(columnsArray);
        });
    }

    private scrollHandler(columnsArray: readonly ElementRef<HTMLDivElement>[]) {
        try {
            if (columnsArray.length === 0) {
                return;
            }

            const columnToScroll = columnsArray[MINIMUM_CATEGORY_WITHOUT_SCROLLING - 1];

            if (columnToScroll?.nativeElement?.childElementCount > 0) {
                const lastColumnIndex = columnsArray.length - 1;
                columnsArray[lastColumnIndex].nativeElement.scrollIntoView({
                    behavior: 'smooth',
                    block: 'end',
                    inline: 'nearest'
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
