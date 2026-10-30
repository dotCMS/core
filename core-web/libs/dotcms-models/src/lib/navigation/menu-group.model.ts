import { MenuItemEntity } from './menu-entity.model';

/**
 * Interface for grouped menu items by parent.
 */
export interface MenuGroup {
    id: string;
    label: string;
    icon: string;
    menuItems: MenuItemEntity[];
    isOpen: boolean;
    /** Position of the group in the user's menu (the backend already sorts by `tabOrder`). */
    tabOrder: number;
}
