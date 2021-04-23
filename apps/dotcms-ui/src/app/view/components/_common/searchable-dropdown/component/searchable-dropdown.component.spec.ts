import { By } from '@angular/platform-browser';
import { ComponentFixture, waitForAsync, fakeAsync, tick, TestBed } from '@angular/core/testing';
import { DebugElement, Component, Input } from '@angular/core';
import { DotMessageService } from '@services/dot-message/dot-messages.service';
import { MockDotMessageService } from '../../../../../test/dot-message-service.mock';
import { SEARCHABLE_NGFACES_MODULES } from '../searchable-dropdown.module';
import { SearchableDropdownComponent } from './searchable-dropdown.component';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { DotIconModule } from '@dotcms/ui';
import * as _ from 'lodash';
import { UiDotIconButtonModule } from '@components/_common/dot-icon-button/dot-icon-button.module';
import { DotPipesModule } from '@pipes/dot-pipes.module';
@Component({
    selector: 'dot-host-component',
    template: ` <dot-searchable-dropdown
        [action]="action"
        [cssClass]="cssClass"
        [data]="data"
        [labelPropertyName]="labelPropertyName"
        [multiple]="multiple"
        [pageLinkSize]="pageLinkSize"
        [persistentPlaceholder]="persistentPlaceholder"
        [placeholder]="placeholder"
        [rows]="rows"
        [totalRecords]="totalRecords"
        [valuePropertyName]="valuePropertyName"
        [overlayWidth]="overlayWidth"
        [width]="width"
        [disabled]="disabled"
    >
    </dot-searchable-dropdown>`
})
class HostTestComponent {
    @Input()
    data: any[];

    @Input()
    cssClass: string;

    @Input() action: (action: any) => void;

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
    overlayWidth: string;

    @Input()
    multiple: boolean;

    @Input()
    disabled: boolean;
}

describe('SearchableDropdownComponent', () => {
    const NROWS = 6;

    let hostFixture: ComponentFixture<HostTestComponent>;
    let hostComp: HostTestComponent;
    let de: DebugElement;
    let comp: SearchableDropdownComponent;
    const data = [];
    let rows: number;
    let pageLinkSize: number;
    let mainButton: DebugElement;

    beforeEach(
        waitForAsync(() => {
            const messageServiceMock = new MockDotMessageService({
                search: 'Search'
            });

            TestBed.configureTestingModule({
                declarations: [SearchableDropdownComponent, HostTestComponent],
                imports: [
                    ...SEARCHABLE_NGFACES_MODULES,
                    BrowserAnimationsModule,
                    DotIconModule,
                    UiDotIconButtonModule,
                    DotPipesModule
                ],
                providers: [{ provide: DotMessageService, useValue: messageServiceMock }]
            }).compileComponents();

            hostFixture = TestBed.createComponent(HostTestComponent);
            hostComp = hostFixture.componentInstance;
            de = hostFixture.debugElement.query(By.css('dot-searchable-dropdown'));
            comp = de.componentInstance;

            for (let i = 0; i < NROWS; i++) {
                data[i] = {
                    id: i,
                    label: `site-${i}`,
                    name: `site-${i}`,
                    parentPermissionable: {
                        hostname: 'demo.dotcms.com'
                    }
                };
            }

            rows = NROWS / 3;
            pageLinkSize = 1;

            hostComp.totalRecords = NROWS;
            hostComp.rows = rows;
            hostComp.pageLinkSize = pageLinkSize;
        })
    );

    beforeEach(() => {
        hostComp.placeholder = 'placeholder';
        hostFixture.detectChanges();

        mainButton = de.query(By.css('button'));
        mainButton.nativeElement.dispatchEvent(new MouseEvent('click'));
    });

    it('should have placeholder set', () => {
        expect(mainButton.nativeElement.innerText).toBe('placeholder');
    });

    it('should disabled', () => {
        comp.disabled = true;
        hostFixture.detectChanges();

        expect(mainButton.componentInstance.disabled).toBe(true);
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

    it('should renderer the data', () => {
        hostComp.data = data;
        hostComp.labelPropertyName = 'name';

        hostFixture.detectChanges();

        const pdataview = de.query(By.css('p-dataview')).componentInstance;

        expect(
            hostComp.data.map((item) => {
                item.label = item.name;
                return item;
            })
        ).toEqual(pdataview.value);
    });

    it('should render a string property in p-dataview', () => {
        hostComp.data = data;
        hostComp.labelPropertyName = 'name';

        hostFixture.detectChanges();

        const dataviewDataEl = de.query(
            By.css('p-dataview .p-dataview-content .searchable-dropdown__data-list-item')
        );
        expect(dataviewDataEl.nativeElement.textContent).toEqual('site-0');
    });

    it('should set CSS class, width', fakeAsync(() => {
        hostComp.data = data;
        hostComp.cssClass = 'testClass';
        hostComp.overlayWidth = '650px';
        hostFixture.detectChanges();
        tick();

        const overlay = de.query(By.css('.p-overlaypanel'));
        expect(comp.cssClass).toContain('searchable-dropdown paginator');

        expect(overlay.componentInstance.styleClass).toBe('testClass');
        expect(overlay.componentInstance.style.width).toEqual('650px');
    }));

    it('should reset Panel Min Height', () => {
        comp.overlayPanelMinHeight = '456';
        comp.resetPanelMinHeight();
        expect(comp.overlayPanelMinHeight).toBe('');
    });

    it('should display Action button', () => {
        hostComp.action = () => {};

        hostFixture.detectChanges();
        const actionBtn = de.query(By.css('.searchable-dropdown__search-action dot-icon-button'))
            .componentInstance;
        expect(actionBtn.icon).toBe('add');
    });

    it('should display defaultFilterTemplate', () => {
        hostFixture.detectChanges();
        const searchInput = de.query(By.css('[data-testid="searchInput"]'));
        console.log(searchInput.attributes);
        expect(searchInput).not.toBeNull();
    });

    it('should not display Action button', () => {
        const actionBtn = de.query(By.css('.searchable-dropdown__search-action dot-icon-button'));
        expect(actionBtn).toBeNull();
    });

    it('should render a string array of properties in p-dataview', () => {
        hostComp.data = data;
        hostComp.labelPropertyName = ['name', 'parentPermissionable.hostname'];

        hostFixture.detectChanges();

        const dataviewDataEl = de.query(
            By.css('p-dataview .p-dataview-content .searchable-dropdown__data-list-item')
        );
        expect(dataviewDataEl.nativeElement.textContent).toEqual('site-0 - demo.dotcms.com');
    });

    it('should the pageChange call the paginate method', fakeAsync(() => {
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

        const dataview = hostFixture.debugElement.query(By.css('p-dataview'));
        const dataviewComponentInstance = dataview.componentInstance;

        dataviewComponentInstance.onLazyLoad.emit({
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
    }));

    describe('emit the change event', () => {
        let items;
        let dataExpected;

        beforeEach(() => {
            hostComp.data = data;
            hostComp.labelPropertyName = 'name';
            spyOn(comp.change, 'emit');

            hostFixture.detectChanges();
            items = de.queryAll(By.css('.searchable-dropdown__data-list-item'));

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
        hostComp.placeholder = 'testing placeholder';

        hostFixture.detectChanges();
        expect(hostComp.placeholder).toEqual(comp.valueString);
    });
});

@Component({
    selector: 'dot-host-component',
    template: ` <dot-searchable-dropdown
        #searchableDropdown
        [action]="action"
        [cssClass]="cssClass"
        [data]="data"
        [labelPropertyName]="labelPropertyName"
        [multiple]="multiple"
        [pageLinkSize]="pageLinkSize"
        [persistentPlaceholder]="persistentPlaceholder"
        [placeholder]="placeholder"
        [rows]="rows"
        [totalRecords]="totalRecords"
        [valuePropertyName]="valuePropertyName"
        [width]="width"
    >
        <ng-template let-data="item" pTemplate="listItem">
            <div
                class="searchable-dropdown__data-list-item templateTestItem"
                (click)="handleClick(item)"
            >
                {{ data.label }}
            </div>
        </ng-template>
        <ng-template let-persona="item" pTemplate="select">
            <div
                class="dot-persona-selector__testContainer"
                (click)="searchableDropdown.toggleOverlayPanel($event)"
            >
                Test
            </div>
        </ng-template>
    </dot-searchable-dropdown>`
})
class HostTestExternalTemplateComponent {
    @Input() data: any[];

    @Input()
    cssClass: string;

    @Input() action: (action: any) => void;

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

    let hostFixture: ComponentFixture<HostTestExternalTemplateComponent>;
    let hostComp: HostTestExternalTemplateComponent;
    let de: DebugElement;
    const data = [];
    let rows: number;
    let pageLinkSize: number;
    let mainButton: DebugElement;

    beforeEach(
        waitForAsync(() => {
            const messageServiceMock = new MockDotMessageService({
                search: 'Search'
            });

            TestBed.configureTestingModule({
                declarations: [SearchableDropdownComponent, HostTestExternalTemplateComponent],
                imports: [
                    ...SEARCHABLE_NGFACES_MODULES,
                    BrowserAnimationsModule,
                    DotIconModule,
                    UiDotIconButtonModule,
                    DotPipesModule
                ],
                providers: [{ provide: DotMessageService, useValue: messageServiceMock }]
            }).compileComponents();

            hostFixture = TestBed.createComponent(HostTestExternalTemplateComponent);
            hostComp = hostFixture.componentInstance;
            de = hostFixture.debugElement.query(By.css('dot-searchable-dropdown'));

            for (let i = 0; i < NROWS; i++) {
                data[i] = {
                    id: i,
                    label: `site-${i}`,
                    name: `site-${i}`,
                    parentPermissionable: {
                        hostname: 'demo.dotcms.com'
                    }
                };
            }

            rows = 10;
            pageLinkSize = 1;

            hostComp.totalRecords = NROWS;
            hostComp.rows = rows;
            hostComp.pageLinkSize = pageLinkSize;
        })
    );

    beforeEach(() => {
        hostComp.placeholder = 'placeholder';
        hostComp.data = data;
        hostComp.labelPropertyName = 'name';
        hostFixture.detectChanges();

        mainButton = de.query(By.css('.dot-persona-selector__testContainer'));
        mainButton.nativeElement.dispatchEvent(new MouseEvent('click'));
    });

    it('should render external dropdown template', () => {
        const dropdown = de.query(By.css('.dot-persona-selector__testContainer')).nativeElement;
        expect(dropdown).not.toBeNull();
    });

    it('should render external listItem template', () => {
        hostFixture.detectChanges();
        const listItems = de.queryAll(
            By.css('.searchable-dropdown__data-list-item.templateTestItem')
        );
        expect(listItems.length).toBe(6);
    });
});
