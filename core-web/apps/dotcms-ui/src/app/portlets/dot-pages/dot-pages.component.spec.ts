import { Component, Input, Injectable, DebugElement } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { DotMessageService } from '@services/dot-message/dot-messages.service';
import { MockDotMessageService } from '@tests/dot-message-service.mock';
import { of } from 'rxjs/internal/observable/of';
import { dotcmsContentletMock } from '@dotcms/app/test/dotcms-contentlet.mock';
import { DotPageStore } from './dot-pages-store/dot-pages.store';
import { CommonModule } from '@angular/common';
import { DotPipesModule } from '@dotcms/app/view/pipes/dot-pipes.module';
import { DotIconModule } from '@dotcms/ui';
import { ButtonModule } from 'primeng/button';
import { TabViewModule } from 'primeng/tabview';
import { DotPagesCardModule } from './dot-pages-card/dot-pages-card.module';
import { DotPagesComponent } from './dot-pages.component';
import { DotESContentService } from '@dotcms/app/api/services/dot-es-content/dot-es-content.service';
import { Observable } from 'rxjs';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { By } from '@angular/platform-browser';
import { PanelModule } from 'primeng/panel';

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
    'see.less': 'See Less'
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
        url: '/index1',
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

    beforeEach(() => {
        TestBed.configureTestingModule({
            declarations: [DotPagesComponent],
            imports: [
                BrowserAnimationsModule,
                CommonModule,
                DotPagesCardModule,
                DotPipesModule,
                DotIconModule,
                PanelModule,
                ButtonModule,
                TabViewModule
            ],
            providers: [
                {
                    provide: DotMessageService,
                    useValue: messageServiceMock
                },
                { provide: DotESContentService, useClass: MockESPaginatorService }
            ]
        }).compileComponents();
    });

    describe('Loading 2 of items', () => {
        beforeEach(() => {
            TestBed.overrideProvider(DotPageStore, {
                useValue: {
                    loadAllFavoritePages: jasmine.createSpy(),
                    setLoading: jasmine.createSpy(),
                    setLoaded: jasmine.createSpy(),
                    setInitialStateData: jasmine.createSpy(),
                    limitFavoritePages: jasmine.createSpy(),
                    vm$: of({
                        favoritePages: {
                            items: pagesInitialTestData,
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

        it('should init store', () => {
            expect(store.setInitialStateData).toHaveBeenCalled();
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
            expect(elem.nativeElement.outerText).toBe('SEE ALL');
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

        describe('Load all items', () => {
            it('should call event to load all items', () => {
                const elem = de.query(By.css('[data-testId="seeAllBtn"]'));
                elem.triggerEventHandler('click', {
                    stopPropagation: () => {
                        //
                    }
                });
                expect(store.loadAllFavoritePages).toHaveBeenCalledTimes(1);
            });
        });
    });

    describe('Loading all items', () => {
        beforeEach(() => {
            TestBed.overrideProvider(DotPageStore, {
                useValue: {
                    loadAllFavoritePages: jasmine.createSpy(),
                    setLoading: jasmine.createSpy(),
                    setLoaded: jasmine.createSpy(),
                    setInitialStateData: jasmine.createSpy(),
                    limitFavoritePages: jasmine.createSpy(),
                    vm$: of({
                        favoritePages: {
                            items: [...pagesInitialTestData, ...pagesInitialTestData],
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
            expect(elem.nativeElement.outerText).toBe('SEE LESS');
        });

        describe('Show less items', () => {
            it('should call event to show less items', () => {
                const elem = de.query(By.css('[data-testId="seeAllBtn"]'));
                elem.triggerEventHandler('click', {
                    stopPropagation: () => {
                        //
                    }
                });
                expect(store.limitFavoritePages).toHaveBeenCalledTimes(1);
            });
        });
    });
});
