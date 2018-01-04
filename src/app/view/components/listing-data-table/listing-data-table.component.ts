import {
    Component,
    Input,
    Output,
    EventEmitter,
    OnChanges,
    ViewChild,
    ElementRef,
    OnInit,
    ViewEncapsulation
} from '@angular/core';
import { LazyLoadEvent } from 'primeng/primeng';
import { ActionHeaderOptions, ButtonAction } from '../../../shared/models/action-header';
import { BaseComponent } from '../_common/_base/base-component';
import { DataTableColumn } from '../../../shared/models/data-table/data-table-column';
import { LoggerService } from 'dotcms-js/dotcms-js';
import { FormatDateService } from '../../../api/services/format-date-service';
import { DotMessageService } from '../../../api/services/dot-messages-service';
import { PaginatorService, OrderDirection } from '../../../api/services/paginator';
import { DotDataTableAction } from '../../../shared/models/data-table/dot-data-table-action';

@Component({
    providers: [PaginatorService],
    selector: 'listing-data-table',
    styleUrls: ['./listing-data-table.component.scss'],
    templateUrl: 'listing-data-table.component.html'
})
export class ListingDataTableComponent extends BaseComponent implements OnChanges, OnInit {
    @Input() columns: DataTableColumn[];
    @Input() url: string;
    @Input() actionHeaderOptions: ActionHeaderOptions;
    @Input() buttonActions: ButtonAction[] = [];
    @Input() sortOrder: number;
    @Input() sortField: string;
    @Input() multipleSelection = false;
    @Input() paginationPerPage: number;
    @Input() actions: DotDataTableAction[];

    @Output() rowWasClicked: EventEmitter<any> = new EventEmitter();

    @ViewChild('gf') globalSearch: ElementRef;

    readonly DATE_FORMAT = 'date';

    items: any[];
    filter;
    dateColumns: DataTableColumn[];

    constructor(
        messageService: DotMessageService,
        public loggerService: LoggerService,
        private paginatorService: PaginatorService,
        private formatDateService: FormatDateService
    ) {
        super(['global-search'], messageService);
        this.paginatorService.url = this.url;
    }

    ngOnChanges(changes): void {
        if (changes.url && changes.url.currentValue) {
            this.paginatorService.url = changes.url.currentValue;
        }

        if (changes.columns && changes.columns.currentValue) {
            this.dateColumns = changes.columns.currentValue.filter(column => column.format === this.DATE_FORMAT);
            this.loadData(0);
        }

        if (changes.paginationPerPage && changes.paginationPerPage.currentValue) {
            this.paginatorService.paginationPerPage = this.paginationPerPage;
        }
    }

    ngOnInit(): void {
        this.globalSearch.nativeElement.focus();
    }

    handleRowClick($event): void {
        this.rowWasClicked.emit($event);
    }

    /**
     * Call when click on any pagination link
     * @param event Pagination event
     */
    loadDataPaginationEvent(event: LazyLoadEvent): void {
        this.loadData(event.first, event.sortField, event.sortOrder);
    }

    loadData(offset: number, sortFieldParam?: string, sortOrderParam?: OrderDirection): void {
        if (this.columns) {
            const sortField = sortFieldParam || this.sortField;
            const sortOrder = sortOrderParam || this.sortOrder;

            this.paginatorService.filter = this.filter;
            this.paginatorService.sortField = sortField;
            this.paginatorService.sortOrder = sortOrder === 1 ? OrderDirection.ASC : OrderDirection.DESC;

            this.paginatorService.getWithOffset(offset).subscribe(items => this.setItems(items));
        }
    }

    /**
     * Reloads data table with updated data on current page
     * @memberof ListingDataTableComponent
     */
    loadCurrentPage(): void {
        if (this.columns) {
            this.paginatorService.getCurrentPage().subscribe(items => this.setItems(items));
        }
    }

    /**
     * Column align, return the DataTableColumn's textAlign property if it exists,
     * otherwise return right if the content is number and left if the content's type is not number.
     * @param {DataTableColumn} col
     * @returns {string}
     * @memberof ListingDataTableComponent
     */
    getAlign(col: DataTableColumn): string {
        return col.textAlign
            ? col.textAlign
            : this.items && this.items[0] && typeof this.items[0][col.fieldName] === 'number' ? 'right' : 'left';
    }

    private formatData(items: any[]): any[] {
        return items.map(item => {
            this.dateColumns.forEach(
                col => (item[col.fieldName] = this.formatDateService.getRelative(item[col.fieldName]))
            );
            return item;
        });
    }

    private setItems(items: any[]): void {
        this.items = this.dateColumns ? this.formatData(items) : items;
    }
}
