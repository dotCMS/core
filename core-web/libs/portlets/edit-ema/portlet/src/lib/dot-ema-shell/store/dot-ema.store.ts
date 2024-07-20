import { ComponentStore, tapResponse } from '@ngrx/component-store';
import { EMPTY, Observable } from 'rxjs';

import { Injectable } from '@angular/core';

import { MessageService } from 'primeng/api';

import { catchError, map, switchMap, tap } from 'rxjs/operators';

import { DotMessageService } from '@dotcms/data-access';
import { DEFAULT_VARIANT_ID } from '@dotcms/dotcms-models';

import {
    DotPageApiParams,
    DotPageApiResponse,
    DotPageApiService
} from '../../services/dot-page-api.service';
import { EDITOR_MODE, EDITOR_STATE } from '../../shared/enums';
import {
    ActionPayload,
    EditEmaState,
    SaveInlineEditing,
    SavePagePayload
} from '../../shared/models';
import { insertContentletInContainer } from '../../utils';

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
        private readonly messageService: MessageService,
        private readonly dotMessageService: DotMessageService
    ) {
        super();
    }

    /*******************
     * Selectors
     *******************/

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

    readonly setEditorMode = this.updater((state, mode: EDITOR_MODE) => ({
        ...state,
        editorData: {
            ...state.editorData,
            mode
        }
    }));
}
