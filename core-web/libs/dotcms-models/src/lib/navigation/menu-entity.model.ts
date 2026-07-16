import { DotMenuItem } from './menu-item.model';

export interface MenuItemEntity extends DotMenuItem {
    /**
     * ID of the parent menu group this item belongs to.
     */
    parentMenuId: string;

    /**
     * Label of the parent menu group.
     */
    parentMenuLabel: string;

    /**
     * Icon of the parent menu group.
     */
    parentMenuIcon: string;
}
