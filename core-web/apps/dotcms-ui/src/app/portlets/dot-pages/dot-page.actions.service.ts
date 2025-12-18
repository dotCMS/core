import { Observable } from 'rxjs';

import { inject, Injectable } from '@angular/core';

import { MenuItem } from 'primeng/api';
import { DialogService } from 'primeng/dynamicdialog';

import { map } from 'rxjs/operators';

import {
    DotEventsService,
    DotHttpErrorManagerService,
    DotMessageService,
    DotRenderMode,
    DotRouterService,
    DotWorkflowActionsFireService,
    DotWorkflowEventHandlerService,
    DotWorkflowsActionsService
} from '@dotcms/data-access';
import { DotCMSContentlet, DotCMSWorkflowAction } from '@dotcms/dotcms-models';
import { DotFavoritePageComponent } from '@dotcms/portlets/dot-ema/ui';

@Injectable()
export class DotPageActionsService {
    readonly #dotMessageService = inject(DotMessageService);
    readonly #dotActionsService = inject(DotWorkflowsActionsService);
    readonly #dotRouterService = inject(DotRouterService);
    readonly #dotEventsService = inject(DotEventsService);
    readonly #dialogService = inject(DialogService);
    readonly #dotWorkflowEventHandlerService = inject(DotWorkflowEventHandlerService);
    readonly #dotWorkflowActionsFireService = inject(DotWorkflowActionsFireService);
    readonly #httpErrorManagerService = inject(DotHttpErrorManagerService);

    getItems(item: DotCMSContentlet): Observable<MenuItem[]> {
        const actions$ = this.#dotActionsService.getByInode(item.inode, DotRenderMode.LISTING).pipe(
            map((actions: DotCMSWorkflowAction[]) =>
                actions.map((action) => {
                    const label = this.#dotMessageService.get(action.name);
                    return {
                        label,
                        command: () => this.handleWorkflowAction(action, item.inode)
                    };
                })
            )
        );

        const editAction = this.editAction(item);
        const deleteFavoritePageAction = this.deleteFavoritePageAction(item);
        const favoritePageAction = this.favoritePageAction(item);
        const addToBundleAction = this.addToBundleAction();
        const pushPublishAction = this.pushPublishAction();

        return actions$.pipe(
            map((actions: MenuItem[]) => [
                favoritePageAction,
                deleteFavoritePageAction,
                ...actions,
                editAction,
                addToBundleAction,
                pushPublishAction
            ])
        );
    }

    /**
     * Edit action
     *
     * @param {DotCMSContentlet} item
     * @returns {MenuItem}
     * @memberof DotPageActionsService
     */
    private editAction({ inode }: DotCMSContentlet): MenuItem {
        return {
            label: this.#dotMessageService.get('Edit'),
            command: () => this.#dotRouterService.goToEditContentlet(inode)
        };
    }

    /**
     * Add to bundle action
     *
     * @returns {MenuItem}
     * @memberof DotPageActionsService
     */
    private addToBundleAction(): MenuItem {
        return {
            label: this.#dotMessageService.get('contenttypes.content.add_to_bundle'),
            command: () => {
                /* TODO: Implement add to bundle */
            }
        };
    }

    /**
     * Push publish action
     *
     * @returns {MenuItem}
     * @memberof DotPageActionsService
     */
    private pushPublishAction(): MenuItem {
        return {
            label: this.#dotMessageService.get('contenttypes.content.push_publish'),
            command: () => {
                /* TODO: Implement push publish */
            }
        };
    }

    /**
     * Handle workflow action
     *
     * @param {DotCMSWorkflowAction} workflow
     * @param {string} inode
     * @returns {void}
     * @memberof DotPageActionsService
     */
    private handleWorkflowAction(workflow: DotCMSWorkflowAction, inode: string): void {
        const hasInputs = workflow.actionInputs?.length > 0;
        const callback = 'ngWorkflowEventCallback';
        const action = { actionId: workflow.id, inode };
        const message = this.#dotMessageService.get('Workflow-executed');

        if (hasInputs) {
            this.#dotWorkflowEventHandlerService.open({ workflow, callback, inode });
            return;
        }

        this.#dotWorkflowActionsFireService.fireTo(action).subscribe({
            next: (payload) =>
                this.#dotEventsService.notify('save-page', { payload, value: message }),
            error: (error) => this.#httpErrorManagerService.handle(error, true)
        });
    }

    /**
     * Favorite page action
     *
     * @param {DotCMSContentlet} item
     * @returns {MenuItem}
     * @memberof DotPageActionsService
     */
    private favoritePageAction(item: DotCMSContentlet): MenuItem {
        const isFavorite = item.contentType === 'dotFavoritePage';

        return {
            label: isFavorite
                ? this.#dotMessageService.get('favoritePage.contextMenu.action.edit')
                : this.#dotMessageService.get('favoritePage.contextMenu.action.add'),
            command: () => {
                this.#dialogService.open(DotFavoritePageComponent, {
                    header: this.#dotMessageService.get('favoritePage.dialog.header'),
                    width: '80rem',
                    data: {
                        page: {
                            favoritePageUrl: '',
                            favoritePage: item
                        },
                        onSave: () => {
                            // eslint-disable-next-line no-console
                            console.log('onSave');
                            // this.getFavoritePages(FAVORITE_PAGE_LIMIT);
                        },
                        onDelete: () => {
                            // eslint-disable-next-line no-console
                            console.log('onDelete');
                            // this.getFavoritePages(FAVORITE_PAGE_LIMIT);
                        }
                    }
                });
            }
        };
    }

    /**
     * Delete favorite page action
     *
     * @param {DotCMSContentlet} item
     * @returns {MenuItem}
     * @memberof DotPageActionsService
     */
    private deleteFavoritePageAction(_item: DotCMSContentlet): MenuItem {
        return {
            label: this.#dotMessageService.get('favoritePage.dialog.delete.button'),
            command: () => {
                // this.deleteFavoritePage(item.inode)
            }
        };
    }
}
