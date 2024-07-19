import { patchState, signalStoreFeature, type, withComputed, withMethods } from '@ngrx/signals';

import { computed } from '@angular/core';

import { DotLayout } from '@dotcms/dotcms-models';

import { mapContainers } from '../../../utils';
import { UVEState } from '../../models';

/**
 * Add computed properties to the store to handle the UVE status
 *
 * @export
 * @return {*}
 */
export function withLayout() {
    return signalStoreFeature(
        {
            state: type<UVEState>()
        },
        withComputed((store) => ({
            layoutProps: computed(() => {
                const pageAPIResponse = store.pageAPIResponse();

                return {
                    containersMap: mapContainers(pageAPIResponse.containers),
                    layout: pageAPIResponse.layout,
                    template: {
                        identifier: pageAPIResponse.template.identifier,
                        // The themeId should be here, in the old store we had a bad reference and we were saving all the templates with themeId undefined
                        themeId: pageAPIResponse.template.theme
                    },
                    pageId: pageAPIResponse.page.identifier
                };
            })
        })),
        withMethods((store) => {
            return {
                updateLayout: (layout: DotLayout) => {
                    patchState(store, {
                        pageAPIResponse: {
                            ...store.pageAPIResponse(),
                            layout
                        }
                    });
                }
            };
        })
    );
}
