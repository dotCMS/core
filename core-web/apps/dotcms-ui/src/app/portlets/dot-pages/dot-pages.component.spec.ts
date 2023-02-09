/* eslint-disable @typescript-eslint/no-unused-vars */
import { Observable } from 'rxjs';

import { CommonModule } from '@angular/common';
import { Component, DebugElement, EventEmitter, Injectable, Input, Output } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { ButtonModule } from 'primeng/button';
import { DialogService } from 'primeng/dynamicdialog';
import { MenuModule } from 'primeng/menu';
import { PanelModule } from 'primeng/panel';
import { TabViewModule } from 'primeng/tabview';

import { of } from 'rxjs/internal/observable/of';

import { DotMessageDisplayServiceMock } from '@components/dot-message-display/dot-message-display.component.spec';
import { DotMessageSeverity, DotMessageType } from '@components/dot-message-display/model';
import { DotMessageDisplayService } from '@components/dot-message-display/services';
import { DotRouterService } from '@dotcms/app/api/services/dot-router/dot-router.service';
import { DotPipesModule } from '@dotcms/app/view/pipes/dot-pipes.module';
import { DotESContentService, DotEventsService, DotMessageService } from '@dotcms/data-access';
import { DotcmsEventsService, DotEventsSocket, DotEventsSocketURL } from '@dotcms/dotcms-js';
import { DotIconModule } from '@dotcms/ui';
import {
    dotcmsContentletMock,
    DotcmsEventsServiceMock,
    MockDotMessageService,
    MockDotRouterService
} from '@dotcms/utils-testing';

import { DotPagesCardEmptyModule } from './dot-pages-favorite-panel/dot-pages-card-empty/dot-pages-card-empty.module';
import { DotPagesCardModule } from './dot-pages-favorite-panel/dot-pages-card/dot-pages-card.module';
import { DotPageStore } from './dot-pages-store/dot-pages.store';
import { DotActionsMenuEventParams, DotPagesComponent } from './dot-pages.component';

// @Component({
//     selector: 'dot-pages-card',
//     template: '<ng-content></ng-content>',
//     styleUrls: []
// })
// export class DotPagesCardMockComponent {
//     @Input() ownerPage: boolean;
//     @Input() imageUri: string;
//     @Input() title: string;
//     @Input() url: string;
// }

// const messageServiceMock = new MockDotMessageService({
//     favorites: 'Favorites',
//     'see.all': 'See All',
//     'see.less': 'See Less',
//     'favoritePage.listing.empty.header': 'Header',
//     'favoritePage.listing.empty.content': 'Content'
// });

// @Injectable()
// class MockESPaginatorService {
//     paginationPerPage = 4;
//     totalRecords = 4;

//     public get(): Observable<unknown[]> {
//         return null;
//     }
// }

@Component({
    selector: 'dot-pages-favorite-panel',
    template: ''
})
class MockDotPagesFavoritePanelComponent {
    @Output() goToUrl = new EventEmitter<string>();
    @Output() showActionsMenu = new EventEmitter<DotActionsMenuEventParams>();
}

@Component({
    selector: 'dot-pages-listing-panel',
    template: ''
})
class MockDotPagesListingPanelComponent {
    @Output() goToUrl = new EventEmitter<string>();
    @Output() showActionsMenu = new EventEmitter<DotActionsMenuEventParams>();
}

@Component({
    selector: 'dot-add-to-bundle',
    template: ''
})
class MockDotAddToBundleComponent {
    @Input() assetIdentifier: string;
    @Output() cancel = new EventEmitter<boolean>();
}

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

const storeMock = {
    get actionMenuDomId$() {
        return of('');
    },
    get languageOptions$() {
        return of([]);
    },
    get languageLabels$() {
        return of({});
    },
    clearMenuActions: jasmine.createSpy(),
    getFavoritePages: jasmine.createSpy(),
    getPages: jasmine.createSpy(),
    showActionsMenu: jasmine.createSpy(),
    setInitialStateData: jasmine.createSpy(),
    limitFavoritePages: jasmine.createSpy(),
    vm$: of({
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
            items: [],
            addToBundleCTId: 'test1'
        }
    })
};

describe('DotPagesComponent', () => {
    let fixture: ComponentFixture<DotPagesComponent>;
    let component: DotPagesComponent;
    let de: DebugElement;
    let store: DotPageStore;
    let dotRouterService: DotRouterService;
    let dotMessageDisplayService: DotMessageDisplayService;
    // let dialogService: DialogService;

    beforeEach(() => {
        TestBed.configureTestingModule({
            declarations: [
                MockDotPagesFavoritePanelComponent,
                MockDotPagesListingPanelComponent,
                MockDotAddToBundleComponent,
                DotPagesComponent
            ],
            imports: [
                MenuModule

                // BrowserAnimationsModule,
                // CommonModule,
                // DotPagesCardModule,
                // DotPagesCardEmptyModule,
                // DotPipesModule,
                // DotIconModule,
                // PanelModule,
                // ButtonModule,
                // TabViewModule
            ],
            providers: [
                DotEventsService,
                // { provide: DotcmsEventsService, useClass: DotcmsEventsServiceMock },
                { provide: DotMessageDisplayService, useClass: DotMessageDisplayServiceMock },
                { provide: DotRouterService, useClass: MockDotRouterService }
                // {
                //     provide: DotMessageService,
                //     useValue: messageServiceMock
                // },
                // { provide: DotESContentService, useClass: MockESPaginatorService }
            ]
        }).compileComponents();
    });

    // describe('Empty State', () => {
    beforeEach(() => {
        TestBed.overrideProvider(DotPageStore, {
            useValue: storeMock
        });
        store = TestBed.inject(DotPageStore);
        dotRouterService = TestBed.inject(DotRouterService);
        dotMessageDisplayService = TestBed.inject(DotMessageDisplayService);
        fixture = TestBed.createComponent(DotPagesComponent);
        de = fixture.debugElement;
        component = fixture.componentInstance;

        fixture.detectChanges();
        spyOn(component.menu, 'hide');
        spyOn(dotMessageDisplayService, 'push');
    });

    it('should init store', () => {
        expect(store.setInitialStateData).toHaveBeenCalledWith(5);
    });

    it('should have favorite page panel, menu, pages panel and DotAddToBundle components', () => {
        expect(de.query(By.css('dot-pages-favorite-panel'))).toBeTruthy();
        expect(de.query(By.css('p-menu'))).toBeTruthy();
        expect(de.query(By.css('dot-pages-listing-panel'))).toBeTruthy();
        expect(de.query(By.css('dot-add-to-bundle'))).toBeTruthy();
    });

    it('should call goToUrl method from DotPagesFavoritePanel', () => {
        const elem = de.query(By.css('dot-pages-favorite-panel'));
        elem.triggerEventHandler('goToUrl', '/page/1?lang=1');

        expect(dotRouterService.goToEditPage).toHaveBeenCalledWith({
            lang: '1',
            url: '/page/1'
        });
    });

    it('should call showActionsMenu method from DotPagesFavoritePanel', () => {
        const eventMock = new MouseEvent('click');
        Object.defineProperty(eventMock, 'currentTarget', {
            value: { id: 'test' },
            enumerable: true
        });

        const actionMenuParam = {
            event: eventMock,
            actionMenuDomId: 'test1',
            item: dotcmsContentletMock
        };

        const elem = de.query(By.css('dot-pages-favorite-panel'));
        elem.triggerEventHandler('showActionsMenu', actionMenuParam);

        expect(component.menu.hide).toHaveBeenCalledTimes(1);
        expect(store.showActionsMenu).toHaveBeenCalledWith({
            item: dotcmsContentletMock,
            actionMenuDomId: 'test1'
        });
    });

    it('should call goToUrl method from DotPagesListingPanel', () => {
        const elem = de.query(By.css('dot-pages-listing-panel'));
        elem.triggerEventHandler('goToUrl', '/page/1?lang=1');

        expect(dotRouterService.goToEditPage).toHaveBeenCalledWith({
            lang: '1',
            url: '/page/1'
        });
    });

    it('should call showActionsMenu method from DotPagesListingPanel', () => {
        const eventMock = new MouseEvent('click');
        Object.defineProperty(eventMock, 'currentTarget', {
            value: { id: 'test' },
            enumerable: true
        });

        const actionMenuParam = {
            event: eventMock,
            actionMenuDomId: 'test1',
            item: dotcmsContentletMock
        };

        const elem = de.query(By.css('dot-pages-listing-panel'));
        elem.triggerEventHandler('showActionsMenu', actionMenuParam);

        expect(component.menu.hide).toHaveBeenCalledTimes(1);
        expect(store.showActionsMenu).toHaveBeenCalledWith({
            item: dotcmsContentletMock,
            actionMenuDomId: 'test1'
        });
    });

    it('should call closedActionsMenu method from p-menu', () => {
        const elem = de.query(By.css('p-menu'));
        elem.triggerEventHandler('onHide', {});

        expect(store.clearMenuActions).toHaveBeenCalledTimes(1);
    });

    it('should call push method in dotMessageDisplayService once a dot-global-message is received', () => {
        const dotEventsService: DotEventsService = de.injector.get(DotEventsService);

        dotEventsService.notify('dot-global-message', { value: 'test3' });

        expect(dotMessageDisplayService.push).toHaveBeenCalledWith({
            life: 3000,
            message: 'test3',
            severity: DotMessageSeverity.SUCCESS,
            type: DotMessageType.SIMPLE_MESSAGE
        });
        expect(store.getPages).toHaveBeenCalledWith({ offset: 0 });
    });

    /*
        it('should set panel with empty state class', () => {
            const elem = de.query(By.css('p-panel'));
            expect(
                elem.nativeElement.classList.contains('dot-pages-panel__empty-state')
            ).toBeTruthy();
            expect(de.query(By.css('.dot-pages-panel-action__button'))).toBeFalsy();
        });

        it('should load empty pages cards container', () => {
            expect(de.queryAll(By.css('dot-pages-card-empty')).length).toBe(5);
            expect(
                de.query(By.css('.dot-pages-empty__container dot-icon')).componentInstance.name
            ).toBe('library_add');
            expect(de.query(By.css('.dot-pages-empty__header')).nativeElement.outerText).toBe(
                'Header'
            );
            expect(de.query(By.css('.dot-pages-empty__container p')).nativeElement.outerText).toBe(
                'Content'
            );
        });
        */
    // });

    /*
    describe('Loading 2 of 4 items', () => {
        beforeEach(() => {
            TestBed.overrideProvider(DotPageStore, {
                useValue: {
                    getFavoritePages: jasmine.createSpy(),
                    setInitialStateData: jasmine.createSpy(),
                    limitFavoritePages: jasmine.createSpy(),
                    vm$: of({
                        favoritePages: {
                            items: favoritePagesInitialTestData,
                            showLoadMoreButton: true,
                            total: 4
                        },
                        loggedUserId: 'admin'
                    })
                }
            });
            store = TestBed.inject(DotPageStore);
            dotRouterService = TestBed.inject(DotRouterService);
            fixture = TestBed.createComponent(DotPagesComponent);
            de = fixture.debugElement;
            dialogService = de.injector.get(DialogService);
            spyOn(dialogService, 'open');
            fixture.detectChanges();
        });

        it('should init store', () => {
            expect(store.setInitialStateData).toHaveBeenCalledWith(5);
        });

        it('should set panel inputs and attributes', () => {
            const elem = de.query(By.css('p-panel'));
            expect(elem.nativeElement.classList.contains('dot-pages-panel__expanded')).toBeFalse();
            expect(elem.componentInstance['iconPos']).toBe('start');
            expect(elem.componentInstance['expandIcon']).toBe('pi pi-angle-down');
            expect(elem.componentInstance['collapseIcon']).toBe('pi pi-angle-up');
            expect(elem.componentInstance['header']).toBe('Favorites');
            expect(elem.componentInstance['toggleable']).toBe(true);
        });

        it('should set secondary button in panel', () => {
            const elem = de.query(By.css('.dot-pages-panel-action__button span'));
            expect(elem.nativeElement.outerText.toUpperCase()).toBe('See All'.toUpperCase());
        });

        it('should load pages cards with attributes', () => {
            const elem = de.queryAll(By.css('dot-pages-card'));
            expect(elem.length).toBe(2);
            expect(
                elem[0].componentInstance.imageUri.includes(
                    favoritePagesInitialTestData[0].screenshot
                )
            ).toBe(true);
            expect(elem[0].componentInstance.title).toBe(favoritePagesInitialTestData[0].title);
            expect(elem[0].componentInstance.url).toBe(favoritePagesInitialTestData[0].url);
            expect(elem[0].componentInstance.ownerPage).toBe(true);
            expect(elem[1].componentInstance.ownerPage).toBe(false);
        });

        describe('Events', () => {
            it('should call event to load all items', () => {
                const elem = de.query(By.css('[data-testId="seeAllBtn"]'));
                elem.triggerEventHandler('click', {
                    stopPropagation: () => {
                        //
                    }
                });
                expect(store.getFavoritePages).toHaveBeenCalledWith(4);
            });

            it('should call edit method to open favorite page dialog', () => {
                const elem = de.query(By.css('dot-pages-card'));
                elem.triggerEventHandler('edit', { ...favoritePagesInitialTestData[0] });

                expect(dialogService.open).toHaveBeenCalledTimes(1);
            });

            it('should call redirect method in DotRouterService', () => {
                const elem = de.query(By.css('dot-pages-card'));
                elem.triggerEventHandler('goTo', {
                    stopPropagation: () => {
                        //
                    }
                });

                expect(dotRouterService.goToEditPage).toHaveBeenCalledWith({
                    device_inode: '123',
                    host_id: 'A',
                    language_id: '1',
                    url: '/index1'
                });
            });
        });
    });

    describe('Loading all items', () => {
        beforeEach(() => {
            TestBed.overrideProvider(DotPageStore, {
                useValue: {
                    getFavoritePages: jasmine.createSpy(),
                    setInitialStateData: jasmine.createSpy(),
                    limitFavoritePages: jasmine.createSpy(),
                    vm$: of({
                        favoritePages: {
                            items: [
                                ...favoritePagesInitialTestData,
                                ...favoritePagesInitialTestData
                            ],
                            showLoadMoreButton: true,
                            total: 4
                        },
                        loggedUserId: 'admin'
                    })
                }
            });
            store = TestBed.inject(DotPageStore);
            fixture = TestBed.createComponent(DotPagesComponent);
            de = fixture.debugElement;
            fixture.detectChanges();
        });

        it('should set panel inputs and attributes', () => {
            const elem = de.query(By.css('p-panel'));
            expect(elem.nativeElement.classList.contains('dot-pages-panel__expanded')).toBeTrue();
        });

        it('should set secondary button in panel', () => {
            const elem = de.query(By.css('.dot-pages-panel-action__button span'));
            expect(elem.nativeElement.outerText.toUpperCase()).toBe('See Less'.toUpperCase());
        });

        describe('Show less items', () => {
            it('should call event to show less items', () => {
                const elem = de.query(By.css('[data-testId="seeAllBtn"]'));
                elem.triggerEventHandler('click', {
                    stopPropagation: () => {
                        //
                    }
                });
                expect(store.limitFavoritePages).toHaveBeenCalledWith(5);
            });
        });
    });

    */
});
