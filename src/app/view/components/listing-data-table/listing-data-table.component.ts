import { BaseComponent } from '../_common/_base/base-component';
import { ActionHeaderOptions, ButtonAction } from './action-header/action-header';
import { Component, Input, Output } from '@angular/core';
import { CrudService } from '../../../api/services/crud';
import { DotcmsConfig } from '../../../api/services/system/dotcms-config';
import { FormatDateService } from '../../../api/services/format-date-service';
import { LazyLoadEvent } from 'primeng/primeng';
import { LoggerService } from '../../../api/services/logger.service';
import { MessageService } from '../../../api/services/messages-service';
import { EventEmitter } from '@angular/core';
import { PaginatorService, OrderDirection } from '../../../api/services/paginator';
@Component({
    providers: [PaginatorService],
    selector: 'listing-data-table',
    styles: [require('./listing-data-table.component.scss')],
    templateUrl: 'listing-data-table.component.html'
})
export class ListingDataTableComponent extends BaseComponent {

    @Input() columns: DataTableColumn[];
    @Input() url: string;
    @Input() actionHeaderOptions: ActionHeaderOptions;
    @Input() buttonActions: ButtonAction[] = [];
    @Input() sortOrder: number;
    @Input() sortField: string;
    @Output() rowWasClicked: EventEmitter<any> = new EventEmitter;

    readonly DATE_FORMAT = 'date';

    private paginatorRows: number;
    private paginatorLinks: number;
    private items: any[];
    private filter;
    // tslint:disable-next-line:no-unused-variable
    private selectedItems = [];
    private dateColumns: DataTableColumn[];

    constructor(private crudService: CrudService, messageService: MessageService, public loggerService: LoggerService,
                    private paginatorService: PaginatorService, private formatDateService: FormatDateService) {

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
            let sortField = sortFieldParam || this.sortField;
            let sortOrder = sortOrderParam || this.sortOrder;

            this.paginatorService.filter = this.filter;
            this.paginatorService.sortField = sortField;
            this.paginatorService.sortOrder = sortOrder === 1 ? OrderDirection.ASC : OrderDirection.DESC;

            this.paginatorService.getWithOffset(offset).subscribe(
                items => this.items = this.dateColumns ? this.formatData(items) : items
            );
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
        return col.textAlign ? col.textAlign :
            (this.items && this.items[0] && typeof this.items[0][col.fieldName] === 'number') ? 'right' : 'left';
    }

    private formatData(items: any[]): any[] {
        return items.map((item) => {
            this.dateColumns.forEach((col) => {
                item[col.fieldName] = this.formatDateService.getRelative(item[col.fieldName]);
            });
            return item;
        });
    }
}

export interface DataTableColumn {
    fieldName: string;
    format?: string;
    header: string;
    icon?: (any) => string;
    sortable?: boolean;
    textAlign?: string;
    width?: string;
}