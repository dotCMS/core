import { signalStoreFeature, type, withComputed, withMethods } from '@ngrx/signals';

import { computed } from '@angular/core';

import { DotCMSLayout, DotCMSPageAsset } from '@dotcms/types';

import { LayoutProps } from './models';

import { mapContainerStructureToDotContainerMap } from '../../../utils';
import { UVEState } from '../../models';

/** WithLayout requires withClient (provides layout computed, graphqlResponse, setGraphqlResponse). */
interface LayoutStoreDeps {
    layout: () => DotCMSLayout | null;
    graphqlResponse: () => { pageAsset: DotCMSPageAsset; content?: Record<string, unknown> } | null;
    setGraphqlResponse: (r: { pageAsset: DotCMSPageAsset; content?: Record<string, unknown> }) => void;
}

/**
 * Add computed properties to the store to handle the Layout UI
 *
 * @export
 * @return {*}
 */
export function withLayout() {
    return signalStoreFeature(
        {
            state: type<UVEState>()
        },
        withComputed((store) => {
            const s = store as typeof store & LayoutStoreDeps;
            const { page, containers, template } = store;
            return {
            $layoutProps: computed<LayoutProps>(() => {
                const pageData = page();
                const containersData = containers();
                const layoutData = s.layout();
                const templateData = template();

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
                    const resp = s.graphqlResponse();
                    if (resp) {
                        s.setGraphqlResponse({
                            ...resp,
                            pageAsset: { ...resp.pageAsset, layout }
                        });
                    }
                }
            };
        })
    );
}
