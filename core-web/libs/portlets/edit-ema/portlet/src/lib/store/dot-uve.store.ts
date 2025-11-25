import { patchState, signalStore, withComputed, withMethods, withState } from '@ngrx/signals';

import { computed } from '@angular/core';

import { withEditor } from './features/editor/withEditor';
import { withLayout } from './features/layout/withLayout';
import { withLoad } from './features/load/withLoad';
import { ShellProps, TranslateProps, UVEState } from './models';

import { DotPageApiResponse } from '../services/dot-page-api.service';
import { UVE_STATUS } from '../shared/enums';
import { getErrorPayload, getRequestHostName, sanitizeURL } from '../utils';

const initialState: UVEState = {
    isEnterprise: false,
    languages: [],
    pageAPIResponse: null,
    currentUser: null,
    experiment: null,
    errorCode: null,
    params: null,
    status: UVE_STATUS.LOADING,
    isTraditionalPage: true,
    canEditPage: false,
    pageIsLocked: true
};

export const UVEStore = signalStore(
    withState<UVEState>(initialState),
    withComputed(
        ({
            pageAPIResponse,
            isTraditionalPage,
            params,
            languages,
            errorCode: error,
            status,
            isEnterprise
        }) => {
            return {
                $translateProps: computed<TranslateProps>(() => {
                    const response = pageAPIResponse();
                    const languageId = response?.viewAs.language?.id;
                    const translatedLanguages = languages();
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

                    const requestHostName = getRequestHostName(isTraditionalPage(), params());

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
                                isDisabled: !page?.canEdit || !isEnterpriseLicense
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
            updatePageResponse(pageAPIResponse: DotPageApiResponse) {
                patchState(store, {
                    status: UVE_STATUS.LOADED,
                    pageAPIResponse
                });
            }
        };
    }),
    withLoad(),
    withLayout(),
    withEditor()
);
