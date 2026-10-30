import {
    Spectator,
    SpyObject,
    createComponentFactory,
    mockProvider
} from '@openng/spectator/vitest';
import { of } from 'rxjs';
import { vi } from 'vitest';

import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { provideRouter } from '@angular/router';

import { TooltipModule } from 'primeng/tooltip';

import {
    DotCurrentUserService,
    DotEventsService,
    DotRouterService,
    DotSystemConfigService
} from '@dotcms/data-access';
import { LoginService } from '@dotcms/dotcms-js';
import { DotMenu, MenuGroup, MenuItemEntity } from '@dotcms/dotcms-models';
import { GlobalStore } from '@dotcms/store';
import { DotIconComponent } from '@dotcms/ui';
import { DotCurrentUserServiceMock, LoginServiceMock } from '@dotcms/utils-testing';

import { DotNavIconComponent } from './components/dot-nav-icon/dot-nav-icon.component';
import { DotNavItemComponent } from './components/dot-nav-item/dot-nav-item.component';
import { DotNavigationComponent } from './dot-navigation.component';
import { DotNavigationService } from './services/dot-navigation.service';
import { dotMenuMock, dotMenuMock1 } from './services/dot-navigation.service.spec';

import { DotRandomIconPipe } from '../../pipes/dot-radom-icon/dot-random-icon.pipe';
import { IframeOverlayService } from '../_common/iframe/service/iframe-overlay.service';

/** Puts a menu in the store the way the app does: one request, answered by the backend. */
const seedMenu = (store: InstanceType<typeof GlobalStore>, menus: DotMenu[]) => {
    store.reload();
    TestBed.inject(HttpTestingController).expectOne('/api/v1/menu').flush({ entity: menus });
};

describe('DotNavigationComponent collapsed', () => {
    let spectator: Spectator<DotNavigationComponent>;
    let iframeOverlayService: SpyObject<IframeOverlayService>;
    let globalStore: InstanceType<typeof GlobalStore>;
    let dotRouterService: SpyObject<DotRouterService>;

    const createComponent = createComponentFactory({
        component: DotNavigationComponent,
        imports: [
            DotNavIconComponent,
            DotIconComponent,
            BrowserAnimationsModule,
            TooltipModule,
            DotRandomIconPipe
        ],
        providers: [
            provideRouter([]),
            { provide: DotCurrentUserService, useClass: DotCurrentUserServiceMock },
            mockProvider(IframeOverlayService),
            mockProvider(DotNavigationService),
            mockProvider(DotEventsService),
            mockProvider(DotRouterService, {
                currentPortlet: { id: '123', parentMenuId: '123' }
            }),
            {
                provide: LoginService,
                useClass: LoginServiceMock
            },
            {
                provide: DotSystemConfigService,
                useValue: { getSystemConfig: () => of({}) }
            },
            GlobalStore,
            provideHttpClient(),
            provideHttpClientTesting()
        ]
    });

    beforeEach(() => {
        spectator = createComponent({
            detectChanges: false
        });

        iframeOverlayService = spectator.inject(IframeOverlayService);
        globalStore = spectator.inject(GlobalStore);
        dotRouterService = spectator.inject(DotRouterService);

        // Load menu items in the GlobalStore instead of using service's items$
        seedMenu(globalStore, [dotMenuMock(), dotMenuMock1()]);
    });

    it('should have all menus closed', () => {
        spectator.detectChanges();
        const items = spectator.queryAll('.dot-nav__list-item');

        items.forEach((item) => {
            expect(item.classList.contains('dot-nav__list-item--active')).toBe(false);
        });
    });

    it('should have dot-nav-item print correctly', () => {
        spectator.detectChanges();

        const items = spectator.queryAll(DotNavItemComponent);

        expect(items.length).toBe(2);
        // The store transforms DotMenu to MenuGroup with additional parent properties
        const expectedMenu1 = {
            id: '123',
            label: 'Name',
            icon: 'icon',
            menuItems: [
                {
                    active: false,
                    ajax: true,
                    angular: true,
                    id: '123',
                    label: 'Label 1',
                    url: 'url/one',
                    menuLink: 'url/one',
                    parentMenuId: '123',
                    parentMenuLabel: 'Name',
                    parentMenuIcon: 'icon',
                    shortLayoutId: '123',
                    tabOrder: 0,
                    itemIndex: 0
                },
                {
                    active: false,
                    ajax: true,
                    angular: true,
                    id: '456',
                    label: 'Label 2',
                    url: 'url/two',
                    menuLink: 'url/two',
                    parentMenuId: '123',
                    parentMenuLabel: 'Name',
                    parentMenuIcon: 'icon',
                    shortLayoutId: '123',
                    tabOrder: 0,
                    itemIndex: 1
                }
            ],
            isOpen: false,
            tabOrder: 0
        };
        expect(items[0].$data()).toEqual(expectedMenu1);
        // For the second menu, we need to check it has the transformed structure
        expect(items[1].$data().id).toBe('456');
        expect(items[1].$data().menuItems.length).toBeGreaterThan(0);
    });

    it('should open parent menu', () => {
        spectator.detectChanges();

        // First, ensure navigation is collapsed
        globalStore.collapseNavigation();
        spectator.detectChanges();

        // Then open a parent menu group
        globalStore.toggleParent('123');
        expect(globalStore.openParentMenuId()).toBe('123');
    });

    describe('itemClick event', () => {
        it('should reload portlet and hide overlay', () => {
            spectator.detectChanges();

            const stopPropSpy = vi.fn();

            spectator.component.onItemClick({
                originalEvent: {
                    stopPropagation: stopPropSpy,
                    ctrlKey: false,
                    metaKey: false
                } as unknown as MouseEvent,
                data: dotMenuMock().menuItems[0]
            });

            expect(stopPropSpy).toHaveBeenCalled();
            expect(dotRouterService.reloadCurrentPortlet).toHaveBeenCalledWith('123');
            expect(dotRouterService.reloadCurrentPortlet).toHaveBeenCalledTimes(1);
            expect(iframeOverlayService.hide).toHaveBeenCalledTimes(1);
        });

        it('should NOT reload portlet', () => {
            spectator.detectChanges();

            const stopPropSpy = vi.fn();

            spectator.component.onItemClick({
                originalEvent: {
                    stopPropagation: stopPropSpy,
                    ctrlKey: true,
                    metaKey: false
                } as unknown as MouseEvent,
                data: dotMenuMock().menuItems[0]
            });

            expect(stopPropSpy).toHaveBeenCalled();
            expect(dotRouterService.reloadCurrentPortlet).not.toHaveBeenCalled();
        });
    });

    describe('menuClick event collapsed', () => {
        it('should navigate to portlet when menu is collapsed', () => {
            spectator.detectChanges();

            const mockMenu = globalStore.menuGroup().find((m) => m.id === '123');
            if (mockMenu) {
                spectator.component.onMenuClick({
                    originalEvent: {} as unknown as MouseEvent,
                    data: mockMenu
                });
            }

            expect(dotRouterService.gotoPortlet).toHaveBeenCalledWith('url/one', {
                queryParams: { mId: '123' }
            });
            expect(dotRouterService.gotoPortlet).toHaveBeenCalledTimes(1);
        });

        it('should not have scroll when collapsed', () => {
            // Ensure navigation is collapsed
            globalStore.collapseNavigation();
            spectator.detectChanges();

            expect(spectator.debugElement.styles.cssText).toEqual('');
        });
    });
});

describe('DotNavigationComponent expanded', () => {
    let spectator: Spectator<DotNavigationComponent>;
    let iframeOverlayService: SpyObject<IframeOverlayService>;
    let globalStore: InstanceType<typeof GlobalStore>;
    let dotEventsService: SpyObject<DotEventsService>;
    let dotRouterService: SpyObject<DotRouterService>;

    const createComponent = createComponentFactory({
        component: DotNavigationComponent,
        imports: [
            DotNavIconComponent,
            DotIconComponent,
            BrowserAnimationsModule,
            TooltipModule,
            DotRandomIconPipe
        ],
        providers: [
            provideRouter([]),
            { provide: DotCurrentUserService, useClass: DotCurrentUserServiceMock },
            mockProvider(IframeOverlayService),
            mockProvider(DotNavigationService),
            mockProvider(DotEventsService),
            mockProvider(DotRouterService, {
                currentPortlet: { id: '123', parentMenuId: '123' }
            }),
            {
                provide: LoginService,
                useClass: LoginServiceMock
            },
            {
                provide: DotSystemConfigService,
                useValue: { getSystemConfig: () => of({}) }
            },
            GlobalStore,
            provideHttpClient(),
            provideHttpClientTesting()
        ]
    });

    beforeEach(() => {
        spectator = createComponent({
            detectChanges: false
        });

        iframeOverlayService = spectator.inject(IframeOverlayService);
        globalStore = spectator.inject(GlobalStore);
        dotEventsService = spectator.inject(DotEventsService);
        dotRouterService = spectator.inject(DotRouterService);

        // Load menu items in the GlobalStore instead of using service's items$
        // Create menus without active items to avoid auto-opening when expanding
        const menuWithoutActive = {
            ...dotMenuMock(),
            menuItems: dotMenuMock().menuItems.map((item) => ({ ...item, active: false }))
        };
        const menu1WithoutActive = {
            ...dotMenuMock1(),
            menuItems: dotMenuMock1().menuItems.map((item) => ({ ...item, active: false }))
        };
        seedMenu(globalStore, [menuWithoutActive, menu1WithoutActive]);
        // Set navigation as expanded
        globalStore.expandNavigation();
    });

    it('should have all menus closed', () => {
        spectator.detectChanges();
        const items = spectator.queryAll('.dot-nav__list-item');

        items.forEach((item) => {
            expect(item.classList.contains('dot-nav__list-item--active')).toBe(false);
        });
    });

    it('should have dot-nav-item print correctly', () => {
        spectator.detectChanges();

        const items = spectator.queryAll(DotNavItemComponent);

        expect(items.length).toBe(2);
        // Verify the structure, not exact equality since we modified active states
        expect(items[0].$data().id).toBe('123');
        expect(items[1].$data().id).toBe('456');
    });

    it('should be in expanded state', () => {
        spectator.detectChanges();

        // Set navigation to collapsed state
        globalStore.collapseNavigation();
        spectator.detectChanges();

        expect(globalStore.isNavigationCollapsed()).toBe(true);
    });

    describe('itemClick event', () => {
        it('should reload portlet and hide overlay', () => {
            spectator.detectChanges();

            const stopPropSpy = vi.fn();

            spectator.component.onItemClick({
                originalEvent: {
                    stopPropagation: stopPropSpy,
                    ctrlKey: false,
                    metaKey: false
                } as unknown as MouseEvent,
                data: dotMenuMock().menuItems[0]
            });

            expect(stopPropSpy).toHaveBeenCalled();
            expect(dotRouterService.reloadCurrentPortlet).toHaveBeenCalledWith('123');
            expect(dotRouterService.reloadCurrentPortlet).toHaveBeenCalledTimes(1);
            expect(iframeOverlayService.hide).toHaveBeenCalledTimes(1);
        });

        it('should NOT reload portlet', () => {
            spectator.detectChanges();

            const stopPropSpy = vi.fn();

            spectator.component.onItemClick({
                originalEvent: {
                    stopPropagation: stopPropSpy,
                    ctrlKey: true,
                    metaKey: false
                } as unknown as MouseEvent,
                data: dotMenuMock().menuItems[0]
            });

            expect(stopPropSpy).toHaveBeenCalled();
            expect(dotRouterService.reloadCurrentPortlet).not.toHaveBeenCalled();
        });

        it('should have scroll', () => {
            spectator.detectChanges();

            expect(spectator.debugElement.styles.cssText).toEqual('overflow-y: auto;');
        });
    });

    describe('menuClick event expanded', () => {
        it('should navigate and set open when menu is closed', () => {
            spectator.detectChanges();

            const mockMenu = globalStore.menuGroup().find((m) => m.id === '123');
            if (mockMenu) {
                mockMenu.isOpen = false;
            }

            if (mockMenu) {
                spectator.component.onMenuClick({
                    originalEvent: {} as unknown as MouseEvent,
                    data: mockMenu
                });
            }

            expect(dotRouterService.gotoPortlet).toHaveBeenCalledWith('url/one', {
                queryParams: { mId: '123' }
            });
            expect(dotRouterService.gotoPortlet).toHaveBeenCalledTimes(1);
            // Verify parent menu group is set as open in GlobalStore
            expect(globalStore.openParentMenuId()).toBe('123');
        });

        it('should only set open when menu is already open', () => {
            spectator.detectChanges();

            // First, open the parent menu group
            globalStore.toggleParent('123');
            expect(globalStore.openParentMenuId()).toBe('123');

            const mockMenu = globalStore.menuGroup().find((m) => m.id === '123');

            spectator.component.onMenuClick({
                originalEvent: {} as unknown as MouseEvent,
                data: mockMenu
            });

            expect(dotRouterService.gotoPortlet).not.toHaveBeenCalled();
            // Verify parent menu group is toggled (closed) in GlobalStore
            expect(globalStore.openParentMenuId()).toBe(null);
        });
    });

    describe('collapse button', () => {
        it('should toggle navigation when collapse button is clicked', () => {
            spectator.detectChanges();

            spectator.component.handleCollapseButtonClick();

            expect(dotEventsService.notify).toHaveBeenCalledWith('dot-side-nav-toggle');
            expect(dotEventsService.notify).toHaveBeenCalledTimes(1);
        });
    });
});

/**
 * Characterization of the navigation clicks (issue #38078). These cases pin what the bar does today
 * so the menu consolidation cannot change it: they must pass on the old code and keep passing.
 * The layout ids are full UUIDs because `mId` is the first four characters of the layout id, which
 * the short ids used by the fixtures above cannot prove.
 */
describe('DotNavigationComponent click characterization', () => {
    const LAYOUT_A = 'aaaa1111-0000-4000-8000-000000000001';
    const LAYOUT_B = 'bbbb2222-0000-4000-8000-000000000002';

    const menus = [
        {
            id: LAYOUT_A,
            name: 'Site',
            tabName: 'Site',
            tabIcon: 'icon-a',
            url: '/pages',
            active: false,
            isOpen: false,
            label: 'Site',
            tabDescription: '',
            menuItems: [
                { id: 'pages', label: 'Pages', url: '/pages', angular: true, ajax: false },
                { id: 'legacy', label: 'Legacy', url: '/legacy?r=1', angular: false, ajax: false }
            ]
        },
        {
            id: LAYOUT_B,
            name: 'Content',
            tabName: 'Content',
            tabIcon: 'icon-b',
            url: '/pages',
            active: false,
            isOpen: false,
            label: 'Content',
            tabDescription: '',
            menuItems: [
                { id: 'pages', label: 'Pages (copy)', url: '/pages', angular: true, ajax: false }
            ]
        }
    ] as unknown as DotMenu[];

    let spectator: Spectator<DotNavigationComponent>;
    let globalStore: InstanceType<typeof GlobalStore>;
    let dotRouterService: SpyObject<DotRouterService>;
    let dotEventsService: SpyObject<DotEventsService>;
    let iframeOverlayService: SpyObject<IframeOverlayService>;

    const createComponent = createComponentFactory({
        component: DotNavigationComponent,
        imports: [
            DotNavIconComponent,
            DotIconComponent,
            BrowserAnimationsModule,
            TooltipModule,
            DotRandomIconPipe
        ],
        providers: [
            provideRouter([]),
            { provide: DotCurrentUserService, useClass: DotCurrentUserServiceMock },
            mockProvider(IframeOverlayService),
            mockProvider(DotNavigationService),
            mockProvider(DotEventsService),
            mockProvider(DotRouterService, {
                currentPortlet: { id: 'pages', parentMenuId: LAYOUT_A }
            }),
            { provide: LoginService, useClass: LoginServiceMock },
            { provide: DotSystemConfigService, useValue: { getSystemConfig: () => of({}) } },
            GlobalStore,
            provideHttpClient(),
            provideHttpClientTesting()
        ]
    });

    const click = (
        item: Parameters<DotNavigationComponent['onItemClick']>[0]['data'],
        keys = {}
    ) => {
        const stopPropagation = vi.fn();
        spectator.component.onItemClick({
            originalEvent: {
                stopPropagation,
                ctrlKey: false,
                metaKey: false,
                ...keys
            } as unknown as MouseEvent,
            data: item
        });

        return stopPropagation;
    };

    const itemOf = (layoutId: string, portletId: string): MenuItemEntity => {
        const item = globalStore
            .menuGroup()
            .find((group) => group.id === layoutId)
            ?.menuItems.find((candidate) => candidate.id === portletId);

        if (!item) {
            throw new Error(`No menu item '${portletId}' in layout '${layoutId}'`);
        }

        return item;
    };

    beforeEach(() => {
        spectator = createComponent({ detectChanges: false });
        globalStore = spectator.inject(GlobalStore);
        dotRouterService = spectator.inject(DotRouterService);
        dotEventsService = spectator.inject(DotEventsService);
        iframeOverlayService = spectator.inject(IframeOverlayService);
        seedMenu(globalStore, menus);
        spectator.detectChanges();
    });

    describe('item click', () => {
        it('opens the item menuLink with mId set to the first 4 characters of the layout id', () => {
            click(itemOf(LAYOUT_A, 'legacy'));

            expect(dotRouterService.gotoPortlet).toHaveBeenCalledWith('/c/legacy', {
                queryParams: { mId: 'aaaa' }
            });
            expect(dotRouterService.reloadCurrentPortlet).not.toHaveBeenCalled();
            expect(iframeOverlayService.hide).toHaveBeenCalledTimes(1);
        });

        it('keeps the Angular item url as its menuLink', () => {
            click(itemOf(LAYOUT_B, 'pages'));

            expect(dotRouterService.gotoPortlet).toHaveBeenCalledWith('/pages', {
                queryParams: { mId: 'bbbb' }
            });
        });

        it('does not navigate, reload or hide the overlay on Cmd-click', () => {
            const stopPropagation = click(itemOf(LAYOUT_A, 'legacy'), { metaKey: true });

            expect(stopPropagation).toHaveBeenCalled();
            expect(dotRouterService.gotoPortlet).not.toHaveBeenCalled();
            expect(dotRouterService.reloadCurrentPortlet).not.toHaveBeenCalled();
            expect(iframeOverlayService.hide).not.toHaveBeenCalled();
        });

        it('does not navigate on Ctrl-click', () => {
            click(itemOf(LAYOUT_A, 'legacy'), { ctrlKey: true });

            expect(dotRouterService.gotoPortlet).not.toHaveBeenCalled();
            expect(iframeOverlayService.hide).not.toHaveBeenCalled();
        });

        it('reloads the portlet when the clicked item is the one already open', () => {
            click(itemOf(LAYOUT_A, 'pages'));

            expect(dotRouterService.reloadCurrentPortlet).toHaveBeenCalledWith('pages');
            expect(dotRouterService.gotoPortlet).not.toHaveBeenCalled();
        });

        it('navigates, instead of reloading, to the same portlet id in another layout', () => {
            click(itemOf(LAYOUT_B, 'pages'));

            expect(dotRouterService.reloadCurrentPortlet).not.toHaveBeenCalled();
            expect(dotRouterService.gotoPortlet).toHaveBeenCalledTimes(1);
        });
    });

    describe('group click', () => {
        const group = (layoutId: string): MenuGroup => {
            const found = globalStore.menuGroup().find((candidate) => candidate.id === layoutId);

            if (!found) {
                throw new Error(`No menu group '${layoutId}'`);
            }

            return found;
        };

        it('expanded and closed: navigates to the first item with mId and opens the group', () => {
            globalStore.expandNavigation();
            globalStore.closeAllParents();

            spectator.component.onMenuClick({
                originalEvent: {} as unknown as MouseEvent,
                data: group(LAYOUT_A)
            });

            expect(dotRouterService.gotoPortlet).toHaveBeenCalledWith('/pages', {
                queryParams: { mId: 'aaaa' }
            });
            expect(globalStore.openParentMenuId()).toBe(LAYOUT_A);
        });

        it('expanded and open: closes the group without navigating', () => {
            globalStore.expandNavigation();
            globalStore.toggleParent(LAYOUT_A);

            spectator.component.onMenuClick({
                originalEvent: {} as unknown as MouseEvent,
                data: group(LAYOUT_A)
            });

            expect(dotRouterService.gotoPortlet).not.toHaveBeenCalled();
            expect(globalStore.openParentMenuId()).toBeNull();
        });

        it('toggleOnly on a closed group opens it without navigating', () => {
            globalStore.expandNavigation();
            globalStore.closeAllParents();

            spectator.component.onMenuClick({
                originalEvent: {} as unknown as MouseEvent,
                data: group(LAYOUT_B),
                toggleOnly: true
            });

            expect(dotRouterService.gotoPortlet).not.toHaveBeenCalled();
            expect(globalStore.openParentMenuId()).toBe(LAYOUT_B);
        });

        it('collapsed: navigates to the first item of the group with mId and does not toggle it', () => {
            globalStore.collapseNavigation();

            spectator.component.onMenuClick({
                originalEvent: {} as unknown as MouseEvent,
                data: group(LAYOUT_B)
            });

            expect(dotRouterService.gotoPortlet).toHaveBeenCalledWith('/pages', {
                queryParams: { mId: 'bbbb' }
            });
            expect(globalStore.openParentMenuId()).toBeNull();
        });
    });

    describe('collapse control', () => {
        it('toggles the bar and emits dot-side-nav-toggle', () => {
            globalStore.collapseNavigation();

            spectator.component.handleCollapseButtonClick();

            expect(globalStore.isNavigationCollapsed()).toBe(false);
            expect(dotEventsService.notify).toHaveBeenCalledWith('dot-side-nav-toggle');

            spectator.component.handleCollapseButtonClick();

            expect(globalStore.isNavigationCollapsed()).toBe(true);
            expect(dotEventsService.notify).toHaveBeenCalledTimes(2);
        });

        it('collapsing does not close the open group (preserved behavior)', () => {
            globalStore.expandNavigation();
            globalStore.toggleParent(LAYOUT_A);

            spectator.component.handleCollapseButtonClick();

            expect(globalStore.isNavigationCollapsed()).toBe(true);
            expect(globalStore.openParentMenuId()).toBe(LAYOUT_A);
        });
    });
});

/**
 * A failed menu request never blanks a navigation that works and never throws in the shell (issue
 * #38078). The first load of a session, if it fails, leaves the bar empty.
 */
describe('DotNavigationComponent when the menu request fails', () => {
    const MENU_URL = '/api/v1/menu';
    const menus = [dotMenuMock(), dotMenuMock1()] as unknown as DotMenu[];

    let spectator: Spectator<DotNavigationComponent>;
    let globalStore: InstanceType<typeof GlobalStore>;
    let httpMock: HttpTestingController;

    const createComponent = createComponentFactory({
        component: DotNavigationComponent,
        imports: [
            DotNavIconComponent,
            DotIconComponent,
            BrowserAnimationsModule,
            TooltipModule,
            DotRandomIconPipe
        ],
        providers: [
            provideRouter([]),
            { provide: DotCurrentUserService, useClass: DotCurrentUserServiceMock },
            mockProvider(IframeOverlayService),
            mockProvider(DotNavigationService),
            mockProvider(DotEventsService),
            mockProvider(DotRouterService, { currentPortlet: { id: '123', parentMenuId: '123' } }),
            { provide: LoginService, useClass: LoginServiceMock },
            { provide: DotSystemConfigService, useValue: { getSystemConfig: () => of({}) } },
            GlobalStore,
            provideHttpClient(),
            provideHttpClientTesting()
        ]
    });

    const failRequest = () =>
        httpMock.expectOne(MENU_URL).flush('x', { status: 500, statusText: 'Server Error' });

    beforeEach(() => {
        spectator = createComponent({ detectChanges: false });
        globalStore = spectator.inject(GlobalStore);
        httpMock = TestBed.inject(HttpTestingController);
    });

    it('renders an empty bar, without throwing, when the first load fails', () => {
        globalStore.reload();
        failRequest();

        expect(() => spectator.detectChanges()).not.toThrow();

        expect(globalStore.status()).toBe('error');
        expect(spectator.queryAll('.dot-nav__list-item')).toHaveLength(0);
    });

    it('shows the groups once a later load succeeds', () => {
        globalStore.reload();
        failRequest();
        spectator.detectChanges();

        seedMenu(globalStore, menus);
        spectator.detectChanges();

        expect(spectator.queryAll('.dot-nav__list-item')).toHaveLength(2);
    });

    it('keeps the groups that were showing when a reload fails', () => {
        seedMenu(globalStore, menus);
        spectator.detectChanges();

        globalStore.reload();
        failRequest();
        spectator.detectChanges();

        expect(globalStore.status()).toBe('error');
        expect(spectator.queryAll('.dot-nav__list-item')).toHaveLength(2);
    });
});
