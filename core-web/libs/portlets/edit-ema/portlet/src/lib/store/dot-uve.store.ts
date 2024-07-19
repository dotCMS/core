import { signalStore, withComputed, withState } from '@ngrx/signals';

import { computed } from '@angular/core';

import { withEditor } from './features/editor/withEditor';
import { withLoad } from './features/load/withLoad';
import { withUveStatus } from './features/uve-status/withUveStatus';
import { ShellState, UVEState } from './models';

import { UVE_STATUS } from '../shared/enums';
import { sanitizeURL } from '../utils';

const initialState: UVEState = {
    isEnterprise: false,
    languages: [],
    pageAPIResponse: undefined,
    currentUser: undefined,
    experiment: undefined,
    error: undefined,
    params: undefined,
    status: UVE_STATUS.LOADING,
    isLegacyPage: true,
    canEditPage: false
};

export const UVEStore = signalStore(
    withState<UVEState>(initialState),
    withComputed((store) => {
        return {
            pageIsLocked: computed(() => {
                const pageAPIResponse = store.pageAPIResponse();

                return (
                    pageAPIResponse.page.locked &&
                    pageAPIResponse.page.lockedBy !== store.currentUser()?.userId
                );
            }),
            shellState: computed<ShellState>(() => {
                const pageAPIResponse = store.pageAPIResponse();
                const currentUrl = '/' + sanitizeURL(pageAPIResponse.page.pageURI);

                const requestHostName = store.params().clientHost ?? window.location.origin;

                const page = pageAPIResponse.page;
                const templateDrawed = pageAPIResponse.template.drawed;

                const isLayoutDisabled = !page.canEdit || !templateDrawed;

                const languageId = pageAPIResponse.viewAs.language.id;
                const languages = store.languages();

                return {
                    canRead: page.canRead,
                    error: store.error(),
                    translateProps: {
                        page,
                        languageId,
                        languages
                    },
                    seoParams: {
                        siteId: pageAPIResponse.site.identifier,
                        languageId: pageAPIResponse.viewAs.language.id,
                        currentUrl,
                        requestHostName
                    },
                    uvePageInfo: {
                        NOT_FOUND: {
                            icon: 'compass',
                            title: 'editema.infopage.notfound.title',
                            description: 'editema.infopage.notfound.description',
                            buttonPath: '/pages',
                            buttonText: 'editema.infopage.button.gotopages'
                        },
                        ACCESS_DENIED: {
                            icon: 'ban',
                            title: 'editema.infopage.accessdenied.title',
                            description: 'editema.infopage.accessdenied.description',
                            buttonPath: '/pages',
                            buttonText: 'editema.infopage.button.gotopages'
                        }
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
                            href: `rules/${page.identifier}`,
                            isDisabled: !page.canEdit
                        },
                        {
                            iconURL: 'experiments',
                            label: 'editema.editor.navbar.experiments',
                            href: `experiments/${page.identifier}`,
                            id: 'experiments',
                            isDisabled: !page.canEdit
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
    withLoad(),
    withUveStatus(),
    withEditor()
);
