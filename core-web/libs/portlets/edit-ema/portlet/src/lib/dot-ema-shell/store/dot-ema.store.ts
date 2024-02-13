import { ComponentStore, tapResponse } from '@ngrx/component-store';
import { EMPTY, Observable, forkJoin } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';
import { Injectable } from '@angular/core';

import { MessageService } from 'primeng/api';

import { catchError, map, shareReplay, switchMap, take, tap } from 'rxjs/operators';

import { DotLicenseService, DotMessageService } from '@dotcms/data-access';
import { DotContainerMap, DotLayout, DotPageContainerStructure } from '@dotcms/dotcms-models';

import {
    DotPageApiParams,
    DotPageApiResponse,
    DotPageApiService
} from '../../services/dot-page-api.service';
import { DEFAULT_PERSONA } from '../../shared/consts';
import { EDITOR_STATE } from '../../shared/enums';
import { ActionPayload, SavePagePayload } from '../../shared/models';
import { insertContentletInContainer, sanitizeURL } from '../../utils';

export interface EditEmaState {
    clientHost: string;
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
            url: state.editor.page.pageURI,
            language_id: state.editor.viewAs.language.id.toString(),
            'com.dotmarketing.persona.id':
                state.editor.viewAs.persona?.identifier ?? DEFAULT_PERSONA.identifier
        });

        const favoritePageURL = this.createFavoritePagesURL({
            languageId: state.editor.viewAs.language.id,
            pageURI: state.editor.page.pageURI,
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
                                    isEnterpriseLicense: licenseData,
                                    editorState: EDITOR_STATE.LOADING
                                });
                            },
                            error: ({ status }: HttpErrorResponse) => {
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

    private createPageURL(params: DotPageApiParams): string {
        const url = sanitizeURL(params.url);

        return `${url}?language_id=${params.language_id}&com.dotmarketing.persona.id=${params['com.dotmarketing.persona.id']}`;
    }

    /*******************
     * Updaters
     *******************/
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
                    pageURI: '',
                    ...permissions
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
            isEnterpriseLicense: false,
            error,
            editorState: EDITOR_STATE.LOADED
        });
    }
}
