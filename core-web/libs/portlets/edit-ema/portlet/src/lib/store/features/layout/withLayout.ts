import { signalStoreFeature, type, withComputed, withMethods } from '@ngrx/signals';

import { computed } from '@angular/core';

import { DotCMSLayout, DotCMSPageAsset } from '@dotcms/types';

import { LayoutProps } from './models';

import { mapContainerStructureToDotContainerMap } from '../../../utils';
import { UVEState } from '../../models';
import { PageSnapshot } from '../page/withPage';

interface LayoutStoreDeps {
    pageAsset: () => PageSnapshot;
    setPageAsset: (pageAsset: DotCMSPageAsset) => void;
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
                $layoutProps: computed<LayoutProps>(() => {
                    const page = store.pageAsset();
                    const pageData = page?.page;
                    const containersData = page?.containers;
                    const layoutData = page?.layout;
                    const templateData = page?.template;

                    return {
                        containersMap: mapContainerStructureToDotContainerMap(containersData ?? {}),
                        layout: layoutData,
                        template: {
                            identifier: templateData?.identifier,
                            // The themeId should be here, in the old store we had a bad reference and we were saving all the templates with themeId undefined
                            themeId: templateData?.theme,
                            anonymous: templateData?.anonymous || false
                        },
                        pageId: pageData?.identifier
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
                            ...asset,
                            layout
                        });
                    }
                }
            };
        })
    );
}
