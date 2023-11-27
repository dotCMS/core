import { ComponentStore, tapResponse } from '@ngrx/component-store';
import { EMPTY, Observable } from 'rxjs';

import { Injectable } from '@angular/core';

import { catchError, switchMap, tap } from 'rxjs/operators';

import { DotPageApiParams, DotPageApiService } from '../../services/dot-page-api.service';
import { ADD_CONTENTLET_URL, EDIT_CONTENTLET_URL } from '../../shared/consts';
import { SavePagePayload } from '../../shared/models';

export interface EditEmaState {
    language_id: string;
    url: string;
    editor: {
        page: {
            title: string;
            identifier: string;
        };
    };
    dialogIframeURL: string;
    dialogVisible: boolean;
    dialogHeader: string;
    dialogIframeLoading: boolean;
}

@Injectable()
export class EditEmaStore extends ComponentStore<EditEmaState> {
    constructor(private dotPageApiService: DotPageApiService) {
        super({
            language_id: '',
            url: '',
            editor: {
                page: {
                    title: '',
                    identifier: ''
                }
            },
            dialogIframeURL: '',
            dialogVisible: false,
            dialogHeader: '',
            dialogIframeLoading: false
        });
    }

    readonly iframeUrl$: Observable<string> = this.select(
        ({ url, language_id }) => `http://localhost:3000/${url}?language_id=${language_id}`
    );
    readonly language_id$: Observable<string> = this.select((state) => state.language_id);
    readonly url$: Observable<string> = this.select((state) => state.url);
    readonly pageTitle$: Observable<string> = this.select((state) => state.editor.page.title);
    readonly editIframeURL$: Observable<string> = this.select((state) => state.dialogIframeURL);

    readonly vm$ = this.select((state) => {
        return {
            iframeUrl: `http://localhost:3000/${state.url}?language_id=${state.language_id}`,
            language_id: state.language_id,
            pageTitle: state.editor.page.title,
            dialogIframeURL: state.dialogIframeURL,
            dialogVisible: state.dialogVisible,
            dialogHeader: state.dialogHeader,
            dialogIframeLoading: state.dialogIframeLoading
        };
    });

    /**
     * Load the page editor
     *
     * @memberof EditEmaStore
     */
    readonly load = this.effect((params$: Observable<DotPageApiParams>) => {
        return params$.pipe(
            switchMap(({ language_id, url }) =>
                this.dotPageApiService.get({ language_id, url }).pipe(
                    tap({
                        next: (editor) => {
                            this.patchState({
                                editor,
                                language_id,
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

    readonly setLanguage = this.updater((state, language_id: string) => ({
        ...state,
        language_id
    }));

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
}
