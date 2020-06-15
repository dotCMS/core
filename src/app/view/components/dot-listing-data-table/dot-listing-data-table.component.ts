import {
    Component,
    Input,
    Output,
    EventEmitter,
    OnChanges,
    ViewChild,
    ElementRef,
    OnInit
} from '@angular/core';
import { LazyLoadEvent } from 'primeng/primeng';
import { Table } from 'primeng/table';
import { ActionHeaderOptions, ButtonAction } from '@models/action-header';
import { DataTableColumn } from '@models/data-table/data-table-column';
import { LoggerService } from 'dotcms-js';
import { FormatDateService } from '@services/format-date-service';
import { PaginatorService, OrderDirection } from '@services/paginator';
import { DotDataTableAction } from '@models/data-table/dot-data-table-action';

@Component({
    providers: [PaginatorService],
    selector: 'dot-listing-data-table',
    styleUrls: ['./dot-listing-data-table.component.scss'],
    templateUrl: 'dot-listing-data-table.component.html'
})
export class DotListingDataTableComponent implements OnChanges, OnInit {
    @Input() columns: DataTableColumn[];
    @Input() url: string;
    @Input() actionHeaderOptions: ActionHeaderOptions;
    @Input() buttonActions: ButtonAction[] = [];
    @Input() sortOrder: string;
    @Input() sortField: string;
    @Input() multipleSelection = false;
    @Input() paginationPerPage = 40;
    @Input() actions: DotDataTableAction[];

    @Output() rowWasClicked: EventEmitter<any> = new EventEmitter();

    @ViewChild('gf') globalSearch: ElementRef;
    @ViewChild('dataTable') dataTable: Table;

    readonly DATE_FORMAT = 'date';

    items: any[];
    filter;
    dateColumns: DataTableColumn[];
    loading = true;

    constructor(
        public loggerService: LoggerService,
        public paginatorService: PaginatorService,
        private formatDateService: FormatDateService
    ) {
        this.paginatorService.url = this.url;
    }

    ngOnChanges(changes): void {
        if (changes.url && changes.url.currentValue) {
            this.paginatorService.url = changes.url.currentValue;
        }

        if (changes.columns && changes.columns.currentValue) {
            this.dateColumns = changes.columns.currentValue.filter(
                column => column.format === this.DATE_FORMAT
            );
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
        this.loading = true;
        if (this.columns) {
            const sortField = sortFieldParam || this.sortField;
            const sortOrder = sortOrderParam || this.sortOrder;

            this.paginatorService.filter = this.filter;
            this.paginatorService.sortField = sortField;
            this.paginatorService.sortOrder =
                sortOrder === 1 ? OrderDirection.ASC : OrderDirection.DESC;

            this.paginatorService.getWithOffset(offset).subscribe(items => this.setItems(items));
        }
    }

    /**
     * Load first page of results and reset the pagination url's and set the table pagination.
     *
     * @memberof ListingDataTableComponent
     */
    loadFirstPage(): void {
        this.loading = true;
        this.paginatorService.get().subscribe(items => {
            this.setItems(items);
            this.dataTable.first = 1;
        });
    }

    /**
     * Reloads data table with updated data on current page
     * @memberof ListingDataTableComponent
     */
    loadCurrentPage(): void {
        this.loading = true;
        if (this.columns) {
            this.paginatorService.getCurrentPage().subscribe(items => this.setItems(items));
        }
    }

    /**
     * Column align, return the DataTableColumn's textAlign property if it exists,
     * otherwise return right if the content is number and left if the content's type is not number.
     * @param DataTableColumn col
     * @returns string
     * @memberof ListingDataTableComponent
     */
    getAlign(col: DataTableColumn): string {
        return col.textAlign
            ? col.textAlign
            : this.items && this.items[0] && typeof this.items[0][col.fieldName] === 'number'
              ? 'right'
              : 'left';
    }

    /**
     * Focus the fist row of the table is there are results.
     * @memberof ListingDataTableComponent
     */
    focusFirstRow(): void {
        const rows: HTMLTableRowElement[] = this.dataTable.tableViewChild.nativeElement.rows;
        if (rows.length > 1) {
            rows[1].focus();
        }
    }

    private formatData(items: any[]): any[] {
        return items.map(item => {
            this.dateColumns.forEach(
                col =>
                    (item[col.fieldName] = this.formatDateService.getRelative(item[col.fieldName]))
            );
            return item;
        });
    }

    private setItems(items: any[]): void {
        this.items = this.dateColumns ? this.formatData(items) : items;
        this.loading = false;
    }
}
