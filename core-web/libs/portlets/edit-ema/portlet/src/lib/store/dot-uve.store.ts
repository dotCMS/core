import {
    patchState,
    signalStore,
    withComputed,
    withHooks,
    withMethods,
    withState
} from '@ngrx/signals';

import { computed, inject, untracked } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

import { UVE_MODE } from '@dotcms/uve/types';

import { withEditor } from './features/editor/withEditor';
import { withFlags } from './features/flags/withFlags';
import { withLayout } from './features/layout/withLayout';
import { withLoad } from './features/load/withLoad';
import { withTrack } from './features/track/withTrack';
import { DotUveViewParams, ShellProps, TranslateProps, UVEState } from './models';

import { UVE_FEATURE_FLAGS } from '../shared/consts';
import { UVE_STATUS } from '../shared/enums';
import { getErrorPayload, normalizeQueryParams, sanitizeURL } from '../utils';

// Some properties can be computed
// Ticket: https://github.com/dotCMS/core/issues/30760
const initialState: UVEState = {
    languages: [],
    isEnterprise: false,
    pageAPIResponse: null,
    currentUser: null,
    experiment: null,
    errorCode: null,
    pageParams: null,
    viewParams: null,
    status: UVE_STATUS.LOADING,
    isTraditionalPage: true
};

export const UVEStore = signalStore(
    // To Remove this we need to update the @ngrx/signals package to the latest version
    // Testing utils were added: https://ngrx.io/guide/signals/signal-store/testing#unprotected
    { protectedState: false },
    withState<UVEState>(initialState),
    withEditor(),
    withComputed(
        ({
            pageAPIResponse,
            pageParams,
            viewParams,
            languages,
            errorCode: error,
            status,
            isEnterprise,
            host
        }) => {
            return {
                $translateProps: computed<TranslateProps>(() => {
                    const response = pageAPIResponse();
                    const languageId = response?.viewAs.language?.id;
                    const translatedLanguages = untracked(() => languages());
                    const currentLanguage = translatedLanguages.find(
                        (lang) => lang.id === languageId
                    );

                    return {
                        page: response?.page,
                        currentLanguage
                    };
                }),
                $shellProps: computed<ShellProps>(() => {
                    const response = pageAPIResponse();
                    const currentUrl = '/' + sanitizeURL(response?.page.pageURI);
                    const requestHostName = host();

                    const page = response?.page;
                    const templateDrawed = response?.template.drawed;

                    const isLayoutDisabled = !page?.canEdit || !templateDrawed;
                    const errorCode = error();

                    const errorPayload = getErrorPayload(errorCode);
                    const isLoading = status() === UVE_STATUS.LOADING;
                    const isEnterpriseLicense = isEnterprise();

                    return {
                        canRead: page?.canRead,
                        error: errorPayload,
                        seoParams: {
                            siteId: response?.site?.identifier,
                            languageId: response?.viewAs.language.id,
                            currentUrl,
                            requestHostName
                        },
                        items: [
                            {
                                icon: 'pi-file',
                                label: 'editema.editor.navbar.content',
                                href: 'content',
                                id: 'content'
                            },
                            {
                                icon: 'pi-table',
                                label: 'editema.editor.navbar.layout',
                                href: 'layout',
                                id: 'layout',
                                isDisabled: isLayoutDisabled || !isEnterpriseLicense,
                                tooltip: templateDrawed
                                    ? null
                                    : 'editema.editor.navbar.layout.tooltip.cannot.edit.advanced.template'
                            },
                            {
                                icon: 'pi-sliders-h',
                                label: 'editema.editor.navbar.rules',
                                id: 'rules',
                                href: `rules/${page?.identifier}`,
                                isDisabled:
                                    !page?.canSeeRules || !page?.canEdit || !isEnterpriseLicense
                            },
                            {
                                iconURL: 'experiments',
                                label: 'editema.editor.navbar.experiments',
                                href: `experiments/${page?.identifier}`,
                                id: 'experiments',
                                isDisabled: !page?.canEdit || !isEnterpriseLicense
                            },
                            {
                                icon: 'pi-th-large',
                                label: 'editema.editor.navbar.page-tools',
                                id: 'page-tools'
                            },
                            {
                                icon: 'pi-ellipsis-v',
                                label: 'editema.editor.navbar.properties',
                                id: 'properties',
                                isDisabled: isLoading
                            }
                        ]
                    };
                }),
                $languageId: computed<number>(() => {
                    return pageAPIResponse()?.viewAs.language?.id || 1;
                }),
                $isPreviewMode: computed<boolean>(() => {
                    return pageParams()?.mode === UVE_MODE.PREVIEW;
                }),
                $isLiveMode: computed<boolean>(() => {
                    return pageParams()?.mode === UVE_MODE.LIVE;
                }),
                $friendlyParams: computed(() => {
                    const params = {
                        ...(pageParams() ?? {}),
                        ...(viewParams() ?? {})
                    };

                    return normalizeQueryParams(params);
                })
            };
        }
    ),
    withMethods((store) => {
        return {
            setUveStatus(status: UVE_STATUS) {
                patchState(store, {
                    status
                });
            },
            patchViewParams(viewParams: Partial<DotUveViewParams>) {
                patchState(store, {
                    viewParams: {
                        ...store.viewParams(),
                        ...viewParams
                    }
                });
            }
        };
    }),
    withHooks((store) => {
        const activatedRoute = inject(ActivatedRoute);

        return {
            onInit() {
                const { data } = activatedRoute.snapshot;
                const { uveConfig } = data;
                patchState(store, { isTraditionalPage: !uveConfig });
            }
        };
    }),
    withLoad(),
    withLayout(),
    withTrack(),
    withFlags(UVE_FEATURE_FLAGS)
);
