import { By } from '@angular/platform-browser';
import { ComponentFixture, async } from '@angular/core/testing';
import { DOTTestBed } from '../../../../../test/dot-test-bed';
import { DebugElement, SimpleChange } from '@angular/core';
import { DotMessageService } from '../../../../../api/services/dot-messages-service';
import { MockDotMessageService } from '../../../../../test/dot-message-service.mock';
import { SEARCHABLE_NGFACES_MODULES } from '../searchable-dropdown.module';
import { SearchableDropdownComponent } from './searchable-dropdown.component';
import { fakeAsync, tick } from '@angular/core/testing';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { DotIconModule } from '../../dot-icon/dot-icon.module';

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
            const messageServiceMock = new MockDotMessageService({
                search: 'Search'
            });

            DOTTestBed.configureTestingModule({
                declarations: [SearchableDropdownComponent],
                imports: [...SEARCHABLE_NGFACES_MODULES, BrowserAnimationsModule, DotIconModule],
                providers: [{ provide: DotMessageService, useValue: messageServiceMock }]
            });

            fixture = DOTTestBed.createComponent(SearchableDropdownComponent);
            comp = fixture.componentInstance;
            de = fixture.debugElement;
            el = de.nativeElement;

            for (let i = 0; i < NROWS; i++) {
                data[i] = {
                    id: i,
                    name: `site-${i}`,
                    parentPermissionable: {
                        hostname: 'demo.dotcms.com'
                    }
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

    it('should render a string property in p-dataList', () => {
        comp.data = data;
        comp.labelPropertyName = 'name';

        fixture.detectChanges();

        const dataListDataEl = fixture.debugElement.query(By.css('p-dataList ul li span'));
        expect(dataListDataEl.nativeElement.textContent).toEqual('site-0');
    });

    it('should render a string array of properties in p-dataList', () => {
        comp.data = data;
        comp.labelPropertyName = ['name', 'parentPermissionable.hostname'];

        fixture.detectChanges();

        const dataListDataEl = fixture.debugElement.query(By.css('p-dataList ul li span'));
        expect(dataListDataEl.nativeElement.textContent).toEqual('site-0 - demo.dotcms.com');
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

    describe('emit the change event', () => {
        let items;

        beforeEach(() => {
            comp.data = data;
            comp.labelPropertyName = 'name';
            spyOn(comp.change, 'emit');

            fixture.detectChanges();
            items = fixture.debugElement.queryAll(By.css('span'));
        });

        it('should change the value', () => {
            items[0].triggerEventHandler('click', null);
            expect(comp.change.emit).toHaveBeenCalledWith(data[0]);
        });

        it('should emit the same value twice when multiple equal true', () => {
            comp.multiple = true;

            items[0].triggerEventHandler('click', null);
            items[0].triggerEventHandler('click', null);

            expect(comp.change.emit).toHaveBeenCalledWith(data[0]);
            expect(comp.change.emit).toHaveBeenCalledTimes(2);
        });

        it('should emit change the value once when multiple equal false', () => {
            items[0].triggerEventHandler('click', null);
            items[0].triggerEventHandler('click', null);

            expect(comp.change.emit).toHaveBeenCalledWith(data[0]);
            expect(comp.change.emit).toHaveBeenCalledTimes(1);
        });
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
