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
import { DialogAction } from '../../shared/models';
import { UVEStore } from '../../store/dot-uve.store';
import { insertContentletInContainer, getTargetUrl, shouldNavigate } from '../../utils';
import { DotPageApiService } from '../dot-page-api.service';

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
     * @param {DialogAction} event - The event object containing details about the action.
     * @return {void}
     */
    public handleNgEvent({ event, actionPayload, clientAction }: DialogAction) {
        const { detail } = event;

        switch (event.detail.name) {
            case NG_CUSTOM_EVENTS.UPDATE_WORKFLOW_ACTION: {
                const pageIdentifier = this.uveStore.pageAPIResponse().page.identifier;
                this.uveStore.getWorkflowActions(pageIdentifier);
                break;
            }

            case NG_CUSTOM_EVENTS.CONTENT_SEARCH_SELECT: {
                const { pageContainers, didInsert } = insertContentletInContainer({
                    ...actionPayload,
                    newContentletId: detail.data.identifier
                });

                if (!didInsert) {
                    this.handleDuplicatedContentlet();

                    return;
                }

                this.uveStore.savePage(pageContainers);
                break;
            }

            case NG_CUSTOM_EVENTS.CREATE_CONTENTLET: {
                this.#dialogService.createContentlet({
                    contentType: detail.data.contentType,
                    url: detail.data.url,
                    actionPayload
                });

                this.#cd.detectChanges();
                break;
            }

            case NG_CUSTOM_EVENTS.CANCEL_SAVING_MENU_ORDER: {
                this.#dialogService.resetDialog();
                this.#cd.detectChanges();
                break;
            }

            case NG_CUSTOM_EVENTS.SAVE_MENU_ORDER: {
                this.#messageService.add({
                    severity: 'success',
                    summary: this.#dotMessageService.get(
                        'editpage.content.contentlet.menu.reorder.title'
                    ),
                    detail: this.#dotMessageService.get('message.menu.reordered'),
                    life: 2000
                });

                this.uveStore.reloadCurrentPage();
                this.#dialogService.resetDialog();
                break;
            }

            case NG_CUSTOM_EVENTS.LANGUAGE_IS_CHANGED: {
                const htmlPageReferer = event.detail.payload?.htmlPageReferer;
                const url = new URL(htmlPageReferer, window.location.origin); // Add base for relative URLs
                const targetUrl = getTargetUrl(
                    url.pathname,
                    this.uveStore.pageAPIResponse().urlContentMap
                );
                const language_id = url.searchParams.get('com.dotmarketing.htmlpage.language');

                if (shouldNavigate(targetUrl, this.uveStore.pageParams().url)) {
                    // Navigate to the new URL if it's different from the current one
                    this.uveStore.loadPageAsset({ url: targetUrl, language_id });

                    return;
                }

                this.uveStore.loadPageAsset({
                    language_id
                });

                break;
            }

            case NG_CUSTOM_EVENTS.ERROR_SAVING_MENU_ORDER: {
                this.#messageService.add({
                    severity: 'error',
                    summary: this.#dotMessageService.get(
                        'editpage.content.contentlet.menu.reorder.title'
                    ),
                    detail: this.#dotMessageService.get(
                        'error.menu.reorder.user_has_not_permission'
                    ),
                    life: 2000
                });

                break;
            }

            case NG_CUSTOM_EVENTS.FORM_SELECTED: {
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
                        const { pageContainers, didInsert } = insertContentletInContainer(response);

                        if (!didInsert) {
                            this.handleDuplicatedContentlet();
                            this.uveStore.setUveStatus(UVE_STATUS.LOADED);
                        } else {
                            this.uveStore.savePage(pageContainers);
                        }
                    });

                break;
            }

            case NG_CUSTOM_EVENTS.SAVE_PAGE: {
                const { shouldReloadPage, contentletIdentifier } = detail.payload ?? {};
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

                break;
            }
        }
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

    private handleContentSave(event: CustomEvent, actionPayload) {
        const newContentletId = event.detail.payload?.newContentletId ?? '';

        const { pageContainers, didInsert } = insertContentletInContainer({
            ...actionPayload,
            newContentletId
        });

        if (!didInsert) {
            this.handleDuplicatedContentlet();

            return;
        }

        this.uveStore.savePage(pageContainers);
    }

    private handleDuplicatedContentlet() {
        this.#messageService.add({
            severity: 'info',
            summary: this.#dotMessageService.get('editpage.content.add.already.title'),
            detail: this.#dotMessageService.get('editpage.content.add.already.message'),
            life: 2000
        });

        this.uveStore.resetEditorProperties();
        this.#dialogService.resetDialog();
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

    private notifyContentOutsidePageHasChanged() {
        // this.contentWindow?.postMessage(
        //     { name: __DOTCMS_UVE_EVENT__.UVE_RELOAD_PAGE },
        //     "*"
        // );
    }
}
