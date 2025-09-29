import {
    patchState,
    signalStore,
    withComputed,
    withHooks,
    withMethods,
    withState
} from '@ngrx/signals';
import { forkJoin, of } from 'rxjs';

import { computed, DestroyRef, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute } from '@angular/router';

import { switchMap } from 'rxjs/operators';

import { DotLanguagesService, DotLicenseService } from '@dotcms/data-access';
import { LoginService } from '@dotcms/dotcms-js';
import { DotLanguage } from '@dotcms/dotcms-models';
import { DotCMSViewAsPersona, UVE_MODE } from '@dotcms/types';

import {
    isLockedByAnotherUser,
    PERSONA_KEY,
    UVE_STATUS,
    UVEConfiguration,
    UVEState
} from './model';

import { DotPageService } from '../service/dot-page.service';
import { DEFAULT_PERSONA, getConfiguration } from '../utils';

const initialConfiguration: UVEConfiguration = {
    url: '',
    device: '',
    publishDate: '',
    language_id: '1',
    mode: UVE_MODE.EDIT,
    [PERSONA_KEY]: ''
};

const initialState: UVEState = {
    pageLanguages: [],
    pageAssetData: null,
    isEnterprise: false,
    editorStatus: UVE_STATUS.LOADING,
    configuration: initialConfiguration,
    currentUser: null
};

/**
 * Store for the UVE state
 */
export const UVEStore = signalStore(
    {
        providedIn: 'root'
    },
    withState<UVEState>(initialState),
    withComputed((store) => {
        return {
            $currentLanguage: computed<DotLanguage | null>(() => {
                const pageAssetData = store.pageAssetData();

                if (!pageAssetData) {
                    return null;
                }

                return pageAssetData.viewAs?.language as DotLanguage;
            }),
            $canEdit: computed<boolean>(() => {
                const hasEditPermission = store.pageAssetData()?.page.canEdit ?? false;
                const isLocked = isLockedByAnotherUser(
                    store.pageAssetData()?.page,
                    store.currentUser()?.userId
                );
                const isBlockedByExperiment = false; // TODO: Add experiment check

                return hasEditPermission && !isLocked && !isBlockedByExperiment;
            }),
            $pageIdentifier: computed<string>(() => store.pageAssetData()?.page.identifier || ''),
            $viewAsPersona: computed<DotCMSViewAsPersona>(
                () => store.pageAssetData()?.viewAs?.persona || DEFAULT_PERSONA
            )
        };
    }),
    withMethods((store) => {
        return {
            setUveStatus(editorStatus: UVE_STATUS) {
                patchState(store, { editorStatus });
            }
        };
    }),
    withHooks((store) => {
        const activatedRoute = inject(ActivatedRoute);
        const destroyRef = inject(DestroyRef);
        const dotLanguagesService = inject(DotLanguagesService);
        const dotPageService = inject(DotPageService);
        const dotLicenseService = inject(DotLicenseService);
        const loginService = inject(LoginService);

        return {
            onInit: () => {
                activatedRoute.queryParams
                    .pipe(takeUntilDestroyed(destroyRef))
                    .subscribe((queryParams) => {
                        const configuration = getConfiguration(queryParams);
                        patchState(store, { configuration });
                    });

                dotPageService
                    .get('/', '1')
                    .pipe(
                        switchMap((pageAssetData) => {
                            return forkJoin({
                                pageAssetData: of(pageAssetData),
                                pageLanguages: dotLanguagesService.getLanguagesUsedPage(
                                    pageAssetData.page.identifier
                                ),
                                isEnterprise: dotLicenseService.isEnterprise(),
                                currentUser: loginService.getCurrentUser()
                            });
                        })
                    )
                    .subscribe(({ pageAssetData, pageLanguages, isEnterprise, currentUser }) => {
                        patchState(store, {
                            pageAssetData,
                            pageLanguages,
                            isEnterprise,
                            currentUser
                        });
                    });
            }
        };
    })
);
