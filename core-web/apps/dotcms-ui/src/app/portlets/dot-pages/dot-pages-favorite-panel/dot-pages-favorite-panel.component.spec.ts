import { throwError } from 'rxjs';

import { HttpErrorResponse, HttpResponse } from '@angular/common/http';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { Component, DebugElement, Input } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { ButtonModule } from 'primeng/button';
import { DialogService } from 'primeng/dynamicdialog';
import { PanelModule } from 'primeng/panel';

import { of } from 'rxjs/internal/observable/of';

import { DotHttpErrorManagerService } from '@dotcms/app/api/services/dot-http-error-manager/dot-http-error-manager.service';
import { MockDotHttpErrorManagerService } from '@dotcms/app/test/dot-http-error-manager.service.mock';
import { DotMessagePipeModule } from '@dotcms/app/view/pipes/dot-message/dot-message-pipe.module';
import { DotMessageService, DotPageRenderService } from '@dotcms/data-access';
import { CoreWebService, CoreWebServiceMock, HttpCode } from '@dotcms/dotcms-js';
import {
    dotcmsContentletMock,
    MockDotMessageService,
    mockResponseView
} from '@dotcms/utils-testing';

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

const messageServiceMock = new MockDotMessageService({});

export const favoritePagesInitialTestData = [
    {
        ...dotcmsContentletMock,
        live: true,
        baseType: 'CONTENT',
        languageId: '1',
        modDate: '2020-09-02 16:45:15.569',
        title: 'preview1',
        screenshot: 'test1',
        url: '/index1?host_id=A&language_id=1&device_inode=123',
        owner: 'admin'
    },
    {
        ...dotcmsContentletMock,
        title: 'preview2',
        languageId: '1',
        modDate: '2020-09-02 16:45:15.569',
        screenshot: 'test2',
        url: '/index2',
        owner: 'admin2'
    }
];

describe('DotPagesFavoritePanelComponent', () => {
    let fixture: ComponentFixture<DotPagesFavoritePanelComponent>;
    let component: DotPagesFavoritePanelComponent;
    let de: DebugElement;
    let store: DotPageStore;
    let dialogService: DialogService;
    let dotPageRenderService: DotPageRenderService;
    let dotHttpErrorManagerService: DotHttpErrorManagerService;

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
        setLocalStorageFavoritePanelCollapsedParams(_collapsed: boolean): void {
            /* */
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
                    PanelModule,
                    HttpClientTestingModule
                ],
                providers: [
                    DialogService,
                    DotPageRenderService,
                    {
                        provide: DotHttpErrorManagerService,
                        useClass: MockDotHttpErrorManagerService
                    },
                    { provide: CoreWebService, useClass: CoreWebServiceMock },
                    { provide: DotPageStore, useClass: storeMock },
                    { provide: DotMessageService, useValue: messageServiceMock }
                ]
            }).compileComponents();
        });

        beforeEach(() => {
            store = TestBed.inject(DotPageStore);
            fixture = TestBed.createComponent(DotPagesFavoritePanelComponent);
            de = fixture.debugElement;
            component = fixture.componentInstance;

            fixture.detectChanges();
        });

        it('should set panel with empty state class', () => {
            const elem = de.query(By.css('p-panel'));
            expect(
                elem.nativeElement.classList.contains('dot-pages-panel__empty-state')
            ).toBeTruthy();
        });

        it('should set panel collapsed state', () => {
            spyOn(store, 'setLocalStorageFavoritePanelCollapsedParams');
            component.toggleFavoritePagesPanel(
                new Event('myevent', {
                    bubbles: true,
                    cancelable: true,
                    composed: false
                })
            );
            expect(store.setLocalStorageFavoritePanelCollapsedParams).toHaveBeenCalledTimes(1);
        });

        it('should load empty pages cards container', () => {
            expect(
                de
                    .query(By.css('.dot-pages-empty__container i'))
                    .nativeElement.classList.contains('pi-star')
            ).toBeTrue();

            expect(de.query(By.css('.dot-pages-empty__header')).nativeElement.outerText).toBe(
                'favoritePage.listing.empty.header'
            );
            expect(
                de.query(By.css('[data-testId="dot-pages-empty__content"')).nativeElement.outerText
            ).toBe('favoritePage.listing.empty.content');
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
            setLocalStorageFavoritePanelCollapsedParams(_collapsed: boolean): void {
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
                    PanelModule,
                    HttpClientTestingModule
                ],
                providers: [
                    DialogService,
                    DotPageRenderService,
                    {
                        provide: DotHttpErrorManagerService,
                        useClass: MockDotHttpErrorManagerService
                    },
                    { provide: CoreWebService, useClass: CoreWebServiceMock },
                    { provide: DotPageStore, useClass: storeMock },
                    { provide: DotMessageService, useValue: messageServiceMock }
                ]
            }).compileComponents();

            store = TestBed.inject(DotPageStore);
            dialogService = TestBed.inject(DialogService);
            dotPageRenderService = TestBed.inject(DotPageRenderService);
            dotHttpErrorManagerService = TestBed.inject(DotHttpErrorManagerService);
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
            expect(elem.componentInstance['iconPos']).toBe('end');
            expect(elem.componentInstance['expandIcon']).toBe('pi pi-angle-down');
            expect(elem.componentInstance['collapseIcon']).toBe('pi pi-angle-up');
            expect(elem.componentInstance['toggleable']).toBe(true);
        });

        it('should have an icon for bookmarks in the header', () => {
            const elem = de.query(By.css('.dot-pages-panel__header [data-testId="bookmarksIcon"]'));
            expect(elem).toBeTruthy();
        });

        it('should set secondary button in panel', () => {
            const elem = de.query(By.css('.dot-pages-panel-action__button span'));
            expect(elem.nativeElement.outerText.toUpperCase()).toBe('SEE.ALL'.toUpperCase());
        });

        it('should load pages cards with attributes', () => {
            const elem = de.queryAll(By.css('dot-pages-card'));
            expect(elem.length).toBe(2);
            expect(
                elem[0].componentInstance.imageUri.includes(
                    `${favoritePagesInitialTestData[0].screenshot}?language_id=${favoritePagesInitialTestData[0].languageId}`
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
                spyOn(dotPageRenderService, 'checkPermission').and.returnValue(of(true));
                fixture.detectChanges();
                const elem = de.query(By.css('dot-pages-card'));
                elem.triggerEventHandler('edit', { ...favoritePagesInitialTestData[0] });

                const urlParams = { url: favoritePagesInitialTestData[0].url.split('?')[0] };
                const searchParams = new URLSearchParams(
                    favoritePagesInitialTestData[0].url.split('?')[1]
                );

                for (const entry of searchParams) {
                    urlParams[entry[0]] = entry[1];
                }

                expect(dotPageRenderService.checkPermission).toHaveBeenCalledWith(urlParams);
                expect(dialogService.open).toHaveBeenCalledTimes(1);
            });

            it('should throw error dialog when call edit method to open favorite page dialog and user does not have access', () => {
                spyOn(dotPageRenderService, 'checkPermission').and.returnValue(of(false));
                spyOn(dotHttpErrorManagerService, 'handle');
                fixture.detectChanges();
                const elem = de.query(By.css('dot-pages-card'));
                elem.triggerEventHandler('edit', { ...favoritePagesInitialTestData[0] });

                expect(dotHttpErrorManagerService.handle).toHaveBeenCalledWith(
                    new HttpErrorResponse(
                        new HttpResponse({
                            body: null,
                            status: HttpCode.FORBIDDEN,
                            headers: null,
                            url: ''
                        })
                    )
                );
            });

            it('should allow to open Favorite Page dialog when URL checked throws a 404 Error', () => {
                const error404 = mockResponseView(404);
                spyOn(dotPageRenderService, 'checkPermission').and.returnValue(
                    throwError(error404)
                );
                fixture.detectChanges();
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
            setLocalStorageFavoritePanelCollapsedParams(_collapsed: boolean): void {
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
                    PanelModule,
                    HttpClientTestingModule
                ],
                providers: [
                    DialogService,
                    DotPageRenderService,
                    {
                        provide: DotHttpErrorManagerService,
                        useClass: MockDotHttpErrorManagerService
                    },
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
            expect(elem.nativeElement.outerText.toUpperCase()).toBe('SEE.LESS'.toUpperCase());
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
});
