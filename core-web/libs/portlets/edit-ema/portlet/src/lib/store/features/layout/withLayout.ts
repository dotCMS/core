import { patchState, signalStoreFeature, type, withComputed, withMethods } from '@ngrx/signals';

import { computed } from '@angular/core';

import { DotCMSLayout } from '@dotcms/types';

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
        withComputed(({ page, containers, layout, template }) => ({
            $layoutProps: computed<LayoutProps>(() => {
                const pageData = page();
                const containersData = containers();
                const layoutData = layout();
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
            // $canEditLayout moved to withPageContext (Phase 4.2 - shared permission)
        })),
        withMethods((store) => {
            return {
                updateLayout: (layout: DotCMSLayout) => {
                    patchState(store, {
                        layout
                    });
                }
            };
        })
    );
}
