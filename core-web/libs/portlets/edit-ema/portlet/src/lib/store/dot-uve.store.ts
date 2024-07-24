import { patchState, signalStore, withComputed, withMethods, withState } from '@ngrx/signals';

import { computed } from '@angular/core';

import { withEditor } from './features/editor/withEditor';
import { withLayout } from './features/layout/withLayout';
import { withLoad } from './features/load/withLoad';
import { ShellProps, UVEState } from './models';

import { COMMON_ERRORS } from '../shared/consts';
import { UVE_STATUS } from '../shared/enums';
import { sanitizeURL } from '../utils';

const initialState: UVEState = {
    isEnterprise: false,
    languages: [],
    pageAPIResponse: null,
    currentUser: null,
    experiment: null,
    error: null,
    params: null,
    status: UVE_STATUS.LOADING,
    isTraditionalPage: true,
    canEditPage: false,
    pageIsLocked: true
};

export const UVEStore = signalStore(
    withState<UVEState>(initialState),
    withComputed((store) => {
        return {
            $shellProps: computed<ShellProps>(() => {
                const pageAPIResponse = store.pageAPIResponse();

                const currentUrl = '/' + sanitizeURL(pageAPIResponse?.page.pageURI);

                const requestHostName = !store.isTraditionalPage()
                    ? store.params()?.clientHost
                    : window.location.origin;

                const page = pageAPIResponse?.page;
                const templateDrawed = pageAPIResponse?.template.drawed;

                const isLayoutDisabled = !page?.canEdit || !templateDrawed;

                const languageId = pageAPIResponse?.viewAs.language.id;
                const languages = store.languages();
                const errorCode = store.error();

                return {
                    canRead: page?.canRead,
                    error: errorCode
                        ? {
                              code: errorCode,
                              pageInfo: COMMON_ERRORS[errorCode?.toString()] ?? null
                          }
                        : null,
                    translateProps: {
                        page,
                        languageId,
                        languages
                    },
                    seoParams: {
                        siteId: pageAPIResponse?.site.identifier,
                        languageId: pageAPIResponse?.viewAs.language.id,
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
                    status: status
                });
            }
        };
    }),
    withLoad(),
    withLayout(),
    withEditor()
);
