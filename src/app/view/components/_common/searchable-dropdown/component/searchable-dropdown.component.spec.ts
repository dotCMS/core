import { By } from '@angular/platform-browser';
import { ComponentFixture, async, fakeAsync, tick } from '@angular/core/testing';
import { DOTTestBed } from '../../../../../test/dot-test-bed';
import { DebugElement, Component, Input } from '@angular/core';
import { DotMessageService } from '@services/dot-messages-service';
import { MockDotMessageService } from '../../../../../test/dot-message-service.mock';
import { SEARCHABLE_NGFACES_MODULES } from '../searchable-dropdown.module';
import { SearchableDropdownComponent } from './searchable-dropdown.component';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { DotIconModule } from '../../dot-icon/dot-icon.module';
import * as _ from 'lodash';
@Component({
    selector: 'dot-host-component',
    template: `<dot-searchable-dropdown  [data] = "data"
                                         [labelPropertyName] = "labelPropertyName"
                                         [valuePropertyName] = "valuePropertyName"
                                         [pageLinkSize] = "pageLinkSize"
                                         [rows] = "rows"
                                         [totalRecords] = "totalRecords"
                                         [placeholder] = "placeholder"
                                         [persistentPlaceholder] = "persistentPlaceholder"
                                         [width] = "width"
                                         [multiple] = "multiple">
               </dot-searchable-dropdown>`
})
class HostTestComponent {
    @Input()
    data: any[];

    @Input()
    labelPropertyName: string | string[];

    @Input()
    valuePropertyName: string;

    @Input()
    pageLinkSize = 3;

    @Input()
    rows: number;

    @Input()
    totalRecords: number;

    @Input()
    placeholder = '';

    @Input()
    persistentPlaceholder: boolean;

    @Input()
    width: string;

    @Input()
    multiple: boolean;
}

describe('SearchableDropdownComponent', () => {
    const NROWS = 6;

    let hostFixture: ComponentFixture<HostTestComponent>;
    let de: DebugElement;
    let comp: SearchableDropdownComponent;
    const data = [];
    let rows: number;
    let pageLinkSize: number;
    let mainButton: DebugElement;

    beforeEach(async(() => {
        const messageServiceMock = new MockDotMessageService({
            search: 'Search'
        });

        DOTTestBed.configureTestingModule({
            declarations: [SearchableDropdownComponent, HostTestComponent],
            imports: [...SEARCHABLE_NGFACES_MODULES, BrowserAnimationsModule, DotIconModule],
            providers: [{ provide: DotMessageService, useValue: messageServiceMock }]
        });

        hostFixture = DOTTestBed.createComponent(HostTestComponent);
        de = hostFixture.debugElement.query(By.css('dot-searchable-dropdown'));
        comp = de.componentInstance;

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

        hostFixture.componentInstance.totalRecords = NROWS;
        hostFixture.componentInstance.rows = rows;
        hostFixture.componentInstance.pageLinkSize = pageLinkSize;
    }));

    beforeEach(() => {
        hostFixture.componentInstance.placeholder = 'placeholder';
        hostFixture.detectChanges();

        mainButton = de.query(By.css('button'));
        mainButton.triggerEventHandler('click', {});
    });

    it('should renderer the pagination links', () => {

        hostFixture.detectChanges();

        const paginator = de.query(By.css('p-paginator'));

        const componentInstance = paginator.componentInstance;
        const rowParameter = componentInstance.rows;
        const totalRecordsParam = componentInstance.totalRecords;
        const pageLinkSizeParam = componentInstance.pageLinkSize;

        expect(rows).toEqual(rowParameter);
        expect(NROWS).toEqual(totalRecordsParam);
        expect(pageLinkSize).toEqual(pageLinkSizeParam);
    });

    it('should renderer the datas', () => {
        hostFixture.componentInstance.data = data;
        hostFixture.componentInstance.labelPropertyName = 'name';

        hostFixture.detectChanges();

        const pDataList = de.query(By.css('p-dataList')).componentInstance;

        expect(hostFixture.componentInstance.data.map(item => {
            item.label = item.name;
            return item;
        })).toEqual(pDataList.value);
    });

    it('should render a string property in p-dataList', () => {
        hostFixture.componentInstance.data = data;
        hostFixture.componentInstance.labelPropertyName = 'name';

        hostFixture.detectChanges();

        const dataListDataEl = de.query(By.css('p-dataList ul li span'));
        expect(dataListDataEl.nativeElement.textContent).toEqual('site-0');
    });

    it('should render a string array of properties in p-dataList', () => {
        hostFixture.componentInstance.data = data;
        hostFixture.componentInstance.labelPropertyName = ['name', 'parentPermissionable.hostname'];

        hostFixture.detectChanges();

        const dataListDataEl = de.query(By.css('p-dataList ul li span'));
        expect(dataListDataEl.nativeElement.textContent).toEqual('site-0 - demo.dotcms.com');
    });

    it('should the pageChange call the paginate method',
        fakeAsync(() => {
            const first = 2;
            const page = 3;
            const pageCount = 4;
            rows = 5;
            const filter = 'filter';
            let event;

            comp.pageChange.subscribe((e) => {
                event = e;
            });

            hostFixture.detectChanges();
            const input = hostFixture.debugElement.query(By.css('input[type="text"]'));
            input.nativeElement.value = filter;

            const dataList = hostFixture.debugElement.query(By.css('p-dataList'));
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
        let dataExpected;

        beforeEach(() => {
            hostFixture.componentInstance.data = data;
            hostFixture.componentInstance.labelPropertyName = 'name';
            spyOn(comp.change, 'emit');

            hostFixture.detectChanges();
            items = de.queryAll(By.css('span'));

            dataExpected = _.cloneDeep(data[0]);
            dataExpected.label = dataExpected.name;
        });

        it('should change the value', () => {
            items[0].triggerEventHandler('click', null);
            expect(comp.change.emit).toHaveBeenCalledWith(dataExpected);
        });

        it('should emit the same value twice when multiple equal true', () => {
            comp.multiple = true;

            items[0].triggerEventHandler('click', null);
            items[0].triggerEventHandler('click', null);

            expect(comp.change.emit).toHaveBeenCalledWith(dataExpected);
            expect(comp.change.emit).toHaveBeenCalledTimes(2);
        });

        it('should emit change the value once when multiple equal false', () => {
            items[0].triggerEventHandler('click', null);
            items[0].triggerEventHandler('click', null);

            expect(comp.change.emit).toHaveBeenCalledWith(dataExpected);
            expect(comp.change.emit).toHaveBeenCalledTimes(1);
        });
    });

    it('should be valueString equals to placeholder', () => {
        hostFixture.componentInstance.placeholder = 'testing placeholder';

        hostFixture.detectChanges();
        expect(hostFixture.componentInstance.placeholder ).toEqual(comp.valueString);
    });

    it('should set width', () => {
        hostFixture.componentInstance.width = '50%';
        hostFixture.detectChanges();

        const button = de.query(By.css('button'));
        expect('50%').toEqual(button.styles.width);
    });

    it('should width undefined', () => {
        hostFixture.detectChanges();

        const button = de.query(By.css('button'));
        expect(button.styles.width).toBeNull();
    });
});
