import { patchState, signalStore, withState, WritableStateSource } from '@ngrx/signals';
import { NamedEntityState, setAllEntities } from '@ngrx/signals/entities';
import { vi } from 'vitest';

import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { DotMenu, getMenuItemKey, MenuItemEntity } from '@dotcms/dotcms-models';

import { DOT_MENU_ROUTES_REGISTRAR } from './menu-routes-registrar.token';
import { menuConfig } from './menu.slice';
import { withMenu } from './with-menu.feature';

describe('withMenu Feature', () => {
    // Create a test store that uses the withMenu feature
    const TestStore = signalStore(withState({}), withMenu());

    let store: InstanceType<typeof TestStore>;
    let httpMock: HttpTestingController;

    /** Puts a menu in the store the way the app does: one request, answered by the backend. */
    const seedMenu = (menus: DotMenu[]) => {
        store.reload();
        httpMock.expectOne('/api/v1/menu').flush({ entity: menus });
    };

    const mockMenuItems: DotMenu[] = [
        {
            active: false,
            id: '1',
            label: 'Home Menu',
            isOpen: false,
            menuItems: [
                {
                    active: false,
                    ajax: true,
                    angular: true,
                    id: '1-1',
                    label: 'Home',
                    url: '/',
                    menuLink: '/',
                    parentMenuId: '1'
                }
            ],
            name: 'Home Menu',
            tabDescription: 'Home section',
            tabIcon: 'pi pi-home',
            tabName: 'Home',
            url: '/'
        },
        {
            active: false,
            id: '2',
            isOpen: false,
            label: 'About Menu',
            menuItems: [
                {
                    active: false,
                    ajax: true,
                    angular: true,
                    id: '2-1',
                    label: 'About',
                    url: '/about',
                    menuLink: '/about',
                    parentMenuId: '2'
                }
            ],
            name: 'About Menu',
            tabDescription: 'About section',
            tabIcon: 'pi pi-info-circle',
            tabName: 'About',
            url: '/about'
        },
        {
            active: false,
            id: '3',
            isOpen: false,
            label: 'Legacy Menu',
            menuItems: [
                {
                    active: false,
                    ajax: true,
                    angular: true,
                    id: '4',
                    label: 'Service 1',
                    url: '/services/1',
                    menuLink: '/services/1',
                    parentMenuId: '3'
                },
                {
                    active: false,
                    ajax: true,
                    angular: true,
                    id: '5',
                    label: 'Service 2',
                    url: '/services/2',
                    menuLink: '/services/2',
                    parentMenuId: '3'
                }
            ],
            name: 'Services Menu',
            tabDescription: 'Services section',
            tabIcon: 'pi pi-briefcase',
            tabName: 'Services',
            url: '/services'
        }
    ];

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [TestStore, provideHttpClient(), provideHttpClientTesting()]
        });

        store = TestBed.inject(TestStore);
        httpMock = TestBed.inject(HttpTestingController);
    });

    describe('Initial State', () => {
        it('should initialize with empty menu items', () => {
            const items = store.menuItemsEntities();
            expect(items).toBeDefined();
            expect(Array.isArray(items)).toBe(true);
            expect(items.length).toBe(0);
        });

        it('should initialize with no active menu item', () => {
            expect(store.activeMenuItem()).toBeNull();
        });

        it('should initialize with navigation collapsed', () => {
            expect(store.isNavigationCollapsed()).toBe(true);
        });

        it('should initialize with no open parent menu', () => {
            expect(store.openParentMenuId()).toBeNull();
        });
    });

    describe('Load Menu', () => {
        it('should load menu items and transform them to entities', () => {
            seedMenu(mockMenuItems);
            const items = store.menuItemsEntities();
            expect(items.length).toBe(4); // 1 + 1 + 2 menu items
        });

        it('should add parentMenuId, parentMenuLabel, and parentMenuIcon to entities', () => {
            seedMenu(mockMenuItems);
            const items = store.menuItemsEntities();
            const firstItem = items[0];
            expect(firstItem.parentMenuId).toBe('1');
            expect(firstItem.parentMenuLabel).toBe('Home');
            expect(firstItem.parentMenuIcon).toBe('pi pi-home');
        });

        it('should replace existing menu items when loading new menu', () => {
            seedMenu(mockMenuItems);
            const newItems: DotMenu[] = [
                {
                    active: false,
                    id: '100',
                    isOpen: false,
                    label: 'New Menu',
                    menuItems: [
                        {
                            active: false,
                            ajax: true,
                            angular: true,
                            id: '100-1',
                            label: 'New Item',
                            url: '/new',
                            menuLink: '/new',
                            parentMenuId: '100'
                        }
                    ],
                    name: 'New Menu',
                    tabDescription: 'New section',
                    tabIcon: 'pi pi-star',
                    tabName: 'New',
                    url: '/new'
                }
            ];
            seedMenu(newItems);
            const items = store.menuItemsEntities();
            expect(items.length).toBe(1);
            expect(items[0].id).toBe('100-1');
        });

        it('should generate menuLink for angular items', () => {
            seedMenu(mockMenuItems);
            const items = store.menuItemsEntities();
            const angularItem = items.find((item) => item.angular);
            expect(angularItem?.menuLink).toBe('/');
        });

        it('should generate menuLink for legacy items with /c/ prefix', () => {
            const legacyMenu: DotMenu[] = [
                {
                    active: false,
                    id: 'legacy',
                    isOpen: false,
                    label: 'Legacy Menu',
                    menuItems: [
                        {
                            active: false,
                            ajax: true,
                            angular: false,
                            id: 'legacy-portlet',
                            label: 'Legacy',
                            url: '/legacy',
                            menuLink: '',
                            parentMenuId: 'legacy'
                        }
                    ],
                    name: 'Legacy Menu',
                    tabDescription: 'Legacy section',
                    tabIcon: 'pi pi-cog',
                    tabName: 'Legacy',
                    url: '/legacy'
                }
            ];
            seedMenu(legacyMenu);
            const items = store.menuItemsEntities();
            expect(items[0].menuLink).toBe('/c/legacy-portlet');
        });
    });

    describe('Menu Group', () => {
        beforeEach(() => {
            seedMenu(mockMenuItems);
        });

        it('should group menu items by parentMenuId', () => {
            const groups = store.menuGroup();
            expect(groups.length).toBe(3);
            expect(groups[0].id).toBe('1');
            expect(groups[0].menuItems.length).toBe(1);
            expect(groups[2].id).toBe('3');
            expect(groups[2].menuItems.length).toBe(2);
        });

        it('should include parent menu label and icon in groups', () => {
            const groups = store.menuGroup();
            expect(groups[0].label).toBe('Home');
            expect(groups[0].icon).toBe('pi pi-home');
            expect(groups[1].label).toBe('About');
            expect(groups[1].icon).toBe('pi pi-info-circle');
        });

        it('should set isOpen based on openParentMenuId', () => {
            store.toggleParent('1');
            const groups = store.menuGroup();
            expect(groups.find((g) => g.id === '1')?.isOpen).toBe(true);
            expect(groups.find((g) => g.id === '2')?.isOpen).toBe(false);
        });
    });

    describe('Active Menu Item', () => {
        beforeEach(() => {
            seedMenu(mockMenuItems);
        });

        it('should activate a menu item by composite key', () => {
            store.activateMenuItem('1-1__1');
            const activeItem = store.activeMenuItem();
            expect(activeItem).toBeDefined();
            expect(activeItem?.id).toBe('1-1');
        });

        it('should deactivate previous item when activating new one', () => {
            store.activateMenuItem('1-1__1');
            store.activateMenuItem('2-1__2');
            const activeItem = store.activeMenuItem();
            expect(activeItem?.id).toBe('2-1');
        });

        it('should activate item and open parent menu group', () => {
            store.activateMenuItemWithParent('1-1__1', '1');
            const activeItem = store.activeMenuItem();
            expect(activeItem?.id).toBe('1-1');
            expect(store.openParentMenuId()).toBe('1');
        });

        it('should activate item without opening parent when collapsed', () => {
            store.collapseNavigation();
            store.activateMenuItemWithParent('1-1__1', null);
            const activeItem = store.activeMenuItem();
            expect(activeItem?.id).toBe('1-1');
            expect(store.openParentMenuId()).toBeNull();
        });

        it('should return null for activeMenuItem when no item is active', () => {
            expect(store.activeMenuItem()).toBeNull();
        });
    });

    describe('Toggle Parent Menu', () => {
        beforeEach(() => {
            seedMenu(mockMenuItems);
        });

        it('should open a parent menu group', () => {
            store.toggleParent('1');
            expect(store.openParentMenuId()).toBe('1');
        });

        it('should close parent menu group if already open', () => {
            store.toggleParent('1');
            store.toggleParent('1');
            expect(store.openParentMenuId()).toBeNull();
        });

        it('should close other parent menu groups when opening a new one', () => {
            store.toggleParent('1');
            store.toggleParent('2');
            expect(store.openParentMenuId()).toBe('2');
        });
    });

    describe('Close All Parents', () => {
        beforeEach(() => {
            seedMenu(mockMenuItems);
            store.toggleParent('1');
        });

        it('should close all parent menu groups', () => {
            expect(store.openParentMenuId()).toBe('1');
            store.closeAllParents();
            expect(store.openParentMenuId()).toBeNull();
        });
    });

    describe('Toggle Navigation', () => {
        beforeEach(() => {
            seedMenu(mockMenuItems);
        });

        it('should toggle navigation from collapsed to expanded', () => {
            expect(store.isNavigationCollapsed()).toBe(true);
            store.toggleNavigation();
            expect(store.isNavigationCollapsed()).toBe(false);
        });

        it('should toggle navigation from expanded to collapsed', () => {
            store.toggleNavigation(); // Expand
            store.toggleNavigation(); // Collapse
            expect(store.isNavigationCollapsed()).toBe(true);
        });

        it('should not close parent menus when collapsing', () => {
            store.toggleParent('1');
            store.expandNavigation(); // Expand (should open active item's parent)
            store.collapseNavigation(); // Collapse (should NOT close all)
            expect(store.openParentMenuId()).toBe('1');
        });

        it('should open active item parent menu when expanding', () => {
            store.activateMenuItem('1-1__1');
            store.expandNavigation(); // Use expandNavigation instead of toggleNavigation
            expect(store.openParentMenuId()).toBe('1');
        });
    });

    describe('Collapse Navigation', () => {
        beforeEach(() => {
            seedMenu(mockMenuItems);
            store.toggleNavigation(); // Expand first
        });

        it('should collapse navigation', () => {
            store.collapseNavigation();
            expect(store.isNavigationCollapsed()).toBe(true);
        });

        it('should not close parent menu groups when collapsing', () => {
            store.toggleParent('1');
            store.collapseNavigation();
            expect(store.openParentMenuId()).toBe('1');
        });
    });

    describe('Expand Navigation', () => {
        beforeEach(() => {
            seedMenu(mockMenuItems);
        });

        it('should expand navigation', () => {
            store.expandNavigation();
            expect(store.isNavigationCollapsed()).toBe(false);
        });

        it('should open active item parent menu when expanding', () => {
            store.activateMenuItem('1-1__1');
            store.expandNavigation();
            expect(store.openParentMenuId()).toBe('1');
        });

        it('should not open any parent menu if no item is active', () => {
            store.expandNavigation();
            expect(store.openParentMenuId()).toBeNull();
        });
    });

    describe('Entity Map', () => {
        beforeEach(() => {
            seedMenu(mockMenuItems);
        });

        it('should provide entity map for direct lookups', () => {
            const entityMap = store.entityMap();
            expect(entityMap).toBeDefined();
            expect(typeof entityMap).toBe('object');
        });

        it('should allow lookup by composite key', () => {
            const entityMap = store.entityMap();
            const key = '1-1__1';
            const item = entityMap[key];
            expect(item).toBeDefined();
            expect(item.id).toBe('1-1');
        });
    });

    describe('Flatten Menu Items', () => {
        beforeEach(() => {
            seedMenu(mockMenuItems);
        });

        it('should return all menu items as flat array', () => {
            const flattened = store.menuItemsEntities();
            expect(flattened.length).toBe(4);
            expect(flattened.every((item) => item instanceof Object)).toBe(true);
        });

        it('should preserve all menu item properties', () => {
            const flattened = store.menuItemsEntities();
            const firstItem = flattened[0];
            expect(firstItem.id).toBe('1-1');
            expect(firstItem.label).toBe('Home');
            expect(firstItem.parentMenuId).toBe('1');
            expect(firstItem.parentMenuLabel).toBe('Home');
        });
    });

    describe('Complex Scenarios', () => {
        beforeEach(() => {
            seedMenu(mockMenuItems);
        });

        it('should handle full menu workflow', () => {
            // Activate item
            store.activateMenuItem('1-1__1');
            expect(store.activeMenuItem()?.id).toBe('1-1');

            // Open parent menu
            store.toggleParent('3');
            expect(store.openParentMenuId()).toBe('3');

            // Change active item
            store.activateMenuItem('2-1__2');
            expect(store.activeMenuItem()?.id).toBe('2-1');
        });

        it('should handle navigation toggle with active item', () => {
            // Activate an item
            store.activateMenuItem('1-1__1');

            // Expand navigation (should open active item's parent)
            store.expandNavigation();
            expect(store.openParentMenuId()).toBe('1');

            // Collapse navigation (should close all)
            store.collapseNavigation();
            expect(store.openParentMenuId()).toBe('1');
        });

        it('should maintain single active item constraint', () => {
            store.activateMenuItem('1-1__1');
            store.activateMenuItem('2-1__2');
            store.activateMenuItem('1-1__1');

            expect(store.activeKey()).toBe('1-1__1');
            expect(store.activeMenuItem()?.id).toBe('1-1');
        });

        it('should maintain single open parent constraint', () => {
            store.toggleParent('1');
            store.toggleParent('2');
            store.toggleParent('3');

            const groups = store.menuGroup();
            const openGroups = groups.filter((g) => g.isOpen);
            expect(openGroups.length).toBe(1);
            expect(openGroups[0].id).toBe('3');
        });
    });
});

/**
 * Characterization of the collapsed/expanded state persistence (issue #38078): the bar restores
 * itself from `localStorage` key `dotcms.menu.status` after a browser reload, and writes it back on
 * every change. These cases pin today's behavior and must keep passing through the consolidation.
 */
describe('withMenu navigation status persistence', () => {
    const STATUS_KEY = 'dotcms.menu.status';
    const PersistentStore = signalStore(withState({}), withMenu());

    const createStore = (stored?: string) => {
        TestBed.resetTestingModule();
        localStorage.clear();
        if (stored !== undefined) {
            localStorage.setItem(STATUS_KEY, stored);
        }
        TestBed.configureTestingModule({ providers: [PersistentStore] });

        return TestBed.inject(PersistentStore);
    };

    afterEach(() => {
        localStorage.clear();
    });

    it('starts collapsed when nothing is stored', () => {
        expect(createStore().isNavigationCollapsed()).toBe(true);
    });

    it('restores an expanded bar from a stored false', () => {
        expect(createStore('false').isNavigationCollapsed()).toBe(false);
    });

    it('restores a collapsed bar from a stored true', () => {
        expect(createStore('true').isNavigationCollapsed()).toBe(true);
    });

    it('falls back to collapsed, without throwing, when the stored value is not a boolean', () => {
        expect(() => createStore('{not-json')).not.toThrow();
        expect(createStore('{not-json').isNavigationCollapsed()).toBe(true);
        expect(createStore('0').isNavigationCollapsed()).toBe(true);
    });

    it('writes the state back to localStorage when it changes', () => {
        const store = createStore('true');
        TestBed.tick();

        store.toggleNavigation();
        TestBed.tick();

        expect(localStorage.getItem(STATUS_KEY)).toBe('false');

        store.toggleNavigation();
        TestBed.tick();

        expect(localStorage.getItem(STATUS_KEY)).toBe('true');
    });

    it('follows a change made in another tab through the storage event', () => {
        const store = createStore('true');

        window.dispatchEvent(new StorageEvent('storage', { key: STATUS_KEY, newValue: 'false' }));
        expect(store.isNavigationCollapsed()).toBe(false);

        window.dispatchEvent(new StorageEvent('storage', { key: STATUS_KEY, newValue: 'true' }));
        expect(store.isNavigationCollapsed()).toBe(true);
    });

    it('ignores storage events for other keys', () => {
        const store = createStore('true');

        window.dispatchEvent(
            new StorageEvent('storage', { key: 'another.key', newValue: 'false' })
        );

        expect(store.isNavigationCollapsed()).toBe(true);
    });

    it('starts with no open group and no active item, as the bar does after a browser reload', () => {
        const store = createStore('false');

        expect(store.openParentMenuId()).toBeNull();
        expect(store.activeMenuItem()).toBeNull();
        expect(store.menuItemsEntities()).toEqual([]);
    });
});

/**
 * The menu feature owns the fetch (issue #38078). These cases use the real feature with the HTTP
 * layer faked at its boundary, so what they assert is what the shell observes: requests sent,
 * `status`, and the entities that stay on screen.
 */
describe('withMenu loading', () => {
    const MENU_URL = '/api/v1/menu';
    const LoadingStore = signalStore(withState({}), withMenu());

    const layoutA = 'aaaa1111-0000-4000-8000-000000000001';
    const layoutB = 'bbbb2222-0000-4000-8000-000000000002';

    const menuA = [
        {
            id: layoutA,
            name: 'Site',
            tabName: 'Site',
            tabIcon: 'icon-a',
            url: '/pages',
            menuItems: [
                { id: 'pages', label: 'Pages', url: '/pages', angular: true, ajax: false },
                { id: 'legacy', label: 'Legacy', url: '/c/legacy?r=1', angular: false, ajax: false }
            ]
        }
    ] as unknown as DotMenu[];

    const menuB = [
        {
            id: layoutB,
            name: 'Content',
            tabName: 'Content',
            tabIcon: 'icon-b',
            url: '/content-drive',
            menuItems: [
                {
                    id: 'content-drive',
                    label: 'Content Drive',
                    url: '/content-drive',
                    angular: true,
                    ajax: false
                }
            ]
        }
    ] as unknown as DotMenu[];

    let store: InstanceType<typeof LoadingStore>;
    let httpMock: HttpTestingController;
    let registrar: ReturnType<typeof vi.fn>;
    let statusSeenByRegistrar: string[];

    const setup = (withRegistrar = true) => {
        statusSeenByRegistrar = [];
        registrar = vi.fn(() => statusSeenByRegistrar.push(store.status()));
        TestBed.resetTestingModule();
        TestBed.configureTestingModule({
            providers: [
                LoadingStore,
                provideHttpClient(),
                provideHttpClientTesting(),
                ...(withRegistrar
                    ? [{ provide: DOT_MENU_ROUTES_REGISTRAR, useValue: registrar }]
                    : [])
            ]
        });
        store = TestBed.inject(LoadingStore);
        httpMock = TestBed.inject(HttpTestingController);
    };

    const settled = () => {
        let completed = false;
        let emissions = 0;
        store.ensureLoaded().subscribe({
            next: () => (emissions += 1),
            complete: () => (completed = true)
        });

        return {
            get completed() {
                return completed;
            },
            get emissions() {
                return emissions;
            }
        };
    };

    const flushOk = (menus: DotMenu[]) => {
        httpMock.expectOne(MENU_URL).flush({ entity: menus });
        TestBed.tick();
    };

    beforeEach(() => setup());

    afterEach(() => {
        httpMock.verify();
        localStorage.clear();
    });

    describe('ensureLoaded', () => {
        it('starts as idle with no request', () => {
            expect(store.status()).toBe('idle');
            httpMock.expectNone(MENU_URL);
        });

        it('on idle sends one request, loads the entities and settles at ready', () => {
            const result = settled();

            expect(store.status()).toBe('loading');
            expect(result.completed).toBe(false);

            flushOk(menuA);

            expect(store.status()).toBe('ready');
            expect(store.menuItemsEntities().map((item) => item.id)).toEqual(['pages', 'legacy']);
            expect(result.completed).toBe(true);
            expect(result.emissions).toBeLessThanOrEqual(1);
        });

        it('on ready completes without sending a request', () => {
            settled();
            flushOk(menuA);

            const again = settled();

            httpMock.expectNone(MENU_URL);
            expect(again.completed).toBe(true);
        });

        it('while loading joins the load in flight: still one request for every caller', () => {
            store.reload();
            const first = settled();
            const second = settled();

            flushOk(menuA);

            expect(first.completed).toBe(true);
            expect(second.completed).toBe(true);
            expect(store.status()).toBe('ready');
        });

        it('settles, without erroring or hanging, when the load fails', () => {
            const result = settled();

            httpMock
                .expectOne(MENU_URL)
                .flush({ message: 'boom' }, { status: 500, statusText: 'Server Error' });
            TestBed.tick();

            expect(store.status()).toBe('error');
            expect(result.completed).toBe(true);
        });

        it('on error starts a new request', () => {
            settled();
            httpMock.expectOne(MENU_URL).flush('x', { status: 500, statusText: 'Server Error' });
            TestBed.tick();

            const retry = settled();

            expect(store.status()).toBe('loading');
            flushOk(menuA);
            expect(store.status()).toBe('ready');
            expect(retry.completed).toBe(true);
        });
    });

    describe('reload', () => {
        beforeEach(() => {
            settled();
            flushOk(menuA);
        });

        it('always sends a request and shows loading at once, keeping the entities on screen', () => {
            store.reload();

            expect(store.status()).toBe('loading');
            expect(store.menuItemsEntities().map((item) => item.id)).toEqual(['pages', 'legacy']);
            flushOk(menuB);
        });

        it('replaces the whole snapshot with the new menu', () => {
            store.reload();
            flushOk(menuB);

            expect(store.status()).toBe('ready');
            expect(store.menuItemsEntities().map((item) => item.id)).toEqual(['content-drive']);
        });

        it('keeps only the latest response when reload is called twice', () => {
            store.reload();
            store.reload();

            const requests = httpMock.match(MENU_URL);
            expect(requests).toHaveLength(2);
            expect(requests[0].cancelled).toBe(true);
            requests[1].flush({ entity: menuB });
            TestBed.tick();

            expect(store.menuItemsEntities().map((item) => item.id)).toEqual(['content-drive']);
            expect(store.status()).toBe('ready');
        });

        it('a failed reload sets error and keeps the entities that were showing', () => {
            store.reload();

            httpMock.expectOne(MENU_URL).flush('x', { status: 500, statusText: 'Server Error' });
            TestBed.tick();

            expect(store.status()).toBe('error');
            expect(store.menuItemsEntities().map((item) => item.id)).toEqual(['pages', 'legacy']);
        });

        it('the next successful load after a failure replaces the snapshot', () => {
            store.reload();
            httpMock.expectOne(MENU_URL).flush('x', { status: 500, statusText: 'Server Error' });
            TestBed.tick();

            store.reload();
            flushOk(menuB);

            expect(store.status()).toBe('ready');
            expect(store.menuItemsEntities().map((item) => item.id)).toEqual(['content-drive']);
        });

        it('leaves the collapsed state and the open group alone', () => {
            store.expandNavigation();
            store.toggleParent(layoutA);

            store.reload();
            flushOk(menuB);

            expect(store.isNavigationCollapsed()).toBe(false);
            expect(store.openParentMenuId()).toBe(layoutA);
        });
    });

    describe('first load failure', () => {
        it('leaves the entities empty and does not throw', () => {
            expect(() => {
                settled();
                httpMock.expectOne(MENU_URL).flush('x', { status: 0, statusText: 'Unknown' });
                TestBed.tick();
            }).not.toThrow();

            expect(store.status()).toBe('error');
            expect(store.menuItemsEntities()).toEqual([]);
            expect(store.menuGroup()).toEqual([]);
            expect(store.firstMenuItem()).toBeNull();
        });
    });

    describe('routes registrar', () => {
        it('is called with every menu item, before the status becomes ready', () => {
            settled();
            flushOk(menuA);

            expect(registrar).toHaveBeenCalledTimes(1);
            expect(registrar.mock.calls[0][0].map((item: { id: string }) => item.id)).toEqual([
                'pages',
                'legacy'
            ]);
            expect(statusSeenByRegistrar).toEqual(['loading']);
        });

        it('is called again after every reload', () => {
            settled();
            flushOk(menuA);

            store.reload();
            flushOk(menuB);

            expect(registrar).toHaveBeenCalledTimes(2);
            expect(registrar.mock.calls[1][0].map((item: { id: string }) => item.id)).toEqual([
                'content-drive'
            ]);
        });

        it('is not called when the load fails', () => {
            settled();
            httpMock.expectOne(MENU_URL).flush('x', { status: 500, statusText: 'Server Error' });
            TestBed.tick();

            expect(registrar).not.toHaveBeenCalled();
        });

        it('is optional: the load works when nothing provides it', () => {
            setup(false);

            settled();
            flushOk(menuA);

            expect(store.status()).toBe('ready');
        });
    });

    describe('loadFreshPortletUrl', () => {
        beforeEach(() => {
            settled();
            flushOk(menuA);
        });

        it('requests the menu now and returns the url of the first non-Angular item with that id', () => {
            let url: string | undefined;

            store.loadFreshPortletUrl('legacy').subscribe((value) => (url = value));
            httpMock.expectOne(MENU_URL).flush({
                entity: [
                    {
                        ...menuA[0],
                        menuItems: [
                            { id: 'legacy', label: 'Legacy', url: '/c/legacy?r=2', angular: false }
                        ]
                    }
                ]
            });

            expect(url).toBe('/c/legacy?r=2');
        });

        it('never changes the entities, the status or the highlight', () => {
            const before = store.menuItemsEntities();
            store.reload();
            flushOk(menuA);
            const statusBefore = store.status();

            store.loadFreshPortletUrl('legacy').subscribe();
            httpMock.expectOne(MENU_URL).flush({ entity: menuB });
            TestBed.tick();

            expect(store.menuItemsEntities().map((item) => item.id)).toEqual(
                before.map((item) => item.id)
            );
            expect(store.status()).toBe(statusBefore);
            expect(store.activeMenuItem()).toBeNull();
        });

        it('answers an Angular item with an empty string without sending a request', () => {
            const urls: string[] = [];

            store.loadFreshPortletUrl('pages').subscribe((value) => urls.push(value));

            httpMock.expectNone(MENU_URL);
            expect(urls).toEqual(['']);
        });

        it('returns an empty string for an id that is not in the menu, after asking', () => {
            const urls: string[] = [];

            store.loadFreshPortletUrl('nope').subscribe((value) => urls.push(value));
            httpMock.expectOne(MENU_URL).flush({ entity: menuA });

            expect(urls).toEqual(['']);
        });

        it('falls back to the url already in the snapshot when the request fails', () => {
            let url: string | undefined;

            store.loadFreshPortletUrl('legacy').subscribe((value) => (url = value));
            httpMock.expectOne(MENU_URL).flush('x', { status: 500, statusText: 'Server Error' });

            expect(url).toBe('/c/legacy?r=1');
        });
    });
});

/**
 * Identity, order and highlight of the menu entities (issue #38078). An item is identified by its
 * portlet id plus the FULL layout id; the 4-character `mId` is only a lookup field. The highlight is
 * the `activeKey` of an item, never a flag written on the entity, so replacing the snapshot does not
 * erase it by itself.
 */
describe('withMenu identity and highlight', () => {
    const MENU_URL = '/api/v1/menu';
    const IdentityStore = signalStore(withState({}), withMenu());

    const SITE = 'aaaa1111-0000-4000-8000-000000000001';
    const SITE_CLONE = 'aaaa9999-0000-4000-8000-000000000002';
    const CONTENT = 'bbbb2222-0000-4000-8000-000000000003';

    const group = (id: string, items: string[]) =>
        ({
            id,
            name: id,
            tabName: id,
            tabIcon: 'icon',
            url: `/${items[0]}`,
            menuItems: items.map((portlet) => ({
                id: portlet,
                label: portlet,
                url: `/${portlet}`,
                angular: true,
                ajax: false
            }))
        }) as unknown as DotMenu;

    let store: InstanceType<typeof IdentityStore>;
    let httpMock: HttpTestingController;

    const seed = (menus: DotMenu[]) => {
        store.reload();
        httpMock.expectOne(MENU_URL).flush({ entity: menus });
    };

    beforeEach(() => {
        TestBed.resetTestingModule();
        TestBed.configureTestingModule({
            providers: [IdentityStore, provideHttpClient(), provideHttpClientTesting()]
        });
        store = TestBed.inject(IdentityStore);
        httpMock = TestBed.inject(HttpTestingController);
        localStorage.clear();
    });

    describe('entity key', () => {
        it('is the portlet id plus the full layout id', () => {
            seed([group(SITE, ['pages'])]);

            expect(Object.keys(store.entityMap())).toEqual([`pages__${SITE}`]);
        });

        it('keeps both items when two layouts share the first 4 characters and a portlet id', () => {
            seed([group(SITE, ['pages']), group(SITE_CLONE, ['pages'])]);

            expect(store.menuItemsEntities()).toHaveLength(2);
            expect(Object.keys(store.entityMap()).sort()).toEqual(
                [`pages__${SITE}`, `pages__${SITE_CLONE}`].sort()
            );
        });

        it('is the one getMenuItemKey builds', () => {
            seed([group(SITE, ['pages'])]);

            expect(getMenuItemKey(store.menuItemsEntities()[0])).toBe(`pages__${SITE}`);
        });
    });

    describe('entity fields', () => {
        it('stamps the short layout id, the group position and the item position', () => {
            seed([group(SITE, ['pages', 'templates']), group(CONTENT, ['content-drive'])]);

            const byId = (id: string) => store.menuItemsEntities().find((item) => item.id === id);

            expect(byId('pages')).toMatchObject({
                shortLayoutId: 'aaaa',
                tabOrder: 0,
                itemIndex: 0,
                parentMenuId: SITE
            });
            expect(byId('templates')).toMatchObject({ tabOrder: 0, itemIndex: 1 });
            expect(byId('content-drive')).toMatchObject({
                shortLayoutId: 'bbbb',
                tabOrder: 1,
                itemIndex: 0
            });
        });

        it('keeps the raw url for the iframe and builds menuLink with the same rule as before', () => {
            seed([
                {
                    ...group(SITE, ['pages']),
                    menuItems: [
                        { id: 'pages', label: 'Pages', url: '/pages', angular: true, ajax: false },
                        { id: 'old', label: 'Old', url: '/c/old?r=1', angular: false, ajax: false }
                    ]
                } as unknown as DotMenu
            ]);

            const entities = store.menuItemsEntities();
            expect(entities.find((i) => i.id === 'pages')).toMatchObject({
                url: '/pages',
                menuLink: '/pages'
            });
            expect(entities.find((i) => i.id === 'old')).toMatchObject({
                url: '/c/old?r=1',
                menuLink: '/c/old'
            });
        });
    });

    describe('group order', () => {
        it('follows the position of each group in the response', () => {
            seed([group(CONTENT, ['content-drive']), group(SITE, ['pages'])]);

            expect(store.menuGroup().map((g) => g.id)).toEqual([CONTENT, SITE]);
            expect(store.menuGroup().map((g) => g.tabOrder)).toEqual([0, 1]);
        });

        it('does not depend on the order the entities are stored in', () => {
            seed([group(SITE, ['pages', 'templates', 'containers']), group(CONTENT, ['drive'])]);

            // The feature's state reaches the store type as read-only signals, so `patchState` rejects it;
            // this test only needs the entity half of the state, which is writable at runtime.
            const entityState = store as unknown as WritableStateSource<
                NamedEntityState<MenuItemEntity, 'menuItems'>
            >;
            patchState(
                entityState,
                setAllEntities([...store.menuItemsEntities()].reverse(), menuConfig)
            );

            expect(store.menuGroup().map((g) => g.id)).toEqual([SITE, CONTENT]);
            expect(store.menuGroup()[0].menuItems.map((item) => item.id)).toEqual([
                'pages',
                'templates',
                'containers'
            ]);
            expect(store.firstMenuItem()?.id).toBe('pages');
        });
    });

    describe('activeKey', () => {
        beforeEach(() => seed([group(SITE, ['pages', 'templates']), group(CONTENT, ['drive'])]));

        it('starts empty, as it does after a browser reload', () => {
            expect(store.activeKey()).toBeNull();
            expect(store.activeMenuItem()).toBeNull();
            expect(store.isGroupActive()).toBe(false);
        });

        it('is set with the item and opens its group', () => {
            store.activateMenuItemWithParent(`templates__${SITE}`, SITE);

            expect(store.activeKey()).toBe(`templates__${SITE}`);
            expect(store.activeMenuItem()?.id).toBe('templates');
            expect(store.openParentMenuId()).toBe(SITE);
            expect(store.isGroupActive()).toBe(true);
        });

        it('only one item is active at a time', () => {
            store.activateMenuItemWithParent(`pages__${SITE}`, SITE);
            store.activateMenuItemWithParent(`drive__${CONTENT}`, CONTENT);

            expect(store.activeMenuItem()?.id).toBe('drive');
        });

        it('ignores a key that is not in the menu', () => {
            store.activateMenuItemWithParent(`pages__${SITE}`, SITE);

            store.activateMenuItemWithParent('ghost__nowhere', 'nowhere');

            expect(store.activeKey()).toBe(`pages__${SITE}`);
        });

        it('never writes an active flag on the entities', () => {
            store.activateMenuItemWithParent(`pages__${SITE}`, SITE);

            expect(store.menuItemsEntities().every((item) => item.active === undefined)).toBe(true);
        });

        it('survives replacing the snapshot while the item still exists', () => {
            store.activateMenuItemWithParent(`templates__${SITE}`, SITE);

            seed([group(SITE, ['pages', 'templates', 'containers'])]);

            expect(store.activeKey()).toBe(`templates__${SITE}`);
            expect(store.activeMenuItem()?.id).toBe('templates');
        });

        it('is cleared when the replacement no longer has the item', () => {
            store.activateMenuItemWithParent(`templates__${SITE}`, SITE);

            seed([group(SITE, ['pages'])]);

            expect(store.activeKey()).toBeNull();
            expect(store.activeMenuItem()).toBeNull();
        });

        it('is kept by a failed reload together with the entities', () => {
            store.activateMenuItemWithParent(`templates__${SITE}`, SITE);

            store.reload();
            httpMock.expectOne(MENU_URL).flush('x', { status: 500, statusText: 'Server Error' });

            expect(store.activeMenuItem()?.id).toBe('templates');
        });

        it('leaves the open group and the collapsed state alone when it is cleared', () => {
            store.expandNavigation();
            store.activateMenuItemWithParent(`templates__${SITE}`, SITE);

            seed([group(CONTENT, ['drive'])]);

            expect(store.isNavigationCollapsed()).toBe(false);
            expect(store.openParentMenuId()).toBe(SITE);
        });
    });

    describe('queries over the snapshot', () => {
        const legacyGroup = {
            ...group(CONTENT, ['workflow', 'drive']),
            menuItems: [
                {
                    id: 'workflow',
                    label: 'Workflow',
                    url: '/c/workflow',
                    angular: false,
                    ajax: false
                },
                { id: 'drive', label: 'Drive', url: '/drive', angular: true, ajax: false }
            ]
        } as unknown as DotMenu;

        beforeEach(() => seed([group(SITE, ['pages', 'locales']), legacyGroup]));

        it('hasPortlet is true for a portlet in any layout and false for one that is not', () => {
            expect(store.hasPortlet('pages')).toBe(true);
            expect(store.hasPortlet('drive')).toBe(true);
            expect(store.hasPortlet('ghost')).toBe(false);
        });

        it('hasPortlet with jspOnly only counts a portlet that is not an Angular one', () => {
            expect(store.hasPortlet('workflow', { jspOnly: true })).toBe(true);
            expect(store.hasPortlet('pages', { jspOnly: true })).toBe(false);
            expect(store.hasPortlet('ghost', { jspOnly: true })).toBe(false);
        });

        it('hasPortlet is false while the menu is empty', () => {
            TestBed.resetTestingModule();
            TestBed.configureTestingModule({
                providers: [IdentityStore, provideHttpClient(), provideHttpClientTesting()]
            });

            expect(TestBed.inject(IdentityStore).hasPortlet('pages')).toBe(false);
        });

        it('layoutIdForPortlet is the full id of the layout that holds the portlet', () => {
            expect(store.layoutIdForPortlet('workflow')).toBe(CONTENT);
            expect(store.layoutIdForPortlet('pages')).toBe(SITE);
        });

        it('layoutIdForPortlet is null for a portlet that is not in the menu', () => {
            expect(store.layoutIdForPortlet('ghost')).toBeNull();
        });

        it('both answers follow a later reload', () => {
            expect(store.hasPortlet('locales')).toBe(true);

            seed([group(SITE, ['pages'])]);

            expect(store.hasPortlet('locales')).toBe(false);
            expect(store.layoutIdForPortlet('workflow')).toBeNull();
        });
    });

    describe('page title', () => {
        beforeEach(() =>
            seed([group(SITE, ['pages', 'templates']), group(CONTENT, ['content-types'])])
        );

        it('is the label of the item the url resolves to', () => {
            expect(store.getPageTitleByUrl('/templates?mId=aaaa')).toBe('templates');
        });

        it('is empty when the url resolves to nothing', () => {
            expect(store.getPageTitleByUrl('/nothing')).toBe('');
        });

        it('does not let a shorter menu link claim a longer path', () => {
            seed([
                {
                    ...group(SITE, ['content']),
                    menuItems: [
                        {
                            id: 'content',
                            label: 'Content Search',
                            url: '/c/content',
                            angular: false,
                            ajax: false
                        }
                    ]
                } as unknown as DotMenu
            ]);

            expect(store.getPageTitleByUrl('/c/content-types')).toBe('');
        });
    });
});
