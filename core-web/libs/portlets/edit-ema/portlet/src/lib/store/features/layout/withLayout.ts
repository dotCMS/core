import { signalStoreFeature, type, withComputed, withMethods } from '@ngrx/signals';

import { computed } from '@angular/core';

import { DotCMSLayout, DotCMSPage, DotCMSPageAsset, DotCMSPageAssetContainers, DotCMSTemplate } from '@dotcms/types';

import { LayoutProps } from './models';

import { mapContainerStructureToDotContainerMap } from '../../../utils';
import { UVEState } from '../../models';

interface LayoutStoreDeps {
    pageData: () => DotCMSPage | null;
    pageContainers: () => DotCMSPageAssetContainers | null;
    pageTemplate: () => DotCMSTemplate | Pick<DotCMSTemplate, 'drawed' | 'theme' | 'anonymous' | 'identifier'> | null;
    pageLayout: () => DotCMSLayout | null;
    pageAssetResponse: () => { pageAsset: DotCMSPageAsset; content?: Record<string, unknown> } | null;
    setPageAssetResponse: (r: { pageAsset: DotCMSPageAsset; content?: Record<string, unknown> }) => void;
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
                const pageData = s.pageData();
                const containersData = s.pageContainers();
                const layoutData = s.pageLayout();
                const templateData = s.pageTemplate();

                return {
                    containersMap: mapContainerStructureToDotContainerMap(
                        containersData ?? {}
                    ),
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
                    const resp = s.pageAssetResponse();
                    if (resp) {
                        s.setPageAssetResponse({
                            ...resp,
                            pageAsset: { ...resp.pageAsset, layout }
                        });
                    }
                }
            };
        })
    );
}
