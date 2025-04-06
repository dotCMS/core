import { CommonModule } from '@angular/common';
import { Component, ElementRef, OnInit, Renderer2, inject } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';

import { MenuItem, TreeNode } from 'primeng/api';
import { BreadcrumbModule } from 'primeng/breadcrumb';
import { ButtonModule } from 'primeng/button';
import { TableModule } from 'primeng/table';
import { TabViewModule } from 'primeng/tabview';
import { TooltipModule } from 'primeng/tooltip';
import { TreeModule } from 'primeng/tree';

import { take } from 'rxjs/operators';

import { DotESContentService, DotRouterService, queryEsParams } from '@dotcms/data-access';
import { DotCMSContentlet, ESContent } from '@dotcms/dotcms-models';
import { DotContentletThumbnailComponent, DotContentletIconComponent } from '@dotcms/ui';

import { MOCK_FOLDERS } from './drive.mock';

interface PathInfo {
    segments: string[];
    fullPath: string;
    normalizedPath: string;
}

@Component({
    selector: 'lib-drive',
    standalone: true,
    imports: [
        CommonModule,
        TableModule,
        TreeModule,
        BreadcrumbModule,
        ButtonModule,
        TooltipModule,
        TabViewModule,
        DotContentletThumbnailComponent,
        DotContentletIconComponent
    ],
    providers: [DotESContentService],
    templateUrl: './drive.component.html',
    styleUrl: './drive.component.scss'
})
export class DriveComponent implements OnInit {
    private dotRouterService: DotRouterService = inject(DotRouterService);
    private route: ActivatedRoute = inject(ActivatedRoute);
    private router: Router = inject(Router);

    items: DotCMSContentlet[] = [];
    loading = false;
    selectedContentlet: DotCMSContentlet | null = null;

    // Tree related properties
    files: TreeNode[] = [];
    selectedFile: TreeNode | null = null;

    // Breadcrumb items
    breadcrumbItems: MenuItem[] = [];
    breadcrumbHome: MenuItem = { icon: 'pi pi-home', routerLink: '/' };

    isInfoVisible = false;
    activeTabIndex = 0;

    constructor(
        private dotESContentService: DotESContentService,
        private elementRef: ElementRef,
        private renderer: Renderer2
    ) {}

    ngOnInit() {
        // Initialize with path from URL, then subscribe to future changes
        const initialPath = this.getCurrentPathFromUrl();
        if (initialPath) {
            this.navigateToPathFromUrl(initialPath);
        } else {
            this.initializeFileTree();
            this.loadContent();
        }

        // Listen for route changes to update content when URL changes directly
        this.route.url.subscribe(() => {
            const currentPath = this.getCurrentPathFromUrl();
            if (currentPath) {
                this.navigateToPathFromUrl(currentPath);
            }
        });
    }

    editContentlet(contentlet: DotCMSContentlet): void {
        if (contentlet.baseType === 'HTMLPAGE') {
            this.dotRouterService.goToEditPage(contentlet);

            return;
        }

        this.dotRouterService.goToEditContentlet(contentlet.inode);
    }

    private loadContent(path = '/*'): void {
        this.loading = true;

        // Hide base type 8 (language variables)
        const params: queryEsParams = {
            query: `+path:${path} -basetype:8`,
            itemsPerPage: 40
        };

        this.dotESContentService
            .get(params)
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
                node.children.forEach((child) => processNode(child, node));
            }

            return node;
        };

        return nodes.map((node) => processNode(node));
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

    // Get current path from URL
    private getCurrentPathFromUrl(): string {
        const url = this.router.url;
        const driveIndex = url.indexOf('/drive');
        if (driveIndex !== -1 && url.length > driveIndex + 6) {
            return url.substring(driveIndex + 6); // +6 to skip '/drive'
        }

        return '';
    }

    // Parse path into segments and normalized forms
    private parsePath(path: string): PathInfo {
        // Normalize path to ensure it starts with a slash
        const normalizedPath = path.startsWith('/') ? path : `/${path}`;
        const segments = normalizedPath.split('/').filter(segment => segment);

        return {
            segments,
            fullPath: normalizedPath,
            normalizedPath: segments.length ? normalizedPath : '/'
        };
    }

    // Navigate to a path from URL
    private navigateToPathFromUrl(path: string): void {
        const pathInfo = this.parsePath(path);

        // Initialize the tree first
        this.initializeFileTree();

        if (pathInfo.segments.length === 0) {
            // If no path segments, just load the root content
            this.loadContent();

            return;
        }

        // Find and expand the tree nodes matching the path
        let currentNodes = this.files;
        let lastMatchedNode: TreeNode | null = null;
        let currentPath = '';

        // Traverse the tree to find the node matching the path
        for (const segment of pathInfo.segments) {
            currentPath += `/${segment}`;
            const matchingNode = currentNodes.find(node => node.label === segment);

            if (matchingNode) {
                matchingNode.expanded = true;
                lastMatchedNode = matchingNode;
                currentNodes = matchingNode.children || [];
            } else {
                // If we can't find a matching node, break and use the last valid node found
                break;
            }
        }

        // If we found a matching node, select it
        if (lastMatchedNode) {
            this.selectedFile = lastMatchedNode;
            this.updateBreadcrumb(lastMatchedNode);
            this.loadContent(`${currentPath}/*`);
        } else {
            this.loadContent();
        }
    }

    // Enhanced method to handle tree node selection with better path handling
    onNodeSelect(event: { node: TreeNode }): void {
        const selectedNode = event.node;
        const path = this.getNodePath(selectedNode);

        // Load content and update UI
        this.loadContent(`${path}/*`);
        this.updateBreadcrumb(selectedNode);
        this.updateBrowserUrl(path);
    }

    // Update browser URL without navigation
    private updateBrowserUrl(path: string): void {
        // No special handling needed, the path from getNodePath is already normalized
        this.router.navigate(['/drive' + path], { replaceUrl: true });
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

        // Load content and update URL
        this.loadContent(`${path}/*`);
        this.updateBrowserUrl(path);
    }

    // Method to handle table row selection
    onRowSelect(contentlet: DotCMSContentlet): void {
        this.selectedContentlet = contentlet;

        // Auto-show the info panel when a row is selected
        if (!this.isInfoVisible) {
            this.onInfoClick();
        }
    }

    // Method to handle info button click
    onInfoClick(): void {
        this.isInfoVisible = !this.isInfoVisible;

        if (this.isInfoVisible) {
            this.renderer.addClass(this.elementRef.nativeElement, 'show-info');
        } else {
            this.renderer.removeClass(this.elementRef.nativeElement, 'show-info');
        }
    }

    // Method to format date for display
    formatDate(dateStr: string): string {
        if (!dateStr) return '';

        const date = new Date(dateStr);

        return date.toLocaleDateString('en-US', {
            month: 'short',
            day: 'numeric',
            year: 'numeric'
        });
    }

    // Get file size in a human-readable format
    getFileSize(contentlet: DotCMSContentlet & { size: number }): string {
        const fileSize = contentlet.size;

        if (!fileSize) {
            return null;
        }

        if (fileSize < 1024) {
            return `${fileSize} B`;
        } else if (fileSize < 1024 * 1024) {
            return `${Math.round(fileSize / 1024)} KB`;
        } else {
            return `${Math.round(fileSize / (1024 * 1024))} MB`;
        }
    }

    getContentletPath({ URL_MAP_FOR_CONTENT, path, urlMap }: DotCMSContentlet): string {
        return urlMap || URL_MAP_FOR_CONTENT || path;
    }
}
