import { patchState, signalStoreFeature, type, withComputed, withMethods } from '@ngrx/signals';

import { computed } from '@angular/core';

import { DotLayout } from '@dotcms/dotcms-models';

import { LayoutProps } from './models';

import { mapContainerStructureToDotContainerMap } from '../../../utils';
import { UVEState } from '../../models';

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
        withComputed(({ pageAPIResponse }) => ({
            $layoutProps: computed<LayoutProps>(() => {
                const response = pageAPIResponse();

                return {
                    containersMap: mapContainerStructureToDotContainerMap(
                        response?.containers ?? {}
                    ),
                    layout: response?.layout,
                    template: {
                        identifier: response?.template.identifier,
                        // The themeId should be here, in the old store we had a bad reference and we were saving all the templates with themeId undefined
                        themeId: response?.template.theme
                    },
                    pageId: response?.page.identifier
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
