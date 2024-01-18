import { ComponentStore, tapResponse } from '@ngrx/component-store';
import { EMPTY, Observable, forkJoin } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';
import { Injectable } from '@angular/core';

import { MessageService } from 'primeng/api';

import { catchError, map, shareReplay, switchMap, take, tap } from 'rxjs/operators';

import { DotLicenseService, DotMessageService } from '@dotcms/data-access';
import { DotContainerMap, DotLayout, DotPageContainerStructure } from '@dotcms/dotcms-models';

import { DotActionUrlService } from '../../services/dot-action-url/dot-action-url.service';
import {
    DotPageApiParams,
    DotPageApiResponse,
    DotPageApiService
} from '../../services/dot-page-api.service';
import { DEFAULT_PERSONA, EDIT_CONTENTLET_URL, ADD_CONTENTLET_URL } from '../../shared/consts';
import { EDITOR_STATE } from '../../shared/enums';
import { ActionPayload, SavePagePayload } from '../../shared/models';
import { insertContentletInContainer, sanitizeURL } from '../../utils';

type DialogType = 'content' | 'form' | 'widget' | 'shell' | null;

export interface EditEmaState {
    clientHost: string;
    dialogHeader: string;
    dialogIframeLoading: boolean;
    dialogIframeURL: string;
    dialogType: DialogType;
    error?: number;
    editor: DotPageApiResponse;
    isEnterpriseLicense: boolean;
    editorState: EDITOR_STATE;
}

function getFormId(dotPageApiService: DotPageApiService) {
    return (source: Observable<unknown>) =>
        source.pipe(
            switchMap(({ payload, formId, whenSaved }) => {
                return dotPageApiService
                    .getFormIndetifier(payload.container.identifier, formId)
                    .pipe(
                        map((newFormId: string) => {
                            return {
                                payload: {
                                    ...payload,
                                    newContentletId: newFormId
                                },
                                whenSaved
                            };
                        })
                    );
            })
        );
}

@Injectable()
export class EditEmaStore extends ComponentStore<EditEmaState> {
    constructor(
        private dotPageApiService: DotPageApiService,
        private dotActionUrl: DotActionUrlService,
        private dotLicenseService: DotLicenseService,
        private messageService: MessageService,
        private dotMessageService: DotMessageService
    ) {
        super();
    }

    /*******************
     * Selectors
     *******************/

    readonly editorState$ = this.select((state) => {
        const pageURL = this.createPageURL({
            url: state.editor.page.url,
            language_id: state.editor.viewAs.language.id.toString(),
            'com.dotmarketing.persona.id':
                state.editor.viewAs.persona?.identifier ?? DEFAULT_PERSONA.identifier
        });

        const favoritePageURL = this.createFavoritePagesURL({
            languageId: state.editor.viewAs.language.id,
            pageURI: state.editor.page.url,
            siteId: state.editor.site.identifier
        });

        return {
            clientHost: state.clientHost,
            favoritePageURL,
            apiURL: `${window.location.origin}/api/v1/page/json/${pageURL}`,
            iframeURL: `${state.clientHost}/${pageURL}`,
            editor: {
                ...state.editor,
                viewAs: {
                    ...state.editor.viewAs,
                    persona: state.editor.viewAs.persona ?? DEFAULT_PERSONA
                }
            },
            isEnterpriseLicense: state.isEnterpriseLicense,
            state: state.editorState ?? EDITOR_STATE.LOADING
        };
    });

    readonly dialogState$ = this.select((state) => ({
        iframeURL: state.dialogIframeURL,
        header: state.dialogHeader,
        iframeLoading: state.dialogIframeLoading,
        type: state.dialogType
    }));

    readonly layoutProperties$ = this.select((state) => ({
        layout: state.editor.layout,
        themeId: state.editor.template.theme,
        pageId: state.editor.page.identifier,
        containersMap: this.mapContainers(state.editor.containers)
    }));

    readonly shellProperties$ = this.select((state) => ({
        page: state.editor.page,
        siteId: state.editor.site.identifier,
        languageId: state.editor.viewAs.language.id,
        currentUrl: '/' + state.editor.page,
        host: state.clientHost,
        error: state.error
    }));

    /**
     * Concurrently loads page and license data to updat the state.
     *
     * @param {Observable<DotPageApiParams & { clientHost: string }>} params$ - Parameters for HTTP requests.
     * @returns {Observable<any>} Response of the HTTP requests.
     */
    readonly load = this.effect(
        (params$: Observable<DotPageApiParams & { clientHost: string }>) => {
            return params$.pipe(
                switchMap((params) => {
                    return forkJoin({
                        pageData: this.dotPageApiService.get(params),
                        licenseData: this.dotLicenseService
                            .isEnterprise()
                            .pipe(take(1), shareReplay())
                    }).pipe(
                        tap({
                            next: ({ pageData, licenseData }) => {
                                this.setState({
                                    clientHost: params.clientHost,
                                    editor: pageData,
                                    dialogIframeURL: '',
                                    dialogHeader: '',
                                    dialogIframeLoading: false,
                                    isEnterpriseLicense: licenseData,
                                    dialogType: null,
                                    editorState: EDITOR_STATE.LOADING
                                });
                            },
                            error: ({ status }: HttpErrorResponse) => {
                                // eslint-disable-next-line no-console
                                this.createEmptyState({ canEdit: false, canRead: false }, status);
                            }
                        }),
                        catchError(() => EMPTY)
                    );
                })
            );
        }
    );

    /**
     * Saves data to a page.
     * Calls `whenSaved` callback on success or error.
     *
     * @param {Observable<SavePagePayload>} payload$ - Page data to save.
     */
    readonly savePage = this.effect((payload$: Observable<SavePagePayload>) => {
        return payload$.pipe(
            tap(() => {
                this.updateEditorState(EDITOR_STATE.LOADING);
            }),
            switchMap((payload) => {
                return this.dotPageApiService.save(payload).pipe(
                    tapResponse(
                        () => {
                            payload.whenSaved?.();
                        },
                        (e) => {
                            console.error(e);
                            payload.whenSaved?.();
                            this.updateEditorState(EDITOR_STATE.ERROR);
                        }
                    )
                );
            })
        );
    });

    /**
     * Saves data to a page but gets the new form identifier first.
     * Calls `whenSaved` callback on success or error.
     *
     * @param {Observable<SavePagePayload>} payload$ - Page data to save.
     */
    readonly saveFormToPage = this.effect(
        (
            payload$: Observable<{
                payload: ActionPayload;
                formId: string;
                whenSaved?: () => void;
            }>
        ) => {
            return payload$.pipe(
                tap(() => {
                    this.updateEditorState(EDITOR_STATE.LOADING);
                }),
                getFormId(this.dotPageApiService),
                switchMap(({ whenSaved, payload }) => {
                    const { pageContainers, didInsert } = insertContentletInContainer(payload);

                    // This should not be called here but since here is where we get the form contentlet
                    // we need to do it here, we need to refactor editor and will fix there.
                    if (!didInsert) {
                        this.messageService.add({
                            severity: 'info',
                            summary: this.dotMessageService.get(
                                'editpage.content.add.already.title'
                            ),
                            detail: this.dotMessageService.get(
                                'editpage.content.add.already.message'
                            ),
                            life: 2000
                        });

                        this.updateEditorState(EDITOR_STATE.LOADED);

                        return EMPTY;
                    }

                    return this.dotPageApiService
                        .save({
                            pageContainers,
                            pageId: payload.pageId
                        })
                        .pipe(
                            tapResponse(
                                () => {
                                    whenSaved?.();
                                    this.updateEditorState(EDITOR_STATE.LOADED);
                                },
                                (e) => {
                                    console.error(e);
                                    whenSaved?.();
                                    this.updateEditorState(EDITOR_STATE.ERROR);
                                }
                            )
                        );
                })
            );
        }
    );

    /**
     * Create a contentlet from the palette
     *
     * @memberof EditEmaStore
     */
    readonly createContentFromPalette = this.effect(
        (contentTypeVariable$: Observable<{ variable: string; name: string }>) => {
            return contentTypeVariable$.pipe(
                switchMap(({ name, variable }) => {
                    return this.dotActionUrl.getCreateContentletUrl(variable).pipe(
                        tapResponse(
                            (url) => {
                                this.setDialogForCreateContent({
                                    url,
                                    name
                                });
                            },
                            (e) => {
                                console.error(e);
                            }
                        )
                    );
                })
            );
        }
    );

    /*******************
     * Updaters
     *******************/
    readonly setDialogForCreateContent = this.updater(
        (state, { url, name }: { url: string; name: string }) => {
            return {
                ...state,
                dialogIframeURL: url,
                dialogHeader: `Create ${name}`,
                dialogIframeLoading: true,
                dialogType: 'content'
            };
        }
    );

    readonly setDialogIframeLoading = this.updater((state, editIframeLoading: boolean) => ({
        ...state,
        dialogIframeLoading: editIframeLoading
    }));

    // This method resets the properties that are being used in for the dialog
    readonly resetDialog = this.updater((state) => {
        return {
            ...state,
            dialogIframeURL: '',
            dialogHeader: '',
            dialogIframeLoading: false,
            dialogType: null
        };
    });

    // This method is called when the user clicks on the edit button
    readonly initActionEdit = this.updater(
        (state, payload: { inode: string; title: string; type: DialogType }) => {
            return {
                ...state,
                dialogHeader: payload.title,
                dialogIframeLoading: true,
                dialogIframeURL: this.createEditContentletUrl(payload.inode),
                dialogType: payload.type
            };
        }
    );

    // This method is called when the user clicks on the [+ add] button
    readonly initActionAdd = this.updater(
        (
            state,
            payload: {
                containerId: string;
                acceptTypes: string;
                language_id: string;
            }
        ) => {
            return {
                ...state,
                dialogHeader: 'Search Content', // Does this need translation?
                dialogIframeLoading: true,
                dialogIframeURL: this.createAddContentletUrl(payload),
                dialogType: 'content'
            };
        }
    );

    readonly initActionAddForm = this.updater((state) => {
        return {
            ...state,
            dialogHeader: 'Search Forms', // Does this need translation?
            dialogIframeLoading: true,
            dialogIframeURL: null,
            dialogType: 'form'
        };
    });

    // This method is called when the user clicks in the + button in the jsp dialog
    readonly initActionCreate = this.updater(
        (state, payload: { contentType: string; url: string }) => {
            return {
                ...state,
                dialogHeader: payload.contentType,
                dialogIframeLoading: true,
                dialogIframeURL: payload.url,
                dialogType: 'content'
            };
        }
    );

    /**
     * Update the page layout
     *
     * @memberof EditEmaStore
     */
    readonly updatePageLayout = this.updater((state, layout: DotLayout) => ({
        ...state,
        editor: {
            ...state.editor,
            layout
        }
    }));

    /**
     * Update the editor state
     *
     * @memberof EditEmaStore
     */
    readonly updateEditorState = this.updater((state, editorState: EDITOR_STATE) => ({
        ...state,
        editorState
    }));

    /**
     * Create the url to edit a contentlet
     *
     * @private
     * @param {string} inode
     * @return {*}
     * @memberof DotEmaComponent
     */
    private createEditContentletUrl(inode: string): string {
        return `${EDIT_CONTENTLET_URL}${inode}`;
    }

    /**
     * Create the url to add a contentlet
     *
     * @private
     * @param {{containerID: string, acceptTypes: string}} {containerID, acceptTypes}
     * @return {*}  {string}
     * @memberof EditEmaStore
     */
    private createAddContentletUrl({
        containerId,
        acceptTypes,
        language_id
    }: {
        containerId: string;
        acceptTypes: string;
        language_id: string;
    }): string {
        return ADD_CONTENTLET_URL.replace('*CONTAINER_ID*', containerId)
            .replace('*BASE_TYPES*', acceptTypes)
            .replace('*LANGUAGE_ID*', language_id);
    }

    private createPageURL(params: DotPageApiParams): string {
        const url = sanitizeURL(params.url);

        return `${url}?language_id=${params.language_id}&com.dotmarketing.persona.id=${params['com.dotmarketing.persona.id']}`;
    }

    /**
     * Create the url to add a page to favorites
     *
     * @private
     * @param {{
     *         languageId: number;
     *         pageURI: string;
     *         deviceInode?: string;
     *         siteId?: string;
     *     }} params
     * @return {*}  {string}
     * @memberof EditEmaStore
     */
    private createFavoritePagesURL(params: {
        languageId: number;
        pageURI: string;
        deviceInode?: string;
        siteId?: string;
    }): string {
        const { languageId, pageURI, siteId } = params;

        return (
            `/${pageURI}?` +
            (siteId ? `host_id=${siteId}` : '') +
            `&language_id=${languageId}`
        ).replace(/\/\//g, '/');
    }

    /**
     * Map the containers to a DotContainerMap
     *
     * @private
     * @param {DotPageContainerStructure} containers
     * @return {*}  {DotContainerMap}
     * @memberof EditEmaStore
     */
    private mapContainers(containers: DotPageContainerStructure): DotContainerMap {
        return Object.keys(containers).reduce((acc, id) => {
            acc[id] = containers[id].container;

            return acc;
        }, {});
    }

    /**
     *
     *
     * @private
     * @param {{ canEdit: boolean; canRead: boolean }} permissions
     * @param {number} [error]
     * @memberof EditEmaStore
     */
    private createEmptyState(permissions: { canEdit: boolean; canRead: boolean }, error?: number) {
        this.setState({
            editor: {
                page: {
                    title: '',
                    identifier: '',
                    inode: '',
                    ...permissions,
                    url: ''
                },
                site: {
                    hostname: '',
                    type: '',
                    identifier: '',
                    archived: false
                },
                viewAs: {
                    language: {
                        id: 0,
                        languageCode: '',
                        countryCode: '',
                        language: '',
                        country: ''
                    },
                    persona: undefined
                },
                layout: null,
                template: undefined,
                containers: undefined
            },
            clientHost: '',
            dialogIframeURL: '',
            dialogHeader: '',
            dialogIframeLoading: false,
            isEnterpriseLicense: false,
            dialogType: null,
            error,
            editorState: EDITOR_STATE.LOADED
        });
    }
}
