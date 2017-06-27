import { ComponentFixture, async } from '@angular/core/testing';
import { DOTTestBed } from '../../../../../test/dot-test-bed';
import { DebugElement } from '@angular/core';
import { SearchableDropdownComponent } from './searchable-dropdown.component';
import { SEARCHABLE_NGFACES_MODULES } from '../searchable-dropdown.module';
import { By } from '@angular/platform-browser';
import { fakeAsync, tick } from '@angular/core/testing';

describe('Searchable Dropdown Component', () => {

  const NROWS = 6;

  let comp: SearchableDropdownComponent;
  let fixture: ComponentFixture<SearchableDropdownComponent>;
  let de: DebugElement;
  let el: HTMLElement;
  let data = [];
  let rows: number;
  let pageLinkSize: number;

  beforeEach(async(() => {

    DOTTestBed.configureTestingModule({
        declarations: [ SearchableDropdownComponent ],
        imports: [ ...SEARCHABLE_NGFACES_MODULES],
        providers: []
    });

    fixture = DOTTestBed.createComponent(SearchableDropdownComponent);
    comp = fixture.componentInstance;
    de = fixture.debugElement;
    el = de.nativeElement;

    for (let i = 0; i < NROWS; i++) {
      data[i] = {
        id: i,
        name: `site-${i}`
      };
    }

    rows = NROWS / 3;
    pageLinkSize = 1;

    comp.totalRecords = NROWS;
    comp.rows = rows;
    comp.pageLinkSize = pageLinkSize;
  }));

  it('should renderer the pagination links', () => {
    fixture.detectChanges();

    let paginator = fixture.debugElement.query(By.css('p-paginator'));

    let componentInstance = paginator.componentInstance;
    let rowParameter = componentInstance.rows;
    let totalRecordsParam = componentInstance.totalRecords;
    let pageLinkSizeParam = componentInstance.pageLinkSize;

    expect(rows).toEqual(rowParameter);
    expect(NROWS).toEqual(totalRecordsParam);
    expect(pageLinkSize).toEqual(pageLinkSizeParam);
  });

  it('should renderer the datas', () => {
    comp.data = data;
    comp.labelPropertyName = 'name';

    fixture.detectChanges();

    let pDataList = fixture.debugElement.query(By.css('p-dataList')).componentInstance;
    expect(comp.data).toEqual(pDataList.value);
  });

  it('should the pageChange call the paginate method', fakeAsync(() => {
    let first = 2;
    let page = 3;
    let pageCount = 4;
    let rows = 5;
    let filter = 'filter';
    let event;

    let input = fixture.debugElement.query(By.css('input[type="text"]'));
    input.nativeElement.value = filter;

    comp.pageChange.subscribe(e => {
      event = e;
    });

    fixture.detectChanges();
    let dataList = fixture.debugElement.query(By.css('p-dataList'));
    let dataListComponentInstance = dataList.componentInstance;

    dataListComponentInstance.onLazyLoad.emit({
      first: first,
      page: page,
      pageCount: pageCount,
      rows: rows,
    });

    tick();

    expect(first).toEqual(event.first);
    expect(page).toEqual(event.page);
    expect(pageCount).toEqual(event.pageCount);
    expect(rows).toEqual(event.rows);
    expect(filter).toEqual(event.filter);
  }));

  it('should change the value', (done) => {
    comp.data = data;
    comp.labelPropertyName = 'name';
    comp.change.subscribe(value => {
      expect(data[0]).toEqual(value);
      done();
    });

    fixture.detectChanges();

    let items = fixture.debugElement.queryAll(By.css('span'));
    items[0].triggerEventHandler('click', null);
  });
});