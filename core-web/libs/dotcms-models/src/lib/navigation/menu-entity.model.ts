import { DotMenuItem } from './menu-item.model';

export interface MenuItemEntity extends DotMenuItem {
    /**
     * ID of the parent menu group this item belongs to: the FULL layout id (a UUID).
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

    /**
     * First 4 characters of {@link parentMenuId}. This is the value the `mId` query param carries,
     * kept next to the full id so URLs can be matched without making it the item's identity.
     */
    shortLayoutId: string;

    /**
     * Position of the item's group in the menu response. The backend sorts the groups by the user's
     * `tabOrder` and does not send that number, so the position is the order.
     */
    tabOrder: number;

    /**
     * Position of the item inside its group, as the backend sent it.
     */
    itemIndex: number;
}

/**
 * The key that identifies a menu item: its portlet id plus the full layout id. The same portlet can
 * appear in several layouts, and two layouts can share the first 4 characters of their id, so
 * neither the portlet id nor the short `mId` alone is unique.
 *
 * @param item the portlet id and the full id of its layout
 * @returns the entity key
 */
export const getMenuItemKey = (item: Pick<MenuItemEntity, 'id' | 'parentMenuId'>): string =>
    `${item.id}__${item.parentMenuId}`;
