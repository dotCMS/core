import { tapResponse } from '@ngrx/operators';
import { Observable, of } from 'rxjs';

import { inject, Injectable } from '@angular/core';

import { MessageService } from 'primeng/api';

import { switchMap, take, tap } from 'rxjs/operators';

import { DotMessageService, DotWorkflowActionsFireService } from '@dotcms/data-access';
import { DotCMSContentlet, DotTreeNode } from '@dotcms/dotcms-models';
import {
    DotCMSInlineEditingPayload,
    DotCMSInlineEditingType,
    DotCMSUVEAction
} from '@dotcms/types';
import { __DOTCMS_UVE_EVENT__, StyleEditorFormSchema } from '@dotcms/types/internal';
import { DotCopyContentModalService } from '@dotcms/ui';

import { DotBlockEditorSidebarComponent } from '../../components/dot-block-editor-sidebar/dot-block-editor-sidebar.component';
import { DotEmaDialogComponent } from '../../components/dot-ema-dialog/dot-ema-dialog.component';
import {
    ClientContentletArea,
    Container,
    InlineEditingContentletDataset,
    UpdatedContentlet
} from '../../edit-ema-editor/components/ema-page-dropzone/types';
import { DEFAULT_PERSONA, PERSONA_KEY } from '../../shared/consts';
import { EDITOR_STATE, UVE_STATUS } from '../../shared/enums';
import { PostMessage, ReorderMenuPayload, SetUrlPayload } from '../../shared/models';
import { UVEStore } from '../../store/dot-uve.store';
import { PageType } from '../../store/models';
import {
    compareUrlPaths,
    convertClientParamsToPageParams,
    createReorderMenuURL,
    isSamePageNavigation
} from '../../utils';
import { InlineEditService } from '../inline-edit/inline-edit.service';

export interface ActionsHandlerDependencies {
    uveStore: InstanceType<typeof UVEStore>;
    dialog: DotEmaDialogComponent;
    blockSidebar?: DotBlockEditorSidebarComponent;
    inlineEditingService: InlineEditService;
    contentWindow: Window | null;
    host: string;
    onCopyContent: (currentTreeNode: DotTreeNode) => Observable<DotCMSContentlet>;
    /** Called when the iframe reports the offsetTop of a section so the editor can scroll to it. */
    onSectionOffset?: (payload: { sectionIndex: number; offsetTop: number }) => void;
}

@Injectable()
export class DotUveActionsHandlerService {
    private readonly dotMessageService = inject(DotMessageService);
    private readonly messageService = inject(MessageService);
    private readonly dotCopyContentModalService = inject(DotCopyContentModalService);
    private readonly dotWorkflowActionsFireService = inject(DotWorkflowActionsFireService);

    handleAction({ action, payload }: PostMessage, deps: ActionsHandlerDependencies): void {
        const {
            uveStore,
            dialog,
            inlineEditingService,
            contentWindow,
            host,
            onCopyContent,
            onSectionOffset
        } = deps;

        const CLIENT_ACTIONS_FUNC_MAP: Record<DotCMSUVEAction, (payload: unknown) => void> = {
            [DotCMSUVEAction.NAVIGATION_UPDATE]: (payload: SetUrlPayload) => {
                const currentPageUrl = uveStore.pageParams()?.url;
                const incomingUrl = payload.url;

                // Same pathname (any hash/query): let the client handle it; do not pageLoad
                if (isSamePageNavigation(incomingUrl, currentPageUrl)) {
                    return;
                }

                const isSameUrl = compareUrlPaths(currentPageUrl, incomingUrl);

                if (isSameUrl) {
                    uveStore.setEditorState(EDITOR_STATE.IDLE);
                } else {
                    uveStore.pageLoad({
                        url: payload.url,
                        [PERSONA_KEY]: DEFAULT_PERSONA.identifier
                    });
                }
            },
            [DotCMSUVEAction.SET_BOUNDS]: (payload: Container[]) => {
                // The store's `withSelectionAnchor` slice owns the
                // re-anchor logic: patches editorBounds, looks up the
                // selected contentlet by inode+container key, updates
                // editorSelected with fresh coords, and
                // releases the iframe-layout lock if it was held.
                uveStore.applyBoundsForSelection(payload);
            },
            [DotCMSUVEAction.SET_CONTENTLET]: (coords: ClientContentletArea | null) => {
                // SDK signals "no hover" with a null payload when the pointer
                // leaves the last hovered contentlet onto dead space (or out
                // of the document entirely). Clear the hover overlay so it
                // doesn't linger.
                if (!coords) {
                    uveStore.resetContentletArea();
                    return;
                }

                const actionPayload = uveStore.getPageSavePayload(coords.payload);

                uveStore.setContentletArea({
                    x: coords.x,
                    y: coords.y,
                    width: coords.width,
                    height: coords.height,
                    payload: actionPayload
                });
            },
            [DotCMSUVEAction.SET_SELECTED_CONTENTLET]: (coords: ClientContentletArea) => {
                // The user clicked a contentlet inside the iframe. Bounds
                // and payload travel together in the unified `editorSelected`
                // record — one write drives both the floating overlay and
                // the side panel's data binding.
                const actionPayload = uveStore.getPageSavePayload(coords.payload);
                uveStore.setSelected({
                    bounds: {
                        x: coords.x,
                        y: coords.y,
                        width: coords.width,
                        height: coords.height
                    },
                    payload: actionPayload
                });
            },
            [DotCMSUVEAction.IFRAME_SCROLL]: () => {
                uveStore.updateEditorScrollState();
            },
            [DotCMSUVEAction.IFRAME_SCROLL_END]: () => {
                // No-op on purpose. We used to flip IDLE here, but that
                // races SET_BOUNDS — the overlay would un-hide before the
                // re-anchored coords arrived, causing a visible jump from
                // stale-position to correct-position. Now SET_BOUNDS itself
                // flips IDLE once the new bounds are patched, guaranteeing
                // the overlay reappears at the right spot.
            },
            [DotCMSUVEAction.COPY_CONTENTLET_INLINE_EDITING]: (payload: {
                dataset: InlineEditingContentletDataset;
            }) => {
                const contentArea = uveStore.editorContentArea();
                const { contentlet, container } = contentArea.payload;

                // Move focus to an inline field that has already cleared (or does
                // not need) the copy/edit decision: headless via postMessage,
                // traditional via TinyMCE init.
                const focusInlineField = (data: {
                    oldInode: string;
                    inode: string;
                    fieldName: string;
                    mode: string;
                    language: string;
                }) => {
                    if (uveStore.pageType() === PageType.HEADLESS) {
                        contentWindow?.postMessage(
                            {
                                name: __DOTCMS_UVE_EVENT__.UVE_COPY_CONTENTLET_INLINE_EDITING_SUCCESS,
                                payload: data
                            },
                            host
                        );

                        return;
                    }

                    inlineEditingService.setTargetInlineMCEDataset(data);
                    inlineEditingService.initEditor();
                };

                if (uveStore.editorState() === EDITOR_STATE.INLINE_EDITING) {
                    // Already inline-editing. When the click targets another field
                    // on the SAME contentlet, the copy/edit decision was already
                    // made for it — just move focus to the new field instead of
                    // re-opening the dialog. Without this the guard silently drops
                    // the click and the user cannot edit any other field on a
                    // content that has many inline-editable fields.
                    if (contentlet?.inode === payload.dataset.inode) {
                        focusInlineField({
                            oldInode: payload.dataset.inode,
                            inode: payload.dataset.inode,
                            fieldName: payload.dataset.fieldName,
                            mode: payload.dataset.mode,
                            language: payload.dataset.language
                        });
                    }

                    return;
                }

                const currentTreeNode = uveStore.getCurrentTreeNode(container, contentlet);

                this.dotCopyContentModalService
                    .open()
                    .pipe(
                        switchMap(({ shouldCopy }) => {
                            if (!shouldCopy) {
                                return of(null);
                            }

                            return onCopyContent(currentTreeNode);
                        }),
                        tap((res) => {
                            uveStore.setEditorState(EDITOR_STATE.INLINE_EDITING);

                            if (res) {
                                uveStore.pageReload();
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

                        if (uveStore.pageType() === PageType.HEADLESS) {
                            const message = {
                                name: __DOTCMS_UVE_EVENT__.UVE_COPY_CONTENTLET_INLINE_EDITING_SUCCESS,
                                payload: data
                            };

                            contentWindow?.postMessage(message, host);
                            return;
                        }

                        inlineEditingService.setTargetInlineMCEDataset(data);

                        if (!res) {
                            inlineEditingService.initEditor();
                        }
                    });
            },
            [DotCMSUVEAction.UPDATE_CONTENTLET_INLINE_EDITING]: (payload: UpdatedContentlet) => {
                uveStore.setEditorState(EDITOR_STATE.IDLE);

                if (!payload) {
                    return;
                }

                const dataset = payload.dataset;

                const contentlet = {
                    inode: dataset['inode'],
                    [dataset.fieldName]: payload.content
                };

                uveStore.setUveStatus(UVE_STATUS.LOADING);
                this.dotWorkflowActionsFireService
                    .saveContentlet({
                        ...contentlet,
                        indexPolicy: 'WAIT_FOR',
                        variantName: uveStore.pageVariantId()
                    })
                    .pipe(
                        take(1),
                        tapResponse({
                            next: () => {
                                this.messageService.add({
                                    severity: 'success',
                                    summary: this.dotMessageService.get('message.content.saved'),
                                    detail: this.dotMessageService.get(
                                        'message.content.note.already.published'
                                    ),
                                    life: 2000
                                });
                            },
                            error: (e) => {
                                console.error(e);
                                this.messageService.add({
                                    severity: 'error',
                                    summary: this.dotMessageService.get(
                                        'editpage.content.update.contentlet.error'
                                    ),
                                    life: 2000
                                });
                            }
                        })
                    )
                    .subscribe(() => uveStore.pageReload());
            },
            [DotCMSUVEAction.CLIENT_READY]: (devConfig: {
                graphql: {
                    query: string;
                    variables: Record<string, unknown>;
                };
                params: Record<string, unknown>;
                query: string;
            }) => {
                const isClientReady = uveStore.isClientReady();

                const { graphql, params } = devConfig || {};
                const { query, variables } = graphql || {};

                // Always refresh the stored client request — it is page-scoped.
                // When the app re-announces itself on a client-side route change,
                // the new page's CLIENT_READY arrives right before its
                // NAVIGATION_UPDATE; installing the config here lets the upcoming
                // pageLoad fetch the new page with its own query/variables instead
                // of the previous page's.
                if (query) {
                    uveStore.setCustomClient({
                        query,
                        variables: (variables ?? {}) as Record<string, string>
                    });
                }

                if (isClientReady) {
                    // Already initialized: don't reload. The navigation (if any)
                    // drives the fetch; a duplicate CLIENT_READY is a no-op.
                    return;
                }

                const pageParams = convertClientParamsToPageParams(params);

                uveStore.pageReload(pageParams);
                uveStore.setIsClientReady(true);
            },
            [DotCMSUVEAction.EDIT_CONTENTLET]: (contentlet: DotCMSContentlet) => {
                dialog.editContentlet({ ...contentlet, clientAction: action });
            },
            /**
             * Handles the `CREATE_CONTENTLET` postMessage action sent by `window.dotUVE.createContentlet()`
             * (or `createContentlet()` from `@dotcms/uve` in headless setups).
             *
             * Opens the contentlet creation dialog for the given content type variable.
             * The newly created contentlet is saved to the system but is NOT inserted
             * into the page layout — the page simply reloads after save. This supports
             * use cases like widgets that pull content automatically (e.g. an events widget).
             *
             * @param {string} contentType - The content type variable (e.g. `'Event'`)
             * @see {DotCMSUVEAction.CREATE_CONTENTLET}
             */
            [DotCMSUVEAction.CREATE_CONTENTLET]: ({ contentType }: { contentType: string }) => {
                dialog.createContentletFromPalette({
                    variable: contentType,
                    name: contentType,
                    language_id: uveStore.pageLanguageId()
                });
            },
            [DotCMSUVEAction.REORDER_MENU]: ({ startLevel, depth }: ReorderMenuPayload) => {
                const urlObject = createReorderMenuURL({
                    startLevel,
                    depth,
                    pagePath: uveStore.pageParams().url,
                    hostId: uveStore.pageAsset()?.site?.identifier
                });

                dialog.openDialogOnUrl(
                    urlObject,
                    this.dotMessageService.get('editpage.content.contentlet.menu.reorder.title')
                );
            },
            [DotCMSUVEAction.INIT_INLINE_EDITING]: (payload: {
                type: DotCMSInlineEditingType;
                data?: DotCMSInlineEditingPayload;
            }) => {
                this.handleInlineEditingEvent(payload, deps);
            },
            [DotCMSUVEAction.REGISTER_STYLE_SCHEMAS]: (payload: {
                schemas: StyleEditorFormSchema[];
            }) => {
                const { schemas } = payload;
                uveStore.setStyleSchemas(schemas);
            },
            [DotCMSUVEAction.NOOP]: () => {
                /* Do Nothing because is not the origin we are expecting */
            },
            [DotCMSUVEAction.PING_EDITOR]: () => {
                /* Ping editor - no action needed */
            },
            [DotCMSUVEAction.GET_PAGE_DATA]: () => {
                /* Get page data - handled by bridge service */
            },
            [DotCMSUVEAction.IFRAME_HEIGHT]: () => {
                /* No-op. Iframe height is editor-controlled, not page-controlled. */
            },
            [DotCMSUVEAction.SECTION_OFFSET]: (payload: {
                sectionIndex: number;
                offsetTop: number;
            }) => {
                onSectionOffset?.(payload);
            }
        };

        const actionToExecute = CLIENT_ACTIONS_FUNC_MAP[action];
        actionToExecute?.(payload);
    }

    private handleInlineEditingEvent(
        { type, data }: { type: DotCMSInlineEditingType; data?: DotCMSInlineEditingPayload },
        deps: ActionsHandlerDependencies
    ): void {
        const { uveStore, blockSidebar, inlineEditingService } = deps;

        // Note: Enterprise check should be done by caller if needed
        switch (type) {
            case 'BLOCK_EDITOR':
                blockSidebar?.open(data);
                break;

            case 'WYSIWYG':
                inlineEditingService.initEditor();
                uveStore.setEditorState(EDITOR_STATE.INLINE_EDITING);
                break;

            default:
                console.warn('Unknown block editor type', type);
                break;
        }
    }
}
