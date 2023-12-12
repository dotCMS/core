import { ComponentStore, tapResponse } from '@ngrx/component-store';
import { EMPTY, Observable } from 'rxjs';

import { Injectable } from '@angular/core';

import { catchError, switchMap, tap } from 'rxjs/operators';

import {
    DotPageApiParams,
    DotPageApiResponse,
    DotPageApiService
} from '../../services/dot-page-api.service';
import {
    ADD_CONTENTLET_URL,
    DEFAULT_PERSONA,
    EDIT_CONTENTLET_URL,
    HOST
} from '../../shared/consts';
import { SavePagePayload } from '../../shared/models';

export interface EditEmaState {
    url: string;
    editor: DotPageApiResponse;
    dialogIframeURL: string;
    dialogVisible: boolean;
    dialogHeader: string;
    dialogIframeLoading: boolean;
}

@Injectable()
export class EditEmaStore extends ComponentStore<EditEmaState> {
    constructor(private dotPageApiService: DotPageApiService) {
        super({
            url: '',
            editor: {
                page: {
                    title: '',
                    identifier: ''
                },
                viewAs: {
                    language: {
                        id: 1,
                        language: '',
                        countryCode: '',
                        languageCode: '',
                        country: ''
                    }
                }
            },
            dialogIframeURL: '',
            dialogVisible: false,
            dialogHeader: '',
            dialogIframeLoading: false
        });
    }

    readonly editorState$ = this.select((state) => {
        const pageURL = this.createPageURL({
            url: state.url,
            language_id: state.editor.viewAs.language.id.toString(),
            persona_id: state.editor.viewAs.persona?.identifier ?? DEFAULT_PERSONA.identifier
        });

        return state.editor.page.identifier
            ? {
                  apiURL: `${window.location.origin}/api/v1/page/json/${pageURL}`,
                  iframeURL: `${HOST}/${pageURL}`,
                  editor: {
                      ...state.editor,
                      viewAs: {
                          ...state.editor.viewAs,
                          persona: state.editor.viewAs.persona ?? DEFAULT_PERSONA
                      }
                  }
              }
            : null; // Don't return anything unless we have page data
    });

    readonly dialogState$ = this.select(
        (state) =>
            state.editor.page.identifier
                ? {
                      dialogIframeURL: state.dialogIframeURL,
                      dialogVisible: state.dialogVisible,
                      dialogHeader: state.dialogHeader,
                      dialogIframeLoading: state.dialogIframeLoading
                  }
                : null // Don't return anything unless we have page data
    );

    /**
     * Load the page editor
     *
     * @memberof EditEmaStore
     */
    readonly load = this.effect((params$: Observable<DotPageApiParams>) => {
        return params$.pipe(
            switchMap(({ language_id, url, persona_id }) =>
                this.dotPageApiService.get({ language_id, url, persona_id }).pipe(
                    tap({
                        next: (editor) => {
                            this.patchState({
                                editor,
                                url
                            });
                        },
                        error: (e) => {
                            // eslint-disable-next-line no-console
                            console.log(e);
                        }
                    }),
                    catchError(() => EMPTY)
                )
            )
        );
    });

    /**
     * Save the page
     *
     * @memberof EditEmaStore
     */
    readonly savePage = this.effect((payload$: Observable<SavePagePayload>) => {
        return payload$.pipe(
            switchMap((payload) =>
                this.dotPageApiService.save(payload).pipe(
                    tapResponse(
                        () => {
                            payload.whenSaved?.();
                        },
                        (e) => {
                            console.error(e);
                            payload.whenSaved?.();
                        }
                    )
                )
            )
        );
    });

    readonly setURL = this.updater((state, url: string) => ({
        ...state,
        url
    }));

    readonly setDialogIframeURL = this.updater((state, editIframeURL: string) => ({
        ...state,
        dialogIframeURL: editIframeURL
    }));

    readonly setDialogVisible = this.updater((state, dialogVisible: boolean) => ({
        ...state,
        dialogVisible
    }));

    readonly setDialogHeader = this.updater((state, dialogHeader: string) => ({
        ...state,
        dialogHeader
    }));

    readonly setDialogIframeLoading = this.updater((state, editIframeLoading: boolean) => ({
        ...state,
        dialogIframeLoading: editIframeLoading
    }));

    // This method resets the properties that are being used in for the dialog
    readonly resetDialog = this.updater((state) => {
        return {
            ...state,
            dialogIframeURL: '',
            dialogVisible: false,
            dialogHeader: '',
            dialogIframeLoading: false
        };
    });

    // This method is called when the user clicks on the edit button
    readonly initActionEdit = this.updater((state, payload: { inode: string; title: string }) => {
        return {
            ...state,
            dialogVisible: true,
            dialogHeader: payload.title,
            dialogIframeLoading: true,
            dialogIframeURL: this.createEditContentletUrl(payload.inode)
        };
    });

    // This method is called when the user clicks on the edit button
    readonly initActionAdd = this.updater(
        (state, payload: { containerID: string; acceptTypes: string }) => {
            return {
                ...state,
                dialogVisible: true,
                dialogHeader: 'Search Content', // Does this need translation?
                dialogIframeLoading: true,
                dialogIframeURL: this.createAddContentletUrl(payload)
            };
        }
    );

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
        containerID,
        acceptTypes
    }: {
        containerID: string;
        acceptTypes: string;
    }): string {
        return ADD_CONTENTLET_URL.replace('*CONTAINER_ID*', containerID).replace(
            '*BASE_TYPES*',
            acceptTypes
        );
    }

    private createPageURL({ url, language_id, persona_id }: DotPageApiParams): string {
        return `${url}?language_id=${language_id}&com.dotmarketing.persona.id=${persona_id}`;
    }
}
