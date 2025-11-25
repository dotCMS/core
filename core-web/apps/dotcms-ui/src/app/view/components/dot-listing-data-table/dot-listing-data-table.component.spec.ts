/* eslint-disable no-console */
/* eslint-disable @typescript-eslint/no-explicit-any */

import { of } from 'rxjs';

import { HttpClientTestingModule } from '@angular/common/http/testing';
import { Component, DebugElement, Input } from '@angular/core';
import { ComponentFixture, fakeAsync, TestBed, tick } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { By } from '@angular/platform-browser';
import { RouterTestingModule } from '@angular/router/testing';

import { ConfirmationService, SharedModule } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { ContextMenuModule } from 'primeng/contextmenu';
import { MenuModule } from 'primeng/menu';
import { TableModule } from 'primeng/table';
import { TooltipModule } from 'primeng/tooltip';

import { DotAlertConfirmService, DotMessageService } from '@dotcms/data-access';
import {
    CoreWebService,
    DotcmsConfigService,
    LoggerService,
    LoginService,
    StringUtils
} from '@dotcms/dotcms-js';
import { DotActionMenuItem } from '@dotcms/dotcms-models';
import {
    DotActionMenuButtonComponent,
    DotIconModule,
    DotMenuComponent,
    DotMessagePipe,
    DotRelativeDatePipe,
    DotSafeHtmlPipe,
    DotStringFormatPipe
} from '@dotcms/ui';
import { CoreWebServiceMock, MockDotMessageService } from '@dotcms/utils-testing';
import { ActionHeaderOptions, ButtonAction } from '@models/action-header';
import { DataTableColumn } from '@models/data-table';

import { ActionHeaderComponent } from './action-header/action-header.component';
import { DotListingDataTableComponent } from './dot-listing-data-table.component';

import { DotActionButtonComponent } from '../_common/dot-action-button/dot-action-button.component';

@Component({
    selector: 'dot-empty-state',
    template: `
        <h1>Im empty</h1>
    `
})
class EmptyMockComponent {}

@Component({
    selector: 'dot-test-host-component',
    template: `
        <dot-listing-data-table
            (rowWasClicked)="rowWasClicked($event)"
            (selectedItems)="selectedItems($event)"
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
            [mapItems]="mapItems"
            [paginatorExtraParams]="paginatorExtraParams">
            <dot-empty-state></dot-empty-state>
        </dot-listing-data-table>
    `
})
class TestHostComponent {
    @Input() columns: DataTableColumn[];
    @Input() url: '/api/data';
    @Input() actionHeaderOptions: ActionHeaderOptions;
    @Input() buttonActions: ButtonAction[] = [];
    @Input() sortOrder: string;
    @Input() sortField: string;
    @Input() multipleSelection = false;
    @Input() paginationPerPage = 40;
    @Input() actions: DotActionMenuItem[];
    @Input() dataKey = '';
    @Input() checkbox = false;
    @Input() paginatorExtraParams: { [key: string]: string } = {};

    rowWasClicked(data: any) {
        console.log(data);
    }

    selectedItems(data: any) {
        console.log(data);
    }

    mapItems(items: any[]): any[] {
        return items.map((item) => {
            item.disableInteraction =
                item.variable === 'Host' || item.identifier === 'SYSTEM_TEMPLATE';

            return item;
        });
    }
}

describe('DotListingDataTableComponent', () => {
    let comp: DotListingDataTableComponent;
    let hostFixture: ComponentFixture<TestHostComponent>;
    let hostComponent: TestHostComponent;
    let de: DebugElement;
    let el: HTMLElement;
    let items;
    let enabledItems;
    let disabledItems;
    let coreWebService: CoreWebService;
    const favoritePagesItem = {
        field1: 'item7-value1',
        field2: 'item7-value2',
        field3: 'item7-value3',
        nEntries: 'item1-value4',
        variable: 'dotFavoritePage',
        system: true
    };

    beforeEach(() => {
        const messageServiceMock = new MockDotMessageService({
            'global-search': 'Global Serach',
            'No-Results-Found': 'No Results Found'
        });

        TestBed.configureTestingModule({
            declarations: [
                ActionHeaderComponent,
                DotActionButtonComponent,
                DotListingDataTableComponent,
                TestHostComponent,
                EmptyMockComponent
            ],
            imports: [
                TableModule,
                SharedModule,
                RouterTestingModule.withRoutes([
                    { path: 'test', component: DotListingDataTableComponent }
                ]),
                MenuModule,
                DotActionMenuButtonComponent,
                DotMenuComponent,
                DotIconModule,
                DotRelativeDatePipe,
                HttpClientTestingModule,
                DotSafeHtmlPipe,
                DotMessagePipe,
                FormsModule,
                ContextMenuModule,
                ButtonModule,
                TooltipModule,
                DotStringFormatPipe
            ],
            providers: [
                { provide: CoreWebService, useClass: CoreWebServiceMock },
                { provide: DotMessageService, useValue: messageServiceMock },
                LoggerService,
                DotAlertConfirmService,
                ConfirmationService,
                StringUtils,
                {
                    provide: DotcmsConfigService,
                    useValue: {
                        getSystemTimeZone: () =>
                            of({
                                id: 'America/Costa_Rica',
                                label: 'Central Standard Time (America/Costa_Rica)',
                                offset: -21600000
                            })
                    }
                },
                {
                    provide: LoginService,
                    useValue: { currentUserLanguageId: 'en-US' }
                }
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
                    command: () => {
                        //
                    }
                }
            }
        ];
        coreWebService = TestBed.inject(CoreWebService);
        comp = hostFixture.debugElement.query(By.css('dot-listing-data-table')).componentInstance;
        de = hostFixture.debugElement.query(By.css('p-table'));
        el = de.nativeElement;

        enabledItems = [
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

        disabledItems = [
            {
                field1: 'item1-value1',
                field2: 'item1-value2',
                field3: 'item1-value3',
                nEntries: 'item1-value4',
                variable: 'Host'
            },
            {
                identifier: 'SYSTEM_TEMPLATE',
                field1: 'item8-value1',
                field2: 'item8-value2',
                field3: 'item8-value3',
                nEntries: 'item1-value4',
                variable: 'Banner'
            }
        ];

        items = [...enabledItems, ...disabledItems];
    });

    it('should have default attributes', () => {
        hostFixture.detectChanges();
        expect(de.componentInstance.responsiveLayout).toBe('scroll');
        expect(de.componentInstance.lazy).toBe(true);
        expect(de.componentInstance.paginator).toBe(true);
    });

    it('should set active element the global search on load', () => {
        const actionHeader = hostFixture.debugElement.query(By.css('dot-action-header'));
        const globalSearch = actionHeader.query(By.css('input'));
        hostFixture.detectChanges();

        expect(globalSearch.nativeElement).toBe(document.activeElement);
    });

    it('renderer basic datatable component', fakeAsync(() => {
        setRequestSpy(items);
        hostComponent.multipleSelection = true;

        hostFixture.detectChanges();
        tick(1);
        hostFixture.detectChanges();
        const rows = el.querySelectorAll('[data-testclass="testTableRow"]');
        expect(items.length).toEqual(rows.length);

        const headRow = el.querySelector('[data-testclass="testHeadTableRow"]');
        const headers = headRow.querySelectorAll('th');
        expect(5).toEqual(headers.length);

        hostComponent.columns.forEach((_col, index) => {
            const sortableIcon = headers[index].querySelector('p-sortIcon');
            index === 0 ? expect(sortableIcon).toBeDefined() : expect(sortableIcon).toBeNull();
            expect(hostComponent.columns[index].header).toEqual(headers[index].textContent.trim());
        });

        rows.forEach((row, rowIndex) => {
            if (rowIndex) {
                const cells = row.querySelectorAll('td');
                const item = items[rowIndex];
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

        expect(hostComponent.url).toEqual(comp.paginatorService.url);
    }));

    it('renderer with format date column', fakeAsync(() => {
        const itemsWithFormat = items.map((item) => {
            item.field3 = 1496178801000;

            return item;
        });
        setRequestSpy(itemsWithFormat);
        hostComponent.columns[2].format = 'date';
        hostComponent.multipleSelection = true;

        hostFixture.detectChanges();
        tick(1);
        hostFixture.detectChanges();

        const rows = el.querySelectorAll('[data-testclass="testTableRow"]');
        expect(items.length).toEqual(rows.length);

        const headRow = el.querySelector('[data-testclass="testHeadTableRow"]');
        const headers = headRow.querySelectorAll('th');
        expect(5).toEqual(headers.length);

        hostComponent.columns.forEach((_col, index) =>
            expect(hostComponent.columns[index].header).toEqual(headers[index].textContent.trim())
        );

        rows.forEach((row, rowIndex) => {
            if (rowIndex) {
                const cells = row.querySelectorAll('td');
                const item = items[rowIndex];

                cells.forEach((_cell, cellIndex) => {
                    if (cellIndex < 4) {
                        const textContent = cells[cellIndex].textContent;
                        const itemContent =
                            comp.columns[cellIndex].format === 'date'
                                ? new Date(
                                      item[comp.columns[cellIndex].fieldName]
                                  ).toLocaleDateString('US-en', {
                                      month: '2-digit',
                                      day: '2-digit',
                                      year: 'numeric'
                                  })
                                : item[comp.columns[cellIndex].fieldName];
                        expect(textContent).toContain(itemContent);
                    }
                });
            }
        });

        expect(hostComponent.url).toEqual(comp.paginatorService.url);
    }));

    it('should renderer table without checkbox', fakeAsync(() => {
        setRequestSpy(items);
        hostFixture.detectChanges();
        tick(1);
        hostFixture.detectChanges();

        const rows = el.querySelectorAll('[data-testclass="testTableRow"]');
        expect(items.length).toEqual(rows.length);

        const headRow = el.querySelector('[data-testclass="testHeadTableRow"]');
        const headers = headRow.querySelectorAll('th');
        expect(5).toEqual(headers.length);
    }));

    it('should add a column if actions are received', fakeAsync(() => {
        const fakeActions: DotActionMenuItem[] = [
            {
                menuItem: {
                    icon: 'fa fa-trash',
                    label: 'Remove',
                    command: () => {
                        //
                    }
                }
            }
        ];
        hostComponent.actions = fakeActions;
        setRequestSpy(items);
        hostFixture.detectChanges();
        tick(1);
        hostFixture.detectChanges();
        const rows = el.querySelectorAll('tr');
        expect(rows[0].cells.length).toEqual(5);
    }));

    it('should receive an action an execute the command after clickling over the action button', fakeAsync(() => {
        const fakeActions: DotActionMenuItem[] = [
            {
                menuItem: {
                    icon: 'fa fa-trash',
                    label: 'Remove',
                    command: () => {
                        //
                    }
                }
            }
        ];
        setRequestSpy(items);

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
        setRequestSpy(items);
        hostFixture.detectChanges();
        tick(1);
        hostFixture.detectChanges();
        comp.loadCurrentPage();
        tick(1);
        expect(comp.loading).toEqual(false);
    }));

    it('should load first page of results and set pagination to 1', fakeAsync(() => {
        setRequestSpy(items);
        hostFixture.detectChanges();
        tick(1);
        hostFixture.detectChanges();
        comp.dataTable.first = 3;
        comp.loadFirstPage();
        tick(1);
        expect(comp.dataTable.first).toBe(1);
        expect(comp.items.length).toBe(items.length);
    }));

    it('should focus first row on arrowDown in Global Search Input', fakeAsync(() => {
        spyOn(comp, 'focusFirstRow').and.callThrough();
        setRequestSpy(items);
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
        setRequestSpy(items);
        hostComponent.paginationPerPage = 5;
        comp.loadFirstPage();
        hostFixture.detectChanges();

        expect(comp.dataTable.rows).toBe(5);
    });

    it('should set pagination extra parameters', () => {
        setRequestSpy(items);
        hostComponent.paginatorExtraParams = { type: 'FORM', name: 'DotCMS' };
        hostFixture.detectChanges();

        expect(comp.paginatorService.extraParams.get('type')).toEqual('FORM');
        expect(comp.paginatorService.extraParams.get('name')).toEqual('DotCMS');
    });

    it('should emit when a row is clicked or enter', fakeAsync(() => {
        setRequestSpy(items);
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

    it('should never emit when a SYSTEM TEMPLATE row is clicked or enter', fakeAsync(() => {
        setRequestSpy(items);
        spyOn(comp.rowWasClicked, 'emit');

        comp.loadFirstPage();

        hostFixture.detectChanges();
        tick(1);
        hostFixture.detectChanges();

        const systemFile: DebugElement = de.query(By.css('tr[data-testRowId="SYSTEM_TEMPLATE"]'));
        systemFile.triggerEventHandler('click', null);
        systemFile.triggerEventHandler('keyup.enter', null);

        expect(comp.rowWasClicked.emit).not.toHaveBeenCalled();
    }));

    it('should set pContextMenuRowDisabled correctly', fakeAsync(() => {
        setRequestSpy(items);
        spyOn(comp.rowWasClicked, 'emit');
        comp.loadFirstPage();
        hostFixture.detectChanges();
        tick(1);
        hostFixture.detectChanges();
        const enabledRow = document.querySelectorAll('[data-testclass="testTableRow"]')[0];
        const disabledRow = document.querySelector('[data-testRowId="SYSTEM_TEMPLATE"]');
        expect(enabledRow.getAttribute('ng-reflect-p-context-menu-row-disabled')).toEqual('false');
        expect(disabledRow.getAttribute('ng-reflect-p-context-menu-row-disabled')).toEqual('true');
    }));

    describe('with checkBox', () => {
        let bodyCheckboxes: DebugElement[];

        beforeEach(fakeAsync(() => {
            setRequestSpy(items);
            hostComponent.checkbox = true;

            hostFixture.detectChanges();
            tick(1);
            hostFixture.detectChanges();
            bodyCheckboxes = de.queryAll(By.css('p-tablecheckbox'));
        }));

        it('should renderer table', () => {
            const headerCheckBoxes = el.querySelectorAll('p-tableheadercheckbox');
            expect(1).toEqual(headerCheckBoxes.length);
            expect(enabledItems.length).toEqual(bodyCheckboxes.length);
        });
    });

    it('should renders the dot empty state component if items array is empty', fakeAsync(() => {
        setRequestSpy([]);
        hostFixture.detectChanges();
        tick(1);
        hostFixture.detectChanges();
        const emptyState = de.query(By.css('dot-empty-state'));
        expect(emptyState.nativeElement.innerText).toBe('Im empty');
    }));

    it('should show no results message if filtered content is empty', fakeAsync(() => {
        setRequestSpy([]);
        hostFixture.detectChanges();
        tick(1);
        comp.globalSearch.nativeElement.value = 'test';
        comp.globalSearch.nativeElement.dispatchEvent(new Event('input'));
        tick(de.componentInstance.filterDelay + 1);
        hostFixture.detectChanges();
        const noResults = de.query(By.css('[data-testid="listing-datatable__empty"]'));
        expect(noResults.nativeElement.innerText).toEqual('No Results Found');
    }));

    it('should hide entries for system content types', fakeAsync(() => {
        setRequestSpy([...items, favoritePagesItem]);
        hostFixture.detectChanges();
        tick(1);
        hostFixture.detectChanges();
        const row = de.query(By.css('[data-testId="row-dotFavoritePage"]'));
        const entriesColumn = row.query(By.css('[data-testId="nEntries"]'));
        expect(entriesColumn.nativeElement.textContent).toBeFalsy();
    }));

    function setRequestSpy(response: any): void {
        spyOn<any>(coreWebService, 'requestView').and.returnValue(
            of({
                entity: response,
                header: (type) => (type === 'Link' ? 'test;test=test' : '10')
            })
        );
    }
});
