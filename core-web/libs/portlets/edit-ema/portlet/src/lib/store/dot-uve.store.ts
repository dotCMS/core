import { patchState, signalStore, withComputed, withMethods, withState } from '@ngrx/signals';

import { computed, untracked } from '@angular/core';

import { DotCMSPageAsset, UVE_MODE } from '@dotcms/types';

import { withSave } from './features/editor/save/withSave';
import { withEditor } from './features/editor/withEditor';
import { withLock } from './features/editor/withLock';
import { withFlags } from './features/flags/withFlags';
import { withLayout } from './features/layout/withLayout';
import { withTrack } from './features/track/withTrack';
import { DotUveViewParams, ShellProps, TranslateProps, UVEState } from './models';

import { UVE_FEATURE_FLAGS } from '../shared/consts';
import { UVE_STATUS } from '../shared/enums';
import { getErrorPayload, getRequestHostName, normalizeQueryParams, sanitizeURL } from '../utils';

// Some properties can be computed
// Ticket: https://github.com/dotCMS/core/issues/30760
const initialState: UVEState = {
    isEnterprise: false,
    languages: [],
    pageAPIResponse: null,
    currentUser: null,
    experiment: null,
    errorCode: null,
    pageParams: null,
    viewParams: null,
    status: UVE_STATUS.LOADING,
    isTraditionalPage: true,
    canEditPage: false,
    pageIsLocked: true,
    isClientReady: false
};

export const UVEStore = signalStore(
    { protectedState: false }, // TODO: remove when the unit tests are fixed
    withState<UVEState>(initialState),
    withMethods((store) => {
        return {
            setUveStatus(status: UVE_STATUS) {
                patchState(store, {
                    status
                });
            },
            updatePageResponse(pageAPIResponse: DotCMSPageAsset) {
                patchState(store, {
                    status: UVE_STATUS.LOADED,
                    pageAPIResponse
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
    withSave(),
    withLayout(),
    withEditor(),
    withTrack(),
    withFlags(UVE_FEATURE_FLAGS),
    withLock(),
    withComputed(
        ({
            pageAPIResponse,
            pageParams,
            viewParams,
            languages,
            errorCode: error,
            status,
            isEnterprise,
            flags
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

                    const url = sanitizeURL(response?.page.pageURI);
                    const currentUrl = url.startsWith('/') ? url : '/' + url;

                    const requestHostName = getRequestHostName(pageParams());

                    const page = response?.page;
                    const templateDrawed = response?.template.drawed;

                    const isLayoutDisabled = !page?.canEdit || !templateDrawed;
                    const errorCode = error();

                    const errorPayload = getErrorPayload(errorCode);
                    const isLoading = status() === UVE_STATUS.LOADING;
                    const isEnterpriseLicense = isEnterprise();

                    const canSeeRulesExists = page && 'canSeeRules' in page;

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
                                    // Check if the page has the canSeeRules property, GraphQL query does suppport this property
                                    (canSeeRulesExists && !page.canSeeRules) ||
                                    !page?.canEdit ||
                                    !isEnterpriseLicense
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
                }),
                $isLockFeatureEnabled: computed<boolean>(() => {
                    return flags().FEATURE_FLAG_UVE_TOGGLE_LOCK;
                })
            };
        }
    )
);
