import { signalStoreFeature, type, withHooks } from '@ngrx/signals';

import { DestroyRef, effect, inject, Signal, untracked } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Event, NavigationEnd, Router } from '@angular/router';

import { MenuItem } from 'primeng/api';

import { filter, map } from 'rxjs/operators';

import { getMenuItemKey, MenuItemEntity } from '@dotcms/dotcms-models';

import { resolveMenuItem } from './menu-resolver';

/**
 * What the selection needs from the menu feature. `withFeature` hands over the whole store, which
 * satisfies this shape.
 */
export interface MenuSelectionSource {
    menuItemsEntities: Signal<MenuItemEntity[]>;
    activeKey: Signal<string | null>;
    openParentMenuId: Signal<string | null>;
    selectMenuItem: (item: MenuItemEntity | null) => void;
}

/**
 * The single place that highlights a menu item in reaction to navigation.
 *
 * It resolves the URL with `resolveMenuItem`, the same rule the breadcrumbs and the document title
 * use, in two situations:
 *
 * - **A route change** (`NavigationEnd`): the resolved item is highlighted and its group opens. A URL
 *   that is not a menu item (for example an edit screen) leaves the highlight where it was.
 * - **The menu was replaced and nothing is highlighted**: after a browser reload the highlight starts
 *   empty and is filled from the current URL once the menu has loaded; after a layout update that
 *   removed or moved the highlighted portlet it is looked up again.
 *
 * It must be composed right after `withBreadcrumbs`, inside the same `withFeature`, whose trail and
 * router it reads.
 */
export function withMenuSelection(menu: MenuSelectionSource) {
    return signalStoreFeature(
        {
            state: type<{ breadcrumbs: MenuItem[] }>(),
            props: type<{ router: Router }>()
        },
        withHooks({
            onInit(store) {
                const destroyRef = inject(DestroyRef);

                const resolve = (url: string): MenuItemEntity | null =>
                    resolveMenuItem({
                        url,
                        items: menu.menuItemsEntities(),
                        openParentMenuId: menu.openParentMenuId(),
                        breadcrumbs: store.breadcrumbs()
                    });

                store.router.events
                    .pipe(
                        filter((event: Event) => event instanceof NavigationEnd),
                        map((event) => (event as NavigationEnd).urlAfterRedirects),
                        takeUntilDestroyed(destroyRef)
                    )
                    .subscribe((url) => menu.selectMenuItem(resolve(url)));

                effect(() => {
                    const items = menu.menuItemsEntities();

                    untracked(() => {
                        if (items.length === 0) {
                            return;
                        }

                        const key = menu.activeKey();
                        if (key !== null && items.some((item) => getMenuItemKey(item) === key)) {
                            return;
                        }

                        const currentUrl = store.router.url;
                        if (currentUrl) {
                            menu.selectMenuItem(resolve(currentUrl));
                        }
                    });
                });
            }
        })
    );
}
