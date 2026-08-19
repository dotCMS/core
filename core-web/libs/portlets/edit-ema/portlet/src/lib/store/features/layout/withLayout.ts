import { signalStoreFeature, type, withComputed, withMethods } from '@ngrx/signals';

import { computed } from '@angular/core';

import { DotCMSLayout, DotCMSPageAsset } from '@dotcms/types';

import { LayoutProps } from './models';

import { mapContainerStructureToDotContainerMap } from '../../../utils';
import { UVEState } from '../../models';
import { PageSnapshot } from '../page/withPage';

interface LayoutStoreDeps {
    pageAsset: () => PageSnapshot;
    setPageAsset: (payload: {
        pageAsset: DotCMSPageAsset;
        content?: Record<string, unknown>;
    }) => void;
}

/** Store type with layout dependencies (pageAsset, setPageAsset); use for type assertion in feature callbacks. */
type StoreWithLayoutDeps<T> = T & LayoutStoreDeps;

export function withLayout() {
    return signalStoreFeature(
        {
            state: type<UVEState>()
        },
        withComputed((uveStore) => {
            const store = uveStore as StoreWithLayoutDeps<typeof uveStore>;
            return {
                /**
                 * `| null` until a page is loaded. `containers`, `layout`, `page` and `template` are
                 * all required on `DotCMSPageAsset`, and `identifier` / `theme` / `anonymous` are
                 * required on the template — so the asset itself is the only thing that can be
                 * absent, and one guard covers every field. `LayoutProps` was declaring four
                 * required values that this could not produce before the first load.
                 */
                $layoutProps: computed<LayoutProps | null>(() => {
                    const page = store.pageAsset();

                    if (!page) {
                        return null;
                    }

                    const templateData = page.template;

                    return {
                        containersMap: mapContainerStructureToDotContainerMap(page.containers),
                        layout: page.layout,
                        template: {
                            identifier: templateData.identifier,
                            // The themeId should be here, in the old store we had a bad reference and we were saving all the templates with themeId undefined
                            themeId: templateData.theme,
                            anonymous: templateData.anonymous || false
                        },
                        pageId: page.page.identifier
                    };
                })
            };
        }),
        withMethods((uveStore) => {
            const store = uveStore as StoreWithLayoutDeps<typeof uveStore>;
            return {
                updateLayout: (layout: DotCMSLayout) => {
                    const page = store.pageAsset();
                    if (page) {
                        const asset = { ...page } as DotCMSPageAsset & {
                            content?: unknown;
                            requestMetadata?: unknown;
                            clientResponse?: unknown;
                        };
                        delete asset.content;
                        delete asset.requestMetadata;
                        delete asset.clientResponse;
                        store.setPageAsset({
                            pageAsset: { ...asset, layout }
                        });
                    }
                }
            };
        })
    );
}
