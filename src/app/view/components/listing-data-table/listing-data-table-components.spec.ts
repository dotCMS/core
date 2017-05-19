import { ComponentFixture } from '@angular/core/testing';
import { DebugElement } from '@angular/core';
import { ListingDataTableComponent } from './listing-data-table-component';
import { By } from '@angular/platform-browser';
import { DataTableModule, SharedModule } from 'primeng/primeng';
import { DotcmsConfig } from '../../../api/services/system/dotcms-config';
import { ListingService } from '../../../api/services/listing-service';
import { Observable } from 'rxjs/Observable';
import { DOTTestBed } from '../../../api/util/test/dot-test-bed';

describe('BannerComponent (inline template)', () => {

  let comp:    ListingDataTableComponent;
  let fixture: ComponentFixture<ListingDataTableComponent>;
  let de:      DebugElement;
  let el:      HTMLElement;

  beforeEach(() => {

    DOTTestBed.configureTestingModule({
        declarations: [ ListingDataTableComponent ],
        imports: [ DataTableModule, SharedModule ],
        providers: [
            ListingService
        ]
    });

    fixture = DOTTestBed.createComponent(ListingDataTableComponent);

    comp = fixture.componentInstance;

    // query for the title <h1> by CSS element selector
    de = fixture.debugElement.query(By.css('p-dataTable'));
    el = de.nativeElement;
  });

  it('should display original a p-dataTable', () => {
    fixture.detectChanges();
    expect(el).toBeDefined(el);
  });

  it('renderer title', () => {
    let dotcmsConfig = fixture.debugElement.injector.get(DotcmsConfig);
    spyOn(dotcmsConfig, 'getConfig').and.returnValue(Observable.of({
        paginatorLinks: 2,
        paginatorRows: 3
    }));

    let items = [
                {field1: 'item1-value1', field2: 'item1-value2', field3: 'item1-value3'},
                {field1: 'item2-value1', field2: 'item2-value2', field3: 'item2-value3'},
                {field1: 'item3-value1', field2: 'item3-value2', field3: 'item3-value3'},
                {field1: 'item4-value1', field2: 'item4-value2', field3: 'item4-value3'},
                {field1: 'item5-value1', field2: 'item5-value2', field3: 'item5-value3'},
                {field1: 'item6-value1', field2: 'item6-value2', field3: 'item6-value3'},
                {field1: 'item7-value1', field2: 'item7-value2', field3: 'item7-value3'}
            ];

    let listingService = fixture.debugElement.injector.get(ListingService);
    spyOn(listingService, 'loadData').and.returnValue(Observable.of({
        items: items,
        totalRecords: items.length,
    }));

    comp.columns = [
        {fieldName: 'field1', header: 'Field 1', width: '45%'},
        {fieldName: 'field2', header: 'Field 2', width: '10%'},
        {fieldName: 'field3', header: 'Field 3', width: '45%'},
    ];

    fixture.detectChanges();

    let rows = el.querySelectorAll('tr');
    expect(4).toEqual(rows.length);

    let headers = rows[0].querySelectorAll('th');
    expect(4).toEqual(headers.length);

    comp.columns.forEach( (col, index ) =>
        expect(!index ? '' : comp.columns[index - 1].header).toEqual(headers[index].querySelector('span').textContent));

    rows.forEach( (row, rowIndex) => {
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
});