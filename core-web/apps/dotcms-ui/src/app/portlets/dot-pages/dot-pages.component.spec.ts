import { createComponentFactory, Spectator } from '@ngneat/spectator/jest';
import { MockComponent, MockInstance, MockProvider } from 'ng-mocks';
import { of, Subject } from 'rxjs';

import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output, signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';

import { LazyLoadEvent, MenuItem } from 'primeng/api';

import { DotEventsService, DotMessageDisplayService, DotRouterService } from '@dotcms/data-access';
import {
    DotCMSContentlet,
    DotEvent,
    DotMessageSeverity,
    DotMessageType
} from '@dotcms/dotcms-models';
import { GlobalStore } from '@dotcms/store';
import { DotAddToBundleComponent } from '@dotcms/ui';

import { DotCreatePageDialogComponent } from './dot-create-page-dialog/dot-create-page-dialog.component';
import { DotPageFavoritesPanelComponent } from './dot-page-favorites-panel/dot-page-favorites-panel.component';
import { DotPagesTableComponent } from './dot-pages-table/dot-pages-table.component';
import { DotActionsMenuEventParams, DotPagesComponent } from './dot-pages.component';
import { DotPageActionsService } from './services/dot-page-actions.service';
import { DotCMSPagesStore } from './store/store';

/* eslint-disable @angular-eslint/component-selector */
@Component({
    selector: 'p-menu',
    standalone: true,
    template: ''
})
class MenuStubComponent {
    @Input() model: MenuItem[] = [];
    @Input() popup = true;
    @Input() appendTo: unknown;

    @Output() onHide = new EventEmitter<void>();

    visible = false;

    hide(): void {
        if (!this.visible) {
            return;
        }
        this.visible = false;
        this.onHide.emit();
    }

    // PrimeNG's Menu.show expects a MouseEvent-like object for positioning; we don't care in unit tests.
    show(_event: unknown): void {
        this.visible = true;
    }
}
/* eslint-enable @angular-eslint/component-selector */

type SavePageEventData = {
    payload?: {
        identifier?: string;
        contentletIdentifier?: string;
        contentType?: string;
        contentletType?: string;
    };
    value?: string;
};

const mockContentlet = (partial: Partial<DotCMSContentlet>): DotCMSContentlet =>
    partial as unknown as DotCMSContentlet;

describe('DotPagesComponent', () => {
    let spectator: Spectator<DotPagesComponent>;
    let store: {
        favoritePages: ReturnType<typeof signal<DotCMSContentlet[]>>;
        $isFavoritePagesLoading: ReturnType<typeof signal<boolean>>;
        pages: ReturnType<typeof signal<DotCMSContentlet[]>>;
        $isPagesLoading: ReturnType<typeof signal<boolean>>;
        $totalRecords: ReturnType<typeof signal<number>>;
        $showBundleDialog: ReturnType<typeof signal<boolean>>;
        $assetIdentifier: ReturnType<typeof signal<string>>;
        searchPages: jest.Mock;
        filterByLanguage: jest.Mock;
        filterByArchived: jest.Mock;
        onLazyLoad: jest.Mock;
        hideBundleDialog: jest.Mock;
        updateFavoritePageNode: jest.Mock;
        updatePageNode: jest.Mock;
    };
    let events$: Subject<DotEvent<SavePageEventData>>;
    let mockDotRouterService: jest.Mocked<Pick<DotRouterService, 'goToEditPage'>>;
    let mockDotMessageDisplayService: jest.Mocked<Pick<DotMessageDisplayService, 'push'>>;
    let mockDotEventsService: jest.Mocked<Pick<DotEventsService, 'listen'>>;
    let mockDotPageActionsService: jest.Mocked<Pick<DotPageActionsService, 'getItems'>>;
    let mockGlobalStore: {
        systemConfig: ReturnType<typeof signal<{ languages: unknown[] } | null>>;
    };

    const createComponent = createComponentFactory({
        component: DotPagesComponent,
        imports: [DotPagesComponent],
        detectChanges: false
    });

    MockInstance.scope();

    beforeEach(() => {
        // MockComponent(DotPagesTableComponent) uses viewChild(signal); provide contextMenu so the mock instance has a valid signal (ng-mocks #8634).
        MockInstance(DotPagesTableComponent, () => ({
            contextMenu: signal(undefined)
        }));

        // Replace heavy child components with mocks/stubs.
        TestBed.overrideComponent(DotPagesComponent, {
            set: {
                imports: [
                    CommonModule,
                    MenuStubComponent,
                    MockComponent(DotPageFavoritesPanelComponent),
                    MockComponent(DotPagesTableComponent),
                    MockComponent(DotCreatePageDialogComponent),
                    MockComponent(DotAddToBundleComponent)
                ]
            }
        });

        // Signal-backed store mock (matches how the template calls them: $pages(), $totalRecords(), etc).
        store = {
            favoritePages: signal<DotCMSContentlet[]>([]),
            $isFavoritePagesLoading: signal<boolean>(false),
            pages: signal<DotCMSContentlet[]>([]),
            $isPagesLoading: signal<boolean>(false),
            $totalRecords: signal<number>(0),
            $showBundleDialog: signal<boolean>(false),
            $assetIdentifier: signal<string>(''),

            searchPages: jest.fn(),
            filterByLanguage: jest.fn(),
            filterByArchived: jest.fn(),
            onLazyLoad: jest.fn(),
            hideBundleDialog: jest.fn(),
            updateFavoritePageNode: jest.fn(),
            updatePageNode: jest.fn()
        };

        events$ = new Subject<DotEvent<SavePageEventData>>();

        mockDotRouterService = { goToEditPage: jest.fn() };
        mockDotMessageDisplayService = { push: jest.fn() };
        mockDotEventsService = { listen: jest.fn().mockReturnValue(events$.asObservable()) };
        mockDotPageActionsService = { getItems: jest.fn().mockReturnValue(of([])) };
        mockGlobalStore = { systemConfig: signal({ languages: [] }) };

        spectator = createComponent({
            providers: [
                { provide: DotCMSPagesStore, useValue: store },
                MockProvider(DotRouterService, mockDotRouterService),
                MockProvider(DotMessageDisplayService, mockDotMessageDisplayService),
                MockProvider(DotEventsService, mockDotEventsService),
                MockProvider(DotPageActionsService, mockDotPageActionsService),
                { provide: GlobalStore, useValue: mockGlobalStore }
            ]
        });

        spectator.detectChanges();
    });

    afterEach(() => {
        jest.clearAllMocks();
        events$.complete();
    });

    it('should create', () => {
        expect(spectator.component).toBeTruthy();
    });

    describe('navigateToPage', () => {
        it('should parse query params and call router.goToEditPage when dot-pages-table emits navigateToPage', () => {
            spectator.triggerEventHandler(
                'dot-pages-table',
                'navigateToPage',
                '/home?host_id=1&language_id=2'
            );

            expect(mockDotRouterService.goToEditPage).toHaveBeenCalledWith({
                url: '/home',
                host_id: '1',
                language_id: '2'
            });
        });
    });

    describe('store delegates', () => {
        it('should call store.searchPages when dot-pages-table emits search', () => {
            spectator.triggerEventHandler('dot-pages-table', 'search', 'hello');

            expect(store.searchPages).toHaveBeenCalledWith('hello');
        });

        it('should call store.filterByLanguage when dot-pages-table emits languageChange', () => {
            spectator.triggerEventHandler('dot-pages-table', 'languageChange', 2);

            expect(store.filterByLanguage).toHaveBeenCalledWith(2);
        });

        it('should call store.filterByArchived when dot-pages-table emits archivedChange', () => {
            spectator.triggerEventHandler('dot-pages-table', 'archivedChange', true);

            expect(store.filterByArchived).toHaveBeenCalledWith(true);
        });

        it('should call store.onLazyLoad when dot-pages-table emits lazyLoad', () => {
            const event = { first: 0 } as LazyLoadEvent;
            spectator.triggerEventHandler('dot-pages-table', 'lazyLoad', event);

            expect(store.onLazyLoad).toHaveBeenCalledWith(event);
        });

        it('onCloseBundleDialog should call store.hideBundleDialog', () => {
            spectator.component['onCloseBundleDialog']();
            expect(store.hideBundleDialog).toHaveBeenCalled();
        });
    });

    describe('menu behavior', () => {
        it('should close menu and clear menuItems when p-menu emits onHide', () => {
            const menu = spectator.component.menu() as unknown as MenuStubComponent;
            menu.visible = true;

            spectator.component.menuItems.set([{ label: 'x' }]);
            spectator.triggerEventHandler('p-menu', 'onHide', null);

            expect(spectator.component.menuItems()).toEqual([]);
            expect(menu.visible).toBe(false);
        });

        it('toggleMenu should close when already visible (triggered by dot-pages-table openMenu)', () => {
            const menu = spectator.component.menu() as unknown as MenuStubComponent;
            menu.visible = true;
            const closeSpy = jest.spyOn(spectator.component, 'closeMenu');

            spectator.triggerEventHandler('dot-pages-table', 'openMenu', {
                originalEvent: { stopPropagation: jest.fn() } as unknown as MouseEvent,
                data: mockContentlet({ identifier: 'p1' })
            } satisfies DotActionsMenuEventParams);

            expect(closeSpy).toHaveBeenCalled();
        });

        it('toggleMenu should load items and show menu anchored to the click target (triggered by favorites panel openMenu)', () => {
            const menu = spectator.component.menu() as unknown as MenuStubComponent;
            menu.visible = false;

            const showSpy = jest.spyOn(menu, 'show');
            const stopPropagation = jest.fn();
            const anchor = document.createElement('button');
            const items: MenuItem[] = [{ label: 'Edit' }];
            mockDotPageActionsService.getItems.mockReturnValueOnce(of(items));

            const eventParams: DotActionsMenuEventParams = {
                originalEvent: {
                    stopPropagation,
                    currentTarget: anchor,
                    target: anchor
                } as unknown as MouseEvent,
                data: mockContentlet({ identifier: 'p1' })
            };

            spectator.triggerEventHandler('dot-page-favorites-panel', 'openMenu', eventParams);

            expect(stopPropagation).toHaveBeenCalled();
            expect(mockDotPageActionsService.getItems).toHaveBeenCalledWith(eventParams.data);
            expect(showSpy).toHaveBeenCalled();
            expect(spectator.component.menuItems()).toEqual(items);
        });
    });

    describe('template wiring', () => {
        it('should call scrollToTop when dot-pages-table emits pageChange', () => {
            const spy = jest.spyOn(spectator.component, 'scrollToTop').mockImplementation(() => {
                // We only care that the output is wired to the handler, not DOM scrolling support in jsdom.
            });
            spectator.triggerEventHandler('dot-pages-table', 'pageChange', null);
            expect(spy).toHaveBeenCalled();
        });

        it('should call navigateToPage when favorites panel emits navigateToPage', () => {
            spectator.triggerEventHandler(
                'dot-page-favorites-panel',
                'navigateToPage',
                '/about?x=1'
            );
            expect(mockDotRouterService.goToEditPage).toHaveBeenCalledWith({
                url: '/about',
                x: '1'
            });
        });
    });

    describe('bundle dialog rendering', () => {
        it('should render dot-add-to-bundle when $showBundleDialog is true and pass assetIdentifier', () => {
            store.$showBundleDialog.set(true);
            store.$assetIdentifier.set('asset-1');
            spectator.detectChanges();

            const addToBundleDe = spectator.debugElement.query(By.css('dot-add-to-bundle'));
            expect(addToBundleDe).toBeTruthy();
            expect(
                (addToBundleDe.componentInstance as { assetIdentifier: string }).assetIdentifier
            ).toBe('asset-1');
        });

        it('should close bundle dialog when dot-add-to-bundle emits cancel', () => {
            store.$showBundleDialog.set(true);
            spectator.detectChanges();

            spectator.triggerEventHandler('dot-add-to-bundle', 'cancel', null);

            expect(store.hideBundleDialog).toHaveBeenCalled();
        });
    });

    describe('save-page event integration', () => {
        it('should call updateFavoritePageNode when saved item is a dotFavoritePage and show success message', () => {
            events$.next({
                data: {
                    value: 'Saved',
                    payload: { contentType: 'dotFavoritePage', identifier: 'fav-1' }
                }
            } as DotEvent<SavePageEventData>);

            expect(store.updateFavoritePageNode).toHaveBeenCalledWith('fav-1');
            expect(store.updatePageNode).not.toHaveBeenCalled();
            expect(mockDotMessageDisplayService.push).toHaveBeenCalledWith(
                expect.objectContaining({
                    message: 'Saved',
                    severity: DotMessageSeverity.SUCCESS,
                    type: DotMessageType.SIMPLE_MESSAGE
                })
            );
        });

        it('should call updatePageNode when saved item is a page and show success message', () => {
            events$.next({
                data: {
                    value: 'Saved',
                    payload: { contentType: 'htmlpage', contentletIdentifier: 'page-1' }
                }
            } as DotEvent<SavePageEventData>);

            expect(store.updatePageNode).toHaveBeenCalledWith('page-1');
            expect(store.updateFavoritePageNode).not.toHaveBeenCalled();
            expect(mockDotMessageDisplayService.push).toHaveBeenCalledWith(
                expect.objectContaining({
                    message: 'Saved',
                    severity: DotMessageSeverity.SUCCESS,
                    type: DotMessageType.SIMPLE_MESSAGE
                })
            );
        });
    });
});
