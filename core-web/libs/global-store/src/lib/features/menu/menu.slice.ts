import { entityConfig } from '@ngrx/signals/entities';

import { MenuItemEntity, MenuSlice } from '@dotcms/dotcms-models';

/**
 * Entity configuration for menu items.
 * Uses a composite key of item ID + parent menu ID as the unique identifier.
 * This allows the same menu item to appear in multiple parent menu groups.
 */
export const menuConfig = entityConfig({
    entity: {} as MenuItemEntity,
    collection: 'menuItems',
    selectId: (item: MenuItemEntity) => `${item.id}__${item.parentMenuId}`
});

export const initialMenuSlice: MenuSlice = {
    isNavigationCollapsed: true,
    openParentMenuId: null
};
