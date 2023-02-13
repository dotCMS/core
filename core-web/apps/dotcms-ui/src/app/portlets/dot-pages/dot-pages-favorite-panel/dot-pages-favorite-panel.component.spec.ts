/* eslint-disable @typescript-eslint/no-unused-vars */
import { Observable } from 'rxjs';

import { CommonModule } from '@angular/common';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { Component, DebugElement, EventEmitter, Injectable, Input, Output } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { ConfirmationService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { DialogService } from 'primeng/dynamicdialog';
import { MenuModule } from 'primeng/menu';
import { PanelModule } from 'primeng/panel';
import { TabViewModule } from 'primeng/tabview';

import { of } from 'rxjs/internal/observable/of';

import { DotGlobalMessageService } from '@components/_common/dot-global-message/dot-global-message.service';
import { PushPublishServiceMock } from '@components/_common/dot-push-publish-env-selector/dot-push-publish-env-selector.component.spec';
import { DotIframeService } from '@components/_common/iframe/service/dot-iframe/dot-iframe.service';
import { DotMessageDisplayServiceMock } from '@components/dot-message-display/dot-message-display.component.spec';
import { DotMessageSeverity, DotMessageType } from '@components/dot-message-display/model';
import { DotMessageDisplayService } from '@components/dot-message-display/services';
import { DotHttpErrorManagerService } from '@dotcms/app/api/services/dot-http-error-manager/dot-http-error-manager.service';
import { DotRouterService } from '@dotcms/app/api/services/dot-router/dot-router.service';
import { DotWizardService } from '@dotcms/app/api/services/dot-wizard/dot-wizard.service';
import { DotWorkflowEventHandlerService } from '@dotcms/app/api/services/dot-workflow-event-handler/dot-workflow-event-handler.service';
import { PushPublishService } from '@dotcms/app/api/services/push-publish/push-publish.service';
import { DotMessagePipeModule } from '@dotcms/app/view/pipes/dot-message/dot-message-pipe.module';
import { DotPipesModule } from '@dotcms/app/view/pipes/dot-pipes.module';
import {
    DotAlertConfirmService,
    DotCurrentUserService,
    DotESContentService,
    DotEventsService,
    DotLanguagesService,
    DotLicenseService,
    DotMessageService,
    DotWorkflowActionsFireService,
    DotWorkflowsActionsService
} from '@dotcms/data-access';
import {
    CoreWebService,
    CoreWebServiceMock,
    DotcmsConfigService,
    DotcmsEventsService,
    DotEventsSocket,
    DotEventsSocketURL,
    LoggerService,
    LoginService,
    StringUtils
} from '@dotcms/dotcms-js';
import { dotcmsContentletMock, MockDotMessageService } from '@dotcms/utils-testing';

import { DotPagesCardEmptyModule } from './dot-pages-card-empty/dot-pages-card-empty.module';
import { DotPagesCardModule } from './dot-pages-card/dot-pages-card.module';
import { DotPagesFavoritePanelComponent } from './dot-pages-favorite-panel.component';

import { DotPageStore } from '../dot-pages-store/dot-pages.store';

@Component({
    selector: 'dot-pages-card',
    template: '<ng-content></ng-content>',
    styleUrls: []
})
export class DotPagesCardMockComponent {
    @Input() ownerPage: boolean;
    @Input() imageUri: string;
    @Input() title: string;
    @Input() url: string;
}

@Component({
    selector: 'dot-icon',
    template: ''
})
class MockDotIconComponent {
    @Input() name: string;
}

const messageServiceMock = new MockDotMessageService({
    favorites: 'Favorites',
    'see.all': 'See All',
    'see.less': 'See Less',
    'favoritePage.listing.empty.header': 'Header',
    'favoritePage.listing.empty.content': 'Content'
});

@Injectable()
class MockESPaginatorService {
    paginationPerPage = 4;
    totalRecords = 4;

    public get(): Observable<unknown[]> {
        return null;
    }
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

/*
@Injectable()
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
                items: [],
                addToBundleCTId: 'test1'
            }
        });
    }
*/
// get actionMenuDomId$() {
//     return of('');
// }
// get languageOptions$() {
//     return of([]);
// }
// get languageLabels$() {
//     return of({});
// }
// clearMenuActions(): void {
//     /* */
// }
// getFavoritePages(_itemsPerPage: number): void {
//     /* */
// }
// getPages(_offset: number, _sortField?: string, _sortOrder?: number): void {
//     /* */
// }
// setInitialStateData(initialFavoritePagesLimit: number): void {
//     /* */
// }
// limitFavoritePages(_limit: number): void {
//     /* */
// }
// }

describe('DotPagesFavoritePanelComponent', () => {
    let fixture: ComponentFixture<DotPagesFavoritePanelComponent>;
    let component: DotPagesFavoritePanelComponent;
    let de: DebugElement;
    let store: DotPageStore;
    let dialogService: DialogService;

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
                    items: [],
                    addToBundleCTId: 'test1'
                }
            });
        }
    }

    describe('Empty state', () => {
        beforeEach(() => {
            TestBed.configureTestingModule({
                declarations: [DotPagesFavoritePanelComponent, MockDotIconComponent],
                imports: [
                    BrowserAnimationsModule,
                    DotMessagePipeModule,
                    ButtonModule,
                    DotPagesCardModule,
                    DotPagesCardEmptyModule,
                    PanelModule,
                    HttpClientTestingModule
                ],
                providers: [
                    DialogService,
                    { provide: CoreWebService, useClass: CoreWebServiceMock },
                    { provide: DotPageStore, useClass: storeMock },
                    { provide: DotMessageService, useValue: messageServiceMock }
                ]
            }).compileComponents();
        });

        // describe('Empty State', () => {
        beforeEach(() => {
            // TestBed.overrideProvider(DotPagesFavoritePanelComponent, {
            //     useValue: storeMock
            // });
            store = TestBed.inject(DotPageStore);
            // dotRouterService = TestBed.inject(DotRouterService);
            // dotMessageDisplayService = TestBed.inject(DotMessageDisplayService);
            fixture = TestBed.createComponent(DotPagesFavoritePanelComponent);
            de = fixture.debugElement;
            component = fixture.componentInstance;

            // spyOn(store, 'setInitialStateData');
            fixture.detectChanges();
            // spyOn(component.menu, 'hide');
            // spyOn(dotMessageDisplayService, 'push');
        });

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
    });

    describe('Loading 2 of 4 items', () => {
        class storeMock {
            get vm$() {
                return of({
                    favoritePages: {
                        items: [...favoritePagesInitialTestData],
                        showLoadMoreButton: true,
                        total: 4
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
                });
            }
            getFavoritePages(_itemsPerPage: number): void {
                /* */
            }
        }
        beforeEach(() => {
            TestBed.configureTestingModule({
                declarations: [DotPagesFavoritePanelComponent, MockDotIconComponent],
                imports: [
                    BrowserAnimationsModule,
                    DotMessagePipeModule,
                    ButtonModule,
                    DotPagesCardModule,
                    DotPagesCardEmptyModule,
                    PanelModule,
                    HttpClientTestingModule
                ],
                providers: [
                    DialogService,
                    { provide: CoreWebService, useClass: CoreWebServiceMock },
                    { provide: DotPageStore, useClass: storeMock },
                    { provide: DotMessageService, useValue: messageServiceMock }
                ]
            }).compileComponents();

            store = TestBed.inject(DotPageStore);
            dialogService = TestBed.inject(DialogService);
            fixture = TestBed.createComponent(DotPagesFavoritePanelComponent);
            de = fixture.debugElement;
            component = fixture.componentInstance;

            spyOn(store, 'getFavoritePages');
            spyOn(dialogService, 'open');
            spyOn(component.goToUrl, 'emit');
            spyOn(component.showActionsMenu, 'emit');

            fixture.detectChanges();
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

            it('should call showActionMenu method to send actions to parent component', () => {
                const elem = de.query(By.css('dot-pages-card'));
                const mouseEvent = new MouseEvent('click');
                const expectedParams = {
                    event: mouseEvent,
                    actionMenuDomId: 'favoritePageActionButton-1',
                    item: dotcmsContentletMock
                };
                elem.triggerEventHandler('showActionMenu', expectedParams);

                expect(component.showActionsMenu.emit).toHaveBeenCalledTimes(1);
            });

            it('should call redirect method in DotRouterService', () => {
                const elem = de.query(By.css('dot-pages-card'));
                elem.triggerEventHandler('goTo', {
                    stopPropagation: () => {
                        //
                    }
                });

                expect(component.goToUrl.emit).toHaveBeenCalledOnceWith(
                    favoritePagesInitialTestData[0].url
                );
            });
        });
    });

    describe('Loading all items', () => {
        class storeMock {
            get vm$() {
                return of({
                    favoritePages: {
                        items: [...favoritePagesInitialTestData, ...favoritePagesInitialTestData],
                        showLoadMoreButton: true,
                        total: 4
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
                });
            }
            getFavoritePages(_itemsPerPage: number): void {
                /* */
            }
            limitFavoritePages(_limit: number): void {
                /* */
            }
        }
        beforeEach(() => {
            TestBed.configureTestingModule({
                declarations: [DotPagesFavoritePanelComponent, MockDotIconComponent],
                imports: [
                    BrowserAnimationsModule,
                    DotMessagePipeModule,
                    ButtonModule,
                    DotPagesCardModule,
                    DotPagesCardEmptyModule,
                    PanelModule,
                    HttpClientTestingModule
                ],
                providers: [
                    DialogService,
                    { provide: CoreWebService, useClass: CoreWebServiceMock },
                    { provide: DotPageStore, useClass: storeMock },
                    { provide: DotMessageService, useValue: messageServiceMock }
                ]
            }).compileComponents();

            store = TestBed.inject(DotPageStore);
            dialogService = TestBed.inject(DialogService);
            fixture = TestBed.createComponent(DotPagesFavoritePanelComponent);
            de = fixture.debugElement;
            component = fixture.componentInstance;

            spyOn(store, 'getFavoritePages');
            spyOn(store, 'limitFavoritePages');
            spyOn(dialogService, 'open');
            spyOn(component.goToUrl, 'emit');

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

    /*
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
*/

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
