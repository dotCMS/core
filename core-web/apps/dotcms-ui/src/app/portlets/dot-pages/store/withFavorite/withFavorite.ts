import {
    patchState,
    signalMethod,
    signalStoreFeature,
    type,
    withComputed,
    withHooks,
    withMethods,
    withState
} from '@ngrx/signals';

import { computed, inject } from '@angular/core';

import { DotCMSContentlet, SiteEntity } from '@dotcms/dotcms-models';
import { GlobalStore } from '@dotcms/store';

import { DotPageListService, ListPagesParams } from '../../services/dot-page-list.service';
import { DotCMSPagesPortletState } from '../store';

export interface FavoriteState {
    favoritePages: DotCMSContentlet[];
    favoriteState: 'loading' | 'loaded' | 'error' | 'idle';
}

const initialState: FavoriteState = {
    favoritePages: [],
    favoriteState: 'loading'
};

export const withFavorites = () => {
    return signalStoreFeature(
        { state: type<DotCMSPagesPortletState>() },
        withState<FavoriteState>(initialState),
        withComputed((store) => {
            return {
                $isFavoritePagesLoading: computed<boolean>(
                    () => store.favoriteState() === 'loading'
                )
            };
        }),
        withMethods((store) => {
            const dotPageListService = inject(DotPageListService);
            const globalStore = inject(GlobalStore);
            const fetchFavoritePages = (params: Partial<ListPagesParams>) => {
                patchState(store, { favoriteState: 'loading' });
                const userId = globalStore.loggedUser()?.userId ?? 'dotcms.org.1';
                dotPageListService
                    .getFavoritePages({ ...store.filters(), ...params }, userId)
                    .subscribe(({ jsonObjectView }) => {
                        patchState(store, {
                            favoritePages: jsonObjectView.contentlets,
                            favoriteState: 'loaded'
                        });
                    });
            };

            return {
                getFavoritePages: (params: Partial<ListPagesParams>) => fetchFavoritePages(params),
                updateFavoritePageNode: (identifier: string) => {
                    dotPageListService.getSinglePage(identifier).subscribe((updatedPage) => {
                        const currentFavoritePages = store.favoritePages();
                        const nextFavoritePages = currentFavoritePages.map((page) =>
                            page?.identifier === identifier ? updatedPage : page
                        );
                        patchState(store, { favoritePages: nextFavoritePages });
                    });
                }
            };
        }),
        withHooks((store) => {
            const globalStore = inject(GlobalStore);
            return {
                onInit: () => {
                    const handleSwitchSite = signalMethod<SiteEntity>((site: SiteEntity) => {
                        const host = site.identifier;
                        store.getFavoritePages({ host });
                    });
                    handleSwitchSite(globalStore.siteDetails);
                }
            };
        })
    );
};
