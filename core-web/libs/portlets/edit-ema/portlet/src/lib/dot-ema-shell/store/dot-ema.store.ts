import { ComponentStore, tapResponse } from '@ngrx/component-store';
import { EMPTY, Observable, forkJoin } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';
import { Injectable } from '@angular/core';

import { MessageService } from 'primeng/api';

import {
    catchError,
    map,
    pairwise,
    shareReplay,
    startWith,
    switchMap,
    take,
    tap,
    filter
} from 'rxjs/operators';

import {
    DotContentletLockerService,
    DotExperimentsService,
    DotLicenseService,
    DotMessageService
} from '@dotcms/data-access';
import { LoginService } from '@dotcms/dotcms-js';
import {
    DotContainerMap,
    DotDevice,
    DotExperimentStatus,
    DotLayout,
    DotPageContainerStructure
} from '@dotcms/dotcms-models';

import {
    Container,
    ContentletArea,
    EmaDragItem
} from '../../edit-ema-editor/components/ema-page-dropzone/types';
import {
    DotPageApiParams,
    DotPageApiResponse,
    DotPageApiService
} from '../../services/dot-page-api.service';
import { DEFAULT_PERSONA } from '../../shared/consts';
import { EDITOR_MODE, EDITOR_STATE } from '../../shared/enums';
import {
    ActionPayload,
    EditEmaState,
    EditorData,
    ReloadPagePayload,
    SaveInlineEditing,
    SavePagePayload
} from '../../shared/models';
import {
    insertContentletInContainer,
    sanitizeURL,
    getPersonalization,
    createPageApiUrlWithQueryParams,
    getIsDefaultVariant
} from '../../utils';

interface GetFormIdPayload extends SavePagePayload {
    payload: ActionPayload;
    formId: string;
}

function getFormId(dotPageApiService: DotPageApiService) {
    return (source: Observable<GetFormIdPayload>) =>
        source.pipe(
            switchMap(({ payload, formId, whenSaved, params }: GetFormIdPayload) => {
                return dotPageApiService
                    .getFormIndetifier(payload.container.identifier, formId)
                    .pipe(
                        map((newFormId: string) => {
                            return {
                                payload: {
                                    ...payload,
                                    newContentletId: newFormId
                                },
                                whenSaved,
                                params
                            };
                        }),
                        catchError(() => EMPTY)
                    );
            })
        );
}

@Injectable()
export class EditEmaStore extends ComponentStore<EditEmaState> {
    constructor(
        private readonly dotPageApiService: DotPageApiService,
        private readonly dotLicenseService: DotLicenseService,
        private readonly messageService: MessageService,
        private readonly dotMessageService: DotMessageService,
        private readonly dotExperimentsService: DotExperimentsService,
        private readonly dotContentletLockerService: DotContentletLockerService,
        private readonly loginService: LoginService
    ) {
        super();
    }

    /*******************
     * Selectors
     *******************/

    readonly clientHost$ = this.select((state) => state.clientHost);
    readonly dragItem$ = this.select((state) => state.dragItem);

    private readonly stateLoad$ = this.select((state) => state.editorState);
    private readonly code$ = this.select((state) => state.editor.page.rendered);
    private readonly pageURL$ = this.select((state) => this.createPageURL(state));
    private readonly favoritePageURL$ = this.select((state) =>
        this.createFavoritePagesURL({
            languageId: state.editor.viewAs.language.id,
            pageURI: state.editor.page.pageURI,
            siteId: state.editor.site.identifier
        })
    );
    private readonly iframeURL$ = this.select(
        this.clientHost$,
        this.pageURL$,
        (clientHost, pageURL) => (clientHost ? `${clientHost}/${pageURL}` : '')
    );

    private readonly previewURL$ = this.select(
        this.clientHost$,
        this.pageURL$,
        (clientHost, pageURL) => (clientHost ? `${clientHost}/${pageURL}` : pageURL)
    );
    private readonly bounds$ = this.select((state) => state.bounds);
    private readonly contentletArea$: Observable<ContentletArea> = this.select(
        (state) => state.contentletArea,
        {
            equal: (prev, curr) => {
                if (!prev) {
                    return false;
                }

                if (prev.x === curr?.x && prev.y === curr?.y) {
                    return true;
                }

                return false;
            }
        }
    );

    private readonly editor$ = this.select((state) => state.editor);
    private readonly isEnterpriseLicense$ = this.select((state) => state.isEnterpriseLicense);
    private readonly currentState$ = this.select(
        (state) => state.editorState ?? EDITOR_STATE.LOADING
    );
    private readonly currentExperiment$ = this.select((state) => state.currentExperiment);
    private readonly templateThemeId$ = this.select((state) => state.editor.template.themeId);
    private readonly templateIdentifier$ = this.select((state) => state.editor.template.identifier);
    private readonly templateDrawed$ = this.select((state) => state.editor.template.drawed);
    private readonly page$ = this.select((state) => state.editor.page);
    private readonly siteId$ = this.select((state) => state.editor.site.identifier);
    private readonly languageId$ = this.select((state) => state.editor.viewAs.language.id);
    private readonly currentUrl$ = this.select(
        (state) => '/' + sanitizeURL(state.editor.page.pageURI)
    );
    private readonly error$ = this.select((state) => state.error);
    /**
     * Before this was layoutProperties, but are separate to "temp" selector.
     * And then is merged with templateIdentifier in layoutProperties$.
     * This is to try avoid extra-calls on the select, and avoid memory leaks
     */
    private readonly layout$ = this.select((state) => state.editor.layout);
    private readonly themeId$ = this.select((state) => state.editor.template.theme);
    private readonly pageId$ = this.select((state) => state.editor.page.identifier);
    private readonly containersMap$ = this.select((state) =>
        this.mapContainers(state.editor.containers)
    );
    private readonly layoutProps$ = this.select(
        this.layout$,
        this.themeId$,
        this.pageId$,
        this.containersMap$,
        (layout, themeId, pageId, containersMap) => ({ layout, themeId, pageId, containersMap })
    );

    private readonly containers$ = this.select((state) =>
        this.getPageContainers(state.editor.containers)
    );
    private readonly personaTag$ = this.select((state) => state.editor.viewAs.persona?.keyTag);
    private readonly personalization$ = this.select((state) =>
        getPersonalization(state.editor.viewAs.persona)
    );
    private readonly shellProps$ = this.select(
        this.page$,
        this.siteId$,
        this.languageId$,
        this.currentUrl$,
        this.clientHost$,
        this.error$,
        (page, siteId, languageId, currentUrl, host, error) => ({
            page,
            siteId,
            languageId,
            currentUrl,
            host,
            error
        })
    );
    readonly editorMode$ = this.select((state) => state.editorData.mode);
    readonly editorData$ = this.select((state) => state.editorData);
    readonly pageRendered$ = this.select((state) => state.editor.page.rendered);

    readonly contentState$ = this.select(
        this.code$,
        this.stateLoad$,
        this.clientHost$,
        (code, state, clientHost) => ({
            state,
            code,
            isVTL: !clientHost
        })
    ).pipe(
        startWith({ state: EDITOR_STATE.LOADING, code: '', isVTL: false }),
        pairwise(),
        filter(([_prev, curr]) => curr?.state === EDITOR_STATE.IDLE),
        map(([prev, curr]) => ({
            changedFromLoading: prev.state === EDITOR_STATE.LOADING,
            isVTL: curr.isVTL,
            code: curr.code,
            state: curr.state
        }))
    );

    readonly vtlIframePage$ = this.select(
        this.pageRendered$,
        this.isEnterpriseLicense$,
        this.editorMode$,
        (rendered, isEnterprise, mode) => ({
            rendered,
            isEnterprise,
            mode
        })
    );
    readonly editorState$ = this.select(
        this.bounds$,
        this.clientHost$,
        this.contentletArea$,
        this.currentExperiment$,
        this.currentState$,
        this.editor$,
        this.editorData$,
        this.favoritePageURL$,
        this.iframeURL$,
        this.isEnterpriseLicense$,
        this.pageURL$,
        this.dragItem$,
        (
            bounds,
            clientHost,
            contentletArea,
            currentExperiment,
            currentState,
            editor,
            editorData,
            favoritePageURL,
            iframeURL,
            isEnterpriseLicense,
            pageURL,
            dragItem
        ) => {
            return {
                apiURL: `${window.location.origin}/api/v1/page/json/${pageURL}`,
                bounds: bounds,
                clientHost: clientHost,
                contentletArea: contentletArea,
                currentExperiment,
                editorData,
                editor: {
                    ...editor,
                    viewAs: {
                        ...editor.viewAs,
                        persona: editor.viewAs.persona ?? DEFAULT_PERSONA
                    }
                },
                favoritePageURL,
                iframeURL,
                isEnterpriseLicense,
                state: currentState,
                dragItem,
                showContentletTools:
                    editorData.canEditVariant &&
                    !!contentletArea &&
                    !editorData.device &&
                    editor.page.canEdit &&
                    (currentState === EDITOR_STATE.IDLE ||
                        currentState === EDITOR_STATE.DRAGGING) &&
                    !editorData.page.isLocked,
                showDropzone:
                    editorData.canEditVariant &&
                    !editorData.device &&
                    (currentState === EDITOR_STATE.DRAGGING ||
                        currentState === EDITOR_STATE.SCROLL_DRAG),
                showPalette:
                    editorData.canEditVariant &&
                    isEnterpriseLicense &&
                    (editorData.mode === EDITOR_MODE.EDIT ||
                        editorData.mode === EDITOR_MODE.EDIT_VARIANT ||
                        editorData.mode === EDITOR_MODE.INLINE_EDITING) &&
                    editor.page.canEdit
            };
        }
    );

    readonly editorToolbarData$ = this.select(
        this.editorState$,
        this.previewURL$,
        (editorState, previewURL) => ({
            ...editorState,
            showWorkflowActions:
                editorState.editorData.mode === EDITOR_MODE.EDIT ||
                editorState.editorData.mode === EDITOR_MODE.INLINE_EDITING,
            showInfoDisplay:
                !editorState.editorData.canEditPage ||
                (editorState.editorData.mode !== EDITOR_MODE.EDIT &&
                    editorState.editorData.mode !== EDITOR_MODE.INLINE_EDITING),
            previewURL
        })
    );

    readonly layoutProperties$ = this.select(
        this.layoutProps$,
        this.templateIdentifier$,
        this.templateThemeId$,
        (props, templateIdentifier, themeId) => ({
            ...props,
            template: { identifier: templateIdentifier, themeId }
        })
    );

    readonly shellProperties$ = this.select(
        this.shellProps$,
        this.templateDrawed$,
        (props, templateDrawed) => ({ ...props, templateDrawed })
    );

    // This data is needed to save the page on CRUD operation
    readonly pageData$ = this.select(
        this.containers$,
        this.pageId$,
        this.languageId$,
        this.personaTag$,
        this.personalization$,
        (containers, id, languageId, personaTag, personalization) => {
            return {
                containers,
                id,
                languageId,
                personaTag,
                personalization
            };
        }
    );

    readonly dragState$ = this.select(this.stateLoad$, this.dragItem$, (editorState, dragItem) => ({
        editorState,
        dragItem
    }));

    readonly isUserDragging$ = this.select((state) => state.editorState).pipe(
        filter((state) => state === EDITOR_STATE.DRAGGING)
    );
    /**
     * Concurrently loads page and license data to updat the state.
     *
     * @param {Observable<DotPageApiParams & { clientHost: string }>} params$ - Parameters for HTTP requests.
     * @returns {Observable<any>} Response of the HTTP requests.
     */
    readonly load = this.effect(
        (params$: Observable<DotPageApiParams & { clientHost?: string }>) => {
            return params$.pipe(
                switchMap((params) => {
                    return forkJoin({
                        pageData: this.dotPageApiService.get(params),
                        licenseData: this.dotLicenseService
                            .isEnterprise()
                            .pipe(take(1), shareReplay()),
                        currentUser: this.loginService.getCurrentUser()
                    }).pipe(
                        tap({
                            error: ({ status }: HttpErrorResponse) => {
                                this.createEmptyState({ canEdit: false, canRead: false }, status);
                            }
                        }),
                        switchMap(({ pageData, licenseData, currentUser }) =>
                            this.dotExperimentsService.getById(params.experimentId ?? '').pipe(
                                tap({
                                    next: (experiment) => {
                                        // Can be blocked by an experiment if there is a running experiment or a scheduled one
                                        const editingBlockedByExperiment = [
                                            DotExperimentStatus.RUNNING,
                                            DotExperimentStatus.SCHEDULED
                                        ].includes(experiment?.status);

                                        const isDefaultVariant = getIsDefaultVariant(
                                            params.variantName
                                        );

                                        // I can edit the variant if the variant is the default one (default can be undefined as well) or if there is no running experiment
                                        const canEditVariant =
                                            isDefaultVariant || !editingBlockedByExperiment;

                                        const isLocked =
                                            pageData.page.locked &&
                                            pageData.page.lockedBy !== currentUser.userId;

                                        const mode = this.getInitialEditorMode({
                                            isDefaultVariant,
                                            canEditVariant,
                                            isLocked
                                        });

                                        return this.setState({
                                            currentExperiment: experiment,
                                            clientHost: params.clientHost,
                                            editor: pageData,
                                            isEnterpriseLicense: licenseData,
                                            editorState: EDITOR_STATE.IDLE,
                                            bounds: [],
                                            contentletArea: null,
                                            editorData: {
                                                mode,
                                                canEditVariant,
                                                canEditPage: pageData.page.canEdit,
                                                variantId: params.variantName,
                                                page: {
                                                    isLocked,
                                                    canLock: pageData.page.canLock,
                                                    lockedByUser: pageData.page.lockedByName
                                                }
                                            }
                                        });
                                    },
                                    error: ({ status }: HttpErrorResponse) => {
                                        this.createEmptyState(
                                            { canEdit: false, canRead: false },
                                            status
                                        );
                                    }
                                })
                            )
                        )
                    );
                })
            );
        }
    );

    readonly reload = this.effect((payload$: Observable<ReloadPagePayload>) => {
        return payload$.pipe(
            tap(() => this.updateEditorState(EDITOR_STATE.LOADING)),
            switchMap(({ params, whenReloaded }) => {
                return this.dotPageApiService.get(params).pipe(
                    tapResponse({
                        next: (editor) => {
                            this.patchState({ editor, editorState: EDITOR_STATE.IDLE });
                        },
                        error: ({ status }: HttpErrorResponse) =>
                            this.createEmptyState({ canEdit: false, canRead: false }, status),
                        finalize: () => whenReloaded?.()
                    }),
                    catchError(() => EMPTY)
                );
            })
        );
    });

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
            switchMap((payload) =>
                this.dotPageApiService.save(payload).pipe(
                    switchMap(() =>
                        this.dotPageApiService.get(payload.params).pipe(
                            tapResponse(
                                (pageData: DotPageApiResponse) => {
                                    this.patchState((state) => ({
                                        ...state,
                                        editor: pageData,
                                        editorState: EDITOR_STATE.IDLE
                                    }));

                                    payload.whenSaved?.();
                                },
                                (e) => {
                                    console.error(e);
                                    payload.whenSaved?.();
                                    this.updateEditorState(EDITOR_STATE.ERROR);
                                }
                            )
                        )
                    ),
                    catchError((e) => {
                        console.error(e);
                        payload.whenSaved?.();
                        this.updateEditorState(EDITOR_STATE.ERROR);

                        return EMPTY;
                    })
                )
            )
        );
    });

    readonly saveFromInlineEditedContentlet = this.effect(
        (payload$: Observable<SaveInlineEditing>) => {
            return payload$.pipe(
                tap(() => this.updateEditorState(EDITOR_STATE.LOADING)),
                switchMap(({ contentlet, params }) => {
                    return this.dotPageApiService.saveContentlet({ contentlet }).pipe(
                        tapResponse(
                            () => {
                                this.messageService.add({
                                    severity: 'success',
                                    summary: this.dotMessageService.get('message.content.saved'),
                                    life: 2000
                                });
                            },
                            (e) => {
                                console.error(e);
                                this.messageService.add({
                                    severity: 'error',
                                    summary: this.dotMessageService.get(
                                        'editpage.content.update.contentlet.error'
                                    ),
                                    life: 2000
                                });
                            }
                        ),
                        switchMap(() => this.dotPageApiService.get(params)),
                        tapResponse(
                            (pageData: DotPageApiResponse) => {
                                this.patchState((state) => ({
                                    ...state,
                                    editor: pageData,
                                    editorState: EDITOR_STATE.IDLE,
                                    editorData: {
                                        ...state.editorData,
                                        mode: EDITOR_MODE.EDIT
                                    }
                                }));
                            },
                            (e) => {
                                console.error(e);
                                this.updateEditorState(EDITOR_STATE.ERROR);
                            }
                        )
                    );
                })
            );
        }
    );

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
                params: DotPageApiParams;
                whenSaved?: () => void;
            }>
        ) => {
            return payload$.pipe(
                tap(() => {
                    this.updateEditorState(EDITOR_STATE.LOADING);
                }),
                getFormId(this.dotPageApiService), // We need to do something with the errors here.
                switchMap((response) => {
                    const { pageContainers, didInsert } = insertContentletInContainer(
                        response.payload
                    );

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

                        this.updateEditorState(EDITOR_STATE.IDLE);

                        return EMPTY;
                    }

                    return this.dotPageApiService
                        .save({
                            pageContainers,
                            pageId: response.payload.pageId,
                            params: response.params
                        })
                        .pipe(
                            switchMap(() => this.dotPageApiService.get(response.params)),
                            tapResponse(
                                (pageData: DotPageApiResponse) => {
                                    this.patchState((state) => ({
                                        ...state,
                                        editor: pageData,
                                        editorState: EDITOR_STATE.IDLE
                                    }));

                                    response.whenSaved?.();
                                },
                                (e) => {
                                    console.error(e);
                                    response.whenSaved?.();
                                    this.updateEditorState(EDITOR_STATE.ERROR);
                                }
                            )
                        );
                })
            );
        }
    );

    readonly unlockPage = this.effect((inode$: Observable<string>) => {
        return inode$.pipe(
            tap(() => this.updateEditorState(EDITOR_STATE.LOADING)),
            switchMap((inode) =>
                this.dotContentletLockerService.unlock(inode).pipe(
                    tapResponse({
                        next: () => {
                            this.patchState((state) => ({
                                ...state,
                                editorState: EDITOR_STATE.IDLE,
                                editorData: {
                                    ...state.editorData,
                                    page: {
                                        ...state.editorData.page,
                                        isLocked: false
                                    },
                                    mode: EDITOR_MODE.EDIT
                                }
                            }));
                        },
                        error: () => {
                            this.updateEditorState(EDITOR_STATE.ERROR);
                        }
                    })
                )
            )
        );
    });

    readonly setDragItem = this.updater((state, dragItem: EmaDragItem) => {
        return {
            ...state,
            dragItem,
            editorState: EDITOR_STATE.DRAGGING
        };
    });

    readonly resetDragProperties = this.updater((state) => {
        return {
            ...state,
            dragItem: undefined,
            bounds: [],
            contentletArea: undefined
        };
    });

    private createPageURL(state: EditEmaState): string {
        const params = {
            url: state.editor.page.pageURI,
            language_id: state.editor.viewAs.language.id.toString(),
            'com.dotmarketing.persona.id': state.editor.viewAs.persona?.identifier,
            variantName: state.editorData.variantId
        };

        const url = sanitizeURL(params.url);

        return createPageApiUrlWithQueryParams(url, params);
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
     * Update the editor state to scroll
     *
     * @memberof EditEmaStore
     */
    readonly setScrollingState = this.updater((state) => {
        return {
            ...state,
            editorState: EDITOR_STATE.SCROLL_DRAG,
            bounds: []
        };
    });

    /**
     * Update the preview state
     *
     * @memberof EditEmaStore
     */
    readonly updateEditorData = this.updater((state, editorData: EditorData) => {
        return {
            ...state,
            editorData: {
                ...state.editorData,
                ...editorData
            },
            editorState: EDITOR_STATE.IDLE
        };
    });

    /**
     * Updates the editor scroll state in the dot-ema store.
     * If a drag item is present, we assume that scrolling was done during a drag and drop, and the state will automatically change to dragging.
     * if there is no dragItem, we change the state to IDLE
     *
     * @returns The updated dot-ema store state.
     */
    readonly updateEditorScrollState = this.updater((state) => {
        const newState = state.dragItem
            ? {
                  ...state,
                  editorState: EDITOR_STATE.SCROLL_DRAG
              }
            : {
                  ...state,
                  editorState: EDITOR_STATE.IDLE,
                  bounds: [],
                  contentletArea: undefined
              };

        return newState;
    });

    readonly updateEditorDragState = this.updater((state) => {
        return {
            ...state,
            editorState: state.dragItem ? EDITOR_STATE.DRAGGING : EDITOR_STATE.IDLE
        };
    });

    readonly setDevice = this.updater((state, device: DotDevice) => {
        return {
            ...state,
            editorData: {
                ...state.editorData,
                mode: EDITOR_MODE.DEVICE,
                device
            }
        };
    });

    readonly setSocialMedia = this.updater((state, socialMedia: string) => {
        return {
            ...state,
            editorData: {
                ...state.editorData,
                mode: EDITOR_MODE.SOCIAL_MEDIA,
                socialMedia
            }
        };
    });

    readonly setBounds = this.updater((state, bounds: Container[]) => ({
        ...state,
        bounds: bounds
    }));

    readonly setContentletArea = this.updater((state, contentletArea: ContentletArea) => ({
        ...state,
        contentletArea
    }));

    readonly setEditorMode = this.updater((state, mode: EDITOR_MODE) => ({
        ...state,
        editorData: {
            ...state.editorData,
            mode
        }
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
        siteId: string;
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
            bounds: [],
            contentletArea: null,
            editor: {
                page: {
                    title: '',
                    identifier: '',
                    inode: '',
                    pageURI: '',
                    contentType: '',
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
            editorState: EDITOR_STATE.IDLE,
            editorData: {
                mode: EDITOR_MODE.EDIT
            }
        });
    }

    /**
     * Get the containers data
     *
     * @private
     * @param {ContainerData} containers
     * @memberof EditEmaStore
     */
    private getPageContainers = (containers: DotPageContainerStructure) => {
        return Object.keys(containers).reduce(
            (
                acc: {
                    identifier: string;
                    uuid: string;
                    contentletsId: string[];
                }[],
                container
            ) => {
                const contentlets = containers[container].contentlets;

                const contentletsKeys = Object.keys(contentlets);

                contentletsKeys.forEach((key) => {
                    acc.push({
                        identifier:
                            containers[container].container.path ??
                            containers[container].container.identifier,
                        uuid: key.replace('uuid-', ''),
                        contentletsId: contentlets[key].map((contentlet) => contentlet.identifier)
                    });
                });

                return acc;
            },
            []
        );
    };

    private getInitialEditorMode({
        isDefaultVariant,
        canEditVariant,
        isLocked
    }: {
        isDefaultVariant: boolean;
        canEditVariant: boolean;
        isLocked: boolean;
    }): EDITOR_MODE {
        if (isLocked) {
            return EDITOR_MODE.LOCKED;
        }

        if (isDefaultVariant) {
            return EDITOR_MODE.EDIT;
        } else if (canEditVariant) {
            return EDITOR_MODE.EDIT_VARIANT;
        }

        return EDITOR_MODE.PREVIEW_VARIANT;
    }
}
