import { By } from '@angular/platform-browser';
import { ComponentFixture, async } from '@angular/core/testing';
import { DOTTestBed } from '../../../../../test/dot-test-bed';
import { DebugElement, SimpleChange } from '@angular/core';
import { MessageService } from '../../../../../api/services/messages-service';
import { MockMessageService } from '../../../../../test/message-service.mock';
import { SEARCHABLE_NGFACES_MODULES } from '../searchable-dropdown.module';
import { SearchableDropdownComponent } from './searchable-dropdown.component';
import { fakeAsync, tick } from '@angular/core/testing';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

describe('SearchableDropdownComponent', () => {
    const NROWS = 6;

    let comp: SearchableDropdownComponent;
    let fixture: ComponentFixture<SearchableDropdownComponent>;
    let de: DebugElement;
    let el: HTMLElement;
    const data = [];
    let rows: number;
    let pageLinkSize: number;

    beforeEach(
        async(() => {
            const messageServiceMock = new MockMessageService({
                search: 'Search'
            });

            DOTTestBed.configureTestingModule({
                declarations: [SearchableDropdownComponent],
                imports: [...SEARCHABLE_NGFACES_MODULES, BrowserAnimationsModule],
                providers: [{ provide: MessageService, useValue: messageServiceMock }]
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
        })
    );

    it('should renderer the pagination links', () => {
        fixture.detectChanges();

        const paginator = fixture.debugElement.query(By.css('p-paginator'));

        const componentInstance = paginator.componentInstance;
        const rowParameter = componentInstance.rows;
        const totalRecordsParam = componentInstance.totalRecords;
        const pageLinkSizeParam = componentInstance.pageLinkSize;

        expect(rows).toEqual(rowParameter);
        expect(NROWS).toEqual(totalRecordsParam);
        expect(pageLinkSize).toEqual(pageLinkSizeParam);
    });

    it('should renderer the datas', () => {
        comp.data = data;
        comp.labelPropertyName = 'name';

        fixture.detectChanges();

        const pDataList = fixture.debugElement.query(By.css('p-dataList')).componentInstance;
        expect(comp.data).toEqual(pDataList.value);
    });

    it(
        'should the pageChange call the paginate method',
        fakeAsync(() => {
            const first = 2;
            const page = 3;
            const pageCount = 4;
            rows = 5;
            const filter = 'filter';
            let event;

            const input = fixture.debugElement.query(By.css('input[type="text"]'));
            input.nativeElement.value = filter;

            comp.pageChange.subscribe(e => {
                event = e;
            });

            fixture.detectChanges();
            const dataList = fixture.debugElement.query(By.css('p-dataList'));
            const dataListComponentInstance = dataList.componentInstance;

            dataListComponentInstance.onLazyLoad.emit({
                first: first,
                page: page,
                pageCount: pageCount,
                rows: rows
            });

            tick();

            expect(first).toEqual(event.first);
            expect(page).toEqual(event.page);
            expect(pageCount).toEqual(event.pageCount);
            expect(rows).toEqual(event.rows);
            expect(filter).toEqual(event.filter);
        })
    );

    it('should change the value', done => {
        comp.data = data;
        comp.labelPropertyName = 'name';
        comp.change.subscribe(value => {
            expect(data[0]).toEqual(value);
            done();
        });

        fixture.detectChanges();

        const items = fixture.debugElement.queryAll(By.css('span'));
        items[0].triggerEventHandler('click', null);
    });

    it('should be valueString equals to placeholder', () => {
        const placeholderValue = 'testing placeholder';

        comp.ngOnChanges({
            placeholder: new SimpleChange(null, placeholderValue, true)
        });

        expect(placeholderValue).toEqual(comp.valueString);
    });

    it('should set width', () => {
        comp.width = '50%';
        fixture.detectChanges();

        const button = fixture.debugElement.query(By.css('button'));
        expect('50%').toEqual(button.styles.width);
    });

    it('should width undefined', () => {
        fixture.detectChanges();

        const button = fixture.debugElement.query(By.css('button'));
        expect(button.styles.width).toBeNull();
    });
});
