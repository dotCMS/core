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
    protected readonly folders = signal<TreeNode[]>([]);

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

        // Skip if children are already loaded
        if (eventNode.children?.length > 0) {
            return;
        }

        const path = eventNode.data?.path;
        if (!path) {
            console.warn('Node data or path is missing for expansion');
            return;
        }

        this.loadChildFolders(eventNode, path);
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
     */
    private loadChildFolders(parentNode: TreeNode, path: string): void {
        this.getFolderByPath(`${this.initialPath}${path}`).subscribe({
            next: (subFolders) => {
                this.updateNodeWithChildren(parentNode, subFolders);
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
    private updateNodeWithChildren(parentNode: TreeNode, children: TreeNode[]): void {
        if (children.length === 0) {
            return;
        }

        this.folders.update((folders) => {
            return this.updateNodeRecursively(folders, parentNode.key, children);
        });

        this.cd.markForCheck();
    }

    /**
     * Recursively searches and updates a node at any level in the tree
     * @param nodes - The current level of nodes to search
     * @param nodeKey - The key of the node to update
     * @param children - The children to add to the found node
     * @returns Updated nodes array
     */
    private updateNodeRecursively(
        nodes: TreeNode[],
        nodeKey: string,
        children: TreeNode[]
    ): TreeNode[] {
        return nodes.map((node) => {
            // If this is the node we're looking for, update it with children
            if (node.key === nodeKey) {
                return { ...node, children };
            }

            // If this node has children, recursively search them
            if (node.children && node.children.length > 0) {
                return {
                    ...node,
                    children: this.updateNodeRecursively(node.children, nodeKey, children)
                };
            }

            // Return the node unchanged if it's not the target and has no children
            return node;
        });
    }

    /**
     * Fetches folders from the API for a given path and transforms them into TreeNode format
     * @param assetPath - The path to fetch folders for
     * @returns Observable of TreeNode array
     */
    private getFolderByPath(assetPath: string) {
        return this.httpClient
            .post<{ entity: { subFolders?: DotCMSFolder[] } }>(this.endpoint, {
                assetPath: assetPath
            })
            .pipe(
                map(({ entity }) => this.transformFoldersToTreeNodes(entity.subFolders || [])),
                catchError((error) => {
                    console.error('Error fetching folders:', error);
                    return of([]);
                })
            );
    }

    /**
     * Transforms DotCMS folder data into PrimeNG TreeNode format
     * @param folders - Array of DotCMS folders
     * @returns Array of TreeNodes
     */
    private transformFoldersToTreeNodes(folders: DotCMSFolder[]): TreeNode[] {
        if (!folders || folders.length === 0) {
            return [];
        }

        return folders.map((folder) => ({
            key: folder.inode,
            label: folder.name,
            data: {
                ...folder,
                path: folder.path.replace(/^\//, '')
            },
            icon: 'pi pi-folder',
            expandedIcon: 'pi pi-folder-open',
            collapsedIcon: 'pi pi-folder',
            leaf: false
        }));
    }
}
