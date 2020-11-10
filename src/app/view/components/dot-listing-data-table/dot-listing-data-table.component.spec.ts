import { of } from 'rxjs';
import { DotAlertConfirmService } from '@services/dot-alert-confirm/dot-alert-confirm.service';
import { DotIconButtonTooltipModule } from '@components/_common/dot-icon-button-tooltip/dot-icon-button-tooltip.module';
import { DotActionMenuButtonComponent } from '../_common/dot-action-menu-button/dot-action-menu-button.component';
import { DotActionButtonComponent } from '../_common/dot-action-button/dot-action-button.component';
import { By } from '@angular/platform-browser';
import { ComponentFixture, fakeAsync, TestBed, tick } from '@angular/core/testing';
import { TableModule } from 'primeng/table';
import { Component, DebugElement, Input } from '@angular/core';
import { FormatDateService } from '@services/format-date-service';
import { DotListingDataTableComponent } from './dot-listing-data-table.component';
import { DotMessageService } from '@services/dot-message/dot-messages.service';
import { MockDotMessageService } from '../../../test/dot-message-service.mock';
import { RouterTestingModule } from '@angular/router/testing';
import { PaginatorService } from '@services/paginator';
import { ActionHeaderComponent } from './action-header/action-header.component';
import { DotActionMenuItem } from '@shared/models/dot-action-menu/dot-action-menu-item.model';
import { DotMenuModule } from '../_common/dot-menu/dot-menu.module';
import { DotIconModule } from '../_common/dot-icon/dot-icon.module';
import { DotIconButtonModule } from '../_common/dot-icon-button/dot-icon-button.module';
import { DotStringFormatPipe } from '@pipes/dot-string-format/dot-string-format.pipe';
import { ConfirmationService, SharedModule } from 'primeng/api';
import { MenuModule } from 'primeng/menu';
import { CoreWebService, LoggerService, StringUtils } from 'dotcms-js';
import { DataTableColumn } from '@models/data-table';
import { ActionHeaderOptions, ButtonAction } from '@models/action-header';
import { CoreWebServiceMock } from '@tests/core-web.service.mock';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { DotPipesModule } from '@pipes/dot-pipes.module';
import { FormsModule } from '@angular/forms';

@Component({
    selector: 'dot-test-host-component',
    template: ` <dot-listing-data-table
        [columns]="columns"
        [url]="url"
        [actionHeaderOptions]="actionHeaderOptions"
        [buttonActions]="buttonActions"
        [sortOrder]="sortOrder"
        [sortField]="sortField"
        [multipleSelection]="multipleSelection"
        [paginationPerPage]="paginationPerPage"
        [actions]="actions"
        [dataKey]="dataKey"
        [checkbox]="checkbox"
        [firstPageData]="firstPageData"
        [paginatorExtraParams]="paginatorExtraParams"
        (rowWasClicked)="rowWasClicked($event)"
        (selectedItems)="selectedItems($event)"
    ></dot-listing-data-table>`
})
class TestHostComponent {
    @Input() columns: DataTableColumn[];
    @Input() url: string;
    @Input() actionHeaderOptions: ActionHeaderOptions;
    @Input() buttonActions: ButtonAction[] = [];
    @Input() sortOrder: string;
    @Input() sortField: string;
    @Input() multipleSelection = false;
    @Input() paginationPerPage = 40;
    @Input() actions: DotActionMenuItem[];
    @Input() dataKey = '';
    @Input() checkbox = false;
    @Input() firstPageData: any[];
    @Input() paginatorExtraParams: { [key: string]: string } = {};

    rowWasClicked(data: any) {
        console.log(data);
    }

    selectedItems(data: any) {
        console.log(data);
    }
}

class TestPaginatorService {
    filter: string;
    url: string;
    sortOrder: string;
    sortField: string;
    paginationPerPage: string;

    getWithOffset(_offset: number) {
        return of([]);
    }

    get() {}
}

describe('DotListingDataTableComponent', () => {
    let comp: DotListingDataTableComponent;
    let hostFixture: ComponentFixture<TestHostComponent>;
    let hostComponent: TestHostComponent;
    let de: DebugElement;
    let el: HTMLElement;
    let items;
    let paginatorService: PaginatorService;

    beforeEach(() => {
        const messageServiceMock = new MockDotMessageService({
            'global-search': 'Global Serach'
        });

        TestBed.configureTestingModule({
            declarations: [
                ActionHeaderComponent,
                DotActionButtonComponent,
                DotListingDataTableComponent,
                DotActionMenuButtonComponent,
                TestHostComponent
            ],
            imports: [
                TableModule,
                SharedModule,
                RouterTestingModule.withRoutes([
                    { path: 'test', component: DotListingDataTableComponent }
                ]),
                DotIconButtonTooltipModule,
                MenuModule,
                DotMenuModule,
                DotIconModule,
                DotIconButtonModule,
                HttpClientTestingModule,
                DotPipesModule,
                FormsModule
            ],
            providers: [
                { provide: CoreWebService, useClass: CoreWebServiceMock },
                { provide: DotMessageService, useValue: messageServiceMock },
                { provide: PaginatorService, useClass: TestPaginatorService },
                LoggerService,
                FormatDateService,
                DotAlertConfirmService,
                ConfirmationService,
                StringUtils
            ]
        });

        hostFixture = TestBed.createComponent(TestHostComponent);
        hostComponent = hostFixture.componentInstance;
        hostComponent.columns = [
            { fieldName: 'field1', header: 'Field 1', width: '45%', sortable: true },
            { fieldName: 'field2', header: 'Field 2', width: '10%' },
            { fieldName: 'field3', header: 'Field 3', width: '30%' },
            { fieldName: 'nEntries', header: 'Field 4', width: '5%', textContent: 'View ({0})' }
        ];
        hostComponent.actions = [
            {
                menuItem: {
                    icon: 'fa fa-trash',
                    label: 'Remove',
                    command: () => {}
                }
            }
        ];
        hostComponent.url = '/test/';

        comp = hostFixture.debugElement.query(By.css('dot-listing-data-table')).componentInstance;
        de = hostFixture.debugElement.query(By.css('p-table'));
        el = de.nativeElement;

        items = [
            {
                field1: 'item1-value1',
                field2: 'item1-value2',
                field3: 'item1-value3',
                nEntries: 'item1-value4',
                variable: 'Host'
            },
            {
                field1: 'item2-value1',
                field2: 'item2-value2',
                field3: 'item2-value3',
                nEntries: 'item1-value4',
                variable: 'Banner'
            },
            {
                field1: 'item3-value1',
                field2: 'item3-value2',
                field3: 'item3-value3',
                nEntries: 'item1-value4',
                variable: 'Banner'
            },
            {
                field1: 'item4-value1',
                field2: 'item4-value2',
                field3: 'item4-value3',
                nEntries: 'item1-value4',
                variable: 'Banner'
            },
            {
                field1: 'item5-value1',
                field2: 'item5-value2',
                field3: 'item5-value3',
                nEntries: 'item1-value4',
                variable: 'Banner'
            },
            {
                field1: 'item6-value1',
                field2: 'item6-value2',
                field3: 'item6-value3',
                nEntries: 'item1-value4',
                variable: 'Banner'
            },
            {
                field1: 'item7-value1',
                field2: 'item7-value2',
                field3: 'item7-value3',
                nEntries: 'item1-value4',
                variable: 'Banner'
            }
        ];
        paginatorService = comp.paginatorService;
        paginatorService.paginationPerPage = 4;
        paginatorService.maxLinksPage = 2;
        paginatorService.totalRecords = items.length;
    });

    it('should set active element the global search on load', () => {
        const actionHeader = hostFixture.debugElement.query(By.css('dot-action-header'));
        const globalSearch = actionHeader.query(By.css('input'));
        hostFixture.detectChanges();

        expect(globalSearch.nativeElement).toBe(document.activeElement);
    });

    it('renderer basic datatable component', fakeAsync(() => {
        spyOn(paginatorService, 'getWithOffset').and.returnValue(of(items));
        hostComponent.multipleSelection = true;

        hostFixture.detectChanges();
        tick(1);
        hostFixture.detectChanges();
        const rows = el.querySelectorAll('tr');
        expect(8).toEqual(rows.length);

        const headers = rows[0].querySelectorAll('th');
        expect(5).toEqual(headers.length);

        hostComponent.columns.forEach((_col, index) => {
            const sortableIcon = headers[index].querySelector('p-sortIcon');
            index === 0 ? expect(sortableIcon).toBeDefined() : expect(sortableIcon).toBeNull();
            expect(hostComponent.columns[index].header).toEqual(headers[index].textContent.trim());
        });

        rows.forEach((row, rowIndex) => {
            if (rowIndex) {
                const cells = row.querySelectorAll('td');
                const item = items[rowIndex - 1];
                cells.forEach((_cell, cellIndex) => {
                    if (cellIndex < 3) {
                        expect(cells[cellIndex].querySelector('span').textContent).toContain(
                            item[hostComponent.columns[cellIndex].fieldName]
                        );
                    }
                    if (cellIndex === 3) {
                        const anchor = cells[cellIndex].querySelector('a');
                        expect(anchor.textContent).toContain(
                            `View (${item[hostComponent.columns[cellIndex].fieldName]})`
                        );
                        expect(anchor.href).toContain(
                            item.variable === 'Host' ? '/c/sites' : '/c/content?filter=Banner'
                        );
                    }
                });
            }
        });

        expect(hostComponent.url).toEqual(paginatorService.url);
    }));

    it('should not call paginatorService when firstPageData comes', () => {
        spyOn(paginatorService, 'getWithOffset');
        hostComponent.firstPageData = items;

        hostFixture.detectChanges();
        expect(paginatorService.getWithOffset).not.toHaveBeenCalled();
    });

    it('renderer with format date column', fakeAsync(() => {
        const dotStringFormatPipe = new DotStringFormatPipe();
        const itemsWithFormat = items.map((item) => {
            item.field3 = 1496178801000;
            return item;
        });
        spyOn(paginatorService, 'getWithOffset').and.returnValue(of(itemsWithFormat));
        hostComponent.columns[2].format = 'date';
        hostComponent.multipleSelection = true;

        hostFixture.detectChanges();
        tick(1);
        hostFixture.detectChanges();

        const rows = el.querySelectorAll('tr');
        expect(8).toEqual(rows.length, 'tr');

        const headers = rows[0].querySelectorAll('th');
        expect(5).toEqual(headers.length, 'th');

        hostComponent.columns.forEach((_col, index) =>
            expect(hostComponent.columns[index].header).toEqual(headers[index].textContent.trim())
        );

        rows.forEach((row, rowIndex) => {
            if (rowIndex) {
                const cells = row.querySelectorAll('td');
                const item = items[rowIndex - 1];

                cells.forEach((_cell, cellIndex) => {
                    if (cellIndex < 4) {
                        const textContent = cells[cellIndex].textContent;
                        const itemContent = comp.columns[cellIndex].textContent
                            ? dotStringFormatPipe.transform(
                                  hostComponent.columns[cellIndex].textContent,
                                  [item[comp.columns[cellIndex].fieldName]]
                              )
                            : item[comp.columns[cellIndex].fieldName];
                        expect(textContent).toContain(itemContent);
                    }
                });
            }
        });

        expect(hostComponent.url).toEqual(paginatorService.url);
    }));

    it('should renderer table without checkbox', fakeAsync(() => {
        spyOn(paginatorService, 'getWithOffset').and.returnValue(of(items));
        hostFixture.detectChanges();
        tick(1);
        hostFixture.detectChanges();
        const rows = el.querySelectorAll('tr');
        expect(8).toEqual(rows.length);

        const headers = rows[0].querySelectorAll('th');
        expect(5).toEqual(headers.length);
    }));

    it('should add a column if actions are received', () => {
        const fakeActions: DotActionMenuItem[] = [
            {
                menuItem: {
                    icon: 'fa fa-trash',
                    label: 'Remove',
                    command: () => {}
                }
            }
        ];
        spyOn(paginatorService, 'getWithOffset').and.returnValue(of(items));
        hostFixture.detectChanges();

        const rows = el.querySelectorAll('tr');
        expect(rows[0].cells.length).toEqual(5);

        hostComponent.actions = fakeActions;
        hostFixture.detectChanges();

        expect(rows[0].cells.length).toEqual(5);
    });

    it('should receive an action an execute the command after clickling over the action button', fakeAsync(() => {
        const fakeActions: DotActionMenuItem[] = [
            {
                menuItem: {
                    icon: 'fa fa-trash',
                    label: 'Remove',
                    command: () => {}
                }
            }
        ];
        spyOn(paginatorService, 'getWithOffset').and.returnValue(of(items));

        hostComponent.actions = fakeActions;

        hostFixture.detectChanges();
        tick(1);
        hostFixture.detectChanges();
        const actionButton = de.query(By.css('dot-action-menu-button'));

        const spy = spyOn(fakeActions[0].menuItem, 'command');

        actionButton.nativeElement.children[0].click();

        expect(spy).toHaveBeenCalled();
    }));

    it('should show the loading indicator while the data is received', fakeAsync(() => {
        expect(comp.loading).toEqual(true);
        spyOn(paginatorService, 'getCurrentPage').and.returnValue(of(items));
        hostFixture.detectChanges();
        tick(1);
        hostFixture.detectChanges();
        comp.loadCurrentPage();
        tick(1);
        expect(comp.loading).toEqual(false);
    }));

    it('should load first page of resutls and set pagination to 1', fakeAsync(() => {
        spyOn(paginatorService, 'get').and.returnValue(of(items));
        hostFixture.detectChanges();
        tick(1);
        hostFixture.detectChanges();
        comp.dataTable.first = 3;
        comp.loadFirstPage();
        tick(1);
        expect(comp.dataTable.first).toBe(1);
        expect(comp.items.length).toBe(7);
    }));

    it('should focus first row on arrowDown in Global Search Input', fakeAsync(() => {
        spyOn(comp, 'focusFirstRow').and.callThrough();
        spyOn(paginatorService, 'get').and.returnValue(of(items));
        comp.loadFirstPage();
        hostFixture.detectChanges();
        tick(1);
        hostFixture.detectChanges();
        comp.globalSearch.nativeElement.dispatchEvent(
            new KeyboardEvent('keydown', { key: 'arrowDown' })
        );
        expect(comp.dataTable.tableViewChild.nativeElement.rows[1]).toBe(document.activeElement);
    }));

    it('should set the pagination size in the Table', () => {
        spyOn(paginatorService, 'get').and.returnValue(of(items));
        hostComponent.paginationPerPage = 5;
        comp.loadFirstPage();
        hostFixture.detectChanges();

        expect(comp.dataTable.rows).toBe(5);
    });

    it('should set pagination extra parameters', () => {
        spyOn(paginatorService, 'get').and.returnValue(of(items));
        hostComponent.paginatorExtraParams = { type: 'FORM', name: 'DotCMS' };
        hostFixture.detectChanges();

        expect(comp.paginatorService.extraParams.get('type')).toEqual('FORM');
        expect(comp.paginatorService.extraParams.get('name')).toEqual('DotCMS');
    });

    it('should emit when a row is clicked or enter', fakeAsync(() => {
        spyOn(paginatorService, 'get').and.returnValue(of(items));
        spyOn(comp.rowWasClicked, 'emit');
        comp.loadFirstPage();
        hostFixture.detectChanges();
        tick(1);
        hostFixture.detectChanges();
        const firstRow: DebugElement = de.queryAll(By.css('tr'))[1];
        firstRow.triggerEventHandler('click', null);
        firstRow.triggerEventHandler('keyup.enter', null);

        expect(comp.rowWasClicked.emit).toHaveBeenCalledTimes(2);
    }));

    describe('with checkBox', () => {
        let bodyCheckboxes: DebugElement[];

        beforeEach(fakeAsync(() => {
            spyOn(paginatorService, 'getWithOffset').and.returnValue(of(items));
            hostComponent.checkbox = true;

            hostFixture.detectChanges();
            tick(1);
            hostFixture.detectChanges();
            bodyCheckboxes = de.queryAll(By.css('p-tablecheckbox'));
        }));

        it('should renderer table', () => {
            const headerCheckBoxes = el.querySelectorAll('p-tableheadercheckbox');
            expect(1).toEqual(headerCheckBoxes.length);
            expect(7).toEqual(bodyCheckboxes.length);
        });
    });
});
