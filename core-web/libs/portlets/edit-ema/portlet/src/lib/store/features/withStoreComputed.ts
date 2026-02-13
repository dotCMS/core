import { signalStoreFeature, type, withComputed } from '@ngrx/signals';

import { computed, untracked } from '@angular/core';

import { normalizeQueryParams } from '../../utils';
import { TranslateProps, UVEState } from '../models';
import { ClientConfigState } from './client/withClient';
import { PageAssetComputed } from './withPageAsset';

/**
 * Store-level computed properties that depend on multiple features
 *
 * This feature provides computed signals that combine data from different parts of the store
 */
export function withStoreComputed() {
    return signalStoreFeature(
        {
            state: type<UVEState & ClientConfigState>(),
            props: type<PageAssetComputed>()
        },
        withComputed((store) => {
            return {
                $translateProps: computed<TranslateProps>(() => {
                    const pageData = store.pageData();
                    const viewAsData = store.pageViewAs();
                    const languageId = viewAsData?.language?.id;
                    const translatedLanguages = untracked(() => store.languages());
                    const currentLanguage = translatedLanguages.find(
                        (lang) => lang.id === languageId
                    );

                    return {
                        page: pageData,
                        currentLanguage
                    };
                }),
                $friendlyParams: computed(() => {
                    const params = {
                        ...(store.pageParams() ?? {}),
                        ...(store.view().viewParams ?? {})
                    };

                    return normalizeQueryParams(params);
                })
            };
        })
    );
}
