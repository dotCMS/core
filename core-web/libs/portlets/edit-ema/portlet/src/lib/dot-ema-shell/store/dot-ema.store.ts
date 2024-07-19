import { ComponentStore, tapResponse } from '@ngrx/component-store';
import { EMPTY, Observable, forkJoin, of } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';
import { Injectable } from '@angular/core';

import { MessageService } from 'primeng/api';

import { catchError, map, shareReplay, switchMap, take, tap, filter } from 'rxjs/operators';

import {
    DotExperimentsService,
    DotLanguagesService,
    DotLicenseService,
    DotMessageService
} from '@dotcms/data-access';
import { LoginService } from '@dotcms/dotcms-js';
import {
    DEFAULT_VARIANT_ID,
    DotExperiment,
    DotExperimentStatus,
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
        private readonly loginService: LoginService,
        private readonly dotLanguagesService: DotLanguagesService
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

    private readonly iframeURL$ = this.select(
        this.clientHost$,
        this.pageURL$,
        (clientHost, pageURL) => (clientHost ? `${clientHost}/${pageURL}` : '')
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

    private readonly languageId$ = this.select((state) => state.editor.viewAs.language.id);

    private readonly pageId$ = this.select((state) => state.editor.page.identifier);

    private readonly containers$ = this.select((state) =>
        this.getPageContainers(state.editor.containers)
    );
    private readonly personaTag$ = this.select((state) => state.editor.viewAs.persona?.keyTag);
    private readonly personalization$ = this.select((state) =>
        getPersonalization(state.editor.viewAs.persona)
    );

    readonly editorMode$ = this.select((state) => state.editorData.mode);

    // THIS IS FROM THE TOOLBAR
    readonly editorData$ = this.select((state) => state.editorData);

    readonly pageRendered$ = this.select((state) => state.editor.page.rendered);

    // I need to get rid of this somehow
    readonly shouldReload$ = this.select((state) => state.shouldReload);

    readonly contentState$ = this.select(
        this.code$,
        this.shouldReload$,
        this.stateLoad$,
        this.clientHost$,
        (code, shouldReload, state, clientHost) => ({
            code,
            shouldReload,
            state,
            isVTL: !clientHost
        })
    ).pipe(filter(({ state }) => state === EDITOR_STATE.IDLE));

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
        this.iframeURL$,
        this.isEnterpriseLicense$,
        this.dragItem$,
        (
            bounds,
            clientHost,
            contentletArea,
            currentExperiment,
            currentState,
            editor,
            editorData,
            iframeURL,
            isEnterpriseLicense,
            dragItem
        ) => {
            return {
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
                iframeURL,
                isEnterpriseLicense,
                state: currentState,
                dragItem,
                showContentletTools:
                    !!contentletArea &&
                    // Page can be edited
                    editorData.canEditVariant &&
                    editor.page.canEdit &&
                    !editorData.page.isLocked &&
                    // editor Can edit
                    !editorData.device &&
                    (currentState === EDITOR_STATE.IDLE || currentState === EDITOR_STATE.DRAGGING),
                showDropzone:
                    // Page can be edited
                    editorData.canEditVariant &&
                    editor.page.canEdit &&
                    !editorData.page.isLocked &&
                    // Drag is Active
                    !editorData.device &&
                    (currentState === EDITOR_STATE.DRAGGING ||
                        currentState === EDITOR_STATE.SCROLL_DRAG),
                showPalette:
                    isEnterpriseLicense &&
                    // page can be edited
                    editorData.canEditVariant &&
                    editor.page.canEdit &&
                    !editorData.page.isLocked &&
                    // Editor is in edit state
                    (editorData.mode === EDITOR_MODE.EDIT ||
                        editorData.mode === EDITOR_MODE.EDIT_VARIANT ||
                        editorData.mode === EDITOR_MODE.INLINE_EDITING)
            };
        }
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
     * @param {Observable<DotPageApiParams>} params$ - Parameters for HTTP requests.
     * @returns {Observable<any>} Response of the HTTP requests.
     */
    readonly load = this.effect((params$: Observable<DotPageApiParams>) => {
        return params$.pipe(
            switchMap((params) => {
                return forkJoin({
                    pageData: this.dotPageApiService.get(params).pipe(
                        switchMap((pageData) => {
                            if (!pageData.vanityUrl) {
                                return of(pageData);
                            }

                            const newParams = {
                                ...params,
                                url: pageData.vanityUrl.forwardTo.replace('/', '')
                            };

                            return this.dotPageApiService.get(newParams).pipe(
                                map((newPageData) => ({
                                    ...newPageData,
                                    vanityUrl: pageData.vanityUrl
                                }))
                            );
                        })
                    ),
                    licenseData: this.dotLicenseService.isEnterprise().pipe(take(1), shareReplay()),
                    currentUser: this.loginService.getCurrentUser()
                }).pipe(
                    tap({
                        error: ({ status }: HttpErrorResponse) => {
                            this.createEmptyState({ canEdit: false, canRead: false }, status);
                        }
                    }),
                    switchMap(({ pageData, licenseData, currentUser }) =>
                        forkJoin({
                            experiment: this.getExperiment(params.experimentId),
                            languages: this.dotLanguagesService.getLanguagesUsedPage(
                                pageData.page.identifier
                            )
                        }).pipe(
                            tap({
                                next: ({ experiment, languages }) => {
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

                                    // In my approach the editor will consume from 2 store, Global and Toolbar, that will be merged in just one coming from the toolbar
                                    // And the only thing it will do is to tell the global store to reload the page
                                    // and update the bounds, contentletArea and editorState
                                    // Anything else will be handled by the global and the toolbar

                                    // Editor store should not fetch, nor manipulate the global or toolbar store
                                    // It should only consume from them

                                    return this.setState({
                                        // This should page instead of editor
                                        editor: pageData, // Global
                                        isEnterpriseLicense: licenseData, // Global
                                        languages, // Global
                                        // The params will live in the global
                                        // so I can have a single source of truth
                                        // params: {
                                        //     //...
                                        //     clientHost: 'host'
                                        // },

                                        clientHost: params.clientHost, // Editor Specific
                                        editorState: EDITOR_STATE.IDLE, // Editor Specific, but depends in the global cycle for loading and idle states
                                        bounds: [], // Editor Specific
                                        contentletArea: null, // Editor Specific

                                        currentExperiment: experiment, // Toolbar specific
                                        editorData: {
                                            // Toolbar specific, we can consume this from the toolbar
                                            mode,
                                            canEditVariant,
                                            canEditPage: pageData.page.canEdit,
                                            variantId: params.variantName,
                                            page: {
                                                isLocked,
                                                canLock: pageData.page.canLock,
                                                lockedByUser: pageData.page.lockedByName
                                            }
                                        },

                                        shouldReload: true // Probably will get rid of this
                                    });
                                }
                            })
                        )
                    )
                );
            })
        );
    });

    readonly reload = this.effect((payload$: Observable<ReloadPagePayload>) => {
        return payload$.pipe(
            tap(() => this.updateEditorState(EDITOR_STATE.LOADING)),
            switchMap(({ params, whenReloaded }) => {
                return this.dotPageApiService.get(params).pipe(
                    switchMap((pageData) =>
                        this.dotLanguagesService
                            .getLanguagesUsedPage(pageData.page.identifier)
                            .pipe(
                                map((languages) => ({
                                    editor: pageData,
                                    languages
                                }))
                            )
                    ),
                    tapResponse({
                        next: ({ editor, languages }) => {
                            this.patchState({
                                editor,
                                languages,
                                editorState: EDITOR_STATE.IDLE,
                                shouldReload: true
                            });
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
                                        editorState: EDITOR_STATE.IDLE,
                                        shouldReload: true
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
                                        mode:
                                            pageData.viewAs.variantId === DEFAULT_VARIANT_ID
                                                ? EDITOR_MODE.EDIT
                                                : EDITOR_MODE.EDIT_VARIANT
                                    },
                                    shouldReload: true
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
                                        editorState: EDITOR_STATE.IDLE,
                                        shouldReload: true
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
        const vanityUrl = state.editor.vanityUrl;

        const vanityURI = vanityUrl?.response === 200 ? vanityUrl.url : vanityUrl?.forwardTo;

        const params = {
            url: vanityUrl ? vanityURI : state.editor.page.pageURI,
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

    readonly setShouldReload = this.updater((state, shouldReload: boolean) => ({
        ...state,
        shouldReload
    }));

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
                    live: false,
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
            },
            shouldReload: false,
            languages: []
        });
    }

    /**
     * Get the experiment data
     *
     * @private
     * @param {string} [experimentId='']
     * @return {*}
     * @memberof EditEmaStore
     */
    private getExperiment(experimentId = ''): Observable<DotExperiment | null> {
        return this.dotExperimentsService.getById(experimentId).pipe(
            // If there is an error, we return null
            // This is to avoid blocking the page if there is an error with the experiment
            catchError(() => of(null))
        );
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
