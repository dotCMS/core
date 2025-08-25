import { of, Observable } from 'rxjs';

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

import {
    FOLDER_TREE_API_ENDPOINT,
    FOLDER_TREE_INITIAL_PATH,
    FOLDER_ICONS
} from '../../shared/constants';
import { DotCMSFolder, FolderTreeData } from '../../shared/models';
import { DotContentDriveStore } from '../../store/dot-content-drive.store';

// Type aliases for better readability
type FolderTreeNode = TreeNode<FolderTreeData>;
type FolderApiResponse = { entity: { subFolders?: DotCMSFolder[] } };

/**
 * Component for displaying and managing the folder tree structure in the content drive.
 *
 * Features:
 * - Lazy loading of folder children on expansion
 * - Efficient tree navigation using index paths
 * - Type-safe folder tree operations
 * - Error handling and loading states
 */
@Component({
    selector: 'dot-content-drive-folder-tree',
    templateUrl: './dot-content-drive-folder-tree.component.html',
    styleUrl: './dot-content-drive-folder-tree.component.scss',
    imports: [TreeModule],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotContentDriveFolderTreeComponent implements OnInit {
    private readonly httpClient = inject(HttpClient);
    private readonly cd = inject(ChangeDetectorRef);
    private readonly store = inject(DotContentDriveStore);

    private readonly endpoint = FOLDER_TREE_API_ENDPOINT;
    private readonly initialPath = FOLDER_TREE_INITIAL_PATH;
    protected readonly folders = signal<FolderTreeNode[]>([]);

    /**
     * Component initialization - loads the initial folder structure
     */
    ngOnInit(): void {
        // Get current site before loading the initial folders
        this.loadInitialFolders();
    }

    /**
     * Handles node expansion events - loads child folders when a node is expanded
     * @param event - The TreeNodeExpandEvent containing the expanded node
     */
    onNodeExpand(event: TreeNodeExpandEvent): void {
        const eventNode = event.node;
        const { path, treeIndexes } = eventNode.data;
        this.store.setPath(path);

        // Early returns for edge cases
        if (this.shouldSkipExpansion(eventNode)) {
            return;
        }

        this.loadChildFolders(eventNode, path, treeIndexes);
    }

    /**
     * Loads the initial folder structure for the tree
     */
    private loadInitialFolders(): void {
        this.getFolderByPath(this.initialPath).subscribe({
            next: (folders) => this.folders.set(folders),
            error: (error) => this.handleFolderLoadError('initial folders', error)
        });
    }

    /**
     * Loads child folders for a specific node
     * @param parentNode - The parent node to load children for
     * @param path - The path of the parent node
     * @param treeIndexes - Array of indices representing the path to the node in the tree structure
     */
    private loadChildFolders(
        parentNode: FolderTreeNode,
        path: string,
        treeIndexes: number[]
    ): void {
        const fullPath = `${this.initialPath}${this.cleanFolderPath(path)}`;
        parentNode.loading = true;

        this.getFolderByPath(fullPath, treeIndexes).subscribe({
            next: (subFolders) => this.updateFolderNode(parentNode, subFolders),
            error: (error) => this.handleFolderLoadError('child folders', error)
        });
    }

    /**
     * Fetches folders from the API for a given path and transforms them into TreeNode format
     * @param assetPath - The path to fetch folders for
     * @param treeIndexes - Array of indices representing the path to the parent node in the tree structure
     * @returns Observable of TreeNode array
     */
    private getFolderByPath(
        assetPath: string,
        treeIndexes?: number[]
    ): Observable<FolderTreeNode[]> {
        return this.httpClient.post<FolderApiResponse>(this.endpoint, { assetPath }).pipe(
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
     * Updates a node with its children and refreshes the tree
     * @param parentNode - The node to update
     * @param children - The child nodes to add
     */
    private updateFolderNode(parentNode: FolderTreeNode, children: FolderTreeNode[]): void {
        if (children.length === 0) {
            parentNode.loading = false;
            this.cd.markForCheck();

            return;
        }

        const { treeIndexes } = parentNode.data;

        this.folders.update(() => {
            const updatedFolders = [...this.folders()];
            const targetNode = this.findFolderNode(treeIndexes, updatedFolders);

            // Updated the children and loading state by reference
            if (targetNode) {
                targetNode.children = children;
            }

            parentNode.loading = false;

            return updatedFolders;
        });

        this.cd.markForCheck();
    }

    /**
     * Finds a specific node in the tree using its index path
     * @param treeIndexes - Array of indices representing the path to the node
     * @param folders - The folder tree to search in
     * @returns The found node or undefined
     */
    private findFolderNode(
        treeIndexes: number[],
        folders: FolderTreeNode[]
    ): FolderTreeNode | undefined {
        if (!treeIndexes?.length) {
            return undefined;
        }

        let currentNode: FolderTreeNode | undefined;

        for (const index of treeIndexes) {
            if (!currentNode) {
                currentNode = folders[index];
            } else {
                currentNode = currentNode.children?.[index];
            }

            // Early exit if path is invalid
            if (!currentNode) {
                console.warn('Invalid tree index path:', treeIndexes);
                return undefined;
            }
        }

        return currentNode;
    }

    /**
     * Transforms DotCMS folder data into PrimeNG TreeNode format
     * @param folders - Array of DotCMS folders
     * @param parentTreeIndexes - Array of indices representing the path to the parent node
     * @returns Array of TreeNodes with treeIndexes data for efficient tree navigation
     */
    private transformFoldersToTreeNodes(
        folders: DotCMSFolder[],
        parentTreeIndexes?: number[]
    ): FolderTreeNode[] {
        if (!folders?.length) {
            return [];
        }

        return folders.map((folder, index) =>
            this.createTreeNode(folder, index, parentTreeIndexes)
        );
    }

    /**
     * Creates a single TreeNode from a DotCMS folder
     * @param folder - The DotCMS folder data
     * @param index - The index of this folder in its parent's children array
     * @param parentTreeIndexes - The parent's tree index path
     * @returns A properly formatted TreeNode
     */
    private createTreeNode(
        folder: DotCMSFolder,
        index: number,
        parentTreeIndexes?: number[]
    ): FolderTreeNode {
        return {
            key: folder.inode,
            label: folder.name,
            data: {
                path: folder.path,
                treeIndexes: this.buildTreeIndexes(parentTreeIndexes, index)
            },
            icon: FOLDER_ICONS.FOLDER,
            expandedIcon: FOLDER_ICONS.FOLDER_OPEN,
            collapsedIcon: FOLDER_ICONS.FOLDER_CLOSED,
            leaf: false
        };
    }

    /**
     * Determines if node expansion should be skipped
     * @param node - The node to check
     * @returns True if expansion should be skipped
     */
    private shouldSkipExpansion(node: TreeNode): boolean {
        // Skip if children are already loaded
        if (node.children?.length > 0) {
            return true;
        }

        // Skip if no path data available
        if (!node.data?.path) {
            console.warn('Node data or path is missing for expansion');
            return true;
        }

        return false;
    }

    /**
     * Builds the tree indexes array for a new node
     * @param parentIndexes - The parent's tree indexes
     * @param currentIndex - The current node's index
     * @returns The complete tree index path
     */
    private buildTreeIndexes(parentIndexes?: number[], currentIndex?: number): number[] {
        if (currentIndex === undefined) {
            return [];
        }

        return parentIndexes ? [...parentIndexes, currentIndex] : [currentIndex];
    }

    /**
     * Cleans the folder path by removing leading slashes
     * @param path - The raw folder path
     * @returns The cleaned path
     */
    private cleanFolderPath(path: string): string {
        return path.replace(/^\//, '');
    }

    /**
     * Handles folder loading errors consistently
     * @param context - Description of what was being loaded
     * @param error - The error that occurred
     */
    private handleFolderLoadError(context: string, error: unknown): void {
        console.error(`Error loading ${context}:`, error);
        this.folders.set([]);
    }
}
