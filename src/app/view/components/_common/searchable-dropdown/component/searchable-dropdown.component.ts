import { DotContainer } from './../../../../../shared/models/container/dot-container.model';
import {
    Component,
    ElementRef,
    EventEmitter,
    Input,
    Output,
    ViewChild,
    ViewEncapsulation,
    forwardRef,
    SimpleChanges,
    OnChanges,
    OnInit
} from '@angular/core';
import { BaseComponent } from '../../_base/base-component';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import { MessageService } from '../../../../../api/services/messages-service';
import { Observable } from 'rxjs/Rx';
import { OverlayPanel } from 'primeng/primeng';

/**
 * Dropdown with pagination and global search
 * @export
 * @extends {BaseComponent}
 * @class SearchableDropdownComponent
 * @implements {ControlValueAccessor}
 */
@Component({
    encapsulation: ViewEncapsulation.None,
    providers: [
        {
            multi: true,
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => SearchableDropdownComponent)
        }
    ],
    selector: 'searchable-dropdown',
    styleUrls: ['./searchable-dropdown.component.scss'],
    templateUrl: './searchable-dropdown.component.html'
})
export class SearchableDropdownComponent extends BaseComponent implements ControlValueAccessor, OnChanges, OnInit {
    @Input() data: string[];
    @Input() labelPropertyName: string | string[];
    @Input() valuePropertyName: string;
    @Input() pageLinkSize = 3;
    @Input() rows: number;
    @Input() totalRecords: number;
    @Input() placeholder = '';
    @Input() persistentPlaceholder: boolean;
    @Input() width: string;
    @Output() change: EventEmitter<any> = new EventEmitter();
    @Output() filterChange: EventEmitter<string> = new EventEmitter();
    @Output() hide: EventEmitter<any> = new EventEmitter();
    @Output() pageChange: EventEmitter<PaginationEvent> = new EventEmitter();
    @Output() show: EventEmitter<any> = new EventEmitter();

    @ViewChild('searchInput') searchInput: ElementRef;
    @ViewChild('searchPanel') searchPanelRef: OverlayPanel;

    value: any = {};
    valueString = '';

    constructor(messageService: MessageService) {
        super(['search'], messageService);
    }

    propagateChange = (_: any) => {};

    ngOnChanges(change: SimpleChanges): void {
        if (change.placeholder && change.placeholder.currentValue) {
            this.valueString = change.placeholder.currentValue;
        }
    }

    ngOnInit(): void {
        Observable.fromEvent(this.searchInput.nativeElement, 'keyup')
            .debounceTime(500)
            .subscribe((keyboardEvent: Event) => {
                this.filterChange.emit(keyboardEvent.target['value']);
            });
    }

    hideDialogHandler(): void {
        if (this.searchInput.nativeElement.value.length) {
            this.searchInput.nativeElement.value = '';
            this.paginate({});
        }
        this.hide.emit();
    }

    /**
     * Call when the current page is changed
     * @param {any} event
     * @memberof SearchableDropdownComponent
     */
    paginate(event): void {
        const paginationEvent = Object.assign({}, event);
        paginationEvent.filter = this.searchInput.nativeElement.value;
        this.pageChange.emit(paginationEvent);
    }

    /**
     * Write a new value to the element
     * @param {*} value
     * @memberof SearchableDropdownComponent
     */
    writeValue(value: any): void {
        this.value = value;
        if (Array.isArray(this.labelPropertyName)) {
            this.valueString = value ? value[this.labelPropertyName[0]] : this.placeholder;
        } else {
            this.valueString = value ? value[this.labelPropertyName] : this.placeholder;
        }
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
     * Get labels from container, if labelPropertyName is an array then loop through it and returns
     * a string containing the labels joining by "-" if is not just returns a label
     * @param {DotContainer} container
     * @returns {string}
     * @memberof SearchableDropdownComponent
     */
    getLabel(container: DotContainer): string {
        let resultProps;

        if (Array.isArray(this.labelPropertyName)) {
            resultProps = this.labelPropertyName.map(item => {
                if (item.indexOf('.') > -1) {
                    let propertyName;
                    item.split('.').forEach(nested => {
                        propertyName = propertyName ? propertyName[nested] : container[nested];
                    });

                    return propertyName;
                }

                return container[item];
            });

            return resultProps.join(' - ');
        } else {
            return container[this.labelPropertyName];
        }
    }

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
            if (Array.isArray(this.labelPropertyName)) {
                this.valueString = item[this.labelPropertyName[0]];
            } else {
                this.valueString = item[this.labelPropertyName];
            }
            this.propagateChange(!this.valuePropertyName ? item : item[this.valuePropertyName]);
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
