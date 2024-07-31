import { patchState, signalStore, withComputed, withMethods, withState } from '@ngrx/signals';

import { computed } from '@angular/core';

import { withEditor } from './features/editor/withEditor';
import { withLayout } from './features/layout/withLayout';
import { withLoad } from './features/load/withLoad';
import { ShellProps, UVEState } from './models';

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
    pageIsLocked: true,
    graphQL: null,
    isClientReady: false
};

export const UVEStore = signalStore(
    withState<UVEState>(initialState),
    withComputed(({ pageAPIResponse, isTraditionalPage, params, languages, errorCode: error }) => {
        return {
            $shellProps: computed<ShellProps>(() => {
                const response = pageAPIResponse();

                const currentUrl = '/' + sanitizeURL(response?.page.pageURI);

                const requestHostName = getRequestHostName(isTraditionalPage(), params());

                const page = response?.page;
                const templateDrawed = response?.template.drawed;

                const isLayoutDisabled = !page?.canEdit || !templateDrawed;

                const languageId = response?.viewAs.language.id;
                const translatedLanguages = languages();
                const errorCode = error();

                const errorPayload = getErrorPayload(errorCode);

                return {
                    canRead: page?.canRead,
                    error: errorPayload,
                    translateProps: {
                        page,
                        languageId,
                        languages: translatedLanguages
                    },
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
                            isDisabled: isLayoutDisabled,
                            tooltip: templateDrawed
                                ? null
                                : 'editema.editor.navbar.layout.tooltip.cannot.edit.advanced.template'
                        },
                        {
                            icon: 'pi-sliders-h',
                            label: 'editema.editor.navbar.rules',
                            id: 'rules',
                            href: `rules/${page?.identifier}`,
                            isDisabled: !page?.canEdit
                        },
                        {
                            iconURL: 'experiments',
                            label: 'editema.editor.navbar.experiments',
                            href: `experiments/${page?.identifier}`,
                            id: 'experiments',
                            isDisabled: !page?.canEdit
                        },
                        {
                            icon: 'pi-th-large',
                            label: 'editema.editor.navbar.page-tools',
                            id: 'page-tools'
                        },
                        {
                            icon: 'pi-ellipsis-v',
                            label: 'editema.editor.navbar.properties',
                            id: 'properties'
                        }
                    ]
                };
            })
        };
    }),
    withMethods((store) => {
        return {
            setUveStatus(status: UVE_STATUS) {
                patchState(store, {
                    status
                });
            }
        };
    }),
    withLoad(),
    withLayout(),
    withEditor()
);
