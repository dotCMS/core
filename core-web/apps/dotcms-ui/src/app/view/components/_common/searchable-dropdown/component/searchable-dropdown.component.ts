import { fromEvent } from 'rxjs';

import { NgTemplateOutlet } from '@angular/common';
import {
    AfterContentInit,
    AfterViewInit,
    ChangeDetectorRef,
    Component,
    ContentChildren,
    ElementRef,
    EventEmitter,
    forwardRef,
    Input,
    OnChanges,
    Output,
    QueryList,
    SimpleChange,
    SimpleChanges,
    TemplateRef,
    ViewChild,
    inject,
    ChangeDetectionStrategy
} from '@angular/core';
import { ControlValueAccessor, FormsModule, NG_VALUE_ACCESSOR } from '@angular/forms';

import { PrimeTemplate } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { DataView, DataViewLazyLoadEvent, DataViewModule } from 'primeng/dataview';
import { InputTextModule } from 'primeng/inputtext';
import { Popover, PopoverModule } from 'primeng/popover';

import { debounceTime, distinctUntilChanged, map, tap } from 'rxjs/operators';

import { DotIconComponent, DotMessagePipe } from '@dotcms/ui';

/**
 * Dropdown with pagination and global search
 * @export
 * @class SearchableDropdownComponent
 * @implements {ControlValueAccessor}
 */
/**
 * A dropdown row.
 *
 * The component reads whichever properties `labelPropertyName` and `valuePropertyName` name, so a
 * row is only ever known by key — never by a fixed shape.
 */
type SearchableDropdownRow = Record<string, unknown>;

@Component({
    providers: [
        {
            multi: true,
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => SearchableDropdownComponent)
        }
    ],
    selector: 'dot-searchable-dropdown',
    styleUrls: ['./searchable-dropdown.component.scss'],
    templateUrl: './searchable-dropdown.component.html',
    changeDetection: ChangeDetectionStrategy.Eager,
    imports: [
        FormsModule,
        ButtonModule,
        DataViewModule,
        InputTextModule,
        PopoverModule,
        DotIconComponent,
        DotMessagePipe,
        NgTemplateOutlet
    ]
})
export class SearchableDropdownComponent
    implements ControlValueAccessor, OnChanges, AfterContentInit, AfterViewInit
{
    private cd = inject(ChangeDetectorRef);

    @Input()
    data!: Record<string, unknown>[];

    @Input() action?: (event: Event) => void;

    @Input()
    labelPropertyName!: string | string[];

    @Input()
    valuePropertyName!: string;

    @Input()
    pageLinkSize = 3;

    @Input()
    rows!: number;

    @Input()
    cssClass!: string;

    @Input()
    cssClassDataList!: string;

    @Input()
    totalRecords!: number;

    @Input()
    placeholder = '';

    @Input()
    persistentPlaceholder!: boolean;

    /**
     * Sets the width of the searchable-dropdown button
     *
     * The CSS unit is **required**.
     * @memberof SearchableDropdownComponent
     */
    @Input()
    width = '300px';

    /**
     * Sets the width of the searchable-dropdown overlay
     *
     * The CSS unit is **required**.
     * @memberof SearchableDropdownComponent
     */
    @Input()
    overlayWidth = '300px';

    @Input()
    multiple!: boolean;

    @Input()
    disabled = false;

    @Input()
    externalItemListTemplate!: TemplateRef<unknown>;

    @Input()
    externalFilterTemplate!: TemplateRef<unknown>;

    @Output()
    switch: EventEmitter<unknown> = new EventEmitter();

    @Output()
    filterChange: EventEmitter<string> = new EventEmitter();

    @Output()
    hide: EventEmitter<unknown> = new EventEmitter();

    @Output()
    pageChange: EventEmitter<PaginationEvent> = new EventEmitter();

    @Output()
    display: EventEmitter<unknown> = new EventEmitter();

    @ViewChild('searchInput', { static: false })
    searchInput!: ElementRef;

    @ViewChild('searchPanel', { static: true })
    searchPanelRef!: Popover;

    @ViewChild('dataView', { static: true })
    dataViewRef!: DataView;

    @ViewChild('button')
    button!: ElementRef;

    @ContentChildren(PrimeTemplate) templates!: QueryList<PrimeTemplate>;

    valueString = '';
    /** Null until a row is picked, which is also what `writeValue(null)` sets. */
    value: SearchableDropdownRow | null = null;
    overlayPanelMinHeight!: string;
    options: SearchableDropdownRow[] = [];
    label: string | null = null;
    externalSelectTemplate!: TemplateRef<unknown>;

    /** Null while the overlay is closed — `hideOverlayHandler` clears it. */
    selectedOptionIndex: number | null = 0;
    selectedOptionValue = '';

    propagateChange = (_: unknown) => {
        /**/
    };

    ngOnChanges(changes: SimpleChanges): void {
        if (this.usePlaceholder(changes['placeholder']) || changes['persistentPlaceholder']) {
            this.setLabel();
        }

        this.setOptions(changes);
        this.totalRecords = this.totalRecords || this.data?.length;
    }

    ngAfterViewInit(): void {
        if (this.searchInput) {
            fromEvent<KeyboardEvent>(this.searchInput.nativeElement, 'keyup')
                .pipe(
                    tap((keyboardEvent) => {
                        if (
                            keyboardEvent.key === 'ArrowUp' ||
                            keyboardEvent.key === 'ArrowDown' ||
                            keyboardEvent.key === 'Enter'
                        ) {
                            this.selectDropdownOption(keyboardEvent.key);
                        }
                    }),
                    map(
                        (keyboardEvent: KeyboardEvent) =>
                            (keyboardEvent.target as HTMLInputElement).value
                    ),
                    distinctUntilChanged(),
                    debounceTime(500)
                )
                .subscribe((value: string) => {
                    this.filterChange.emit(value);
                });
        }
    }

    ngAfterContentInit() {
        this.totalRecords = this.totalRecords || this.data?.length;
        this.templates.forEach((item: PrimeTemplate) => {
            if (item.getType() === 'list') {
                this.externalItemListTemplate = item.template;
            } else if (item.getType() === 'select') {
                this.externalSelectTemplate = item.template;
            }
        });
    }

    /**
     * Emits hide event and clears any value on filter's input
     *
     * @memberof SearchableDropdownComponent
     */
    hideOverlayHandler(): void {
        if (this.searchInput?.nativeElement.value.length) {
            this.searchInput.nativeElement.value = '';
            this.paginate(null);
        }

        this.hide.emit();
        this.selectedOptionIndex = null;
    }

    /**
     * Emits show event, sets height of overlay panel based on content
     * and add css class if paginator present
     *
     * @memberof SearchableDropdownComponent
     */
    showOverlayHandler(): void {
        const cssClass =
            this.totalRecords > this.rows
                ? ' searchable-dropdown paginator'
                : ' searchable-dropdown';
        if (typeof this.cssClass === 'undefined') {
            this.cssClass = cssClass;
        } else {
            this.cssClass += cssClass;
        }

        setTimeout(() => {
            // `container` is only set while the popover is mounted, and this runs a tick later.
            const container = this.searchPanelRef.container;

            if (!this.overlayPanelMinHeight && container) {
                this.overlayPanelMinHeight = container.getBoundingClientRect().height.toString();
            }
        }, 0);
        this.display.emit();
        this.dataViewRef.paginate({
            first: 0,
            rows: this.rows
        });
    }

    /**
     * Call when the current page is changed
     *
     * @param {PaginationEvent} event
     * @memberof SearchableDropdownComponent
     */
    paginate(event: DataViewLazyLoadEvent | null): void {
        const paginationEvent = {
            first: event?.first ?? 0,
            rows: event?.rows ?? this.rows,
            filter: ''
        };
        if (this.searchInput) {
            paginationEvent.filter = this.searchInput.nativeElement.value;
        }

        this.pageChange.emit(paginationEvent as PaginationEvent);
    }

    /**
     * Write a new value to the element
     * @param * value
     * @memberof SearchableDropdownComponent
     */
    writeValue(value: SearchableDropdownRow | null): void {
        this.setValue(value);
    }

    /**
     * Set the function to be called when the control receives a change event.
     *
     * @param {*} fn
     * @memberof SearchableDropdownComponent
     */
    registerOnChange(fn: (value: unknown) => void): void {
        this.propagateChange = fn;
    }

    registerOnTouched(): void {
        /**/
    }

    /**
     * Get labels from container, if labelPropertyName is an array then loop through it and returns
     * a string containing the labels joining by "-" if is not just returns a label
     *
     * @param {*} dropDownItem
     * @returns {string}
     * @memberof SearchableDropdownComponent
     */
    getItemLabel(dropDownItem: SearchableDropdownRow | null | undefined): string {
        if (!dropDownItem) {
            return '';
        }

        if (Array.isArray(this.labelPropertyName)) {
            const resultProps = this.labelPropertyName.map((item) => {
                if (item.indexOf('.') > -1) {
                    let propertyName: unknown;
                    item.split('.').forEach((nested) => {
                        propertyName = propertyName
                            ? (propertyName as SearchableDropdownRow)[nested]
                            : dropDownItem[nested];
                    });

                    return propertyName;
                }

                return dropDownItem[item];
            });

            return resultProps.join(' - ');
        }

        return String(dropDownItem[`${this.labelPropertyName}`] ?? '');
    }

    /**
     * Call when a option is clicked, if this option is not the same of the current value then the
     * change events is emitted. If multiple is true allow to emit the same value.
     *
     * @param {*} item
     * @memberof SearchableDropdownComponent
     */
    handleClick(item: SearchableDropdownRow): void {
        if (this.value !== item || this.multiple) {
            this.setValue(item);
            this.propagateChange(this.getValueToPropagate());
            this.switch.emit(Object.assign({}, this.value));
        }

        this.toggleOverlayPanel();
    }

    /**
     * Shows or hide the list of options.
     *
     * @param {MouseEvent} [$event]
     * @memberof SearchableDropdownComponent
     */
    toggleOverlayPanel($event?: MouseEvent): void {
        $event ? this.searchPanelRef.toggle($event) : this.searchPanelRef.hide();
    }

    /**
     * Disabled the component, for more information see:
     * {@link https://angular.io/api/forms/ControlValueAccessor#setdisabledstate}
     *
     * @param {boolean} isDisabled if it is true the component is disabled
     * @memberof SearchableDropdownComponent
     */
    setDisabledState(isDisabled: boolean): void {
        this.disabled = isDisabled;
    }

    /**
     * Resets height value from Overlay Panel
     *
     * @memberof SearchableDropdownComponent
     */
    resetPanelMinHeight(): void {
        this.overlayPanelMinHeight = '';
    }

    private selectDropdownOption(actionKey: string) {
        const itemsCount = this.rows
            ? this.rows <= this.options.length
                ? this.rows
                : this.options.length
            : this.options.length;
        const index = this.selectedOptionIndex;

        if (index === null) {
            return;
        }

        if (actionKey === 'ArrowDown' && itemsCount - 1 > index) {
            this.selectedOptionIndex = index + 1;
            this.selectedOptionValue = this.getItemLabel(this.options[index + 1]);
        } else if (actionKey === 'ArrowUp' && 0 < index) {
            this.selectedOptionIndex = index - 1;
            this.selectedOptionValue = this.getItemLabel(this.options[index - 1]);
        } else if (actionKey === 'Enter') {
            this.handleClick(this.options[index]);
        }

        this.cd.detectChanges();
    }

    private setLabel(): void {
        // Cast rather than coerced: the template's `[class.selected]` compares
        // `item[getValueLabelPropertyName()]` against this, so both sides must read the property
        // the same way — including when no `labelPropertyName` is configured and both are
        // `undefined`. Coercing this side to `''` makes that comparison false and the selected row
        // loses its class.
        this.valueString = this.value
            ? (this.value[this.getValueLabelPropertyName()] as string)
            : this.placeholder;
        this.label = this.persistentPlaceholder ? this.placeholder : this.valueString;
        this.cd.markForCheck();
    }

    private setOptions(change: SimpleChanges): void {
        if (change['data'] && change['data'].currentValue) {
            this.options = (
                structuredClone(change['data'].currentValue) as SearchableDropdownRow[]
            ).map((item) => {
                item['label'] = this.getItemLabel(item);

                return item;
            });
            this.selectedOptionValue = this.getItemLabel(this.options[0]);
            this.selectedOptionIndex = 0;
        }
    }

    private usePlaceholder(placeholderChange: SimpleChange): boolean {
        return placeholderChange && placeholderChange.currentValue && !this.value;
    }

    private setValue(newValue: SearchableDropdownRow | null): void {
        this.value = newValue;

        this.setLabel();
    }

    public getValueLabelPropertyName(): string {
        return Array.isArray(this.labelPropertyName)
            ? this.labelPropertyName[0]
            : this.labelPropertyName;
    }

    private getValueToPropagate() {
        return !this.valuePropertyName ? this.value : this.value?.[this.valuePropertyName];
    }
}

export interface PaginationEvent {
    first: number; // Index of the first record
    filter: string;
    page: number; // Index of the new page
    pageCount: number; // Total number of pages
    rows: number; // Number of rows to display in new page
}
