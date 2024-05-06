import { ComponentStore, tapResponse } from '@ngrx/component-store';
import { Observable } from 'rxjs';

import { Injectable, inject } from '@angular/core';

import { switchMap } from 'rxjs/operators';

import { DotMessageService } from '@dotcms/data-access';

import { DotActionUrlService } from '../../../services/dot-action-url/dot-action-url.service';
import { EDIT_CONTENTLET_URL, ADD_CONTENTLET_URL } from '../../../shared/consts';
import { ActionPayload } from '../../../shared/models';

type DialogType = 'content' | 'form' | 'widget' | null;

export enum DialogStatus {
    IDLE = 'IDLE',
    LOADING = 'LOADING',
    INIT = 'INIT'
}

export interface EditEmaDialogState {
    header: string;
    status: DialogStatus;
    url: string;
    type: DialogType;
    payload?: ActionPayload;
}

// We can modify this if we add more events, for now I think is enough
export interface CreateFromPaletteAction {
    variable: string;
    name: string;
    payload: ActionPayload;
}

interface EditContentletPayload {
    inode: string;
    title: string;
}

export interface CreateContentletAction {
    url: string;
    contentType: string;
    payload: ActionPayload;
}

@Injectable()
export class DotEmaDialogStore extends ComponentStore<EditEmaDialogState> {
    constructor() {
        super({ header: '', url: '', type: null, status: DialogStatus.IDLE });
    }

    private dotActionUrlService = inject(DotActionUrlService);

    private dotMessageService = inject(DotMessageService);

    readonly dialogState$ = this.select((state) => state);

    /**
     * Create a contentlet from the palette
     *
     * @memberof EditEmaStore
     */
    readonly createContentletFromPalette = this.effect(
        (contentTypeVariable$: Observable<CreateFromPaletteAction>) => {
            return contentTypeVariable$.pipe(
                switchMap(({ name, variable, payload }) => {
                    return this.dotActionUrlService.getCreateContentletUrl(variable).pipe(
                        tapResponse(
                            (url) => {
                                this.createContentlet({
                                    url,
                                    contentType: name,
                                    payload
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

    /**
     * This method is called when we need to open a dialog with a specific URL
     *
     * @memberof DotEmaDialogStore
     */
    readonly openDialogOnURL = this.updater(
        (state, { url, title }: { url: string; title: string }) => {
            return {
                ...state,
                header: title,
                status: DialogStatus.LOADING,
                url,
                type: 'content'
            };
        }
    );

    /**
     * This method is called when the user clicks in the + button in the jsp dialog or drag a contentType from the palette
     *
     * @memberof DotEmaDialogStore
     */
    readonly createContentlet = this.updater(
        (state, { url, contentType, payload }: CreateContentletAction) => {
            return {
                ...state,
                url: url,
                header: this.dotMessageService.get(
                    'contenttypes.content.create.contenttype',
                    contentType
                ),
                status: DialogStatus.LOADING,
                type: 'content',
                payload
            };
        }
    );

    /**
     * This method is called when the user clicks on the edit button
     *
     * @memberof DotEmaDialogStore
     */
    readonly loadingIframe = this.updater((state, title: string) => {
        return {
            ...state,
            header: title,
            status: DialogStatus.LOADING,
            url: '',
            type: 'content'
        };
    });

    /**
     * This method is called when the user clicks on the edit button
     *
     * @memberof DotEmaDialogStore
     */
    readonly editContentlet = this.updater((state, { inode, title }: EditContentletPayload) => {
        return {
            ...state,
            header: title,
            status: DialogStatus.LOADING,
            type: 'content',
            url: this.createEditContentletUrl(inode)
        };
    });

    /**
     * This method is called when the user clicks on the edit URL Content Map button
     *
     * @memberof DotEmaDialogStore
     */
    readonly editUrlContentMapContentlet = this.updater(
        (state, { inode, title }: EditContentletPayload) => {
            const url = this.createEditContentletUrl(inode) + '&isURLMap=true';

            return {
                ...state,
                header: title,
                status: DialogStatus.LOADING,
                type: 'content',
                url
            };
        }
    );

    /**
     * This method is called when the user clicks on the [+ add] button
     *
     * @memberof DotEmaDialogStore
     */
    readonly addContentlet = this.updater(
        (
            state,
            data: {
                containerId: string;
                acceptTypes: string;
                language_id: string;
                payload: ActionPayload;
            }
        ) => {
            return {
                ...state,
                header: this.dotMessageService.get('edit.ema.page.dialog.header.search.content'),
                status: DialogStatus.LOADING,
                url: this.createAddContentletUrl(data),
                type: 'content',
                payload: data.payload
            };
        }
    );

    /**
     * This method is called when the user clicks on the [+ add] button and selects form as content type
     *
     * @memberof DotEmaDialogStore
     */
    readonly addFormContentlet = this.updater((state, payload: ActionPayload) => {
        return {
            ...state,
            header: this.dotMessageService.get('edit.ema.page.dialog.header.search.form'),
            status: DialogStatus.LOADING,
            url: null,
            type: 'form',
            payload
        };
    });

    /**
     *  This method resets the properties that are being used in for the dialog
     *
     * @memberof DotEmaDialogStore
     */
    readonly resetDialog = this.updater((state) => {
        return {
            ...state,
            url: '',
            header: '',
            status: DialogStatus.IDLE,
            type: null,
            payload: undefined
        };
    });

    /**
     * This method sets the loading property
     *
     * @memberof DotEmaDialogStore
     */
    readonly setStatus = this.updater((state, status: DialogStatus) => {
        return {
            ...state,
            status
        };
    });

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
}
