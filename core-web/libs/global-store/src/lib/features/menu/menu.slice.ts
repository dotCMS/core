import { type } from '@ngrx/signals';
import { entityConfig } from '@ngrx/signals/entities';

import { MenuItemEntity, MenuSlice } from '@dotcms/dotcms-models';

/**
 * Entity configuration for menu items.
 * Uses a composite key of item ID + parent menu ID as the unique identifier.
 * This allows the same menu item to appear in multiple parent menu groups.
 */
export const menuConfig = entityConfig({
    entity: type<MenuItemEntity>(),
    collection: 'menuItems',
    selectId: (item: MenuItemEntity) => `${item.id}__${item.parentMenuId.substring(0, 4)}`
    // Only the first 4 characters of parentMenuId are used to generate the composite key.
    // This is done because parentMenuId can be quite long (UUID format)
});

export const initialMenuSlice: MenuSlice = {
    isNavigationCollapsed: true,
    openParentMenuId: null
};

/**
 * Map for replacing legacy section IDs with current ones.
 * Maintains backward compatibility for bookmarks and old URLs.
 */
export const REPLACE_SECTIONS_MAP: Record<string, string> = {
    'edit-page': 'site-browser',
    analytics: 'analytics-dashboard'
};
