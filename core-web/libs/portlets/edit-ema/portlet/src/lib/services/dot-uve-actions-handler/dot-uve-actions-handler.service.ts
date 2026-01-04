import { tapResponse } from '@ngrx/operators';
import { Observable, of } from 'rxjs';

import { Injectable, inject } from '@angular/core';

import { MessageService } from 'primeng/api';

import { switchMap, take, tap } from 'rxjs/operators';


import {
    DotMessageService,
} from '@dotcms/data-access';
import { DotCMSContentlet, DotTreeNode } from '@dotcms/dotcms-models';
import {
    DotCMSInlineEditingPayload,
    DotCMSInlineEditingType,
    DotCMSUVEAction
} from '@dotcms/types';
import { __DOTCMS_UVE_EVENT__ } from '@dotcms/types/internal';
import { DotCopyContentModalService } from '@dotcms/ui';
import { StyleEditorFormSchema } from '@dotcms/uve';

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
    createReorderMenuURL
} from '../../utils';
import { DotPageApiService } from '../dot-page-api.service';
import { InlineEditService } from '../inline-edit/inline-edit.service';

export interface ActionsHandlerDependencies {
    uveStore: InstanceType<typeof UVEStore>;
    dialog: DotEmaDialogComponent;
    blockSidebar?: DotBlockEditorSidebarComponent;
    inlineEditingService: InlineEditService;
    dotPageApiService: DotPageApiService;
    contentWindow: Window | null;
    host: string;
    onCopyContent: (currentTreeNode: DotTreeNode) => Observable<DotCMSContentlet>;
}

@Injectable()
export class DotUveActionsHandlerService {
    private readonly dotMessageService = inject(DotMessageService);
    private readonly messageService = inject(MessageService);
    private readonly dotCopyContentModalService = inject(DotCopyContentModalService);

    handleAction(
        { action, payload }: PostMessage,
        deps: ActionsHandlerDependencies
    ): void {
        const {
            uveStore,
            dialog,
            inlineEditingService,
            dotPageApiService,
            contentWindow,
            host,
            onCopyContent
        } = deps;

        const CLIENT_ACTIONS_FUNC_MAP: Record<
            DotCMSUVEAction,
            (payload: unknown) => void
        > = {
            [DotCMSUVEAction.NAVIGATION_UPDATE]: (payload: SetUrlPayload) => {
                const isSameUrl = compareUrlPaths(uveStore.pageParams()?.url, payload.url);

                if (isSameUrl) {
                    uveStore.setEditorState(EDITOR_STATE.IDLE);
                } else {
                    uveStore.loadPageAsset({
                        url: payload.url,
                        [PERSONA_KEY]: DEFAULT_PERSONA.identifier
                    });
                }
            },
            [DotCMSUVEAction.SET_BOUNDS]: (payload: Container[]) => {
                uveStore.setEditorBounds(payload);
            },
            [DotCMSUVEAction.SET_CONTENTLET]: (coords: ClientContentletArea) => {
                const actionPayload = uveStore.getPageSavePayload(coords.payload);

                uveStore.setContentletArea({
                    x: coords.x,
                    y: coords.y,
                    width: coords.width,
                    height: coords.height,
                    payload: actionPayload
                });
            },
            [DotCMSUVEAction.IFRAME_SCROLL]: () => {
                uveStore.updateEditorScrollState();
            },
            [DotCMSUVEAction.IFRAME_SCROLL_END]: () => {
                uveStore.updateEditorOnScrollEnd();
            },
            [DotCMSUVEAction.COPY_CONTENTLET_INLINE_EDITING]: (payload: {
                dataset: InlineEditingContentletDataset;
            }) => {
                if (uveStore.editor().state === EDITOR_STATE.INLINE_EDITING) {
                    return;
                }

                const { contentlet, container } = uveStore.editor().contentArea.payload;
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
                                uveStore.reloadCurrentPage();
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
                dotPageApiService
                    .saveContentlet({ contentlet })
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
                    .subscribe(() => uveStore.reloadCurrentPage());
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

                if (isClientReady) {
                    return;
                }

                const { graphql, params, query: rawQuery } = devConfig || {};
                const { query = rawQuery, variables } = graphql || {};
                const legacyGraphqlResponse = !!rawQuery;

                if (query || rawQuery) {
                    uveStore.setCustomGraphQL({ query, variables }, legacyGraphqlResponse);
                }

                const pageParams = convertClientParamsToPageParams(params);
                uveStore.reloadCurrentPage(pageParams);
                uveStore.setIsClientReady(true);
            },
            [DotCMSUVEAction.EDIT_CONTENTLET]: (contentlet: DotCMSContentlet) => {
                dialog.editContentlet({ ...contentlet, clientAction: action });
            },
            [DotCMSUVEAction.REORDER_MENU]: ({ startLevel, depth }: ReorderMenuPayload) => {
                const urlObject = createReorderMenuURL({
                    startLevel,
                    depth,
                    pagePath: uveStore.pageParams().url,
                    hostId: uveStore.site().identifier
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

