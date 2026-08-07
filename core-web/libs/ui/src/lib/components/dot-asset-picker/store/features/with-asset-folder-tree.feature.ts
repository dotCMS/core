import { patchState, signalStoreFeature, type, withMethods, withState } from '@ngrx/signals';
import { Observable, of } from 'rxjs';

import { inject } from '@angular/core';

import { catchError, take } from 'rxjs/operators';

import {
    applyLoadMoreToHierarchy,
    buildTreeFolderNodes,
    DotFolderService,
    DotHttpErrorManagerService,
    FolderTreeHierarchyLevel,
    getFolderHierarchyByPath,
    getFolderNodesByPath
} from '@dotcms/data-access';
import { ComponentStatus, TreeNodeItem } from '@dotcms/dotcms-models';

import { ALL_FOLDER, SYSTEM_HOST_ID } from '../../../dot-folder-tree/constants';
import { DotAssetPickerFolderTreeState, DotAssetPickerState } from '../models';

const initialState: DotAssetPickerFolderTreeState = {
    folders: [],
    selectedNode: ALL_FOLDER,
    foldersStatus: ComponentStatus.INIT
};

/**
 * Sidebar folder tree: the navigation half of the picker.
 *
 * Deliberately has no `withHooks` of its own — the tree loads when the host calls `initPicker`, not
 * when the store is constructed, because until then there is no site to load folders for.
 */
export function withAssetFolderTree() {
    return signalStoreFeature(
        { state: type<DotAssetPickerState>() },
        withState<DotAssetPickerFolderTreeState>(initialState),
        withMethods(
            (
                store,
                dotFolderService = inject(DotFolderService),
                httpErrorManager = inject(DotHttpErrorManagerService)
            ) => ({
                /**
                 * Loads the tree expanded down to the configured path, so a picker that opens on a
                 * remembered folder shows that folder in context rather than at the root.
                 */
                loadFolders: (): void => {
                    const site = store.config()?.site;

                    if (!site || site.identifier === SYSTEM_HOST_ID) {
                        return;
                    }

                    // Per-site clone of the synthetic root. It is prepended to the tree rather than
                    // parenting it, which is why root-level load-more sentinels end up as its
                    // siblings.
                    const siteRootNode: TreeNodeItem = {
                        ...ALL_FOLDER,
                        data: {
                            type: 'folder',
                            path: '',
                            hostname: site.hostname,
                            id: site.identifier
                        }
                    };

                    const targetPath = store.path() || '';

                    patchState(store, { foldersStatus: ComponentStatus.LOADING });

                    getFolderHierarchyByPath(targetPath, site, dotFolderService)
                        .pipe(
                            take(1),
                            catchError((error) => {
                                httpErrorManager.handle(error);
                                patchState(store, { foldersStatus: ComponentStatus.ERROR });

                                return of([] as FolderTreeHierarchyLevel[]);
                            })
                        )
                        .subscribe((levels) => {
                            const { rootNodes, selectedNode } = buildTreeFolderNodes({
                                folderHierarchyLevels: levels.map((level) => level.folders),
                                targetPath: targetPath || '/',
                                rootNode: siteRootNode
                            });

                            patchState(store, {
                                folders: [
                                    siteRootNode,
                                    ...applyLoadMoreToHierarchy(rootNodes, levels, site.hostname)
                                ],
                                selectedNode,
                                foldersStatus: ComponentStatus.LOADED
                            });
                        });
                },

                /**
                 * One page of a folder's children. Returns the observable **unsubscribed**: the
                 * consumer splices the result into the node it expanded and calls `updateFolders`.
                 */
                loadChildFolders: (
                    path: string,
                    hostname?: string,
                    page = 1
                ): Observable<{ folders: TreeNodeItem[]; totalEntries: number }> => {
                    const site = store.config()?.site;

                    if (!site) {
                        return of({ folders: [], totalEntries: 0 });
                    }

                    return getFolderNodesByPath(
                        path,
                        { ...site, hostname: hostname || site.hostname },
                        dotFolderService,
                        page
                    );
                },

                setSelectedNode: (selectedNode: TreeNodeItem): void => {
                    patchState(store, { selectedNode });
                },

                /**
                 * `structuredClone` is load-bearing: tree nodes are mutated in place (children,
                 * loading flags), and a shallow copy would keep the same references, so change
                 * detection would never see the update.
                 */
                updateFolders: (folders: TreeNodeItem[]): void => {
                    patchState(store, { folders: structuredClone(folders) });
                }
            })
        )
    );
}
