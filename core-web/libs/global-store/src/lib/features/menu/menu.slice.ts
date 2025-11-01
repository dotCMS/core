import { MenuItem } from 'primeng/api';

/**
 * Mock menu items for initial state.
 * TODO: Replace with data from the API
 */
const mockMenuItems: MenuItem[] = [
    { id: '1', label: 'Home', url: '/', icon: 'pi pi-home' },
    { id: '2', label: 'About', url: '/about', icon: 'pi pi-info-circle' },
    { id: '3', label: 'Contact', url: '/contact', icon: 'pi pi-envelope' },
    {
        id: '4',
        label: 'Services',
        url: '/services',
        icon: 'pi pi-briefcase',
        items: [
            { id: '5', label: 'Service 1', url: '/services/service1' },
            { id: '6', label: 'Service 2', url: '/services/service2' },
            { id: '7', label: 'Service 3', url: '/services/service3' }
        ]
    }
];

/**
 * Menu slice state interface.
 * Contains the menu items and the active menu item ID.
 *
 * @property menuItems - Array of MenuItem objects (supports nested structure)
 * @property activeMenuItemId - ID of the currently active menu item, or null if none
 */
export interface MenuSlice {
    /**
     * Array of menu items using PrimeNG's MenuItem interface.
     * Supports nested menu structures through the `items` property.
     */
    menuItems: MenuItem[];

    /**
     * ID of the currently active menu item.
     * Set to `null` when no menu item is active.
     */
    activeMenuItemId: string | null;
}

export const initialMenuSlice: MenuSlice = {
    menuItems: mockMenuItems,
    activeMenuItemId: null
};
