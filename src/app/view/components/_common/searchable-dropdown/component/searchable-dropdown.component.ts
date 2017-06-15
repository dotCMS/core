import {
        Component,
        ElementRef,
        EventEmitter,
        Input,
        NgZone,
        Output,
        ViewChild,
        ViewEncapsulation,
        forwardRef
    } from '@angular/core';
import { DataTableColumn } from '../../listing-data-table/listing-data-table-component';
import { Observable } from 'rxjs/Rx';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import { OverlayPanel } from 'primeng/primeng';

/**
 * Dropdown with pagination and global search
 * @export
 * @class SearchableDropdownComponent
 * @implements {ControlValueAccessor}
 */
@Component({
    encapsulation: ViewEncapsulation.Emulated,
    providers: [
        {
            multi: true,
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => SearchableDropdownComponent)
        }
    ],
    selector: 'searchable-dropdown',
    styles: [require('./searchable-dropdown.component.scss')],
    templateUrl: './searchable-dropdown.component.html',
})
export class SearchableDropdownComponent implements ControlValueAccessor {

    @Input() data: string[];
    @Input() totalRecords: number;
    @Input() rows: number;
    @Input() pageLinkSize = 3;
    @Input() labelPropertyName;

    @ViewChild('searchInput') inputElRef: ElementRef;
    @ViewChild('searchPanel') searchPanelRef: OverlayPanel;

    @Output() filterChange: EventEmitter<string> = new EventEmitter();
    @Output() pageChange: EventEmitter<PaginationEvent> = new EventEmitter();
    @Output() change: EventEmitter<any> = new EventEmitter();

    public value: any = {};
    private valueString = '';
    propagateChange = (_: any) => {};

    ngOnInit(): void {
        Observable.fromEvent(this.inputElRef.nativeElement, 'keyup')
            .debounceTime(500)
            .subscribe((keyboardEvent: Event) => this.filterChange.emit(keyboardEvent.target['value']));
    }

    /**
     * Call when the current page is changed
     * @param {any} event
     * @memberof SearchableDropdownComponent
     */
    paginate(event): void {
        let paginationEvent = Object.assign({}, event);
        paginationEvent.filter = this.inputElRef.nativeElement.value;
        this.pageChange.emit(paginationEvent);
    }

    /**
     * Write a new value to the element
     * @param {*} value
     * @memberof SearchableDropdownComponent
     */
    writeValue(value: any): void {
        this.value = value;
        this.valueString = value ? value[this.labelPropertyName] : '';
    }

    /**
     * Set the function to be called when the control receives a change event.
     * @param {any} fn
     * @memberof SearchableDropdownComponent
     */
    registerOnChange(fn): void {
        this.propagateChange = fn;
    }

    registerOnTouched(): void {}

    /**
     * Call when a option is clicked, if this option is not the same of the current value then
     * the change events is emitted
     * @private
     * @param {*} item
     * @memberof SearchableDropdownComponent
     */
    private handleClick(item: any): void {
        if (this.value !== item) {
            this.value = item;
            this.valueString = item[this.labelPropertyName];
            this.propagateChange(item);
            this.change.emit(Object.assign({}, this.value));
        }

        this.searchPanelRef.hide();
    }
}

export interface PaginationEvent {
    first: number; // Index of the first record
    filter: string;
    page: number; // Index of the new page
    pageCount: number; // Total number of pages
    rows: number; // Number of rows to display in new page
}