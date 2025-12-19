import { Observable } from 'rxjs';

import { inject, Injectable, signal } from '@angular/core';

import { MenuItem } from 'primeng/api';
import { DialogService } from 'primeng/dynamicdialog';

import { map, take } from 'rxjs/operators';

import {
    DotCurrentUserService,
    DotEventsService,
    DotHttpErrorManagerService,
    DotMessageService,
    DotRenderMode,
    DotRouterService,
    DotWorkflowActionsFireService,
    DotWorkflowEventHandlerService,
    DotWorkflowsActionsService,
    PushPublishService
} from '@dotcms/data-access';
import { DotPushPublishDialogService } from '@dotcms/dotcms-js';
import {
    DotCMSBaseTypesContentTypes,
    DotCMSContentlet,
    DotCMSWorkflowAction,
    DotEnvironment,
    DotPermissionsType,
    PermissionsType,
    UserPermissions
} from '@dotcms/dotcms-models';
import { DotFavoritePageComponent } from '@dotcms/portlets/dot-ema/ui';
import { GlobalStore } from '@dotcms/store';

interface DotPermissions {
    canRead: boolean;
    canWrite: boolean;
}

/**
 * Builds the context-menu items shown in the Pages listing.
 *
 * Notes:
 * - Workflow actions are resolved per contentlet via `DotWorkflowsActionsService`.
 * - Edit permission is derived from the logged-in user's permissions for HTML Pages and Contentlets.
 * - Push Publish is only shown when at least one environment exists.
 *
 * Menu ordering (high-level):
 * - Favorite (add/edit) (hidden for archived items)
 * - Delete favorite (disabled for non-favorites)
 * - Separator
 * - Workflow actions (per item)
 * - Edit (only when `#canEdit()` is true)
 * - Add to bundle (currently disabled until Pages UI wiring is enabled)
 * - Push Publish (only when push-publish environments exist)
 */
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
    readonly #dotPushPublishDialogService = inject(DotPushPublishDialogService);
    readonly #dotCurrentUser = inject(DotCurrentUserService);
    readonly #pushPublishService = inject(PushPublishService);
    readonly #globalStore = inject(GlobalStore);

    /**
     * Cached result of whether Push Publish is actionable for this installation.
     * We compute it once from the environments API and store it in a signal for fast synchronous checks.
     */
    readonly #havePushPublishEnvironments = signal<boolean>(false);

    readonly #separatorItem: MenuItem = { separator: true };

    /**
     * Cached CONTENTLETS permissions for the currently logged in user.
     */
    readonly #contentletsPermissions = signal<DotPermissions>({
        canRead: false,
        canWrite: false
    });
    /**
     * Cached HTMLPAGES permissions for the currently logged in user.
     */
    readonly #htmlPagesPermissions = signal<DotPermissions>({
        canRead: false,
        canWrite: false
    });

    constructor() {
        this.#initUserPermissions();
        this.#initPushPublishEnvironments();
    }

    /**
     * Returns the full context-menu model for a given contentlet.
     *
     * This method is intentionally pure from the caller’s perspective: it fetches workflow actions
     * for the item and combines them with static actions (favorite, edit, push publish, etc.).
     *
     * @param item Contentlet for which to build the menu model.
     * @returns PrimeNG menu model.
     */
    getItems(item: DotCMSContentlet): Observable<MenuItem[]> {
        return this.#dotActionsService.getByInode(item.inode, DotRenderMode.LISTING).pipe(
            map((actions: DotCMSWorkflowAction[]) => this.#buildMenuItems(item, actions))
        );
    }

    /**
     * Edit action
     *
     * Navigates to the edit screen for the given inode.
     */
    #editAction({ inode }: DotCMSContentlet): MenuItem {
        return {
            label: this.#dotMessageService.get('Edit'),
            command: () => this.#dotRouterService.goToEditContentlet(inode)
        };
    }

    /**
     * Add to bundle action
     *
     * This feature is not wired up in the Pages listing yet (the UI hook is currently commented out
     * in `dot-pages.component.html`). We keep the item present but disabled to avoid a no-op click.
     *
     * Once enabled, this should open `DotAddToBundleComponent` and pass the item identifier.
     *
     * @returns The menu item model.
     */
    #addToBundleAction(): MenuItem {
        return {
            label: this.#dotMessageService.get('contenttypes.content.add_to_bundle'),
            disabled: true
        };
    }

    /**
     * Push publish action
     *
     * Opens the Push Publish dialog for the provided identifier.
     */
    #pushPublishAction({ identifier }: DotCMSContentlet): MenuItem {
        return {
            label: this.#dotMessageService.get('contenttypes.content.push_publish'),
            command: () => {
                this.#dotPushPublishDialogService.open({
                    assetIdentifier: identifier,
                    title: this.#dotMessageService.get('contenttypes.content.push_publish')
                });
            }
        };
    }

    /**
     * Handle workflow action
     *
     * Behavior:
     * - If the workflow action has inputs, opens the workflow wizard/modal.
     * - Otherwise, fires the action immediately and emits a `save-page` event on success.
     *
     * @param workflow Workflow action definition.
     * @param inode Contentlet inode the action is applied to.
     */
    #handleWorkflowAction(workflow: DotCMSWorkflowAction, inode: string): void {
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
     * Opens the favorite page dialog.
     * The label changes depending on whether the item is already a favorite.
     */
    #favoritePageAction(item: DotCMSContentlet): MenuItem {
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
     * Currently a placeholder (disabled unless item is a favorite).
     * When implemented, it should delete the favorite contentlet and refresh the listing.
     */
    #deleteFavoritePageAction(): MenuItem {

        return {
            label: this.#dotMessageService.get('favoritePage.dialog.delete.button'),
            command: () => {
                // TODO: Implement delete favorite page (requires DotFavoritePageService integration).
            }
        };
    }

    /**
     * Determines whether the logged-in user can edit the provided item.
     *
     * Permission source:
     * - HTMLPAGE → `#htmlPagesPermissions`
     * - CONTENT → `#contentletsPermissions`
     */
    #canEdit(item: DotCMSContentlet): boolean {
        if (item.baseType === DotCMSBaseTypesContentTypes.HTMLPAGE) {
            return this.#htmlPagesPermissions().canWrite;
        }

        if (item.baseType === DotCMSBaseTypesContentTypes.CONTENT) {
            return this.#contentletsPermissions().canWrite;
        }

        return false;
    }

    /**
     * Maps workflow actions into PrimeNG menu items.
     *
     * @param actions Workflow actions available for the inode.
     * @param inode Target inode for execution.
     */
    #buildWorkflowMenuItems(actions: DotCMSWorkflowAction[], inode: string): MenuItem[] {
        return actions.map((action) => ({
            label: this.#dotMessageService.get(action.name),
            command: () => this.#handleWorkflowAction(action, inode)
        }));
    }

    /**
     * Combines static actions and workflow actions into the final menu model.
     *
     * @param item Selected contentlet.
     * @param actions Workflow actions resolved for the item.
     */
    #buildMenuItems(item: DotCMSContentlet, actions: DotCMSWorkflowAction[]): MenuItem[] {
        const menuActions: MenuItem[] = [];

        if (!item.archived) {
            menuActions.push(this.#favoritePageAction(item));
        }
        const isFavorite = item.contentType === 'dotFavoritePage';
        if (isFavorite) {
            menuActions.push(this.#deleteFavoritePageAction());
        }
        menuActions.push(this.#separatorItem);

        menuActions.push(...this.#buildWorkflowMenuItems(actions, item.inode));

        if (this.#canEdit(item)) {
            menuActions.push(this.#editAction(item));
        }

        menuActions.push(this.#addToBundleAction());

        if (this.#havePushPublishEnvironments()) {
            menuActions.push(this.#pushPublishAction(item));
        }

        return menuActions;
    }

    /**
     * Fetches logged-user permissions once and caches them in signals.
     *
     * We intentionally `take(1)` because this service is used for menu building only; permissions are
     * expected to be stable for the session, and reactivity is not required here.
     */
    #initUserPermissions(): void {
        this.#dotCurrentUser
            .getUserPermissions(
                this.#globalStore.loggedUser().userId,
                [UserPermissions.READ, UserPermissions.WRITE],
                [PermissionsType.CONTENTLETS, PermissionsType.HTMLPAGES]
            )
            .pipe(take(1))
            .subscribe({
                next: (permissions: DotPermissionsType) => {
                    this.#contentletsPermissions.set(permissions['CONTENTLETS'] as DotPermissions);
                    this.#htmlPagesPermissions.set(permissions['HTMLPAGES'] as DotPermissions);
                },
                error: (error) => this.#httpErrorManagerService.handle(error, true)
            });
    }

    /**
     * Fetches push-publish environments once and caches whether any exist.
     *
     * `Push Publish` is only shown if at least one environment is available.
     */
    #initPushPublishEnvironments(): void {
        this.#pushPublishService
            .getEnvironments()
            .pipe(
                take(1),
                map((environments: DotEnvironment[]) => !!environments.length)
            )
            .subscribe({
                next: (havePushPublishEnvironments: boolean) =>
                    this.#havePushPublishEnvironments.set(havePushPublishEnvironments),
                error: (error) => this.#httpErrorManagerService.handle(error, true)
            });
    }
}
