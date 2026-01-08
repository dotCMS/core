import { signalStore, withState } from '@ngrx/signals';

import { TestBed } from '@angular/core/testing';

import { DotMenu } from '@dotcms/dotcms-models';

import { withMenu } from './with-menu.feature';

describe('withMenu Feature', () => {
    // Create a test store that uses the withMenu feature
    const TestStore = signalStore(withState({}), withMenu());

    let store: InstanceType<typeof TestStore>;

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
            providers: [TestStore]
        });

        store = TestBed.inject(TestStore);
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
            store.loadMenu(mockMenuItems);
            const items = store.menuItemsEntities();
            expect(items.length).toBe(4); // 1 + 1 + 2 menu items
        });

        it('should add parentMenuId, parentMenuLabel, and parentMenuIcon to entities', () => {
            store.loadMenu(mockMenuItems);
            const items = store.menuItemsEntities();
            const firstItem = items[0];
            expect(firstItem.parentMenuId).toBe('1');
            expect(firstItem.parentMenuLabel).toBe('Home');
            expect(firstItem.parentMenuIcon).toBe('pi pi-home');
        });

        it('should replace existing menu items when loading new menu', () => {
            store.loadMenu(mockMenuItems);
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
            store.loadMenu(newItems);
            const items = store.menuItemsEntities();
            expect(items.length).toBe(1);
            expect(items[0].id).toBe('100-1');
        });

        it('should generate menuLink for angular items', () => {
            store.loadMenu(mockMenuItems);
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
            store.loadMenu(legacyMenu);
            const items = store.menuItemsEntities();
            expect(items[0].menuLink).toBe('/c/legacy-portlet');
        });
    });

    describe('Menu Group', () => {
        beforeEach(() => {
            store.loadMenu(mockMenuItems);
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
            store.loadMenu(mockMenuItems);
        });

        it('should activate a menu item by composite key', () => {
            store.activateMenuItem('1-1__1');
            const activeItem = store.activeMenuItem();
            expect(activeItem).toBeDefined();
            expect(activeItem?.id).toBe('1-1');
            expect(activeItem?.active).toBe(true);
        });

        it('should deactivate previous item when activating new one', () => {
            store.activateMenuItem('1-1__1');
            store.activateMenuItem('2-1__2');
            const activeItem = store.activeMenuItem();
            expect(activeItem?.id).toBe('2-1');
            const allItems = store.menuItemsEntities();
            const firstItem = allItems.find((item) => item.id === '1-1');
            expect(firstItem?.active).toBe(false);
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
            store.loadMenu(mockMenuItems);
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
            store.loadMenu(mockMenuItems);
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
            store.loadMenu(mockMenuItems);
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
            store.loadMenu(mockMenuItems);
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
            store.loadMenu(mockMenuItems);
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

    describe('Set Active Menu', () => {
        beforeEach(() => {
            store.loadMenu(mockMenuItems);
        });

        it('should set active menu item by portletId and parentMenuId', () => {
            // Expand navigation first so parent menu can be opened
            store.expandNavigation();
            store.setActiveMenu({ portletId: '1-1', shortParentMenuId: '1' });
            const activeItem = store.activeMenuItem();
            expect(activeItem?.id).toBe('1-1');
            expect(store.openParentMenuId()).toBe('1');
        });

        it('should activate parent menu when navigation is collapsed', () => {
            store.collapseNavigation();
            store.setActiveMenu({ portletId: '1-1', shortParentMenuId: '1' });
            const activeItem = store.activeMenuItem();
            expect(activeItem?.id).toBe('1-1');
        });

        it('should not activate if portletId is empty', () => {
            const initialActive = store.activeMenuItem();
            store.setActiveMenu({ portletId: '', shortParentMenuId: '1' });
            const finalActive = store.activeMenuItem();
            expect(finalActive).toEqual(initialActive);
        });

        it('should resolve legacy section IDs using REPLACE_SECTIONS_MAP', () => {
            // Add items that match the mapped sections
            const menuWithMappedSections: DotMenu[] = [
                ...mockMenuItems,
                {
                    active: false,
                    id: 'CONTENT',
                    label: 'Content',
                    isOpen: false,
                    menuItems: [
                        {
                            active: false,
                            ajax: true,
                            angular: true,
                            id: 'site-browser',
                            label: 'Site Browser',
                            url: '/c/site-browser',
                            menuLink: '/c/site-browser',
                            parentMenuId: 'CONTENT'
                        }
                    ],
                    name: 'Content',
                    tabDescription: 'Content',
                    tabIcon: 'pi pi-folder',
                    tabName: 'Content',
                    url: '/content'
                },
                {
                    active: false,
                    id: 'MARKETING',
                    label: 'Marketing',
                    isOpen: false,
                    menuItems: [
                        {
                            active: false,
                            ajax: true,
                            angular: true,
                            id: 'analytics-dashboard',
                            label: 'Analytics Dashboard',
                            url: '/c/analytics-dashboard',
                            menuLink: '/c/analytics-dashboard',
                            parentMenuId: 'MARKETING'
                        },
                        {
                            active: false,
                            ajax: true,
                            angular: true,
                            id: 'analytics-search',
                            label: 'Analytics Search',
                            url: '/c/analytics-search',
                            menuLink: '/c/analytics-search',
                            parentMenuId: 'MARKETING'
                        }
                    ],
                    name: 'Marketing',
                    tabDescription: 'Marketing',
                    tabIcon: 'pi pi-chart-bar',
                    tabName: 'Marketing',
                    url: '/marketing'
                }
            ];

            store.loadMenu(menuWithMappedSections);

            // Test legacy ID 'edit-page' maps to 'site-browser'
            store.setActiveMenu({ portletId: 'edit-page', shortParentMenuId: 'CONT' });
            expect(store.activeMenuItem()?.id).toBe('site-browser');

            // Test current ID still works
            store.setActiveMenu({ portletId: 'site-browser', shortParentMenuId: 'CONT' });
            expect(store.activeMenuItem()?.id).toBe('site-browser');

            // Test analytics-dashboard ID works directly
            // getPortletId() now resolves /analytics/dashboard → analytics-dashboard directly
            store.setActiveMenu({ portletId: 'analytics-dashboard', shortParentMenuId: 'MARK' });
            expect(store.activeMenuItem()?.id).toBe('analytics-dashboard');

            // Test analytics-search ID works directly
            store.setActiveMenu({ portletId: 'analytics-search', shortParentMenuId: 'MARK' });
            expect(store.activeMenuItem()?.id).toBe('analytics-search');
        });

        it('should activate menu item using breadcrumbs when bookmark is true', () => {
            const breadcrumbs = [
                { label: 'Home', url: '/' },
                { label: 'About', url: '/about' }
            ];

            store.setActiveMenu({
                portletId: 'non-existent-id',
                shortParentMenuId: '',
                bookmark: true,
                breadcrumbs
            });

            const activeItem = store.activeMenuItem();
            expect(activeItem?.id).toBe('1-1');
            expect(activeItem?.label).toBe('Home');
            expect(store.openParentMenuId()).toBe('1');
        });

        it('should use first breadcrumb with URL when matching menu items', () => {
            const breadcrumbs = [
                { label: 'No URL Item' }, // No URL
                { label: 'About', url: '/about' }, // Should match this
                { label: 'Home', url: '/' }
            ];

            store.setActiveMenu({
                portletId: 'non-existent-id',
                shortParentMenuId: '',
                bookmark: true,
                breadcrumbs
            });

            const activeItem = store.activeMenuItem();
            expect(activeItem?.id).toBe('2-1');
            expect(activeItem?.label).toBe('About');
        });

        it('should fallback to ID matching when breadcrumbs do not match', () => {
            const breadcrumbs = [{ label: 'Non-existent Label', url: '/non-existent' }];

            store.setActiveMenu({
                portletId: '2-1',
                shortParentMenuId: '',
                bookmark: true,
                breadcrumbs
            });

            const activeItem = store.activeMenuItem();
            expect(activeItem?.id).toBe('2-1');
            expect(store.openParentMenuId()).toBe('2');
        });

        it('should not activate if bookmark is false and shortParentMenuId is missing', () => {
            const initialActive = store.activeMenuItem();

            store.setActiveMenu({
                portletId: 'some-id',
                shortParentMenuId: '',
                bookmark: false
            });

            const finalActive = store.activeMenuItem();
            expect(finalActive).toEqual(initialActive);
        });

        it('should handle empty breadcrumbs array when bookmark is true', () => {
            store.setActiveMenu({
                portletId: '1-1',
                shortParentMenuId: '',
                bookmark: true,
                breadcrumbs: []
            });

            const activeItem = store.activeMenuItem();
            expect(activeItem?.id).toBe('1-1');
        });

        it('should handle breadcrumbs with no URLs when bookmark is true', () => {
            const breadcrumbs = [
                { label: 'Home' }, // No URL
                { label: 'About' } // No URL
            ];

            store.setActiveMenu({
                portletId: '2-1',
                shortParentMenuId: '',
                bookmark: true,
                breadcrumbs
            });

            const activeItem = store.activeMenuItem();
            expect(activeItem?.id).toBe('2-1');
        });

        it('should not activate non-existent menu item even with breadcrumbs', () => {
            const breadcrumbs = [{ label: 'Non-existent', url: '/non-existent' }];

            const initialActive = store.activeMenuItem();

            store.setActiveMenu({
                portletId: 'non-existent-portlet-id',
                shortParentMenuId: '',
                bookmark: true,
                breadcrumbs
            });

            const finalActive = store.activeMenuItem();
            expect(finalActive).toEqual(initialActive);
        });

        it('should activate menu item using breadcrumbs when bookmark is true', () => {
            const breadcrumbs = [
                { label: 'Home', url: '/' },
                { label: 'About', url: '/about' }
            ];

            store.setActiveMenu({
                portletId: 'non-existent-id',
                shortParentMenuId: '',
                bookmark: true,
                breadcrumbs
            });

            const activeItem = store.activeMenuItem();
            expect(activeItem?.id).toBe('1-1');
            expect(activeItem?.label).toBe('Home');
            expect(store.openParentMenuId()).toBe('1');
        });

        it('should use first breadcrumb with URL when matching menu items', () => {
            const breadcrumbs = [
                { label: 'No URL Item' }, // No URL
                { label: 'About', url: '/about' }, // Should match this
                { label: 'Home', url: '/' }
            ];

            store.setActiveMenu({
                portletId: 'non-existent-id',
                shortParentMenuId: '',
                bookmark: true,
                breadcrumbs
            });

            const activeItem = store.activeMenuItem();
            expect(activeItem?.id).toBe('2-1');
            expect(activeItem?.label).toBe('About');
        });

        it('should fallback to ID matching when breadcrumbs do not match', () => {
            const breadcrumbs = [{ label: 'Non-existent Label', url: '/non-existent' }];

            store.setActiveMenu({
                portletId: '2-1',
                shortParentMenuId: '',
                bookmark: true,
                breadcrumbs
            });

            const activeItem = store.activeMenuItem();
            expect(activeItem?.id).toBe('2-1');
            expect(store.openParentMenuId()).toBe('2');
        });

        it('should not activate if bookmark is false and shortParentMenuId is missing', () => {
            const initialActive = store.activeMenuItem();

            store.setActiveMenu({
                portletId: 'some-id',
                shortParentMenuId: '',
                bookmark: false
            });

            const finalActive = store.activeMenuItem();
            expect(finalActive).toEqual(initialActive);
        });

        it('should handle empty breadcrumbs array when bookmark is true', () => {
            store.setActiveMenu({
                portletId: '1-1',
                shortParentMenuId: '',
                bookmark: true,
                breadcrumbs: []
            });

            const activeItem = store.activeMenuItem();
            expect(activeItem?.id).toBe('1-1');
        });

        it('should handle breadcrumbs with no URLs when bookmark is true', () => {
            const breadcrumbs = [
                { label: 'Home' }, // No URL
                { label: 'About' } // No URL
            ];

            store.setActiveMenu({
                portletId: '2-1',
                shortParentMenuId: '',
                bookmark: true,
                breadcrumbs
            });

            const activeItem = store.activeMenuItem();
            expect(activeItem?.id).toBe('2-1');
        });

        it('should not activate non-existent menu item even with breadcrumbs', () => {
            const breadcrumbs = [{ label: 'Non-existent', url: '/non-existent' }];

            const initialActive = store.activeMenuItem();

            store.setActiveMenu({
                portletId: 'non-existent-portlet-id',
                shortParentMenuId: '',
                bookmark: true,
                breadcrumbs
            });

            const finalActive = store.activeMenuItem();
            expect(finalActive).toEqual(initialActive);
        });
    });

    describe('Entity Map', () => {
        beforeEach(() => {
            store.loadMenu(mockMenuItems);
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
            store.loadMenu(mockMenuItems);
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
            store.loadMenu(mockMenuItems);
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

            const activeItems = store.menuItemsEntities().filter((item) => item.active);
            expect(activeItems.length).toBe(1);
            expect(activeItems[0].id).toBe('1-1');
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
