import { signalStore, withState } from '@ngrx/signals';

import { TestBed } from '@angular/core/testing';

import { MenuItem } from 'primeng/api';

import { withMenu } from './with-menu.feature';

describe('withMenu Feature (PrimeNG MenuItem)', () => {
    // Create a test store that uses the withMenu feature
    const TestStore = signalStore(withState({}), withMenu());

    let store: InstanceType<typeof TestStore>;

    const mockMenuItems: MenuItem[] = [
        { id: '1', label: 'Home', url: '/', icon: 'pi pi-home' },
        { id: '2', label: 'About', url: '/about', icon: 'pi pi-info-circle' },
        {
            id: '3',
            label: 'Services',
            icon: 'pi pi-briefcase',
            items: [
                { id: '4', label: 'Service 1', url: '/services/1' },
                { id: '5', label: 'Service 2', url: '/services/2' }
            ]
        },
        { id: '6', label: 'Contact', url: '/contact', visible: false }
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
        });

        it('should initialize with no active menu item', () => {
            expect(store.activeMenuItemId()).toBeNull();
        });

        it('should have allMenuItems computed returning menu items', () => {
            const items = store.allMenuItems();
            expect(items).toBeDefined();
            expect(Array.isArray(items)).toBe(true);
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
            const newItems: MenuItem[] = [{ id: '100', label: 'New Item' }];
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
            expect(activeItem?.label).toBe('About');
        });

        it('should return null for activeMenuItem when no item is active', () => {
            expect(store.activeMenuItem()).toBeNull();
        });

        it('should find nested active menu item', () => {
            store.setActiveMenuItemId('4');
            const activeItem = store.activeMenuItem();
            expect(activeItem).toBeDefined();
            expect(activeItem?.id).toBe('4');
            expect(activeItem?.label).toBe('Service 1');
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

    describe('Toggle Menu Item Expanded', () => {
        beforeEach(() => {
            store.setMenuItems(mockMenuItems);
        });

        it('should toggle menu item from collapsed to expanded', () => {
            store.toggleMenuItemExpanded('3');
            const items = store.menuItems();
            const servicesItem = items.find((item) => item.id === '3');
            expect(servicesItem?.expanded).toBe(true);
        });

        it('should toggle menu item from expanded to collapsed', () => {
            store.toggleMenuItemExpanded('3'); // Expand
            store.toggleMenuItemExpanded('3'); // Collapse
            const items = store.menuItems();
            const servicesItem = items.find((item) => item.id === '3');
            expect(servicesItem?.expanded).toBe(false);
        });

        it('should work with nested items', () => {
            const nestedItems: MenuItem[] = [
                {
                    id: '1',
                    label: 'Parent',
                    items: [
                        {
                            id: '2',
                            label: 'Child',
                            items: [{ id: '3', label: 'Grandchild' }]
                        }
                    ]
                }
            ];
            store.setMenuItems(nestedItems);
            store.toggleMenuItemExpanded('2');

            const parent = store.menuItems()[0];
            const child = parent.items?.[0];
            expect(child?.expanded).toBe(true);
        });
    });

    describe('Expand/Collapse Menu Items', () => {
        beforeEach(() => {
            store.setMenuItems(mockMenuItems);
        });

        it('should expand menu item explicitly', () => {
            store.expandMenuItem('3');
            const items = store.menuItems();
            const servicesItem = items.find((item) => item.id === '3');
            expect(servicesItem?.expanded).toBe(true);
        });

        it('should collapse menu item explicitly', () => {
            store.expandMenuItem('3');
            store.collapseMenuItem('3');
            const items = store.menuItems();
            const servicesItem = items.find((item) => item.id === '3');
            expect(servicesItem?.expanded).toBe(false);
        });

        it('should collapse all menu items', () => {
            // Expand multiple items
            store.expandMenuItem('3');

            // Collapse all
            store.collapseAllMenuItems();

            const items = store.menuItems();
            items.forEach((item) => {
                expect(item.expanded).toBeFalsy();
            });
        });

        it('should collapse nested items when collapseAllMenuItems is called', () => {
            const nestedItems: MenuItem[] = [
                {
                    id: '1',
                    label: 'Parent',
                    expanded: true,
                    items: [
                        {
                            id: '2',
                            label: 'Child',
                            expanded: true,
                            items: [{ id: '3', label: 'Grandchild', expanded: true }]
                        }
                    ]
                }
            ];
            store.setMenuItems(nestedItems);
            store.collapseAllMenuItems();

            const items = store.menuItems();
            expect(items[0].expanded).toBe(false);
            expect(items[0].items?.[0].expanded).toBe(false);
            expect(items[0].items?.[0].items?.[0].expanded).toBe(false);
        });
    });

    describe('Update Menu Item', () => {
        beforeEach(() => {
            store.setMenuItems(mockMenuItems);
        });

        it('should update menu item properties', () => {
            store.updateMenuItem('1', { badge: '5', disabled: true });
            const items = store.menuItems();
            const homeItem = items.find((item) => item.id === '1');
            expect(homeItem?.badge).toBe('5');
            expect(homeItem?.disabled).toBe(true);
        });

        it('should update nested menu item', () => {
            store.updateMenuItem('4', { badge: 'New' });
            const items = store.menuItems();
            const servicesItem = items.find((item) => item.id === '3');
            const service1 = servicesItem?.items?.find((item) => item.id === '4');
            expect(service1?.badge).toBe('New');
        });

        it('should preserve other properties when updating', () => {
            store.updateMenuItem('1', { badge: '5' });
            const items = store.menuItems();
            const homeItem = items.find((item) => item.id === '1');
            expect(homeItem?.label).toBe('Home');
            expect(homeItem?.url).toBe('/');
            expect(homeItem?.badge).toBe('5');
        });
    });

    describe('Computed: findMenuItemById', () => {
        beforeEach(() => {
            store.setMenuItems(mockMenuItems);
        });

        it('should find menu item by ID', () => {
            const item = store.findMenuItemById()('2');
            expect(item).toBeDefined();
            expect(item?.label).toBe('About');
        });

        it('should find nested menu item by ID', () => {
            const item = store.findMenuItemById()('4');
            expect(item).toBeDefined();
            expect(item?.label).toBe('Service 1');
        });

        it('should return null for non-existent ID', () => {
            const item = store.findMenuItemById()('999');
            expect(item).toBeNull();
        });
    });

    describe('Computed: isMenuItemExpanded', () => {
        beforeEach(() => {
            store.setMenuItems(mockMenuItems);
        });

        it('should return false for non-expanded item', () => {
            expect(store.isMenuItemExpanded()('3')).toBe(false);
        });

        it('should return true for expanded item', () => {
            store.expandMenuItem('3');
            expect(store.isMenuItemExpanded()('3')).toBe(true);
        });

        it('should return false for non-existent item', () => {
            expect(store.isMenuItemExpanded()('999')).toBe(false);
        });
    });

    describe('Computed: visibleMenuItems', () => {
        beforeEach(() => {
            store.setMenuItems(mockMenuItems);
        });

        it('should filter out items with visible: false', () => {
            const visibleItems = store.visibleMenuItems();
            expect(visibleItems.length).toBe(3); // Should exclude Contact
            expect(visibleItems.find((item) => item.id === '6')).toBeUndefined();
        });

        it('should include items with visible: true or undefined', () => {
            const visibleItems = store.visibleMenuItems();
            expect(visibleItems.find((item) => item.id === '1')).toBeDefined();
            expect(visibleItems.find((item) => item.id === '2')).toBeDefined();
        });
    });

    describe('Computed: expandedMenuItemsCount', () => {
        beforeEach(() => {
            store.setMenuItems(mockMenuItems);
        });

        it('should return 0 when no items are expanded', () => {
            expect(store.expandedMenuItemsCount()).toBe(0);
        });

        it('should count expanded items', () => {
            store.expandMenuItem('3');
            expect(store.expandedMenuItemsCount()).toBe(1);
        });

        it('should count nested expanded items', () => {
            const nestedItems: MenuItem[] = [
                {
                    id: '1',
                    label: 'Parent',
                    expanded: true,
                    items: [{ id: '2', label: 'Child', expanded: true }]
                }
            ];
            store.setMenuItems(nestedItems);
            expect(store.expandedMenuItemsCount()).toBe(2);
        });
    });

    describe('Computed: hasExpandedMenuItems', () => {
        beforeEach(() => {
            store.setMenuItems(mockMenuItems);
        });

        it('should return false when no items are expanded', () => {
            expect(store.hasExpandedMenuItems()).toBe(false);
        });

        it('should return true when at least one item is expanded', () => {
            store.expandMenuItem('3');
            expect(store.hasExpandedMenuItems()).toBe(true);
        });
    });

    describe('Computed: parentMenuItems', () => {
        beforeEach(() => {
            store.setMenuItems(mockMenuItems);
        });

        it('should return only items with children', () => {
            const parents = store.parentMenuItems();
            expect(parents.length).toBe(1);
            expect(parents[0].id).toBe('3');
        });

        it('should handle nested parent items', () => {
            const nestedItems: MenuItem[] = [
                {
                    id: '1',
                    label: 'Parent',
                    items: [
                        {
                            id: '2',
                            label: 'Child',
                            items: [{ id: '3', label: 'Grandchild' }]
                        }
                    ]
                }
            ];
            store.setMenuItems(nestedItems);
            const parents = store.parentMenuItems();
            expect(parents.length).toBe(2); // Both parent and child have items
        });
    });

    describe('Reset Menu State', () => {
        it('should reset all menu state to initial values', () => {
            // Modify state
            store.setMenuItems(mockMenuItems);
            store.setActiveMenuItemId('1');
            store.expandMenuItem('3');

            // Reset state
            store.resetMenuState();

            // Verify state is back to initial
            expect(store.activeMenuItemId()).toBeNull();
            const items = store.menuItems();
            expect(items).toBeDefined();
        });
    });

    describe('Complex Scenarios', () => {
        beforeEach(() => {
            store.setMenuItems(mockMenuItems);
        });

        it('should handle full menu workflow', () => {
            // Set active item
            store.setActiveMenuItemId('1');
            expect(store.activeMenuItem()?.label).toBe('Home');

            // Expand submenu
            store.expandMenuItem('3');
            expect(store.isMenuItemExpanded()('3')).toBe(true);

            // Set nested item as active
            store.setActiveMenuItemId('4');
            expect(store.activeMenuItem()?.label).toBe('Service 1');

            // Update menu item
            store.updateMenuItem('1', { badge: '10' });
            expect(store.findMenuItemById()('1')?.badge).toBe('10');
        });

        it('should handle multiple expansions and state changes', () => {
            store.expandMenuItem('3');
            store.setActiveMenuItemId('4');
            store.updateMenuItem('4', { badge: 'Hot' });

            expect(store.isMenuItemExpanded()('3')).toBe(true);
            expect(store.activeMenuItem()?.id).toBe('4');
            expect(store.findMenuItemById()('4')?.badge).toBe('Hot');

            store.collapseAllMenuItems();
            expect(store.isMenuItemExpanded()('3')).toBe(false);
            // Active item should remain
            expect(store.activeMenuItem()?.id).toBe('4');
        });
    });
});
