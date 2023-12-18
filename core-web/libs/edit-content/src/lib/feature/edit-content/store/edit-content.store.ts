import { ComponentStore, tapResponse } from '@ngrx/component-store';
import { Observable, forkJoin, of } from 'rxjs';

import { Location } from '@angular/common';
import { Injectable, inject } from '@angular/core';

import { MessageService } from 'primeng/api';

import { switchMap, tap } from 'rxjs/operators';

import {
    DotFireActionOptions,
    DotMessageService,
    DotRenderMode,
    DotWorkflowActionsFireService,
    DotWorkflowsActionsService
} from '@dotcms/data-access';
import { DotCMSContentType, DotCMSContentlet, DotCMSWorkflowAction } from '@dotcms/dotcms-models';

import { DotEditContentService } from '../../../services/dot-edit-content.service';

interface EditContentState {
    actions: DotCMSWorkflowAction[];
    contentType: DotCMSContentType;
    contentlet: DotCMSContentlet;
}

const initialState: EditContentState = {
    actions: [],
    contentType: null,
    contentlet: null
};

/**
 * Temporary store to handle the edit content page state
 * until we have a proper store for the edit content page [https://github.com/dotCMS/core/issues/27022]
 *
 * @export
 * @class DotEditContentStore
 * @extends {ComponentStore<EditContentState>}
 */
@Injectable()
export class DotEditContentStore extends ComponentStore<EditContentState> {
    private readonly dotEditContentService = inject(DotEditContentService);
    private readonly workflowActionService = inject(DotWorkflowsActionsService);
    private readonly WorkflowActionsFireService = inject(DotWorkflowActionsFireService);

    private readonly messageService = inject(MessageService);
    private readonly dotMessageService = inject(DotMessageService);
    private readonly location = inject(Location);

    readonly vm$ = this.select(({ actions, contentType, contentlet }) => ({
        actions,
        contentType,
        contentlet
    }));

    constructor() {
        super(initialState);
    }

    readonly updateState = this.updater((state, newState: EditContentState) => ({
        ...state,
        ...newState
    }));

    readonly updateContentletAndActions = this.updater<{
        actions: DotCMSWorkflowAction[];
        contentlet: DotCMSContentlet;
    }>((state, { contentlet, actions }) => ({
        ...state,
        contentlet,
        actions
    }));

    readonly loadContentEffect = this.effect(
        (
            data$: Observable<{
                isNewContent: boolean;
                idOrVar: string;
            }>
        ) => {
            return data$.pipe(
                switchMap(({ isNewContent, idOrVar }) => {
                    return isNewContent
                        ? this.getNewContent(idOrVar)
                        : this.getExistingContent(idOrVar);
                }),
                tap(({ contentType, actions, contentlet }: EditContentState) => {
                    this.updateState({
                        contentType,
                        actions,
                        contentlet
                    });
                })
            );
        }
    );

    readonly fireWorkflowActionEffect = this.effect(
        (data$: Observable<DotFireActionOptions<{ [key: string]: string | object }>>) => {
            return data$.pipe(
                switchMap((options) => {
                    return this.fireWorkflowAction(options).pipe(
                        tapResponse(
                            ({ contentlet, actions }) => {
                                this.updateURL(contentlet.inode);
                                this.updateContentletAndActions({
                                    contentlet,
                                    actions
                                });

                                this.messageService.add({
                                    severity: 'success',
                                    summary: this.dotMessageService.get(
                                        'dot.common.message.success'
                                    ),
                                    detail: this.dotMessageService.get(
                                        'edit.content.fire.action.success'
                                    )
                                });
                            },
                            ({ error }) => {
                                this.messageService.add({
                                    severity: 'error',
                                    summary: this.dotMessageService.get('dot.common.message.error'),
                                    detail: error.message
                                });
                            }
                        )
                    );
                })
            );
        }
    );

    private getNewContent(contentTypeVar: string) {
        return forkJoin({
            contentType: this.dotEditContentService.getContentType(contentTypeVar),
            actions: this.workflowActionService.getDefaultActions(contentTypeVar),
            contentlet: of(null)
        });
    }

    private getExistingContent(inode) {
        return this.dotEditContentService.getContentById(inode).pipe(
            switchMap((contentlet) => {
                const { contentType } = contentlet;

                return forkJoin({
                    contentType: this.dotEditContentService.getContentType(contentType),
                    actions: this.workflowActionService.getByInode(inode, DotRenderMode.EDITING),
                    contentlet: of(contentlet)
                });
            })
        );
    }

    private fireWorkflowAction(
        options: DotFireActionOptions<{ [key: string]: string | object }>
    ): Observable<{
        actions: DotCMSWorkflowAction[];
        contentlet: DotCMSContentlet;
    }> {
        return this.WorkflowActionsFireService.fireTo(options).pipe(
            switchMap((contentlet) => {
                return forkJoin({
                    actions: this.workflowActionService.getByInode(
                        contentlet.inode,
                        DotRenderMode.EDITING
                    ),
                    contentlet: of(contentlet)
                });
            })
        );
    }

    private updateURL(inode: string) {
        this.location.replaceState(`/content/${inode}`); // Replace the URL with the new inode without reloading the page
    }
}
