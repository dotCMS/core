import { Subject } from 'rxjs';

import { CommonModule } from '@angular/common';
import { Component, DebugElement, Input } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';

import { ButtonModule } from 'primeng/button';
import { CheckboxModule } from 'primeng/checkbox';
import { DropdownModule } from 'primeng/dropdown';
import { DialogService } from 'primeng/dynamicdialog';
import { InputTextModule } from 'primeng/inputtext';
import { OverlayPanelModule } from 'primeng/overlaypanel';
import { SkeletonModule } from 'primeng/skeleton';
import { TableModule } from 'primeng/table';
import { TooltipModule } from 'primeng/tooltip';

import { of } from 'rxjs/internal/observable/of';

import { DotAutofocusModule } from '@directives/dot-autofocus/dot-autofocus.module';
import { DotRelativeDatePipe } from '@dotcms/app/view/pipes/dot-relative-date/dot-relative-date.pipe';
import { DotMessageService } from '@dotcms/data-access';
import {
    CoreWebService,
    CoreWebServiceMock,
    DotcmsConfigService,
    SiteService
} from '@dotcms/dotcms-js';
import { DotMessagePipe, UiDotIconButtonModule } from '@dotcms/ui';
import {
    DotcmsConfigServiceMock,
    dotcmsContentletMock,
    dotcmsContentTypeBasicMock,
    MockDotMessageService,
    mockSites
} from '@dotcms/utils-testing';

import { DotPagesListingPanelComponent } from './dot-pages-listing-panel.component';

import { DotPageStore } from '../dot-pages-store/dot-pages.store';

@Component({
    selector: 'dot-icon',
    template: ''
})
class MockDotIconComponent {
    @Input() name: string;
}

const messageServiceMock = new MockDotMessageService({});

export const favoritePagesInitialTestData = [
    {
        ...dotcmsContentletMock,
        live: true,
        baseType: 'CONTENT',
        modDate: '2020-09-02 16:45:15.569',
        title: 'preview1',
        screenshot: 'test1',
        url: '/index1?host_id=A&language_id=1&device_inode=123',
        owner: 'admin'
    },
    {
        ...dotcmsContentletMock,
        title: 'preview2',
        modDate: '2020-09-02 16:45:15.569',
        screenshot: 'test2',
        url: '/index2',
        owner: 'admin2'
    }
];

describe('DotPagesListingPanelComponent', () => {
    let fixture: ComponentFixture<DotPagesListingPanelComponent>;
    let component: DotPagesListingPanelComponent;
    let de: DebugElement;
    let store: DotPageStore;

    const switchSiteSubject = new Subject();

    class storeMock {
        get vm$() {
            return of({
                favoritePages: {
                    items: [],
                    showLoadMoreButton: false,
                    total: 0
                },
                isEnterprise: true,
                environments: true,
                languages: [],
                loggedUser: {
                    id: 'admin',
                    canRead: { contentlets: true, htmlPages: true },
                    canWrite: { contentlets: true, htmlPages: true }
                },
                pages: {
                    actionMenuDomId: '',
                    items: [...favoritePagesInitialTestData],
                    addToBundleCTId: 'test1'
                },
                languageOptions: [
                    { label: 'En-en', value: 1 },
                    { label: 'ES-es', value: 2 }
                ],
                languageLabels: { 1: 'En-en', 2: 'Es-es' }
            });
        }

        get languageOptions$() {
            return of([
                { label: 'En-en', value: 1 },
                { label: 'ES-es', value: 2 }
            ]);
        }

        get languageLabels$() {
            return of({ 1: 'En-en', 2: 'Es-es' });
        }

        get actionMenuDomId$() {
            return of('');
        }

        get pageTypes$() {
            return of([{ ...dotcmsContentTypeBasicMock }]);
        }

        getPages(): void {
            /* */
        }

        getPageTypes(): void {
            /* */
        }

        setKeyword(): void {
            /* */
        }

        setLanguageId(): void {
            /* */
        }

        setArchived(): void {
            /* */
        }

        setSessionStorageFilterParams(): void {
            /* */
        }
    }

    describe('Empty state', () => {
        beforeEach(() => {
            TestBed.configureTestingModule({
                declarations: [DotPagesListingPanelComponent, MockDotIconComponent],
                imports: [
                    CommonModule,
                    ButtonModule,
                    CheckboxModule,
                    DropdownModule,
                    DotAutofocusModule,
                    DotMessagePipe,
                    DotRelativeDatePipe,
                    InputTextModule,
                    SkeletonModule,
                    TableModule,
                    TooltipModule,
                    UiDotIconButtonModule,
                    OverlayPanelModule
                ],
                providers: [
                    DialogService,
                    { provide: DotcmsConfigService, useClass: DotcmsConfigServiceMock },
                    { provide: CoreWebService, useClass: CoreWebServiceMock },
                    { provide: DotPageStore, useClass: storeMock },
                    { provide: DotMessageService, useValue: messageServiceMock },
                    {
                        provide: SiteService,
                        useValue: {
                            get currentSite() {
                                return undefined;
                            },

                            get switchSite$() {
                                return switchSiteSubject.asObservable();
                            }
                        }
                    }
                ]
            }).compileComponents();
        });

        beforeEach(async () => {
            store = TestBed.inject(DotPageStore);
            fixture = TestBed.createComponent(DotPagesListingPanelComponent);
            de = fixture.debugElement;
            component = fixture.componentInstance;

            spyOn(store, 'getPages');
            spyOn(store, 'getPageTypes');
            spyOn(store, 'setKeyword');
            spyOn(store, 'setLanguageId');
            spyOn(store, 'setArchived');
            spyOn(store, 'setSessionStorageFilterParams');
            spyOn(component.goToUrl, 'emit');

            fixture.detectChanges();
            await fixture.whenStable();
        });

        it('should set table with params', () => {
            const elem = de.query(By.css('p-table')).componentInstance;
            expect(elem.loading).toBe(undefined);
            expect(elem.lazy).toBe(true);
            expect(elem.selectionMode).toBe('single');
            expect(elem.sortField).toEqual('modDate');
            expect(elem.sortOrder).toEqual(-1);
            expect(elem.rows).toEqual(40);
            expect(elem.paginator).toEqual(true);
            expect(elem.showPageLinks).toEqual(false);
            expect(elem.showCurrentPageReport).toEqual(true);
            expect(elem.showFirstLastIcon).toEqual(false);
        });

        it('should contain header with filter for keyword, language and archived', () => {
            const elem = de.query(By.css('[data-testId="dot-pages-listing-header__keyword-input"'));
            expect(elem.attributes.dotAutofocus).toBeDefined();
            expect(elem.attributes.placeholder).toBe('Type-To-Search');
            expect(de.query(By.css('.dot-pages-listing-header__inputs p-dropdown'))).toBeTruthy();
            expect(
                de.query(By.css('.dot-pages-listing-header__inputs p-checkbox')).componentInstance
                    .label
            ).toBe('Show-Archived');
        });

        it('should getPages method from store have been called', () => {
            expect(store.getPages).toHaveBeenCalledWith({
                offset: 0,
                sortField: 'modDate',
                sortOrder: -1
            });
        });

        it('should send event to create page when button clicked', () => {
            const elem = de.query(By.css('[data-testId="createPageButton"'));
            elem.triggerEventHandler('click', {});

            expect(store.getPageTypes).toHaveBeenCalledTimes(1);
        });

        it('should send event to filter keyword', () => {
            const elem = de.query(By.css('.dot-pages-listing-header__inputs input'));
            elem.triggerEventHandler('input', { target: { value: 'test' } });

            expect(store.setKeyword).toHaveBeenCalledWith('test');
            expect(store.getPages).toHaveBeenCalledWith({ offset: 0 });
            expect(store.setSessionStorageFilterParams).toHaveBeenCalledTimes(1);
        });

        it('should send event to filter keyword when cleaning', () => {
            const elem = de.query(By.css('.dot-pages-listing-header__inputs input'));
            elem.triggerEventHandler('input', { target: { value: 'test' } });

            const elemClean = de.query(
                By.css('[data-testid="dot-pages-listing-header__keyword-input-clear"]')
            );

            elemClean.triggerEventHandler('click', {});

            expect(store.setKeyword).toHaveBeenCalledWith('');
            expect(store.getPages).toHaveBeenCalledWith({ offset: 0 });
            expect(store.setSessionStorageFilterParams).toHaveBeenCalledTimes(2);
        });

        it('should send event to filter language', () => {
            const elem = de.query(By.css('.dot-pages-listing-header__inputs p-dropdown'));
            elem.triggerEventHandler('onChange', { value: '1' });

            expect(store.setLanguageId).toHaveBeenCalledWith('1');
            expect(store.getPages).toHaveBeenCalledWith({ offset: 0 });
            expect(store.setSessionStorageFilterParams).toHaveBeenCalledTimes(1);
        });

        it('should send event to filter archived', () => {
            const elem = de.query(By.css('.dot-pages-listing-header__inputs p-checkbox'));
            elem.triggerEventHandler('onChange', { checked: '1' });

            expect(store.setArchived).toHaveBeenCalledWith('1');
            expect(store.getPages).toHaveBeenCalledWith({ offset: 0 });
            expect(store.setSessionStorageFilterParams).toHaveBeenCalledTimes(1);
        });

        it('should send event to emit URL value', () => {
            const elem = de.query(By.css('p-table'));
            elem.triggerEventHandler('onRowSelect', { data: { url: 'abc123', languageId: '1' } });

            expect(component.goToUrl.emit).toHaveBeenCalledOnceWith(
                'abc123?language_id=1&device_inode='
            );
        });

        it('should reload portlet only when the site change', () => {
            switchSiteSubject.next(mockSites[0]); // setting the site
            switchSiteSubject.next(mockSites[1]); // switching the site
            expect(store.getPages).toHaveBeenCalledWith({ offset: 0 });
        });
    });
});
