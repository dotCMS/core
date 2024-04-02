import { ComponentStore, tapResponse } from '@ngrx/component-store';
import { EMPTY, Observable, forkJoin } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';
import { Injectable } from '@angular/core';

import { MessageService } from 'primeng/api';

import { catchError, map, shareReplay, switchMap, take, tap } from 'rxjs/operators';

import { DotExperimentsService, DotLicenseService, DotMessageService } from '@dotcms/data-access';
import {
    DotContainerMap,
    DotDevice,
    DotExperimentStatus,
    DotLayout,
    DotPageContainerStructure
} from '@dotcms/dotcms-models';

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
        private readonly dotExperimentsService: DotExperimentsService
    ) {
        super();
    }

    /*******************
     * Selectors
     *******************/

    readonly code$ = this.select((state) => state.editor.page.rendered);

    readonly stateLoad$ = this.select((state) => state.editorState);

    readonly templateThemeId$ = this.select((state) => state.editor.template.themeId);

    readonly templateIdentifier$ = this.select((state) => state.editor.template.identifier);

    readonly templateDrawed$ = this.select((state) => state.editor.template.drawed);

    readonly contentState$ = this.select(this.code$, this.stateLoad$, (code, state) => {
        return {
            state,
            code
        };
    });

    readonly editorState$ = this.select((state) => {
        const pageURL = this.createPageURL({
            url: state.editor.page.pageURI,
            language_id: state.editor.viewAs.language.id.toString(),
            'com.dotmarketing.persona.id': state.editor.viewAs.persona?.identifier,
            variantName: state.editorData.variantId
        });

        const favoritePageURL = this.createFavoritePagesURL({
            languageId: state.editor.viewAs.language.id,
            pageURI: state.editor.page.pageURI,
            siteId: state.editor.site.identifier
        });

        const iframeURL = state.clientHost ? `${state.clientHost}/${pageURL}` : '';

        return {
            clientHost: state.clientHost,
            favoritePageURL,
            apiURL: `${window.location.origin}/api/v1/page/json/${pageURL}`,
            iframeURL,
            editor: {
                ...state.editor,
                viewAs: {
                    ...state.editor.viewAs,
                    persona: state.editor.viewAs.persona ?? DEFAULT_PERSONA
                }
            },
            isEnterpriseLicense: state.isEnterpriseLicense,
            state: state.editorState ?? EDITOR_STATE.LOADING,
            editorData: state.editorData,
            currentExperiment: state.currentExperiment
        };
    });

    readonly clientHost$ = this.select((state) => state.clientHost);

    /**
     * Before this was layoutProperties, but are separate to "temp" selector.
     * And then is merged with templateIdentifier in layoutProperties$.
     * This is to try avoid extra-calls on the select, and avoid memory leaks
     */
    private readonly layoutProps$ = this.select((state) => ({
        layout: state.editor.layout,
        themeId: state.editor.template.theme,
        pageId: state.editor.page.identifier,
        containersMap: this.mapContainers(state.editor.containers)
    }));

    readonly layoutProperties$ = this.select(
        this.layoutProps$,
        this.templateIdentifier$,
        this.templateThemeId$,
        (props, templateIdentifier, themeId) => ({
            ...props,
            template: { identifier: templateIdentifier, themeId }
        })
    );

    readonly shellProps$ = this.select((state) => ({
        page: state.editor.page,
        siteId: state.editor.site.identifier,
        languageId: state.editor.viewAs.language.id,
        currentUrl: '/' + sanitizeURL(state.editor.page.pageURI),
        host: state.clientHost,
        error: state.error
    }));

    readonly shellProperties$ = this.select(
        this.shellProps$,
        this.templateDrawed$,
        (props, templateDrawed) => ({ ...props, templateDrawed })
    );

    // This data is needed to save the page on CRUD operation
    readonly pageData$ = this.select((state) => {
        const containers = this.getPageContainers(state.editor.containers);

        return {
            containers,
            id: state.editor.page.identifier,
            languageId: state.editor.viewAs.language.id,
            personaTag: state.editor.viewAs.persona?.keyTag,
            personalization: getPersonalization(state.editor.viewAs.persona)
        };
    });

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
                            .pipe(take(1), shareReplay())
                    }).pipe(
                        tap({
                            error: ({ status }: HttpErrorResponse) => {
                                this.createEmptyState({ canEdit: false, canRead: false }, status);
                            }
                        }),
                        switchMap(({ pageData, licenseData }) =>
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

                                        const mode = this.getInitialEditorMode(
                                            isDefaultVariant,
                                            canEditVariant
                                        );

                                        return this.setState({
                                            currentExperiment: experiment,
                                            clientHost: params.clientHost,
                                            editor: pageData,
                                            isEnterpriseLicense: licenseData,
                                            editorState: EDITOR_STATE.IDLE,
                                            editorData: {
                                                mode,
                                                canEditVariant,
                                                canEditPage: pageData.page.canEdit,
                                                variantId: params.variantName
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

    private createPageURL(params: DotPageApiParams): string {
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

    private getInitialEditorMode(isDefaultVariant: boolean, canEditVariant: boolean): EDITOR_MODE {
        if (isDefaultVariant) {
            return EDITOR_MODE.EDIT;
        } else if (canEditVariant) {
            return EDITOR_MODE.EDIT_VARIANT;
        }

        return EDITOR_MODE.PREVIEW_VARIANT;
    }
}
