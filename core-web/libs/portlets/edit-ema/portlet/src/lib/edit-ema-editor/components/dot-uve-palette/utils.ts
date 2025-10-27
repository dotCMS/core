import { DotCMSBaseTypesContentTypes } from '@dotcms/dotcms-models';

import { SortOption } from './components/dot-uve-palette-list/model';

export const BASETYPES_FOR_CONTENT = [
    DotCMSBaseTypesContentTypes.CONTENT,
    DotCMSBaseTypesContentTypes.FILEASSET,
    DotCMSBaseTypesContentTypes.DOTASSET
];

export const BASETYPES_FOR_WIDGET = [DotCMSBaseTypesContentTypes.WIDGET];

/**
 * Determines the CSS class for sort menu items based on current sort state.
 * Returns 'active-menu-item' if the item matches the current sort configuration.
 *
 * @param orderby - Sort field to check
 * @param direction - Sort direction to check
 * @param currentSort - Current sort state
 * @returns CSS class string for the menu item
 */
export function isSortActive(itemSort: SortOption, currentSort: SortOption): string {
    const sameOrderby = currentSort.orderby === itemSort.orderby;
    const sameDirection = currentSort.direction === itemSort.direction;
    const isActive = sameOrderby && sameDirection;

    return isActive ? 'active-menu-item' : '';
}
