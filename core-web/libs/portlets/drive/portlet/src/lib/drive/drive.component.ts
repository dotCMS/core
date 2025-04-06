import { CommonModule } from '@angular/common';
import { Component, ElementRef, OnInit, Renderer2, computed, inject, signal } from '@angular/core';
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
import { SearchFormComponent } from './search-form/search-form.component';

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
        DotContentletIconComponent,
        SearchFormComponent
    ],
    providers: [DotESContentService],
    templateUrl: './drive.component.html',
    styleUrl: './drive.component.scss'
})
export class DriveComponent implements OnInit {
    private dotRouterService: DotRouterService = inject(DotRouterService);
    private route: ActivatedRoute = inject(ActivatedRoute);
    private router: Router = inject(Router);
    private dotESContentService: DotESContentService = inject(DotESContentService);
    private elementRef: ElementRef = inject(ElementRef);
    private renderer: Renderer2 = inject(Renderer2);

    items = signal<DotCMSContentlet[]>([]);
    loading = signal<boolean>(false);
    selectedContentlet = signal<DotCMSContentlet | null>(null);

    // Tree related properties
    files = signal<TreeNode[]>([]);
    selectedFile = signal<TreeNode | null>(null);

    // Breadcrumb items
    breadcrumbItems = signal<MenuItem[]>([]);
    breadcrumbHome = signal<MenuItem>({ icon: 'pi pi-home', routerLink: '/' });

    isInfoVisible = signal<boolean>(false);
    activeTabIndex = signal<number>(0);
    activeTabIndex2 = signal<number>(0);
    // Computed signals for derived data
    formatDate = computed(() => {
        return (dateStr: string): string => {
            if (!dateStr) return '';

            const date = new Date(dateStr);

            return date.toLocaleDateString('en-US', {
                month: 'short',
                day: 'numeric',
                year: 'numeric'
            });
        };
    });

    // Get file size in a human-readable format
    getFileSize = computed(() => {
        return (contentlet: DotCMSContentlet & { size: number }): string => {
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
        };
    });

    getContentletPath = computed(() => {
        return ({ URL_MAP_FOR_CONTENT, path, urlMap }: DotCMSContentlet): string => {
            return urlMap || URL_MAP_FOR_CONTENT || path;
        };
    });

    ngOnInit() {
        // Initialize with path from URL, then subscribe to future changes
        const initialPath = this.getCurrentPathFromUrl();
        if (initialPath) {
            this.navigateToPathFromUrl(initialPath);
        } else {
            this.initializeFileTree();
            this.loadContent();
        }

        // Listen for route query parameter changes
        this.route.queryParamMap.subscribe((params) => {
            const currentPath = params.get('path') || '';
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

        // Preserve the current path in query params when navigating to the detail page
        const currentPath = this.route.snapshot.queryParams['path'] || '';
        this.router.navigate([`/drive/${contentlet.inode}`], {
            queryParams: { path: currentPath }
        });
    }

    onNodeSelect(event: { node: TreeNode }): void {
        const selectedNode = event.node;
        const path = this.getNodePath(selectedNode);

        // Load content and update UI
        this.loadContent(`${path}/*`);
        this.updateBreadcrumb(selectedNode);
        this.updateBrowserUrl(path);
    }

    // Method to handle table row selection
    onRowSelect(contentlet: DotCMSContentlet): void {
        this.selectedContentlet.set(contentlet);

        // Auto-show the info panel when a row is selected
        if (!this.isInfoVisible()) {
            this.onInfoClick();
        }
    }

    // Method to handle info button click
    onInfoClick(): void {
        this.isInfoVisible.update(value => !value);

        if (this.isInfoVisible()) {
            this.renderer.addClass(this.elementRef.nativeElement, 'show-info');
        } else {
            this.renderer.removeClass(this.elementRef.nativeElement, 'show-info');
        }
    }

    private loadContent(path = '/*'): void {
        this.loading.set(true);

        // Hide base type 8 (language variables)
        const params: queryEsParams = {
            query: `+path:${path} -basetype:8`,
            itemsPerPage: 40
        };

        this.dotESContentService
            .get(params)
            .pipe(take(1))
            .subscribe((response: ESContent) => {
                this.items.set(response.jsonObjectView.contentlets);
                this.loading.set(false);
            });
    }

    private initializeFileTree(): void {
        // Use the mock folder structure and process it to add parent references
        this.files.set(this.prepareTreeNodes(MOCK_FOLDERS));
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
        const queryParams = this.route.snapshot.queryParams;

        return queryParams['path'] || '';
    }

    // Parse path into segments and normalized forms
    private parsePath(path: string): PathInfo {
        // Normalize path to ensure it starts with a slash
        const normalizedPath = path.startsWith('/') ? path : `/${path}`;
        const segments = normalizedPath.split('/').filter((segment) => segment);

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
        let currentNodes = this.files();
        let lastMatchedNode: TreeNode | null = null;
        let currentPath = '';

        // Traverse the tree to find the node matching the path
        for (const segment of pathInfo.segments) {
            currentPath += `/${segment}`;
            const matchingNode = currentNodes.find((node) => node.label === segment);

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
            this.selectedFile.set(lastMatchedNode);
            this.updateBreadcrumb(lastMatchedNode);
            this.loadContent(`${currentPath}/*`);
        } else {
            this.loadContent();
        }
    }

    // Update browser URL without navigation
    private updateBrowserUrl(path: string): void {
        this.router.navigate(['/drive'], {
            queryParams: { path },
            replaceUrl: true
        });
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

        this.breadcrumbItems.set(breadcrumbItems);
    }

    // Navigate to a specific node when clicking breadcrumb item
    private navigateToNode(node: TreeNode): void {
        this.selectedFile.set(node);
        const path = this.getNodePath(node);

        // Load content and update URL
        this.loadContent(`${path}/*`);
        this.updateBrowserUrl(path);
    }

    // Method to handle search form submission
    onSearch(formData: { searchQuery: string; selectedTypes: { name: string; value: number }[] }): void {
        if (!formData.searchQuery && (!formData.selectedTypes || formData.selectedTypes.length === 0)) {
            return;
        }

        this.loading.set(true);

        let queryString = '';

        // Add text search if provided
        if (formData.searchQuery) {
            queryString += `+text:*${formData.searchQuery}*`;
        }

        // Add content type filters if selected
        if (formData.selectedTypes && formData.selectedTypes.length > 0) {
            // Create a list of base types to include
            const typeValues = formData.selectedTypes.map(type => type.value);

            // Always exclude language variables (basetype 8) if not explicitly selected
            if (!typeValues.includes(3)) {
                queryString += ' -basetype:8';
            }

            // If specific types are selected, add them to the query
            if (typeValues.length > 0 && typeValues.length < 5) {
                queryString += ' +(';

                // Map content type values to appropriate basetype values in the query
                typeValues.forEach((value, index) => {
                    if (index > 0) {
                        queryString += ' OR ';
                    }

                    // Map our UI values to actual basetype values in the system
                    switch (value) {
                        case 1: // Content
                            queryString += 'basetype:1';
                            break;

                        case 2: // Pages
                            queryString += 'basetype:5';
                            break;

                        case 3: // Language Variables
                            queryString += 'basetype:8';
                            break;

                        case 4: // Widgets
                            queryString += 'basetype:6';
                            break;

                        case 5: // Files
                            queryString += 'basetype:3';
                            break;
                    }
                });

                queryString += ')';
            }
        } else {
            // Default exclusion
            queryString += ' -basetype:8';
        }

        const params: queryEsParams = {
            query: queryString || '+contenttype:*',
            itemsPerPage: 40
        };

        this.dotESContentService
            .get(params)
            .pipe(take(1))
            .subscribe((response: ESContent) => {
                this.items.set(response.jsonObjectView.contentlets);
                this.loading.set(false);
            });
    }
}
