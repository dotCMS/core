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

export function withLayout() {
    return signalStoreFeature(
        {
            state: type<UVEState>()
        },
        withComputed((store) => {
            const s = store as typeof store & LayoutStoreDeps;
            return {
                $layoutProps: computed<LayoutProps>(() => {
                    const page = s.pageAsset();
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
        withMethods((store) => {
            const s = store as typeof store & LayoutStoreDeps;
            return {
                updateLayout: (layout: DotCMSLayout) => {
                    const page = s.pageAsset();
                    if (page) {
                        const asset = { ...page } as DotCMSPageAsset & {
                            content?: unknown;
                            requestMetadata?: unknown;
                            clientResponse?: unknown;
                        };
                        delete asset.content;
                        delete asset.requestMetadata;
                        delete asset.clientResponse;
                        s.setPageAsset({
                            ...asset,
                            layout
                        });
                    }
                }
            };
        })
    );
}
