import { ComponentStore, tapResponse } from '@ngrx/component-store';
import { Observable, forkJoin, of } from 'rxjs';

import { Location } from '@angular/common';
import { Injectable, inject } from '@angular/core';
import { Router } from '@angular/router';

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

export const SIDEBAR_LOCAL_STORAGE_KEY = 'dot-edit-content-form-sidebar';

interface EditContentState {
    actions: DotCMSWorkflowAction[];
    contentType: DotCMSContentType;
    contentlet: DotCMSContentlet;
    loading: boolean;
    layout: {
        showSidebar: boolean;
    };
}

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
    private readonly router = inject(Router);

    private readonly workflowActionService = inject(DotWorkflowsActionsService);
    private readonly WorkflowActionsFireService = inject(DotWorkflowActionsFireService);

    private readonly messageService = inject(MessageService);
    private readonly dotMessageService = inject(DotMessageService);
    private readonly location = inject(Location);

    readonly vm$ = this.select(({ actions, contentType, contentlet, loading, layout }) => ({
        actions,
        contentType,
        contentlet,
        loading,
        layout
    }));

    readonly layout$ = this.select(({ layout }) => layout);

    /**
     * Update the state
     *
     * @memberof DotEditContentStore
     */
    readonly updateState = this.updater((state, newState: EditContentState) => ({
        ...state,
        ...newState
    }));

    /**
     * Update the sidebar state and save it in local storage.
     *
     * @memberof DotEditContentStore
     */
    readonly updateSidebarState = this.updater((state, showSidebar: boolean) => {
        localStorage.setItem(SIDEBAR_LOCAL_STORAGE_KEY, String(showSidebar));

        return {
            ...state,
            layout: {
                ...state.layout,
                showSidebar
            }
        };
    });

    /**
     * Update the loading state
     *
     * @memberof DotEditContentStore
     */
    readonly updateLoading = this.updater((state, loading: boolean) => ({
        ...state,
        loading
    }));

    /**
     * Update the contentlet and actions
     *
     * @memberof DotEditContentStore
     */
    readonly updateContentletAndActions = this.updater<{
        actions: DotCMSWorkflowAction[];
        contentlet: DotCMSContentlet;
    }>((state, { contentlet, actions }) => ({
        ...state,
        contentlet,
        actions,
        loading: false
    }));

    /**
     * Fire the workflow action and update the contentlet and actions
     *
     * @memberof DotEditContentStore
     */
    readonly fireWorkflowActionEffect = this.effect(
        (data$: Observable<DotFireActionOptions<{ [key: string]: string | object }>>) => {
            return data$.pipe(
                tap(() => this.updateLoading(true)),
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
                                this.updateLoading(false);
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

    /**
     * Fire the workflow action and update the contentlet and actions
     *
     * @private
     * @param {(DotFireActionOptions<{ [key: string]: string | object }>)} options
     * @return {*}  {Observable<{
     *         actions: DotCMSWorkflowAction[];
     *         contentlet: DotCMSContentlet;
     *     }>}
     * @memberof DotEditContentStore
     */
    private fireWorkflowAction(
        options: DotFireActionOptions<{ [key: string]: string | object }>
    ): Observable<{
        actions: DotCMSWorkflowAction[];
        contentlet: DotCMSContentlet;
    }> {
        return this.WorkflowActionsFireService.fireTo(options).pipe(
            tap((contentlet) => {
                if (!contentlet.inode) {
                    this.router.navigate(['/c/content']);
                }
            }),
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

    /**
     * Update the URL with the new inode without reloading the page
     *
     * @private
     * @param {string} inode
     * @memberof DotEditContentStore
     */
    private updateURL(inode: string) {
        this.location.replaceState(`/content/${inode}`); // Replace the URL with the new inode without reloading the page
    }
}
