import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';

import { MenuItem, TreeNode } from 'primeng/api';
import { BreadcrumbModule } from 'primeng/breadcrumb';
import { TableModule } from 'primeng/table';
import { TreeModule } from 'primeng/tree';

import { take } from 'rxjs/operators';

import { DotESContentService, queryEsParams } from '@dotcms/data-access';
import { DotCMSContentlet, ESContent } from '@dotcms/dotcms-models';
import { DotContentletThumbnailComponent } from '@dotcms/ui';

import { MOCK_FOLDERS } from './drive.mock';

@Component({
    selector: 'lib-drive',
    standalone: true,
    imports: [
        CommonModule,
        TableModule,
        TreeModule,
        BreadcrumbModule,
        DotContentletThumbnailComponent
    ],
    providers: [DotESContentService],
    templateUrl: './drive.component.html',
    styleUrl: './drive.component.scss'
})
export class DriveComponent implements OnInit {
    items: DotCMSContentlet[] = [];
    loading = false;

    // Tree related properties
    files: TreeNode[] = [];
    selectedFile: TreeNode | null = null;

    // Breadcrumb items
    breadcrumbItems: MenuItem[] = [];
    breadcrumbHome: MenuItem = { icon: 'pi pi-home', routerLink: '/' };

    constructor(private dotESContentService: DotESContentService) {}

    ngOnInit() {
        this.loadContent();
        this.initializeFileTree();
    }

    private loadContent(path = '/*'): void {
        this.loading = true;

        // Hide base type 8 (language variables)
        const params: queryEsParams = {
            query: `+path:${path} -basetype:8`,
            itemsPerPage: 40
        };

        this.dotESContentService.get(params)
            .pipe(take(1))
            .subscribe((response: ESContent) => {
                this.items = response.jsonObjectView.contentlets;
                this.loading = false;
            });
    }

    private initializeFileTree(): void {
        // Use the mock folder structure and process it to add parent references
        this.files = this.prepareTreeNodes(MOCK_FOLDERS);
    }

    // Prepare tree nodes by adding parent references for path traversal
    private prepareTreeNodes(nodes: TreeNode[]): TreeNode[] {
        const processNode = (node: TreeNode, parent?: TreeNode) => {
            // Set parent reference to allow path traversal
            node.parent = parent;

            if (node.children) {
                node.children.forEach(child => processNode(child, node));
            }

            return node;
        };

        return nodes.map(node => processNode(node));
    }

    // Get the full path of a node by traversing up the tree
    private getNodePath(node: TreeNode): string {
        const pathParts: string[] = [];
        let currentNode: TreeNode | undefined = node;

        // Traverse up the tree using parent references
        while (currentNode) {
            pathParts.unshift(currentNode.label as string);
            currentNode = currentNode.parent;
        }

        return '/' + pathParts.join('/');
    }

    // Handle tree node selection
    onNodeSelect(event: { node: TreeNode }): void {
        const selectedNode = event.node;
        const path = this.getNodePath(selectedNode);
        this.loadContent(`${path}/*`);

        // Update breadcrumb when a node is selected
        this.updateBreadcrumb(selectedNode);
    }

    // Update breadcrumb based on selected node
    private updateBreadcrumb(node: TreeNode): void {
        const breadcrumbItems: MenuItem[] = [];
        let currentNode: TreeNode | undefined = node;

        // Build the breadcrumb items in reverse order (from selected node to root)
        while (currentNode) {
            breadcrumbItems.unshift({
                label: currentNode.label as string,
                command: () => this.navigateToNode(currentNode as TreeNode)
            });
            currentNode = currentNode.parent;
        }

        this.breadcrumbItems = breadcrumbItems;
    }

    // Navigate to a specific node when clicking breadcrumb item
    private navigateToNode(node: TreeNode): void {
        this.selectedFile = node;
        const path = this.getNodePath(node);
        this.loadContent(`${path}/*`);
    }
}
