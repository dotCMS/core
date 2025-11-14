import { signalStore, withState } from '@ngrx/signals';

import { TestBed } from '@angular/core/testing';

import { DotMenu } from '@dotcms/dotcms-models';

import { withMenu } from './with-menu.feature';

describe('withMenu Feature (DotMenu)', () => {
    // Create a test store that uses the withMenu feature
    const TestStore = signalStore(withState({}), withMenu());

    let store: InstanceType<typeof TestStore>;

    const mockMenuItems: DotMenu[] = [
        {
            active: false,
            id: '1',
            isOpen: false,
            menuItems: [
                {
                    active: false,
                    ajax: true,
                    angular: true,
                    id: '1-1',
                    label: 'Home',
                    url: '/',
                    menuLink: '/'
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
            menuItems: [
                {
                    active: false,
                    ajax: true,
                    angular: true,
                    id: '2-1',
                    label: 'About',
                    url: '/about',
                    menuLink: '/about'
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
            menuItems: [
                {
                    active: false,
                    ajax: true,
                    angular: true,
                    id: '4',
                    label: 'Service 1',
                    url: '/services/1',
                    menuLink: '/services/1'
                },
                {
                    active: false,
                    ajax: true,
                    angular: true,
                    id: '5',
                    label: 'Service 2',
                    url: '/services/2',
                    menuLink: '/services/2'
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
            providers: [TestStore]
        });

        store = TestBed.inject(TestStore);
    });

    describe('Initial State', () => {
        it('should initialize with menu items from initial slice', () => {
            const items = store.menuItems();
            expect(items).toBeDefined();
            expect(Array.isArray(items)).toBe(true);
            expect(items.length).toBe(0);
        });

        it('should initialize with no active menu item', () => {
            expect(store.activeMenuItemId()).toBeNull();
        });

        it('should initialize with navigation collapsed', () => {
            expect(store.isNavigationCollapsed()).toBe(true);
        });
    });

    describe('Set Menu Items', () => {
        it('should set menu items', () => {
            store.setMenuItems(mockMenuItems);
            const items = store.menuItems();
            expect(items).toEqual(mockMenuItems);
        });

        it('should replace existing menu items', () => {
            store.setMenuItems(mockMenuItems);
            const newItems: DotMenu[] = [
                {
                    active: false,
                    id: '100',
                    isOpen: false,
                    menuItems: [],
                    name: 'New Menu',
                    tabDescription: 'New section',
                    tabIcon: 'pi pi-star',
                    tabName: 'New',
                    url: '/new'
                }
            ];
            store.setMenuItems(newItems);
            expect(store.menuItems()).toEqual(newItems);
        });
    });

    describe('Active Menu Item', () => {
        beforeEach(() => {
            store.setMenuItems(mockMenuItems);
        });

        it('should set active menu item ID', () => {
            store.setActiveMenuItemId('1');
            expect(store.activeMenuItemId()).toBe('1');
        });

        it('should change active menu item ID', () => {
            store.setActiveMenuItemId('1');
            store.setActiveMenuItemId('2');
            expect(store.activeMenuItemId()).toBe('2');
        });

        it('should clear active menu item when set to null', () => {
            store.setActiveMenuItemId('1');
            store.setActiveMenuItemId(null);
            expect(store.activeMenuItemId()).toBeNull();
        });

        it('should return active menu item object', () => {
            store.setActiveMenuItemId('2');
            const activeItem = store.activeMenuItem();
            expect(activeItem).toBeDefined();
            expect(activeItem?.id).toBe('2');
            expect(activeItem?.name).toBe('About Menu');
        });

        it('should return null for activeMenuItem when no item is active', () => {
            expect(store.activeMenuItem()).toBeNull();
        });

        it('should check if a menu item is active using isMenuItemActive', () => {
            store.setActiveMenuItemId('1');
            expect(store.isMenuItemActive()('1')).toBe(true);
            expect(store.isMenuItemActive()('2')).toBe(false);
        });

        it('should return false for isMenuItemActive when no item is active', () => {
            expect(store.isMenuItemActive()('1')).toBe(false);
        });
    });

    describe('Set Menu Open', () => {
        beforeEach(() => {
            store.setMenuItems(mockMenuItems);
        });

        it('should set menu as open by ID', () => {
            store.setMenuOpen('3');
            const items = store.menuItems();
            const servicesMenu = items.find((menu) => menu.id === '3');
            expect(servicesMenu?.isOpen).toBe(true);
        });

        it('should close other menus when opening a new one', () => {
            store.setMenuOpen('1');
            store.setMenuOpen('2');
            const items = store.menuItems();
            expect(items.find((m) => m.id === '1')?.isOpen).toBe(false);
            expect(items.find((m) => m.id === '2')?.isOpen).toBe(true);
        });

        it('should toggle menu closed if already open', () => {
            store.setMenuOpen('3');
            store.setMenuOpen('3');
            const items = store.menuItems();
            const servicesMenu = items.find((menu) => menu.id === '3');
            expect(servicesMenu?.isOpen).toBe(false);
        });
    });

    describe('Close All Menu Sections', () => {
        beforeEach(() => {
            store.setMenuItems(mockMenuItems);
            store.setMenuOpen('1');
            store.setMenuOpen('2');
        });

        it('should close all menu sections', () => {
            store.closeAllMenuSections();
            const items = store.menuItems();
            items.forEach((menu) => {
                expect(menu.isOpen).toBe(false);
            });
        });
    });

    describe('Toggle Navigation', () => {
        beforeEach(() => {
            store.setMenuItems(mockMenuItems);
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

        it('should close all sections when collapsing', () => {
            store.setMenuOpen('1');
            store.toggleNavigation(); // Expand (should open active sections)
            store.toggleNavigation(); // Collapse (should close all)
            const items = store.menuItems();
            items.forEach((menu) => {
                expect(menu.isOpen).toBe(false);
            });
        });
    });

    describe('Collapse Navigation', () => {
        beforeEach(() => {
            store.setMenuItems(mockMenuItems);
            store.toggleNavigation(); // Expand first
        });

        it('should collapse navigation', () => {
            store.collapseNavigation();
            expect(store.isNavigationCollapsed()).toBe(true);
        });

        it('should close all menu sections when collapsing', () => {
            store.setMenuOpen('1');
            store.collapseNavigation();
            const items = store.menuItems();
            items.forEach((menu) => {
                expect(menu.isOpen).toBe(false);
            });
        });
    });

    describe('Expand Navigation', () => {
        beforeEach(() => {
            store.setMenuItems(mockMenuItems);
        });

        it('should expand navigation', () => {
            store.expandNavigation();
            expect(store.isNavigationCollapsed()).toBe(false);
        });

        it('should open active sections when expanding', () => {
            // Set an item as active
            const items = store.menuItems();
            const menuWithActiveItem = {
                ...items[2],
                menuItems: items[2].menuItems.map((item, idx) => ({
                    ...item,
                    active: idx === 0
                }))
            };
            store.setMenuItems([...items.slice(0, 2), menuWithActiveItem]);

            store.expandNavigation();
            const updatedItems = store.menuItems();
            const servicesMenu = updatedItems.find((m) => m.id === '3');
            expect(servicesMenu?.isOpen).toBe(true);
        });
    });

    describe('Computed: findMenuItemById', () => {
        beforeEach(() => {
            store.setMenuItems(mockMenuItems);
        });

        it('should find menu item by ID', () => {
            const item = store.findMenuItemById()('2');
            expect(item).toBeDefined();
            expect(item?.name).toBe('About Menu');
        });

        it('should return null for non-existent ID', () => {
            const item = store.findMenuItemById()('999');
            expect(item).toBeNull();
        });
    });

    describe('Computed: flattenMenuItems', () => {
        beforeEach(() => {
            store.setMenuItems(mockMenuItems);
        });

        it('should flatten menu items with labelParent', () => {
            const flattened = store.flattenMenuItems();
            expect(flattened.length).toBe(4); // 1 + 1 + 2 menu items
            expect(flattened[0].labelParent).toBe('Home');
            expect(flattened[1].labelParent).toBe('About');
            expect(flattened[2].labelParent).toBe('Services');
            expect(flattened[3].labelParent).toBe('Services');
        });

        it('should preserve menu item properties', () => {
            const flattened = store.flattenMenuItems();
            expect(flattened[0].id).toBe('1-1');
            expect(flattened[0].label).toBe('Home');
        });
    });

    describe('Reset Menu State', () => {
        it('should reset all menu state to initial values', () => {
            // Modify state
            store.setMenuItems(mockMenuItems);
            store.setActiveMenuItemId('1');
            store.setMenuOpen('2');
            store.toggleNavigation();

            // Reset state
            store.resetMenuState();

            // Verify state is back to initial
            expect(store.activeMenuItemId()).toBeNull();
            expect(store.isNavigationCollapsed()).toBe(true);
            const items = store.menuItems();
            expect(items.length).toBe(0);
        });
    });

    describe('Complex Scenarios', () => {
        beforeEach(() => {
            store.setMenuItems(mockMenuItems);
        });

        it('should handle full menu workflow', () => {
            // Set active item
            store.setActiveMenuItemId('1');
            expect(store.activeMenuItem()?.name).toBe('Home Menu');

            // Open menu
            store.setMenuOpen('3');
            const items = store.menuItems();
            expect(items.find((m) => m.id === '3')?.isOpen).toBe(true);

            // Change active item
            store.setActiveMenuItemId('2');
            expect(store.activeMenuItem()?.name).toBe('About Menu');
        });

        it('should handle navigation toggle with menu state', () => {
            // Set an active item in a menu
            const items = store.menuItems();
            const menuWithActiveItem = {
                ...items[2],
                menuItems: items[2].menuItems.map((item, idx) => ({
                    ...item,
                    active: idx === 0
                }))
            };
            store.setMenuItems([...items.slice(0, 2), menuWithActiveItem]);

            // Expand navigation (should open active sections)
            store.expandNavigation();
            const updatedItems = store.menuItems();
            expect(updatedItems.find((m) => m.id === '3')?.isOpen).toBe(true);

            // Collapse navigation (should close all)
            store.collapseNavigation();
            const collapsedItems = store.menuItems();
            collapsedItems.forEach((menu) => {
                expect(menu.isOpen).toBe(false);
            });
        });
    });
});
