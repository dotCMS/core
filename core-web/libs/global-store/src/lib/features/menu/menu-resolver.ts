import { MenuItem } from 'primeng/api';

import { getPortletIdFromUrl } from '@dotcms/data-access';
import { MenuItemEntity } from '@dotcms/dotcms-models';

export interface ResolveMenuItemParams {
    /** The route: `/templates?mId=ab12`, `#/c/content`, `/dotAdmin/#/pages`. */
    url: string;
    /** Every item of the menu snapshot. */
    items: MenuItemEntity[];
    /** The group that is open now; it breaks a tie between layouts that share a short id. */
    openParentMenuId?: string | null;
    /** The current breadcrumb trail; only used to resolve old bookmarks that carry no `mId`. */
    breadcrumbs?: MenuItem[];
}

interface ParsedUrl {
    path: string;
    mId: string | null;
    hasQuery: boolean;
}

const parseUrl = (url: string): ParsedUrl => {
    const route = url.replace(/^.*#/, '');
    const separator = route.indexOf('?');
    const path = separator < 0 ? route : route.substring(0, separator);
    const query = separator < 0 ? '' : route.substring(separator + 1);

    return {
        path,
        mId: new URLSearchParams(query).get('mId') || null,
        hasQuery: query.length > 0
    };
};

const byPosition = (a: MenuItemEntity, b: MenuItemEntity): number =>
    a.tabOrder - b.tabOrder || a.itemIndex - b.itemIndex;

/**
 * Picks one item out of the candidates. When several layouts hold the same portlet the one whose
 * group is already open wins, otherwise the first in menu order. No candidate is ever dropped from
 * the menu; this only decides which one a URL means.
 */
const pickOne = (
    candidates: MenuItemEntity[],
    openParentMenuId: string | null
): MenuItemEntity | null => {
    if (candidates.length <= 1) {
        return candidates[0] ?? null;
    }

    return (
        candidates.find((item) => item.parentMenuId === openParentMenuId) ??
        [...candidates].sort(byPosition)[0]
    );
};

/**
 * Decides which menu item a URL belongs to. The highlight, the breadcrumbs and the document title
 * all call this, so they cannot disagree. In order:
 *
 * 1. The portlet id of the URL inside the layout the `mId` names (an `mId` that matches no layout
 *    of that portlet, or that is stale, ends the search with no item).
 * 2. With no query at all (an old bookmark): the label of the first breadcrumb that has a url, then
 *    the portlet id. A query without `mId` is not a bookmark and matches nothing.
 * 3. An exact match of the item's `menuLink` with the path, never a prefix of it.
 *
 * @param params the url, the menu snapshot and the context used to break ties
 * @returns the item the url belongs to, or `null` when it belongs to none
 */
export function resolveMenuItem({
    url,
    items,
    openParentMenuId = null,
    breadcrumbs = []
}: ResolveMenuItemParams): MenuItemEntity | null {
    if (!items.length) {
        return null;
    }

    const { path, mId, hasQuery } = parseUrl(url);
    const portletId = getPortletIdFromUrl(path);

    if (mId) {
        const inLayout = (item: MenuItemEntity) => item.parentMenuId.startsWith(mId);

        return (
            pickOne(
                items.filter((item) => item.id === portletId && inLayout(item)),
                openParentMenuId
            ) ??
            pickOne(
                items.filter((item) => item.menuLink === path && inLayout(item)),
                openParentMenuId
            )
        );
    }

    if (hasQuery) {
        return null;
    }

    const bookmarkLabel = breadcrumbs.find((crumb) => crumb.url)?.label;

    return (
        (bookmarkLabel
            ? pickOne(
                  items.filter((item) => item.label === bookmarkLabel),
                  openParentMenuId
              )
            : null) ??
        pickOne(
            items.filter((item) => item.id === portletId),
            openParentMenuId
        ) ??
        pickOne(
            items.filter((item) => item.menuLink === path),
            openParentMenuId
        )
    );
}
