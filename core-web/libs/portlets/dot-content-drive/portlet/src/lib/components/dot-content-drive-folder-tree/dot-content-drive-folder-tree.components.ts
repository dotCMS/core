import { of } from 'rxjs';

import { HttpClient } from '@angular/common/http';
import {
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    inject,
    OnInit,
    signal
} from '@angular/core';

import { TreeNode } from 'primeng/api';
import { TreeModule, TreeNodeExpandEvent } from 'primeng/tree';

import { catchError, map } from 'rxjs/operators';

import { FOLDER_TREE_API_ENDPOINT, FOLDER_TREE_INITIAL_PATH } from '../../shared/constants';
import { DotCMSFolder } from '../../shared/models';

interface FolderTreeData {
    treeIndexes: number[];
    path: string;
}

/**
 * Component for displaying and managing the folder tree structure in the content drive.
 * Handles loading folders from the DotCMS assets API and manages tree node expansion.
 */
@Component({
    selector: 'dot-content-drive-folder-tree',
    templateUrl: './dot-content-drive-folder-tree.component.html',
    styleUrl: './dot-content-drive-folder-tree.component.scss',
    imports: [TreeModule],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotContentDriveFolderTreeComponent implements OnInit {
    // Dependencies
    private readonly httpClient = inject(HttpClient);
    private readonly cd = inject(ChangeDetectorRef);

    // Constants
    private readonly endpoint = FOLDER_TREE_API_ENDPOINT;
    private readonly initialPath = FOLDER_TREE_INITIAL_PATH;

    // State
    protected readonly folders = signal<TreeNode<FolderTreeData>[]>([]);

    /**
     * Component initialization - loads the initial folder structure
     */
    ngOnInit(): void {
        this.loadInitialFolders();
    }

    /**
     * Handles node expansion events - loads child folders when a node is expanded
     * @param event - The TreeNodeExpandEvent containing the expanded node
     */
    onNodeExpand(event: TreeNodeExpandEvent): void {
        const eventNode = event.node;
        const treeIndexes = eventNode.data?.treeIndexes;

        // Skip if children are already loaded
        if (eventNode.children?.length > 0) {
            return;
        }

        const path = eventNode.data?.path;
        if (!path) {
            console.warn('Node data or path is missing for expansion');
            return;
        }

        this.loadChildFolders(eventNode, path, treeIndexes);
    }

    /**
     * Loads the initial folder structure for the tree
     */
    private loadInitialFolders(): void {
        this.getFolderByPath(this.initialPath).subscribe({
            next: (response) => {
                this.folders.set(response);
            },
            error: (error) => {
                console.error('Error loading initial folders:', error);
                this.folders.set([]);
            }
        });
    }

    /**
     * Loads child folders for a specific node
     * @param parentNode - The parent node to load children for
     * @param path - The path of the parent node
     * @param treeIndexes - Array of indices representing the path to the node in the tree structure
     */
    private loadChildFolders(parentNode: TreeNode, path: string, treeIndexes?: number[]): void {
        this.getFolderByPath(`${this.initialPath}${path}`, treeIndexes).subscribe({
            next: (subFolders) => {
                this.updateFolderNode(parentNode, subFolders);
            },
            error: (error) => {
                console.error('Error loading child folders:', error);
            }
        });
    }

    /**
     * Updates a node with its children and refreshes the tree
     * @param parentNode - The node to update
     * @param children - The child nodes to add
     */
    private updateFolderNode(parentNode: TreeNode, children: TreeNode[]): void {
        if (children.length === 0) {
            return;
        }

        const treeIndexes = parentNode.data?.treeIndexes;

        this.folders.update(() => {
            const updatedFolders = [...this.folders()];
            const currentFolder = this.findFolderNode(treeIndexes, updatedFolders);

            // Updated by Reference
            if (currentFolder) {
                currentFolder.children = children;
            }

            return updatedFolders;
        });

        this.cd.markForCheck();
    }

    /**
     * Fetches folders from the API for a given path and transforms them into TreeNode format
     * @param assetPath - The path to fetch folders for
     * @param treeIndexes - Array of indices representing the path to the parent node in the tree structure
     * @returns Observable of TreeNode array
     */
    private getFolderByPath(assetPath: string, treeIndexes?: number[]) {
        return this.httpClient
            .post<{ entity: { subFolders?: DotCMSFolder[] } }>(this.endpoint, {
                assetPath: assetPath
            })
            .pipe(
                map(({ entity }) =>
                    this.transformFoldersToTreeNodes(entity.subFolders || [], treeIndexes)
                ),
                catchError((error) => {
                    console.error('Error fetching folders:', error);
                    return of([]);
                })
            );
    }

    /**
     * Transforms DotCMS folder data into PrimeNG TreeNode format
     * @param folders - Array of DotCMS folders
     * @param treeIndexes - Array of indices representing the path to the parent node in the tree structure
     * @returns Array of TreeNodes with treeIndexes data for efficient tree navigation
     */
    private transformFoldersToTreeNodes(
        folders: DotCMSFolder[],
        treeIndexes?: number[]
    ): TreeNode[] {
        if (!folders || folders.length === 0) {
            return [];
        }

        return folders.map((folder, index) => ({
            key: folder.inode,
            label: folder.name,
            data: {
                path: folder.path.replace(/^\//, ''),
                treeIndexes: treeIndexes ? [...treeIndexes, index] : [index]
            },
            icon: 'pi pi-folder',
            expandedIcon: 'pi pi-folder-open',
            collapsedIcon: 'pi pi-folder',
            leaf: false
        }));
    }

    private findFolderNode(
        treeIndexes: number[],
        updatedFolders: TreeNode<FolderTreeData>[]
    ): TreeNode<FolderTreeData> | undefined {
        let currentFolder: TreeNode<FolderTreeData> | undefined;
        treeIndexes?.forEach((index) => {
            if (!currentFolder) {
                currentFolder = updatedFolders[index];
            } else {
                currentFolder = currentFolder.children?.[index];
            }
        });

        return currentFolder;
    }
}
