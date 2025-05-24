import { EMPTY } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectorRef, inject, Injectable } from '@angular/core';

import { MessageService } from 'primeng/api';

import { catchError, filter, map, take, tap } from 'rxjs/operators';

import { CLIENT_ACTIONS } from '@dotcms/client';
import {
    DotContentletService,
    DotHttpErrorManagerService,
    DotMessageService
} from '@dotcms/data-access';

import { DotEditorDialogService } from '../../components/dot-ema-dialog/services/dot-ema-dialog.service';
import { EDITOR_STATE, NG_CUSTOM_EVENTS, UVE_STATUS } from '../../shared/enums';
import { ActionPayload, DialogAction } from '../../shared/models';
import { UVEStore } from '../../store/dot-uve.store';
import { insertContentletInContainer, getTargetUrl, shouldNavigate } from '../../utils';
import { DotPageApiService } from '../dot-page-api.service';

// Define interface for event detail structure
interface EventDetail {
    data: {
        identifier?: string;
        contentType?: string;
        url?: string;
        [key: string]: unknown;
    };
    name?: string;
    payload?: {
        htmlPageReferer?: string;
        newContentletId?: string;
        shouldReloadPage?: boolean;
        contentletIdentifier?: string;
        [key: string]: unknown;
    };
}

@Injectable({
    providedIn: 'root'
})
export class DotUVENgEvenHandlerService {
    readonly uveStore = inject(UVEStore);
    readonly #dialogService = inject(DotEditorDialogService);
    readonly #cd = inject(ChangeDetectorRef);
    readonly #messageService = inject(MessageService);
    readonly #dotMessageService = inject(DotMessageService);
    readonly #dotPageApiService = inject(DotPageApiService);
    readonly #dotContentletService = inject(DotContentletService);
    readonly #dotHttpErrorManagerService = inject(DotHttpErrorManagerService);

    /**
     * Handles the event triggered from the dialog.
     *
     * @param {DialogAction} dialogAction - The event object containing details about the action.
     * @return {void}
     */
    public handleNgEvent(dialogAction: DialogAction) {
        const { event, actionPayload, clientAction } = dialogAction;
        const { detail } = event;

        switch (event.detail.name) {
            case NG_CUSTOM_EVENTS.UPDATE_WORKFLOW_ACTION: {
                this.handleUpdateWorkflowAction();
                break;
            }

            case NG_CUSTOM_EVENTS.CONTENT_SEARCH_SELECT: {
                this.handleContentSearchSelect(detail as EventDetail, actionPayload);
                break;
            }

            case NG_CUSTOM_EVENTS.CREATE_CONTENTLET: {
                this.handleCreateContentlet(detail as EventDetail, actionPayload);
                break;
            }

            case NG_CUSTOM_EVENTS.CANCEL_SAVING_MENU_ORDER: {
                this.handleCancelSavingMenuOrder();
                break;
            }

            case NG_CUSTOM_EVENTS.SAVE_MENU_ORDER: {
                this.handleSaveMenuOrder();
                break;
            }

            case NG_CUSTOM_EVENTS.LANGUAGE_IS_CHANGED: {
                this.handleLanguageChanged(event);
                break;
            }

            case NG_CUSTOM_EVENTS.ERROR_SAVING_MENU_ORDER: {
                this.handleErrorSavingMenuOrder();
                break;
            }

            case NG_CUSTOM_EVENTS.FORM_SELECTED: {
                this.handleFormSelected(detail as EventDetail, actionPayload);
                break;
            }

            case NG_CUSTOM_EVENTS.SAVE_PAGE: {
                this.handleSavePage(event, actionPayload, clientAction);
                break;
            }
        }
    }

    /**
     * Handles the update workflow action event
     * This event is trigger in any change of the workflow action
     * Including:
     * - Lock
     * - Unlock
     * - Publish
     * - Unpublish
     * - Save
     *
     * @private
     */
    private handleUpdateWorkflowAction(): void {
        // With the identifier, we always get the latest workflow actions
        const pageIdentifier = this.uveStore.pageAPIResponse().page.identifier;
        this.uveStore.getWorkflowActions(pageIdentifier);
    }

    /**
     * Handles the content search select event
     *
     * This event is trigger when the user select a contentlet in the content search
     * @private
     * @param {EventDetail} detail - The event detail
     * @param {ActionPayload} actionPayload - The action payload
     */
    private handleContentSearchSelect(detail: EventDetail, actionPayload: ActionPayload): void {
        const payload = {
            ...actionPayload,
            newContentletId: detail.data.identifier
        };

        // If you see this, remind me to test this case in Headless and traditional pages
        // Not sure if `this.uveStore.setUveStatus(UVE_STATUS.LOADED);` is needed here
        this.handleContentletAdded(payload);
    }

    /**
     * Handles the create contentlet event
     *
     * This event is trigger when the user create a new contentlet
     * @private
     * @param {EventDetail} detail - The event detail
     * @param {ActionPayload} actionPayload - The action payload
     */
    private handleCreateContentlet(detail: EventDetail, actionPayload: ActionPayload): void {
        this.#dialogService.createContentlet({
            contentType: detail.data.contentType,
            url: detail.data.url,
            actionPayload
        });

        this.#cd.detectChanges();
    }

    /**
     * Handles the cancel saving menu order event
     *
     * @private
     */
    private handleCancelSavingMenuOrder(): void {
        this.#dialogService.resetDialog();
        this.#cd.detectChanges();
    }

    /**
     * Handles the save menu order event
     *
     * This event is trigger when the user attempt to reorder the navigation menu
     * @private
     */
    private handleSaveMenuOrder(): void {
        this.#messageService.add({
            severity: 'success',
            summary: this.#dotMessageService.get('editpage.content.contentlet.menu.reorder.title'),
            detail: this.#dotMessageService.get('message.menu.reordered'),
            life: 2000
        });

        this.uveStore.reloadCurrentPage();
        this.#dialogService.resetDialog();
    }

    /**
     * Handles the language changed event
     *
     * This event is trigger when the user change the language of the page
     * @private
     * @param {CustomEvent} event - The event
     */
    private handleLanguageChanged(event: CustomEvent): void {
        const htmlPageReferer = event.detail.payload?.htmlPageReferer;
        const url = new URL(htmlPageReferer, window.location.origin); // Add base for relative URLs
        const targetUrl = getTargetUrl(url.pathname, this.uveStore.pageAPIResponse().urlContentMap);
        // CHECK THIS PARAMETER
        const language_id = url.searchParams.get('com.dotmarketing.htmlpage.language');

        if (shouldNavigate(targetUrl, this.uveStore.pageParams().url)) {
            // Navigate to the new URL if it's different from the current one
            this.uveStore.loadPageAsset({ url: targetUrl, language_id });

            return;
        }

        this.uveStore.loadPageAsset({
            language_id
        });
    }

    /**
     * Handles the error saving menu order event
     *
     * @private
     */
    private handleErrorSavingMenuOrder(): void {
        this.#messageService.add({
            severity: 'error',
            summary: this.#dotMessageService.get('editpage.content.contentlet.menu.reorder.title'),
            detail: this.#dotMessageService.get('error.menu.reorder.user_has_not_permission'),
            life: 2000
        });
    }

    /**
     * Handles the form selected event
     *
     * @private
     * @param {EventDetail} detail - The event detail
     * @param {ActionPayload} actionPayload - The action payload
     */
    private handleFormSelected(detail: EventDetail, actionPayload: ActionPayload): void {
        const formId = detail.data.identifier;

        this.#dotPageApiService
            .getFormIndetifier(actionPayload.container.identifier, formId)
            .pipe(
                tap(() => {
                    this.uveStore.setUveStatus(UVE_STATUS.LOADING);
                }),
                map((newFormId: string) => {
                    return {
                        ...actionPayload,
                        newContentletId: newFormId
                    };
                }),
                catchError(() => EMPTY),
                take(1)
            )
            .subscribe((response) => {
                this.handleContentletAdded(response);
            });
    }

    /**
     * Handles the save page event
     *
     * @private
     * @param {CustomEvent} event - The event
     * @param {ActionPayload} actionPayload - The action payload
     * @param {string} clientAction - The client action
     */
    private handleSavePage(
        event: CustomEvent,
        actionPayload: ActionPayload,
        clientAction: string
    ): void {
        const { shouldReloadPage, contentletIdentifier } = event.detail.payload ?? {};
        const pageIdentifier = this.uveStore.pageAPIResponse().page.identifier;
        const isGraphqlPage = !!this.uveStore.graphql();

        if (shouldReloadPage) {
            this.reloadURLContentMapPage(contentletIdentifier);

            return;
        }

        if (clientAction === CLIENT_ACTIONS.EDIT_CONTENTLET || !isGraphqlPage) {
            this.notifyContentOutsidePageHasChanged();
        }

        if (contentletIdentifier === pageIdentifier || !actionPayload) {
            this.handleReloadPage(event);

            return;
        }

        this.handleContentSave(event, actionPayload);
    }

    /**
     * Handles the save page event triggered from the dialog.
     *
     * @param {CustomEvent} event - The event object containing details about the save action.
     * @return {void}
     */
    private handleReloadPage(event: CustomEvent): void {
        // Move this to the GetTargetUrl function
        const htmlPageReferer = event.detail.payload?.htmlPageReferer;
        const url = new URL(htmlPageReferer, window.location.origin);
        const targetUrl = getTargetUrl(url.pathname, this.uveStore.pageAPIResponse().urlContentMap);
        // END

        if (shouldNavigate(targetUrl, this.uveStore.pageParams().url)) {
            // Navigate to the new URL if it's different from the current one
            this.uveStore.loadPageAsset({ url: targetUrl });

            return;
        }

        this.uveStore.reloadCurrentPage();
    }

    /**
     * Handles saving content
     *
     * @private
     * @param {CustomEvent} event - The event
     * @param {ActionPayload} actionPayload - The action payload
     */
    private handleContentSave(event: CustomEvent, actionPayload: ActionPayload): void {
        const newContentletId = event.detail.payload?.newContentletId ?? '';

        const payload = {
            ...actionPayload,
            newContentletId
        };

        // If you see this, remind me to test this case in Headless and traditional pages
        // Not sure if `this.uveStore.setUveStatus(UVE_STATUS.LOADED);` is needed here
        this.handleContentletAdded(payload);
    }

    /**
     * Handles duplicated contentlet message
     *
     * @private
     */
    private handleDuplicatedContentlet(): void {
        this.#messageService.add({
            severity: 'info',
            summary: this.#dotMessageService.get('editpage.content.add.already.title'),
            detail: this.#dotMessageService.get('editpage.content.add.already.message'),
            life: 2000
        });

        this.#dialogService.resetDialog();
        this.uveStore.resetEditorProperties();
        this.uveStore.setUveStatus(UVE_STATUS.LOADED);
    }

    /**
     * Reload the URL content map page
     *
     * @private
     * @param {string} inodeOrIdentifier
     * @memberof EditEmaEditorComponent
     */
    private reloadURLContentMapPage(inodeOrIdentifier: string): void {
        // Set loading state to prevent the user to interact with the iframe
        this.uveStore.setUveStatus(UVE_STATUS.LOADING);

        this.#dotContentletService
            .getContentletByInode(inodeOrIdentifier)
            .pipe(
                catchError((error) => this.handlerError(error)),
                filter((contentlet) => !!contentlet)
            )
            .subscribe(({ URL_MAP_FOR_CONTENT }) => {
                if (URL_MAP_FOR_CONTENT != this.uveStore.pageParams().url) {
                    // If the URL is different, we need to navigate to the new URL
                    this.uveStore.loadPageAsset({ url: URL_MAP_FOR_CONTENT });

                    return;
                }

                // If the URL is the same, we need to fetch the new page data
                this.uveStore.reloadCurrentPage();
            });
    }

    /**
     * Handle the error
     *
     * @private
     * @param {HttpErrorResponse} error
     * @return {*}
     * @memberof EditEmaEditorComponent
     */
    private handlerError(error: HttpErrorResponse) {
        this.uveStore.setEditorState(EDITOR_STATE.ERROR);

        return this.#dotHttpErrorManagerService.handle(error).pipe(map(() => null));
    }

    /**
     * Notify that content outside the page has changed
     *
     * @private
     */
    private notifyContentOutsidePageHasChanged(): void {
        // this.contentWindow?.postMessage(
        //     { name: __DOTCMS_UVE_EVENT__.UVE_RELOAD_PAGE },
        //     "*"
        // );
    }

    private handleContentletAdded(actionPayload: ActionPayload): void {
        const { pageContainers, didInsert } = insertContentletInContainer(actionPayload);

        if (!didInsert) {
            this.handleDuplicatedContentlet();
        }

        this.uveStore.savePage(pageContainers);
    }
}
