import { ActionHeaderComponent, ButtonAction } from '../listing-data-table/action-header/action-header';
import { ActionButtonComponent } from '../_common/action-button/action-button.component';
import { By } from '@angular/platform-browser';
import { ComponentFixture, async } from '@angular/core/testing';
import { CrudService } from '../../../api/services/crud/crud.service';
import { DOTTestBed } from '../../../test/dot-test-bed';
import { DataTableModule, SharedModule } from 'primeng/primeng';
import { DebugElement, SimpleChange } from '@angular/core';
import { DotcmsConfig } from '../../../api/services/system/dotcms-config';
import { FormatDateService } from '../../../api/services/format-date-service';
import { ListingDataTableComponent } from './listing-data-table.component';
import { MessageService } from '../../../api/services/messages-service';
import { MockMessageService } from '../../../test/message-service.mock';
import { RouterTestingModule } from '@angular/router/testing';
import { Observable } from 'rxjs/Observable';

describe('Listing Component', () => {

  let comp: ListingDataTableComponent;
  let fixture: ComponentFixture<ListingDataTableComponent>;
  let de: DebugElement;
  let el: HTMLElement;

  beforeEach(async(() => {
    let messageServiceMock = new MockMessageService({
        'global-search': 'Global Serach'
    });

    DOTTestBed.configureTestingModule({
        declarations: [ ActionHeaderComponent, ActionButtonComponent, ListingDataTableComponent ],
        imports: [ DataTableModule, SharedModule, RouterTestingModule.withRoutes([
            { path: 'test', component: ListingDataTableComponent }
        ]) ],
        providers: [
            {provide: MessageService, useValue: messageServiceMock},
            CrudService, FormatDateService
        ]
    });

    fixture = DOTTestBed.createComponent(ListingDataTableComponent);

    comp = fixture.componentInstance;

    // query for the title <h1> by CSS element selector
    de = fixture.debugElement.query(By.css('p-dataTable'));
    el = de.nativeElement;

    let dotcmsConfig = fixture.debugElement.injector.get(DotcmsConfig);
    spyOn(dotcmsConfig, 'getConfig').and.returnValue(Observable.of({
        paginatorLinks: 2,
        paginatorRows: 3
    }));

  }));

  it('renderer title', () => {
    let items = [
        {field1: 'item1-value1', field2: 'item1-value2', field3: 'item1-value3'},
        {field1: 'item2-value1', field2: 'item2-value2', field3: 'item2-value3'},
        {field1: 'item3-value1', field2: 'item3-value2', field3: 'item3-value3'},
        {field1: 'item4-value1', field2: 'item4-value2', field3: 'item4-value3'},
        {field1: 'item5-value1', field2: 'item5-value2', field3: 'item5-value3'},
        {field1: 'item6-value1', field2: 'item6-value2', field3: 'item6-value3'},
        {field1: 'item7-value1', field2: 'item7-value2', field3: 'item7-value3'}
    ];

    let crudService = fixture.debugElement.injector.get(CrudService);
    spyOn(crudService, 'loadData').and.returnValue(Observable.of({
        items: items,
        totalRecords: items.length,
    }));

    comp.columns = [
        {fieldName: 'field1', header: 'Field 1', width: '45%'},
        {fieldName: 'field2', header: 'Field 2', width: '10%'},
        {fieldName: 'field3', header: 'Field 3', width: '45%'},
    ];

    comp.ngOnChanges({
        columns: new SimpleChange(null, comp.columns, true)
    });

    fixture.detectChanges();

    let rows = el.querySelectorAll('tr');
    expect(4).toEqual(rows.length);

    let headers = rows[0].querySelectorAll('th');
    expect(4).toEqual(headers.length);

    comp.columns.forEach((col, index ) =>
    expect(!index ? '' : comp.columns[index - 1].header).toEqual(headers[index].querySelector('span').textContent));

    rows.forEach((row, rowIndex) => {
        if (rowIndex) {
            let cells = row.querySelectorAll('td');
            let item = items[rowIndex - 1];

            cells.forEach((cell, cellIndex) => {
                if (cellIndex) {
                    expect(item[comp.columns[cellIndex - 1].fieldName])
                        .toEqual(cells[cellIndex].querySelector('span').textContent);
                }
            });
        }
    });
  });

  it('renderer with format date column', () => {

    let items = [
        {field1: 'item1-value1', field2: 'item1-value2', field3: 1496178801000},
        {field1: 'item2-value1', field2: 'item2-value2', field3: 1496178802000},
        {field1: 'item3-value1', field2: 'item3-value2', field3: 1496178803000},
        {field1: 'item4-value1', field2: 'item4-value2', field3: 1496178804000},
        {field1: 'item5-value1', field2: 'item5-value2', field3: 1496178805000},
        {field1: 'item6-value1', field2: 'item6-value2', field3: 1496178806000},
        {field1: 'item7-value1', field2: 'item7-value2', field3: 1496178807000}
    ];

    let crudService = fixture.debugElement.injector.get(CrudService);
    spyOn(crudService, 'loadData').and.returnValue(Observable.of({
        items: items,
        totalRecords: items.length,
    }));

    comp.columns = [
        {fieldName: 'field1', header: 'Field 1', width: '45%'},
        {fieldName: 'field2', header: 'Field 2', width: '10%'},
        {fieldName: 'field3', header: 'Field 3', width: '45%', format: 'date'},
    ];

    comp.ngOnChanges({
        columns: new SimpleChange(null, comp.columns, true)
    });

    fixture.detectChanges();

    let rows = el.querySelectorAll('tr');
    expect(4).toEqual(rows.length);

    let headers = rows[0].querySelectorAll('th');
    expect(4).toEqual(headers.length);

    comp.columns.forEach((col, index ) =>
    expect(!index ? '' : comp.columns[index - 1].header).toEqual(headers[index].querySelector('span').textContent));

    rows.forEach((row, rowIndex) => {
        if (rowIndex) {
            let cells = row.querySelectorAll('td');
            let item = items[rowIndex - 1];

            cells.forEach((cell, cellIndex) => {
                if (cellIndex) {
                    expect((item[comp.columns[cellIndex - 1].fieldName]).toString())
                        .toEqual(cells[cellIndex].querySelector('span').textContent);
                }
            });
        }
    });
  });
});