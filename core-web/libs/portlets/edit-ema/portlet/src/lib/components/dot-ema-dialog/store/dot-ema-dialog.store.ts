import { ComponentStore, tapResponse } from '@ngrx/component-store';
import { Observable } from 'rxjs';

import { Injectable, inject } from '@angular/core';

import { switchMap } from 'rxjs/operators';

import { DotMessageService } from '@dotcms/data-access';

import { DotActionUrlService } from '../../../services/dot-action-url/dot-action-url.service';
import { LAYOUT_URL, CONTENTLET_SELECTOR_URL } from '../../../shared/consts';
import { DialogStatus, FormStatus } from '../../../shared/enums';
import {
    ActionPayload,
    CreateContentletAction,
    CreateFromPaletteAction,
    DotPage,
    EditContentletPayload,
    EditEmaDialogState
} from '../../../shared/models';

@Injectable()
export class DotEmaDialogStore extends ComponentStore<EditEmaDialogState> {
    constructor() {
        super({
            header: '',
            url: '',
            type: null,
            status: DialogStatus.IDLE,
            editContentForm: {
                status: FormStatus.PRISTINE,
                isTranslation: false
            }
        });
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
     * This method is called when the user tries to navigate to a different language
     *
     * @memberof DotEmaDialogStore
     */
    readonly translatePage = this.updater(
        (state, { page, newLanguage }: { page: DotPage; newLanguage: number | string }) => {
            return {
                ...state,
                header: page.title,
                status: DialogStatus.LOADING,
                type: 'content',
                url: this.createTranslatePageUrl(page, newLanguage),
                editContentForm: {
                    status: FormStatus.PRISTINE,
                    isTranslation: true
                }
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
     * This method is called when the user make changes in the form
     *
     * @memberof DotEmaDialogStore
     */
    readonly setDirty = this.updater((state) => {
        return {
            ...state,
            editContentForm: {
                ...state.editContentForm,
                status: FormStatus.DIRTY
            }
        };
    });

    /**
     * This method is called when the user save the form
     *
     * @memberof DotEmaDialogStore
     */
    readonly setSaved = this.updater((state) => {
        return {
            ...state,
            editContentForm: {
                ...state.editContentForm,
                status: FormStatus.SAVED
            }
        };
    });

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
            payload: undefined,
            editContentForm: {
                status: FormStatus.PRISTINE,
                isTranslation: false
            }
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
        const queryParams = new URLSearchParams({
            p_p_id: 'content',
            p_p_action: '1',
            p_p_state: 'maximized',
            p_p_mode: 'view',
            _content_struts_action: '/ext/contentlet/edit_contentlet',
            _content_cmd: 'edit',
            inode: inode
        });

        return `${LAYOUT_URL}?${queryParams.toString()}`;
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
        const queryParams = new URLSearchParams({
            ng: 'true',
            container_id: containerId,
            add: acceptTypes,
            language_id
        });

        return `${CONTENTLET_SELECTOR_URL}?${queryParams.toString()}`;
    }

    private createTranslatePageUrl(page: DotPage, newLanguage: number | string) {
        const { working, workingInode, inode } = page;
        const pageInode = working ? workingInode : inode;
        const queryParams = new URLSearchParams({
            p_p_id: 'content',
            p_p_action: '1',
            p_p_state: 'maximized',
            angularCurrentPortlet: 'edit-page',
            _content_sibbling: pageInode,
            _content_cmd: 'edit',
            p_p_mode: 'view',
            _content_sibblingStructure: pageInode,
            _content_struts_action: '/ext/contentlet/edit_contentlet',
            inode: '',
            lang: newLanguage.toString(),
            populateaccept: 'true',
            reuseLastLang: 'true'
        });

        return `${LAYOUT_URL}?${queryParams.toString()}`;
    }
}
