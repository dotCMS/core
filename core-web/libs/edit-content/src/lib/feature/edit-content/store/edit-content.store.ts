import { ComponentStore, tapResponse } from '@ngrx/component-store';
import { Observable, forkJoin, of } from 'rxjs';

import { Location } from '@angular/common';
import { Injectable, inject } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';

import { MessageService } from 'primeng/api';

import { map, switchMap, tap } from 'rxjs/operators';

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
    private readonly activatedRoute = inject(ActivatedRoute);
    private readonly router = inject(Router);

    private readonly dotEditContentService = inject(DotEditContentService);
    private readonly workflowActionService = inject(DotWorkflowsActionsService);
    private readonly WorkflowActionsFireService = inject(DotWorkflowActionsFireService);

    private readonly messageService = inject(MessageService);
    private readonly dotMessageService = inject(DotMessageService);
    private readonly location = inject(Location);

    private contentType = this.activatedRoute.snapshot.params['contentType'];
    private inode = this.activatedRoute.snapshot.params['id'];

    readonly vm$ = this.select(
        ({ actions, contentType: { variable, layout, fields }, contentlet }) => ({
            actions,
            contentType: contentlet?.contentType || variable,
            layout: layout || [],
            fields: fields || [],
            contentlet
        })
    ).pipe(
        // Keep the inode and contentType in sync new content
        tap(({ contentlet, contentType }) => {
            this.updateInodeAndContentType({
                inode: contentlet?.inode,
                contentType
            });
        })
    );

    constructor() {
        super();

        const obs$ = !this.inode
            ? this.getNewContent(this.contentType)
            : this.getExistingContent(this.inode);

        obs$.subscribe(({ contentType, actions, contentlet }) => {
            this.setState({
                contentType,
                actions,
                contentlet
            });
        });
    }

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
        actions
    }));

    /**
     * Fire the workflow action and update the contentlet and actions
     *
     * @memberof DotEditContentStore
     */
    readonly fireWorkflowActionEffect = this.effect(
        (
            data$: Observable<{
                actionId: string;
                formData: { [key: string]: string };
            }>
        ) => {
            return data$.pipe(
                map(({ actionId, formData }) => this.getFireActionOptions(actionId, formData)),
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

    /**
     * Get the content type, actions and contentlet for the given contentTypeVar
     *
     * @private
     * @param {string} contentTypeVar
     * @return {*}
     * @memberof DotEditContentStore
     */
    private getNewContent(contentTypeVar: string) {
        return forkJoin({
            contentType: this.dotEditContentService.getContentType(contentTypeVar),
            actions: this.workflowActionService.getDefaultActions(contentTypeVar),
            contentlet: of(null)
        });
    }

    /**
     * Get the contentlet, content type and actions for the given inode
     *
     * @private
     * @param {*} inode
     * @return {*}
     * @memberof DotEditContentStore
     */
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

    /**
     * Update the inode and contentType
     *
     * @private
     * @param {*} { inode, contentType }
     * @memberof DotEditContentStore
     */
    private updateInodeAndContentType({ inode, contentType }) {
        this.inode = inode;
        this.contentType = contentType;
    }

    /**
     * Get the options to fire the workflow action.
     *
     * @private
     * @param {string} actionId
     * @param {{ [key: string]: string }} formData
     * @return {*}  {(DotFireActionOptions<{ [key: string]: string | object }>)}
     * @memberof DotEditContentStore
     */
    private getFireActionOptions(
        actionId: string,
        formData: { [key: string]: string }
    ): DotFireActionOptions<{ [key: string]: string | object }> {
        return {
            actionId,
            data: {
                contentlet: {
                    ...formData,
                    contentType: this.contentType
                }
            },
            inode: this.inode
        };
    }
}
