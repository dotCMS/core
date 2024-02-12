import { ComponentStore, tapResponse } from '@ngrx/component-store';
import { Observable } from 'rxjs';

import { Injectable, inject } from '@angular/core';

import { switchMap } from 'rxjs/operators';

import { DotMessageService } from '@dotcms/data-access';

import { DotActionUrlService } from '../../../services/dot-action-url/dot-action-url.service';
import { EDIT_CONTENTLET_URL, ADD_CONTENTLET_URL } from '../../../shared/consts';
import { ActionPayload } from '../../../shared/models';

type DialogType = 'content' | 'form' | 'widget' | null;

export interface EditEmaDialogState {
    visible: boolean;
    header: string;
    loading: boolean;
    url: string;
    type: DialogType;
    payload?: ActionPayload;
}

@Injectable()
export class DotEmaDialogStore extends ComponentStore<EditEmaDialogState> {
    constructor() {
        super({ header: '', loading: false, url: '', type: null, visible: false });
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
        (
            contentTypeVariable$: Observable<{
                variable: string;
                name: string;
                payload: ActionPayload;
            }>
        ) => {
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
     * This method is called when the user clicks in the + button in the jsp dialog or drag a contentType from the palette
     *
     * @memberof DotEmaDialogStore
     */
    readonly createContentlet = this.updater(
        (
            state,
            {
                url,
                contentType,
                payload
            }: { url: string; contentType: string; payload?: ActionPayload }
        ) => {
            return {
                ...state,
                url: url,
                header: this.dotMessageService.get(
                    'contenttypes.content.create.contenttype',
                    contentType
                ),
                loading: true,
                type: 'content',
                visible: true,
                payload
            };
        }
    );

    /**
     * This method is called when the user clicks on the edit button
     *
     * @memberof DotEmaDialogStore
     */
    readonly editContentlet = this.updater((state, payload: { inode: string; title: string }) => {
        return {
            ...state,
            header: payload.title,
            loading: true,
            url: this.createEditContentletUrl(payload.inode),
            type: 'content',
            visible: true
        };
    });

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
                loading: true,
                url: this.createAddContentletUrl(data),
                type: 'content',
                visible: true,
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
            loading: true,
            url: null,
            type: 'form',
            visible: true,
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
            loading: false,
            type: null,
            visible: false,
            payload: undefined
        };
    });

    /**
     * This method sets the loading property
     *
     * @memberof DotEmaDialogStore
     */
    readonly setLoading = this.updater((state, loading: boolean) => {
        return {
            ...state,
            loading: loading
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
