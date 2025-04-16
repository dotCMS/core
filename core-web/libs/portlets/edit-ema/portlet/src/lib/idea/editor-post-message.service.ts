/* eslint-disable @typescript-eslint/no-unused-vars */
/* eslint-disable @typescript-eslint/no-explicit-any */
import { Observable, of } from 'rxjs';

import { inject, Injectable } from '@angular/core';

import { MessageService } from 'primeng/api';

import { catchError, filter, map, switchMap, tap } from 'rxjs/operators';

import { CLIENT_ACTIONS, INLINE_EDITING_EVENT_KEY, InlineEditEventData } from '@dotcms/client';
import {
    DotAlertConfirmService,
    DotCopyContentService,
    DotHttpErrorManagerService,
    DotMessageService
} from '@dotcms/data-access';
import { DotCMSContentlet, DotTreeNode } from '@dotcms/dotcms-models';
import { DotCopyContentModalService } from '@dotcms/ui';
import { __DOTCMS_UVE_EVENT__ } from '@dotcms/uve/internal';

import { DotEditorDialogService } from '../components/dot-ema-dialog/services/dot-ema-dialog.service';
import { DotPageApiService } from '../services/dot-page-api.service';
import { InlineEditService } from '../services/inline-edit/inline-edit.service';
import { DEFAULT_PERSONA } from '../shared/consts';
import { EDITOR_STATE, UVE_STATUS } from '../shared/enums';
import { UVEStore } from '../store/dot-uve.store';
import { compareUrlPaths, createReorderMenuURL } from '../utils';

// editor-post-message.service.ts
@Injectable({
    providedIn: 'root'
})
export class HandleClientActionsService {
    protected readonly uveStore = inject(UVEStore);
    protected readonly messageService = inject(MessageService);
    protected readonly dotPageApiService = inject(DotPageApiService);
    protected readonly dotMessageService = inject(DotMessageService);
    protected readonly dialogService = inject(DotEditorDialogService);
    protected readonly dotCopyContentModalService = inject(DotCopyContentModalService);
    protected readonly inlineEditingService = inject(InlineEditService);
    protected readonly dotCopyContentService = inject(DotCopyContentService);
    protected readonly dotHttpErrorManagerService = inject(DotHttpErrorManagerService);
    protected readonly dotAlertConfirmService = inject(DotAlertConfirmService);

    protected readonly NOTIFICATION_MESSAGES = {
        SUCCESS: {
            severity: 'success',
            summary: this.dotMessageService.get('message.content.saved')
        },
        ERROR: {
            severity: 'error',
            summary: this.dotMessageService.get('editpage.content.update.contentlet.error')
        }
    };

    /**
     * Processes commands received from the client
     *
     * @param command Object containing action type and payload
     */
    handleClientAction({ action, payload }: { action: string; payload: any }): void {
        const clientCommandHandler = this.getClientCommandHandlers()[action];
        clientCommandHandler?.(payload);
    }

    /**
     * Returns a map of client actions to their handler functions
     */
    private getClientCommandHandlers(): Record<string, (payload: any) => void> {
        return {
            [CLIENT_ACTIONS.REORDER_MENU]: this.handleReorderMenu.bind(this),
            [CLIENT_ACTIONS.NAVIGATION_UPDATE]: this.handleNavigationUpdate.bind(this),
            [CLIENT_ACTIONS.SET_BOUNDS]: this.handleSetBounds.bind(this),
            [CLIENT_ACTIONS.SET_CONTENTLET]: this.handleSetContentlet.bind(this),
            [CLIENT_ACTIONS.IFRAME_SCROLL]: this.handleIframeScroll.bind(this),
            [CLIENT_ACTIONS.IFRAME_SCROLL_END]: this.handleIframeScrollEnd.bind(this),
            [CLIENT_ACTIONS.COPY_CONTENTLET_INLINE_EDITING]:
                this.handleCopyContentletInlineEditing.bind(this),
            [CLIENT_ACTIONS.UPDATE_CONTENTLET_INLINE_EDITING]:
                this.handleUpdateContentletInlineEditing.bind(this),
            [CLIENT_ACTIONS.CLIENT_READY]: this.handleClientReady.bind(this),
            [CLIENT_ACTIONS.EDIT_CONTENTLET]: this.handleEditContentlet.bind(this),
            [CLIENT_ACTIONS.INIT_INLINE_EDITING]: this.handleInitInlineEditing.bind(this)
        };
    }

    /**
     * Handles menu reordering
     */
    private handleReorderMenu({ startLevel, depth }: any): void {
        const url = createReorderMenuURL({
            startLevel,
            depth,
            pagePath: this.uveStore.pageAPIResponse().page?.pageURI,
            hostId: this.uveStore.pageAPIResponse().site.identifier
        });
        const title = this.dotMessageService.get('editpage.content.contentlet.menu.reorder.title');

        console.warn(url);

        this.dialogService.openDialogOnURL({
            url,
            title
        });
    }

    /**
     * Handles navigation updates
     */
    private handleNavigationUpdate(payload: any): void {
        const isSameUrl = compareUrlPaths(
            this.uveStore.pageAPIResponse().page?.pageURI,
            payload.url
        );

        if (isSameUrl) {
            this.uveStore.setEditorState(EDITOR_STATE.IDLE);

            return;
        }

        this.uveStore.loadPageAsset({
            url: payload.url,
            params: {
                personaId: DEFAULT_PERSONA.identifier
            }
        });
    }

    /**
     * Handles setting editor bounds
     */
    private handleSetBounds(payload: any): void {
        this.uveStore.setEditorBounds(payload);
    }

    /**
     * Handles setting contentlet area
     */
    private handleSetContentlet(contentletArea: any): void {
        const payload = this.uveStore.getPageSavePayload(contentletArea.payload);

        this.uveStore.setEditorContentletArea({
            ...contentletArea,
            payload
        });
    }

    /**
     * Handles iframe scroll event
     */
    private handleIframeScroll(): void {
        this.uveStore.updateEditorScrollState();
    }

    /**
     * Handles iframe scroll end event
     */
    private handleIframeScrollEnd(): void {
        this.uveStore.updateEditorOnScrollEnd();
    }

    /**
     * Handles copying contentlet for inline editing
     */
    private handleCopyContentletInlineEditing(payload: any): void {
        this.dotCopyContentModalService
            .open()
            .pipe(
                switchMap(({ shouldCopy }) => {
                    if (!shouldCopy) {
                        return of(null);
                    }

                    const { contentlet, container } = this.uveStore.contentletArea().payload;
                    const currentTreeNode = this.getContentTreeNode(container, contentlet);

                    return this.processCopyContent(currentTreeNode);
                }),
                tap((res) => {
                    this.uveStore.setEditorState(EDITOR_STATE.INLINE_EDITING);
                    if (res) {
                        this.uveStore.reloadCurrentPage();
                    }
                })
            )
            .subscribe((res: DotCMSContentlet | null) => {
                const data = {
                    oldInode: payload.dataset.inode,
                    inode: res?.inode || payload.dataset.inode,
                    fieldName: payload.dataset.fieldName,
                    mode: payload.dataset.mode,
                    language: payload.dataset.language
                };

                const message = {
                    name: __DOTCMS_UVE_EVENT__.UVE_COPY_CONTENTLET_INLINE_EDITING_SUCCESS,
                    payload: data
                };

                if (!this.uveStore.isTraditionalPage()) {
                    const iframe = document.querySelector('#uve-iframe') as HTMLIFrameElement;
                    iframe?.contentWindow?.postMessage(message, '*');

                    return;
                }

                this.inlineEditingService.setTargetInlineMCEDataset(data);

                if (!res) {
                    this.inlineEditingService.initEditor();
                }
            });
    }

    /**
     * Handles updating contentlet for inline editing
     */
    private async handleUpdateContentletInlineEditing(payload: any): Promise<void> {
        if (!payload) {
            console.warn('UPDATE_CONTENTLET_INLINE_EDITING: No payload found: ', payload);

            return;
        }

        const dataset = payload.dataset;
        const contentlet = {
            inode: dataset['inode'],
            [dataset.fieldName]: payload.content
        };

        await this.saveContentletChanges(contentlet);
    }

    /**
     * Handles client ready event
     */
    private handleClientReady(clientConfig: any): void {
        const { query, params } = clientConfig || {};
        const isClientReady = this.uveStore.isClientReady();

        // Frameworks Navigation triggers the client ready event, so we need to prevent it
        // Until we manually trigger the reload
        if (isClientReady) {
            return;
        }

        this.uveStore.setClientConfiguration({ query, params });
        this.uveStore.reloadCurrentPage();
    }

    /**
     * Handles edit contentlet event
     */
    private handleEditContentlet(contentlet: DotCMSContentlet): void {
        this.dialogService.editContentlet({
            ...contentlet,
            clientAction: CLIENT_ACTIONS.EDIT_CONTENTLET
        });
    }

    /**
     * Handles initializing inline editing
     */
    private handleInitInlineEditing(payload: any): void {
        this.processInlineEditingEvent(payload);
    }

    /**
     * Saves contentlet changes
     */
    private async saveContentletChanges(contentlet: any): Promise<void> {
        this.uveStore.setUveStatus(UVE_STATUS.LOADING);

        try {
            await this.dotPageApiService.saveContentlet({ contentlet }).toPromise();
            this.messageService.add(this.NOTIFICATION_MESSAGES.SUCCESS);
        } catch (error) {
            console.error('Error saving contentlet:', error);
            this.messageService.add(this.NOTIFICATION_MESSAGES.ERROR);
        } finally {
            this.uveStore.reloadCurrentPage();
        }
    }

    /**
     * Processes copy content operation
     */
    private processCopyContent(currentTreeNode: DotTreeNode): Observable<DotCMSContentlet> {
        return this.dotCopyContentService.copyInPage(currentTreeNode).pipe(
            catchError((error) =>
                this.dotHttpErrorManagerService.handle(error).pipe(
                    tap(() => this.dialogService.resetDialog()), // If there is an error, we set the status to idle
                    map(() => null)
                )
            ),
            filter((contentlet: DotCMSContentlet) => !!contentlet?.inode)
        );
    }

    /**
     * Processes the inline editing event
     */
    private processInlineEditingEvent({
        type,
        data
    }: {
        type: INLINE_EDITING_EVENT_KEY;
        data?: InlineEditEventData;
    }): void {
        if (!this.uveStore.isEnterprise()) {
            this.dotAlertConfirmService.alert({
                header: this.dotMessageService.get('dot.common.license.enterprise.only.error'),
                message: this.dotMessageService.get('editpage.not.lincese.error')
            });

            return;
        }

        switch (type) {
            case 'BLOCK_EDITOR':
                // this.blockSidebar?.open(data);
                break;

            case 'WYSIWYG':
                this.inlineEditingService.initEditor();
                this.uveStore.setEditorState(EDITOR_STATE.INLINE_EDITING);
                break;

            default:
                console.warn('Unknown block editor type', type);

                break;
        }
    }

    /**
     * Creates a tree node for content operations
     */
    private getContentTreeNode(container: any, contentlet: any): DotTreeNode {
        const contentId = contentlet.identifier;
        const containerId = container.identifier;
        const variantId = container.variantId;
        const relationType = container.uuid;
        const contentletsId = container.contentletsId;

        const treeOrder = contentletsId.findIndex((id) => id === contentId).toString();

        return {
            pageId: '12134',
            contentId,
            variantId,
            treeOrder,
            containerId,
            relationType,
            personalization: 'dot:default'
        };
    }
}
