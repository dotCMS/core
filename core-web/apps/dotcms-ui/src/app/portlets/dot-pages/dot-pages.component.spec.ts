import { Component, Input, Injectable, DebugElement } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { DotESContentService, DotMessageService } from '@dotcms/data-access';
import { of } from 'rxjs/internal/observable/of';
import {
    dotcmsContentletMock,
    MockDotMessageService,
    MockDotRouterService
} from '@dotcms/utils-testing';
import { DotPageStore } from './dot-pages-store/dot-pages.store';
import { CommonModule } from '@angular/common';
import { DotPipesModule } from '@dotcms/app/view/pipes/dot-pipes.module';
import { DotIconModule } from '@dotcms/ui';
import { ButtonModule } from 'primeng/button';
import { TabViewModule } from 'primeng/tabview';
import { DotPagesCardModule } from './dot-pages-card/dot-pages-card.module';
import { DotPagesComponent } from './dot-pages.component';
import { Observable } from 'rxjs';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { By } from '@angular/platform-browser';
import { PanelModule } from 'primeng/panel';
import { DotPagesCardEmptyModule } from './dot-pages-card-empty/dot-pages-card-empty.module';
import { DotRouterService } from '@dotcms/app/api/services/dot-router/dot-router.service';

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

export const pagesInitialTestData = [
    {
        ...dotcmsContentletMock,
        title: 'preview1',
        screenshot: 'test1',
        url: '/index1?host_id=A&language_id=1&device_inode=123',
        owner: 'admin'
    },
    {
        ...dotcmsContentletMock,
        title: 'preview2',
        screenshot: 'test2',
        url: '/index2',
        owner: 'admin2'
    }
];

describe('DotPagesComponent', () => {
    let fixture: ComponentFixture<DotPagesComponent>;
    let de: DebugElement;
    let store: DotPageStore;
    let dotRouterService: DotRouterService;

    beforeEach(() => {
        TestBed.configureTestingModule({
            declarations: [DotPagesComponent],
            imports: [
                BrowserAnimationsModule,
                CommonModule,
                DotPagesCardModule,
                DotPagesCardEmptyModule,
                DotPipesModule,
                DotIconModule,
                PanelModule,
                ButtonModule,
                TabViewModule
            ],
            providers: [
                { provide: DotRouterService, useClass: MockDotRouterService },
                {
                    provide: DotMessageService,
                    useValue: messageServiceMock
                },
                { provide: DotESContentService, useClass: MockESPaginatorService }
            ]
        }).compileComponents();
    });

    describe('Empty State', () => {
        beforeEach(() => {
            TestBed.overrideProvider(DotPageStore, {
                useValue: {
                    getFavoritePages: jasmine.createSpy(),
                    setInitialStateData: jasmine.createSpy(),
                    limitFavoritePages: jasmine.createSpy(),
                    vm$: of({
                        favoritePages: {
                            items: [],
                            showLoadMoreButton: false,
                            total: 0
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

        it('should init store', () => {
            expect(store.setInitialStateData).toHaveBeenCalledWith(5);
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
        beforeEach(() => {
            TestBed.overrideProvider(DotPageStore, {
                useValue: {
                    getFavoritePages: jasmine.createSpy(),
                    setInitialStateData: jasmine.createSpy(),
                    limitFavoritePages: jasmine.createSpy(),
                    vm$: of({
                        favoritePages: {
                            items: pagesInitialTestData,
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
            expect(elem[0].componentInstance.imageUri).toBe(pagesInitialTestData[0].screenshot);
            expect(elem[0].componentInstance.title).toBe(pagesInitialTestData[0].title);
            expect(elem[0].componentInstance.url).toBe(pagesInitialTestData[0].url);
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

            it('should call redirect method in DotRouterService', () => {
                const elem = de.query(By.css('dot-pages-card'));
                elem.triggerEventHandler('click', {
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
                            items: [...pagesInitialTestData, ...pagesInitialTestData],
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
});
