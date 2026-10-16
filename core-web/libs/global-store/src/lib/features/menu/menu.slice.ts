import { type } from '@ngrx/signals';
import { entityConfig } from '@ngrx/signals/entities';

import { getMenuItemKey, MenuItemEntity, MenuSlice } from '@dotcms/dotcms-models';

/**
 * Entity configuration for menu items.
 * The id is the portlet id plus the FULL layout id, so the same portlet can appear in several
 * layouts and two layouts that share the first 4 characters of their id never collapse into one
 * item. The 4-character `mId` is kept on the entity as `shortLayoutId` for URL lookups.
 */
export const menuConfig = entityConfig({
    entity: type<MenuItemEntity>(),
    collection: 'menuItems',
    selectId: (item: MenuItemEntity) => getMenuItemKey(item)
});

export const initialMenuSlice: MenuSlice = {
    status: 'idle',
    activeKey: null,
    isNavigationCollapsed: true,
    openParentMenuId: null
};
