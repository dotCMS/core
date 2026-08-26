import { lastValueFrom } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';
import {
    ChangeDetectionStrategy,
    Component,
    effect,
    inject,
    signal,
    viewChild
} from '@angular/core';

import { MenuItem, MessageService } from 'primeng/api';
import { ContextMenu, ContextMenuModule } from 'primeng/contextmenu';
import { DialogService } from 'primeng/dynamicdialog';

import { take } from 'rxjs/operators';

import {
    DotAlertConfirmService,
    DotContentletService,
    DotFolderService,
    DotHttpErrorManagerService,
    DotMessageService,
    DotRenderMode,
    DotWizardService,
    DotWorkflowActionsFireService,
    DotWorkflowEventHandlerService,
    DotWorkflowsActionsService
} from '@dotcms/data-access';
import { DotPushPublishDialogService } from '@dotcms/dotcms-js';
import {
    DotCMSBaseTypesContentTypes,
    DotCMSContentlet,
    DotCMSWorkflowAction,
    DotContentDriveActionableFolder,
    DotContentletCanLock,
    DotProcessedWorkflowPayload,
    DotWorkflowPayload,
    PERMISSIONS_TYPE
} from '@dotcms/dotcms-models';
import { DotJspIframeDialogComponent, DotJspIframeDialogData } from '@dotcms/ui';

import {
    DIALOG_TYPE,
    ERROR_MESSAGE_LIFE,
    SUCCESS_MESSAGE_LIFE,
    MOVE_TO_FOLDER_WORKFLOW_ACTION_ID,
    ROOT_PATH
} from '../../shared/constants';
import { DotContentDriveContextMenu, DotContentDriveStatus } from '../../shared/models';
import { DotContentDriveNavigationService } from '../../shared/services';
import { DotContentDriveStore } from '../../store/dot-content-drive.store';
import { isFolder } from '../../utils/functions';

@Component({
    selector: 'dot-folder-list-context-menu',
    templateUrl: './dot-folder-list-context-menu.component.html',
    imports: [ContextMenuModule],
    providers: [DotContentletService, DialogService],
    changeDetection: ChangeDetectionStrategy.OnPush,
    host: { class: 'relative' }
})
export class DotFolderListViewContextMenuComponent {
    contextMenu = viewChild<ContextMenu>('contextMenu');

    #dotMessageService = inject(DotMessageService);
    #workflowsActionsService = inject(DotWorkflowsActionsService);
    #workflowActionsFireService = inject(DotWorkflowActionsFireService);
    #dotWorkflowEventHandlerService = inject(DotWorkflowEventHandlerService);
    #store = inject(DotContentDriveStore);
    #navigationService = inject(DotContentDriveNavigationService);
    #messageService = inject(MessageService);
    #dotWizardService = inject(DotWizardService);
    #dotContentletService = inject(DotContentletService);
    #dialogService = inject(DialogService);
    #dotPushPublishDialogService = inject(DotPushPublishDialogService);
    #dotAlertConfirmService = inject(DotAlertConfirmService);
    #dotFolderService = inject(DotFolderService);
    #httpErrorManagerService = inject(DotHttpErrorManagerService);

    /** The menu items for the context menu. */
    $items = signal<MenuItem[]>([]);

    /** The memoized menu items for the context menu.
     * Helps to avoid fetching the menu items multiple times.
     * */
    $memoizedMenuItems = signal<Record<string, MenuItem[]>>({});

    /** The context menu data for the store. */
    $contextMenuData = this.#store.contextMenu;

    /**
     * Effect that handles right-click context menu events.
     * When context menu data is available and the menu is not currently visible,
     * it triggers fetching and displaying the menu items.
     */
    readonly rightClickEffect = effect(() => {
        const contextMenuData = this.$contextMenuData();

        if (contextMenuData) {
            this.getMenuItems(contextMenuData);
        }
    });

    /**
     * Effect that clears the memoized menu items when loading state is detected.
     * This ensures the context menu items are regenerated when new content is being loaded.
     * The memoized items are cleared to force a refresh of the menu options.
     */
    readonly statusEffect = effect(() => {
        const status = this.#store.status();

        if (status === DotContentDriveStatus.LOADING) {
            this.$memoizedMenuItems.set({});
        }
    });

    /**
     * Drops the memo when the push publish environments lookup settles.
     *
     * The Push Publish item's label and `disabled` are computed when the menu is *built*, and menus
     * are memoized per folder. The lookup is one-shot at portlet init, so if it lands after a menu
     * was cached that folder would keep saying "(no environment)" while the Action Center, which
     * reads the signal reactively, already shows it enabled.
     *
     * The signal is read before anything else so it stays a dependency of this effect.
     */
    readonly pushPublishEnvironmentsEffect = effect(() => {
        this.#store.hasPushPublishEnvironments();

        this.$memoizedMenuItems.set({});
    });

    readonly closeOnContextMenuReset = effect(() => {
        const data = this.#store.contextMenu();

        if (!data?.contentlet && this.contextMenu()?.visible()) {
            this.contextMenu()?.hide();
        }
    });

    /**
     * Hides the context menu by clearing the triggered event.
     */
    hideContextMenu() {
        this.#store.patchContextMenu({ triggeredEvent: null });
    }

    /**
     * Retrieves and displays the context menu items for a given contentlet.
     * It checks if the menu has already been memoized and displays it if available.
     * Otherwise, it fetches the workflow actions and builds the menu.
     */
    async getMenuItems({ triggeredEvent, contentlet }: DotContentDriveContextMenu) {
        if (!triggeredEvent || !contentlet) {
            return;
        }

        this.$items.set([]);
        const memoizedMenuItems = this.$memoizedMenuItems();

        const key = isFolder(contentlet) ? contentlet.identifier : contentlet.inode;

        if (memoizedMenuItems[key]) {
            this.$items.set(memoizedMenuItems[key]);
            this.contextMenu()?.show(triggeredEvent);
            return;
        }

        if (isFolder(contentlet)) {
            const folderMenuItems = [];

            // Optional chaining is deliberate: a folder can reach here without `permissions` if it
            // came from a source that did not resolve them (an older backend, or a search that did
            // not opt into `includePermissions`). Gating must degrade to "no actions", never throw.
            if (contentlet.permissions?.includes(PERMISSIONS_TYPE.EDIT)) {
                folderMenuItems.push({
                    label: this.#dotMessageService.get('content-drive.context-menu.edit-folder'),
                    command: () => {
                        this.#store.setDialog({
                            type: DIALOG_TYPE.FOLDER,
                            header: this.#dotMessageService.get(
                                'content-drive.dialog.folder.header.edit'
                            ),
                            payload: contentlet
                        });
                    }
                });
            }

            const canEditPermissions = contentlet.permissions?.includes(
                PERMISSIONS_TYPE.EDIT_PERMISSIONS
            );

            if (canEditPermissions) {
                folderMenuItems.push({
                    label: this.#dotMessageService.get('Edit-Permissions'),
                    command: () => this.#openPermissionsDialog(contentlet.identifier)
                });
            }

            // Both push actions resolve the folder server-side and enforce PUBLISH there
            // (`PublisherAPIImpl`), reporting a denial as a per-asset error rather than throwing.
            // Gating here keeps the menu honest rather than offering an action that will be refused.
            if (contentlet.permissions?.includes(PERMISSIONS_TYPE.PUBLISH)) {
                folderMenuItems.push(this.#buildPushPublishItem(contentlet.identifier));

                folderMenuItems.push({
                    label: this.#dotMessageService.get('contenttypes.content.add_to_bundle'),
                    command: () => this.#store.setShowAddToBundle(true)
                });
            }

            // Gated on EDIT_PERMISSIONS like the Permissions item, matching the legacy folder editor
            // where both tabs sat behind the same check, but ordered after the push group rather than
            // beside Permissions: it is read-only audit data, so it reads last.
            if (canEditPermissions) {
                folderMenuItems.push({
                    label: this.#dotMessageService.get('content-drive.context-menu.push-history'),
                    command: () => this.#openPushHistoryDialog(contentlet.identifier)
                });
            }

            // Last, and gated on **both** permissions, because `FolderAPIImpl.delete` enforces both:
            // EDIT at `:438` and EDIT_PERMISSIONS at `:456`. Gating on EDIT alone offered Delete to
            // a contributor who would confirm the destructive dialog and only then be refused.
            // Ordered after everything else because it is the one entry here that destroys something.
            if (contentlet.permissions?.includes(PERMISSIONS_TYPE.EDIT) && canEditPermissions) {
                folderMenuItems.push({
                    label: this.#dotMessageService.get('content-drive.context-menu.delete-folder'),
                    command: () => this.#confirmDeleteFolder(contentlet)
                });
            }

            if (!folderMenuItems.length) {
                // `$items` was cleared above, so a menu still open from a previous right-click would
                // otherwise sit there empty rather than closing.
                this.contextMenu()?.hide();

                return;
            }

            this.$items.set(folderMenuItems);
            this.$memoizedMenuItems.set({
                ...this.$memoizedMenuItems(),
                [key]: folderMenuItems
            });
            this.contextMenu()?.show(triggeredEvent);
            return;
        }

        const canLockData = await lastValueFrom(
            this.#dotContentletService.canLock(contentlet.inode)
        );

        const workflowActions = await lastValueFrom(
            this.#workflowsActionsService.getByInode(contentlet.inode, DotRenderMode.LISTING)
        );

        const actionsMenu = [];

        const label =
            contentlet.baseType === DotCMSBaseTypesContentTypes.HTMLPAGE ? 'page' : 'content';

        actionsMenu.push({
            label: this.#dotMessageService.get(`content-drive.context-menu.edit-${label}`),
            command: () => {
                this.#navigationService.editContent(contentlet);
            }
        });

        if (canLockData.canLock) {
            actionsMenu.push({
                label: canLockData.locked
                    ? this.#dotMessageService.get('content-drive.context-menu.unlock')
                    : this.#dotMessageService.get('content-drive.context-menu.lock'),
                command: () => {
                    this.#resolveLockAction(contentlet, canLockData);
                }
            });
        }

        // The push group, ordered the same way as on a folder: Push Publish then Add to Bundle.
        // Push Publish is what the old content search offered outside its workflow dropdown, so it
        // belongs here rather than among the workflow actions, which are scheme-driven.
        actionsMenu.push(this.#buildPushPublishItem(contentlet.identifier));

        actionsMenu.push({
            label: this.#dotMessageService.get('contenttypes.content.add_to_bundle'),
            command: () => {
                this.#store.setShowAddToBundle(true);
            }
        });

        // Workflow actions get their own labelled section. "Workflows" is a real dotCMS concept, so
        // it reads as a name rather than an invented category — which matters because the actions
        // themselves carry no groupable intent: the API exposes no actionlet class names, no
        // category and no tag, `order` is a within-scheme sort index and `icon` is admin-authored
        // free text. Any finer grouping would be guesswork that breaks on custom schemes.
        //
        // Archive is the one exception, split off below.
        const selectableActions = workflowActions.filter(
            (action) => action.name !== 'Move' || action.id !== MOVE_TO_FOLDER_WORKFLOW_ACTION_ID
        );

        // hasArchiveActionlet is computed from the action's actual sub-actionlets, not its name, so
        // a scheme's "Retire this blog" still reports true. Name- and locale-independent, which a
        // label match or a hardcoded id would not be.
        const archiveActions = selectableActions.filter((action) => action.hasArchiveActionlet);
        const otherActions = selectableActions.filter((action) => !action.hasArchiveActionlet);

        const toMenuItem = (action: DotCMSWorkflowAction): MenuItem => ({
            label: this.#dotMessageService.get(action.name),
            command: () => this.#executeWorkflowActions(action, contentlet)
        });

        if (otherActions.length) {
            // A real nested submenu, so PrimeNG renders the label, the chevron and the flyout
            // natively — and the group is announced as a submenu rather than as a disabled
            // menuitem, which is what a caption item would have been.
            actionsMenu.push({
                label: this.#dotMessageService.get('content-drive.context-menu.workflows'),
                items: otherActions.map(toMenuItem)
            });
        }

        // Separated rather than merely last: archiving is the destructive one, and a separator is
        // what stops it being clicked by momentum after the action above it.
        if (archiveActions.length) {
            actionsMenu.push({ separator: true }, ...archiveActions.map(toMenuItem));
        }

        if (!actionsMenu.length) {
            // Same as the folder branch: close rather than leave an emptied menu on screen.
            this.contextMenu()?.hide();

            return;
        }

        this.$items.set(actionsMenu);
        this.$memoizedMenuItems.set({
            ...this.$memoizedMenuItems(),
            [key]: this.$items()
        });

        this.contextMenu()?.show(triggeredEvent);
    }

    #executeWorkflowActions(workflowAction: DotCMSWorkflowAction, contentlet: DotCMSContentlet) {
        if (workflowAction.actionInputs?.length > 0) {
            this.#openWizard(workflowAction, contentlet);
        } else {
            this.#fireWorkflowAction({
                contentletInode: contentlet.inode,
                actionId: workflowAction.id
            });
        }
    }

    #openWizard(workflowAction: DotCMSWorkflowAction, contentlet: DotCMSContentlet) {
        this.#dotWizardService
            .open<DotWorkflowPayload>(
                this.#dotWorkflowEventHandlerService.setWizardInput(
                    workflowAction,
                    this.#dotMessageService.get('Workflow-Action')
                )
            )
            .pipe(take(1))
            .subscribe((data: DotWorkflowPayload) => {
                const payload = this.#dotWorkflowEventHandlerService.processWorkflowPayload(
                    data,
                    workflowAction.actionInputs
                );

                this.#store.setStatus(DotContentDriveStatus.LOADING);
                this.#fireWorkflowAction({
                    contentletInode: contentlet.inode,
                    actionId: workflowAction.id,
                    payload
                });
            });
    }

    #fireWorkflowAction({
        contentletInode,
        actionId,
        payload
    }: {
        contentletInode: string;
        actionId: string;
        payload?: DotProcessedWorkflowPayload;
    }) {
        this.#store.setStatus(DotContentDriveStatus.LOADING);
        this.#workflowActionsFireService
            .fireTo({ actionId, inode: contentletInode, data: payload })
            .subscribe(
                () => {
                    this.#store.reloadContentDrive();

                    this.#messageService.add({
                        severity: 'success',
                        summary: this.#dotMessageService.get(
                            'content-drive.toast.workflow-executed'
                        )
                    });
                },
                (error) => {
                    this.#messageService.add({
                        severity: 'error',
                        summary: this.#dotMessageService.get('content-drive.toast.workflow-error'),
                        life: ERROR_MESSAGE_LIFE
                    });
                    this.#store.setStatus(DotContentDriveStatus.LOADED);
                    console.error('Error firing workflow action', error);
                }
            );
    }

    #resolveLockAction(contentlet: DotCMSContentlet, canLockData: DotContentletCanLock) {
        if (canLockData.locked) {
            this.#dotContentletService
                .unlockContent(contentlet.inode)
                .pipe(take(1))
                .subscribe(
                    ({ title }: DotCMSContentlet) => {
                        this.#messageService.add({
                            severity: 'success',
                            summary: this.#dotMessageService.get(
                                'content-drive.toast.unlock-success',
                                title
                            ),
                            detail: this.#dotMessageService.get(
                                'content-drive.toast.unlock-success-detail'
                            )
                        });

                        this.#store.reloadContentDrive();
                    },
                    (error) => {
                        console.error('Error unlocking content', error);
                        this.#messageService.add({
                            severity: 'error',
                            summary: this.#dotMessageService.get(
                                'content-drive.toast.unlock-error'
                            ),
                            detail: this.#dotMessageService.get(
                                'content-drive.toast.unlock-error-detail'
                            ),
                            life: ERROR_MESSAGE_LIFE
                        });
                        console.error('Error unlocking content', error);
                    }
                );
        } else {
            this.#dotContentletService
                .lockContent(contentlet.inode)
                .pipe(take(1))
                .subscribe(
                    ({ title }: DotCMSContentlet) => {
                        this.#messageService.add({
                            severity: 'success',
                            summary: this.#dotMessageService.get(
                                'content-drive.toast.lock-success',
                                title
                            ),
                            detail: this.#dotMessageService.get(
                                'content-drive.toast.lock-success-detail'
                            )
                        });
                        this.#store.reloadContentDrive();
                    },
                    (error) => {
                        console.error('Error locking content', error);
                        this.#messageService.add({
                            severity: 'error',
                            summary: this.#dotMessageService.get('content-drive.toast.lock-error'),
                            detail: this.#dotMessageService.get(
                                'content-drive.toast.lock-error-detail'
                            ),
                            life: ERROR_MESSAGE_LIFE
                        });
                    }
                );
        }
    }

    #openPermissionsDialog(identifier: string): void {
        this.#dialogService.open(DotJspIframeDialogComponent, {
            header: this.#dotMessageService.get('Edit-Permissions'),
            width: 'min(92vw, 75rem)',
            contentStyle: { overflow: 'hidden' },
            data: {
                url: this.#buildPermissionsUrl(identifier),
                titleKey: 'Permissions',
                emptyKey: 'dot.permissions.iframe.dialog.no-asset',
                testIdPrefix: 'permissions'
            } satisfies DotJspIframeDialogData,
            modal: true,
            appendTo: 'body',
            closable: true,
            closeOnEscape: true,
            draggable: false,
            resizable: false,
            position: 'center'
        });
    }

    #buildPermissionsUrl(identifier: string): string {
        const params = new URLSearchParams({
            folderIdentifier: identifier,
            popup: 'true'
        });
        return `/html/portlet/ext/folders/permissions.jsp?${params.toString()}`;
    }

    #openPushHistoryDialog(identifier: string): void {
        this.#dialogService.open(DotJspIframeDialogComponent, {
            header: this.#dotMessageService.get('content-drive.context-menu.push-history'),
            width: 'min(92vw, 75rem)',
            contentStyle: { overflow: 'hidden' },
            data: {
                url: this.#buildPushHistoryUrl(identifier),
                titleKey: 'publisher_push_history',
                emptyKey: 'dot.push-history.iframe.dialog.no-asset',
                testIdPrefix: 'push-history'
            } satisfies DotJspIframeDialogData,
            modal: true,
            appendTo: 'body',
            closable: true,
            closeOnEscape: true,
            draggable: false,
            resizable: false,
            position: 'center'
        });
    }

    /**
     * The Push Publish item, shared by the folder and contentlet branches.
     *
     * Offered but **disabled** when no environment is reachable, rather than hidden: nothing is
     * missing from dotCMS, something is missing from the configuration, and the fix is an
     * administrator's. An unresolved lookup reads as disabled too, so the item never enables and
     * then retracts.
     *
     * The reason sits in the **label**, not a tooltip. A disabled context menu item computes
     * `pointer-events: none` (measured in the browser), so no hover ever reaches it and no tooltip
     * can fire, whichever of PrimeNG's tooltip inputs it carries — and ContextMenu binds `pTooltip`
     * from `tooltipOptions` alone, so a plain `tooltip` is ignored on top of that. A suffixed label
     * needs neither hover nor click.
     */
    #buildPushPublishItem(identifier: string): MenuItem {
        const hasEnvironments = this.#store.hasPushPublishEnvironments();
        const label = this.#dotMessageService.get('contenttypes.content.push_publish');

        return {
            label: hasEnvironments
                ? label
                : this.#dotMessageService.get(
                      'content-drive.context-menu.push-publish.no-environment',
                      label
                  ),
            disabled: !hasEnvironments,
            command: () => this.#openPushPublishDialog(identifier)
        };
    }

    /**
     * Spawns the app-wide push publish dialog for a single asset.
     *
     * The guard is not redundant with `disabled`: PrimeNG suppresses the click, but the command is
     * still reachable programmatically, and a push with nowhere to go fails at the servlet with a
     * message the user cannot act on.
     */
    #openPushPublishDialog(identifier: string): void {
        if (!this.#store.hasPushPublishEnvironments()) {
            return;
        }

        this.#dotPushPublishDialogService.open({
            assetIdentifier: identifier,
            title: this.#dotMessageService.get('contenttypes.content.push_publish')
        });
    }

    /**
     * Asks before deleting, because the server delete is recursive and irreversible.
     *
     * The message names the folder and says its contents go with it: `FolderAPI.delete` removes the
     * whole subtree, and a confirmation that only says "delete this folder" would understate that.
     */
    #confirmDeleteFolder(folder: DotContentDriveActionableFolder): void {
        this.#dotAlertConfirmService.confirm({
            header: this.#dotMessageService.get('content-drive.context-menu.delete-folder'),
            message: this.#dotMessageService.get(
                'content-drive.dialog.delete-folder.message',
                folder.name
            ),
            // The service defaults this to "Accept", which says nothing about what is about to
            // happen. The reject side already reads "Cancel", so only this one needs naming.
            footerLabel: { accept: this.#dotMessageService.get('Delete') },
            accept: () => this.#deleteFolder(folder)
        });
    }

    /**
     * Deletes the folder by path, which is what the endpoint takes.
     *
     * Built from the browsed site's hostname rather than the folder's `hostId`: the drive search is
     * scoped to `//<hostname><path>`, so every folder listed is on the site being browsed. Without a
     * resolved site there is no path to send, so the call is skipped rather than posting `//undefined`.
     */
    #deleteFolder(folder: DotContentDriveActionableFolder): void {
        const hostname = this.#store.currentSite()?.hostname;

        if (!hostname) {
            // The user already confirmed a destructive action, so this cannot just return: without
            // a resolved site there is no path to delete by, and silence would read as "it worked".
            this.#messageService.add({
                severity: 'error',
                summary: this.#dotMessageService.get('content-drive.context-menu.delete-folder'),
                detail: this.#dotMessageService.get('content-drive.dialog.delete-folder.no-site'),
                life: ERROR_MESSAGE_LIFE
            });

            return;
        }

        this.#dotFolderService
            .deleteFolder(`//${hostname}${folder.path}`)
            .pipe(take(1))
            .subscribe({
                next: () => {
                    this.#messageService.add({
                        severity: 'success',
                        summary: this.#dotMessageService.get(
                            'content-drive.context-menu.delete-folder'
                        ),
                        detail: this.#dotMessageService.get(
                            'content-drive.dialog.delete-folder.success',
                            folder.name
                        ),
                        life: SUCCESS_MESSAGE_LIFE
                    });
                    // The tree serves this menu too, so the deleted folder can be an ancestor of
                    // the one being browsed — or the browsed folder itself. Reloading the current
                    // path would then fetch a path that no longer exists, leaving an empty grid
                    // and a breadcrumb pointing inside a deleted folder. Moving to the root is the
                    // one destination guaranteed to still be there.
                    if (this.#browsingInside(folder.path)) {
                        this.#store.setPath(ROOT_PATH);
                    } else {
                        this.#store.reloadContentDrive();
                    }

                    // Always: the tree reloads separately from the grid, so without this it keeps
                    // showing a folder that no longer exists until the next navigation.
                    this.#store.loadFolders();
                },
                error: (error: HttpErrorResponse) => {
                    this.#httpErrorManagerService.handle(error);
                }
            });
    }

    /**
     * Whether the browsed path sits at or under `folderPath`.
     *
     * Compared with a trailing slash on both sides so `/blog-archive/` is not read as living inside
     * `/blog/`; folder paths from the drive already carry one, and the root's own `undefined` path
     * can never be inside anything.
     */
    #browsingInside(folderPath: string): boolean {
        const currentPath = this.#store.path();

        if (!currentPath) {
            return false;
        }

        const target = folderPath.endsWith('/') ? folderPath : `${folderPath}/`;
        const current = currentPath.endsWith('/') ? currentPath : `${currentPath}/`;

        return current.startsWith(target);
    }

    #buildPushHistoryUrl(identifier: string): string {
        // `popup=true` is what un-hides the body of a legacy JSP loaded outside the portal frame.
        const params = new URLSearchParams({
            folderIdentifier: identifier,
            popup: 'true'
        });
        return `/html/portlet/ext/folders/push_history.jsp?${params.toString()}`;
    }
}
