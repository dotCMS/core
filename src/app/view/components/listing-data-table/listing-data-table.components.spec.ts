import { of as observableOf, Observable } from 'rxjs';
import { DotAlertConfirmService } from '@services/dot-alert-confirm/dot-alert-confirm.service';
import { IconButtonTooltipModule } from '../_common/icon-button-tooltip/icon-button-tooltip.module';
import { ActionMenuButtonComponent } from '../_common/action-menu-button/action-menu-button.component';
import { DotActionButtonComponent } from '../_common/dot-action-button/dot-action-button.component';
import { By } from '@angular/platform-browser';
import { ComponentFixture } from '@angular/core/testing';
import { CrudService } from '@services/crud/crud.service';
import { DOTTestBed } from '../../../test/dot-test-bed';
import { DataTableModule, SharedModule, MenuModule } from 'primeng/primeng';
import { DebugElement, SimpleChange } from '@angular/core';
import { FormatDateService } from '@services/format-date-service';
import { ListingDataTableComponent } from './listing-data-table.component';
import { DotMessageService } from '@services/dot-messages-service';
import { MockDotMessageService } from '../../../test/dot-message-service.mock';
import { RouterTestingModule } from '@angular/router/testing';
import { PaginatorService } from '@services/paginator';
import { ActionHeaderComponent } from './action-header/action-header';
import { DotDataTableAction } from '@models/data-table/dot-data-table-action';
import { DotMenuModule } from '../_common/dot-menu/dot-menu.module';
import { DotIconModule } from '../_common/dot-icon/dot-icon.module';
import { DotIconButtonModule } from '../_common/dot-icon-button/dot-icon-button.module';

describe('ListingDataTableComponent', () => {
    let comp: ListingDataTableComponent;
    let fixture: ComponentFixture<ListingDataTableComponent>;
    let de: DebugElement;
    let el: HTMLElement;

    beforeEach(() => {
        const messageServiceMock = new MockDotMessageService({
            'global-search': 'Global Serach'
        });

        DOTTestBed.configureTestingModule({
            declarations: [ActionHeaderComponent, DotActionButtonComponent, ListingDataTableComponent, ActionMenuButtonComponent],
            imports: [
                DataTableModule,
                SharedModule,
                RouterTestingModule.withRoutes([{ path: 'test', component: ListingDataTableComponent }]),
                IconButtonTooltipModule,
                MenuModule,
                DotMenuModule,
                DotIconModule,
                DotIconButtonModule
            ],
            providers: [
                { provide: DotMessageService, useValue: messageServiceMock },
                CrudService,
                FormatDateService,
                PaginatorService,
                DotAlertConfirmService
            ]
        });

        fixture = DOTTestBed.createComponent(ListingDataTableComponent);
        comp = fixture.componentInstance;
        de = fixture.debugElement.query(By.css('p-dataTable'));
        el = de.nativeElement;

        this.items = [
            { field1: 'item1-value1', field2: 'item1-value2', field3: 'item1-value3', field4: 'item1-value4' },
            { field1: 'item2-value1', field2: 'item2-value2', field3: 'item2-value3', field4: 'item1-value4' },
            { field1: 'item3-value1', field2: 'item3-value2', field3: 'item3-value3', field4: 'item1-value4' },
            { field1: 'item4-value1', field2: 'item4-value2', field3: 'item4-value3', field4: 'item1-value4' },
            { field1: 'item5-value1', field2: 'item5-value2', field3: 'item5-value3', field4: 'item1-value4' },
            { field1: 'item6-value1', field2: 'item6-value2', field3: 'item6-value3', field4: 'item1-value4' },
            { field1: 'item7-value1', field2: 'item7-value2', field3: 'item7-value3', field4: 'item1-value4' }
        ];

        this.paginatorService = fixture.debugElement.injector.get(PaginatorService);
        this.paginatorService.paginationPerPage = 4;
        this.paginatorService.maxLinksPage = 2;
        this.paginatorService.totalRecords = this.items.length;

        this.columns = [
            { fieldName: 'field1', header: 'Field 1', width: '45%' },
            { fieldName: 'field2', header: 'Field 2', width: '10%' },
            { fieldName: 'field3', header: 'Field 3', width: '30%' },
            { fieldName: 'field4', header: 'Field 4', width: '5%' }
        ];

        this.url = '/test/';
    });

    it('renderer basic datatable component', () => {
        const actionHeader = fixture.debugElement.query(By.css('dot-action-header'));
        const globalSearch = actionHeader.query(By.css('input'));

        comp.ngOnInit();

        expect(globalSearch.nativeElement).toBe(document.activeElement);
    });

    it('renderer basic datatable component', () => {
        spyOn(this.paginatorService, 'getWithOffset').and.callFake(() => {
            return Observable.create((observer) => {
                observer.next(Object.assign([], this.items));
            });
        });

        comp.columns = this.columns;
        comp.url = this.url;
        comp.multipleSelection = true;

        comp.ngOnChanges({
            columns: new SimpleChange(null, comp.columns, true),
            url: new SimpleChange(null, this.url, true)
        });

        fixture.detectChanges();

        const rows = el.querySelectorAll('tr');
        expect(5).toEqual(rows.length);

        const headers = rows[0].querySelectorAll('th');
        expect(5).toEqual(headers.length);

        comp.columns.forEach((_col, index) =>
            expect(!index ? '' : comp.columns[index - 1].header).toEqual(headers[index].querySelector('span').textContent)
        );

        rows.forEach((row, rowIndex) => {
            if (rowIndex) {
                const cells = row.querySelectorAll('td');
                const item = this.items[rowIndex - 1];

                cells.forEach((_cell, cellIndex) => {
                    if (cellIndex && cellIndex < 5) {
                        expect(cells[cellIndex].querySelector('span').textContent).toContain(item[comp.columns[cellIndex - 1].fieldName]);
                    }
                });
            }
        });

        expect(this.url).toEqual(this.paginatorService.url);
        const checkboxs = fixture.debugElement.queryAll(By.css('input[type="checkbox"]'));
        expect(5).toEqual(checkboxs.length);
    });

    it('renderer with format date column', () => {
        const itemsWithFormat = this.items.map((item) => {
            item.field3 = 1496178801000;
            return item;
        });

        spyOn(this.paginatorService, 'getWithOffset').and.callFake(() => {
            return Observable.create((observer) => {
                observer.next(Object.assign([], itemsWithFormat));
            });
        });

        this.columns[2].format = 'date';
        comp.columns = this.columns;
        comp.url = this.url;
        comp.multipleSelection = true;

        comp.ngOnChanges({
            columns: new SimpleChange(null, comp.columns, true),
            url: new SimpleChange(null, this.url, true)
        });

        fixture.detectChanges();

        const rows = el.querySelectorAll('tr');
        expect(5).toEqual(rows.length, 'tr');

        const headers = rows[0].querySelectorAll('th');
        expect(5).toEqual(headers.length, 'th');

        comp.columns.forEach((_col, index) =>
            expect(!index ? '' : comp.columns[index - 1].header).toEqual(headers[index].querySelector('span').textContent)
        );

        rows.forEach((row, rowIndex) => {
            if (rowIndex) {
                const cells = row.querySelectorAll('td');
                const item = this.items[rowIndex - 1];

                cells.forEach((_cell, cellIndex) => {
                    if (cellIndex && cellIndex < 5) {
                        const textContent = cells[cellIndex].querySelector('span').textContent;
                        const itemCOntent = item[comp.columns[cellIndex - 1].fieldName];
                        expect(textContent).toContain(itemCOntent);
                    }
                });
            }
        });

        expect(this.url).toEqual(this.paginatorService.url);
    });

    it('should renderer table without checkbox', () => {
        spyOn(this.paginatorService, 'getWithOffset').and.callFake(() => {
            return Observable.create((observer) => {
                observer.next(Object.assign([], this.items));
            });
        });

        comp.columns = this.columns;
        comp.url = this.url;

        comp.ngOnChanges({
            columns: new SimpleChange(null, comp.columns, true),
            url: new SimpleChange(null, this.url, true)
        });

        const dataList = fixture.debugElement.query(By.css('p-dataTable'));
        const dataListComponentInstance = dataList.componentInstance;

        dataListComponentInstance.onLazyLoad.emit({
            first: 0
        });

        fixture.detectChanges();

        const rows = el.querySelectorAll('tr');
        expect(5).toEqual(rows.length);

        const headers = rows[0].querySelectorAll('th');
        expect(4).toEqual(headers.length);

        const checkboxs = fixture.debugElement.queryAll(By.css('input[type="checkbox"]'));
        expect(0).toEqual(checkboxs.length);
    });

    it('should add a column if actions are received', () => {
        const fakeActions: DotDataTableAction[] = [
            {
                menuItem: {
                    icon: 'fa-trash',
                    label: 'Remove',
                    command: () => {}
                }
            }
        ];
        spyOn(this.paginatorService, 'getWithOffset').and.callFake(() => {
            return Observable.create((observer) => {
                observer.next(Object.assign([], this.items));
            });
        });

        comp.columns = this.columns;

        comp.ngOnChanges({
            columns: new SimpleChange(null, comp.columns, true)
        });

        fixture.detectChanges();

        const rows = el.querySelectorAll('tr');
        expect(rows[0].cells.length).toEqual(4);

        comp.actions = fakeActions;
        fixture.detectChanges();

        expect(rows[0].cells.length).toEqual(5);
    });

    it('should receive an action an execute the command after clickling over the action button', () => {
        const fakeActions: DotDataTableAction[] = [
            {
                menuItem: {
                    icon: 'fa-trash',
                    label: 'Remove',
                    command: () => {}
                }
            }
        ];
        spyOn(this.paginatorService, 'getWithOffset').and.callFake(() => {
            return Observable.create((observer) => {
                observer.next(Object.assign([], this.items));
            });
        });

        comp.columns = this.columns;
        comp.actions = fakeActions;

        comp.ngOnChanges({
            columns: new SimpleChange(null, comp.columns, true)
        });

        fixture.detectChanges();
        const actionButton = de.query(By.css('dot-action-menu-button'));

        const spy = spyOn(fakeActions[0].menuItem, 'command');

        actionButton.nativeElement.children[0].click();

        expect(spy).toHaveBeenCalled();
    });

    it('should show the loading indicator while the data is received', () => {
        expect(comp.loading).toEqual(true);
        spyOn(this.paginatorService, 'getCurrentPage').and.returnValue(observableOf(this.items));
        comp.columns = this.columns;
        comp.loadCurrentPage();
        expect(comp.loading).toEqual(false);
    });

    it('should load first page of resutls and set pagination to 1', () => {
        comp.dataTable.first = 3;
        spyOn(this.paginatorService, 'get').and.returnValue(observableOf(this.items));

        comp.loadFirstPage();

        expect(comp.dataTable.first).toBe(1);
        expect(comp.items.length).toBe(7);
    });
});
