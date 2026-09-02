import { Subject } from 'rxjs';

import {
    AfterViewInit,
    ChangeDetectionStrategy,
    Component,
    computed,
    effect,
    ElementRef,
    inject,
    input,
    OnDestroy,
    output,
    signal,
    ViewChild
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { SkeletonModule } from 'primeng/skeleton';
import { TableModule, TableRowSelectEvent, TableRowUnSelectEvent } from 'primeng/table';
import { TooltipModule } from 'primeng/tooltip';

import { debounceTime } from 'rxjs/operators';

import { DotMessageService } from '@dotcms/data-access';
import { ComponentStatus } from '@dotcms/dotcms-models';
import { DotEmptyContainerComponent, DotMessagePipe, PrincipalConfiguration } from '@dotcms/ui';

import { CATEGORY_FIELD_EMPTY_MESSAGES } from '../../../../models/dot-edit-content-field.constant';
import {
    DotCategoryFieldKeyValueObj,
    DotTableHeaderCheckboxSelectEvent
} from '../../models/dot-category-field.models';
import { DotTableSkeletonComponent } from '../dot-table-skeleton/dot-table-skeleton.component';

/**
 * Represents a search list component for category field.
 */
@Component({
    selector: 'dot-category-field-search-list',
    imports: [
        TableModule,
        SkeletonModule,
        DotTableSkeletonComponent,
        DotMessagePipe,
        TooltipModule,
        DotEmptyContainerComponent
    ],
    templateUrl: './dot-category-field-search-list.component.html',
    host: {
        '[class.category-field__search-list--empty]': '$tableIsEmpty()'
    },
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotCategoryFieldSearchListComponent implements AfterViewInit, OnDestroy {
    /**
     * Represents a reference to a table container element in the DOM to calculate the
     * viewport to use in the virtual scroll
     */
    @ViewChild('tableContainer', { static: false }) tableContainer!: ElementRef;

    /**
     * The scrollHeight variable represents a signal with a default value of '0px'.
     * It can be used to track and manipulate the height of a scrollable element.
     */
    $scrollHeight = signal<string>('0px');

    /**
     * Represents the categories found with the filter
     */
    $categories = input.required<DotCategoryFieldKeyValueObj[]>({ alias: 'categories' });

    /**
     * Represent the selected items in the store
     */
    $selected = input.required<DotCategoryFieldKeyValueObj[]>({ alias: 'selected' });

    /**
     * Represents the current state of the component.
     */
    $state = input.required<ComponentStatus>({ alias: 'state' });

    /**
     * Output for emit the selected category(ies).
     */
    itemChecked = output<DotCategoryFieldKeyValueObj | DotCategoryFieldKeyValueObj[]>();

    /**
     * Output that emits events to remove a selected item(s).
     */
    removeItem = output<string | string[]>();

    /**
     * Model of the items selected
     */
    itemsSelected: DotCategoryFieldKeyValueObj[];

    /**
     * Represents an array of temporary selected items.
     */
    temporarySelectedAll: string[] = [];

    /**
     * Flag indicating whether the table is empty.
     */
    $tableIsEmpty = computed(() => !this.$isLoading() && this.$categories().length === 0);

    /**
     * A computed variable that represents the loading state of a component.
     */
    $isLoading = computed(() => this.$state() === ComponentStatus.LOADING);

    /**
     * Gets the computed value of $emptyOrErrorMessage.
     */
    $emptyOrErrorMessage = computed(() => this.getMessageConfig());

    #messageService = inject(DotMessageService);

    readonly #effectRef = effect(() => {
        // Todo: find a better way to update this
        this.itemsSelected = this.$selected();
    });
    readonly #resize$ = new Subject<ResizeObserverEntry>();
    readonly #resizeObserver = new ResizeObserver((entries) => this.#resize$.next(entries[0]));

    constructor() {
        this.#resize$.pipe(debounceTime(500), takeUntilDestroyed()).subscribe(() => {
            this.setTableScrollHeight();
        });
    }

    /**
     * This method is called when an item is selected.
     *
     * @param {TableRowSelectEvent<DotCategoryFieldKeyValueObj>} $event - The event object containing the selected item data.
     * @return {void}
     */
    onSelectItem({ data }: TableRowSelectEvent<DotCategoryFieldKeyValueObj>): void {
        this.itemChecked.emit(data as DotCategoryFieldKeyValueObj);
    }

    /**
     * Removes an item from the list.
     *
     * @param {TableRowUnSelectEvent<DotCategoryFieldKeyValueObj>} $event - The event that triggered the item removal.
     * @return {void}
     */
    onRemoveItem({ data }: TableRowUnSelectEvent<DotCategoryFieldKeyValueObj>): void {
        this.removeItem.emit((data as DotCategoryFieldKeyValueObj).key);
    }

    /**
     * Handles the event when the header checkbox is toggled.
     *
     * @param {DotTableHeaderCheckboxSelectEvent} event - The event triggered when the header checkbox is toggled.
     *
     * @return {void}
     */
    onHeaderCheckboxToggle({ checked }: DotTableHeaderCheckboxSelectEvent): void {
        if (checked) {
            const values = this.$categories().map((item) => item.key);
            this.itemChecked.emit(this.$categories());
            this.temporarySelectedAll = [...values];
        } else {
            this.removeItem.emit(this.temporarySelectedAll);
            this.temporarySelectedAll = [];
        }
    }

    ngAfterViewInit(): void {
        this.setTableScrollHeight();
        this.#resizeObserver.observe(this.tableContainer.nativeElement);
    }

    ngOnDestroy() {
        this.#effectRef.destroy();
        this.#resizeObserver.unobserve(this.tableContainer.nativeElement);
    }

    /**
     * Calculate the high of the container for the virtual scroll
     * @private
     */
    private setTableScrollHeight() {
        if (this.tableContainer) {
            const containerHeight = this.tableContainer.nativeElement.clientHeight;
            this.$scrollHeight.set(`${containerHeight}px`);
        }
    }

    /**
     * Retrieves the message configuration based on the current component state.
     *
     * @private
     * @returns {PrincipalConfiguration | null} Returns the message configuration, or null if no configuration is found.
     */
    private getMessageConfig(): PrincipalConfiguration | null {
        const configKey =
            this.$state() === ComponentStatus.ERROR ? ComponentStatus.ERROR : 'noResults';
        const { title, icon, subtitle } = CATEGORY_FIELD_EMPTY_MESSAGES[configKey];

        return {
            title: this.#messageService.get(title),
            icon: icon,
            subtitle: this.#messageService.get(subtitle)
        };
    }
}
