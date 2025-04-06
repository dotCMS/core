import { CommonModule } from '@angular/common';
import { Component, ElementRef, OnInit, Renderer2, computed, effect, inject, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';

import { MenuItem, TreeNode } from 'primeng/api';
import { BreadcrumbModule } from 'primeng/breadcrumb';
import { ButtonModule } from 'primeng/button';
import { DropdownModule } from 'primeng/dropdown';
import { PaginatorModule } from 'primeng/paginator';
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
        SearchFormComponent,
        DropdownModule,
        PaginatorModule
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

    rows2 = 40;

    paginatorOptions = [
        { label: 20, value: 20 },
        { label: 40, value: 40 },
        { label: 60, value: 80 },
        { label: 80, value: 80 }
    ];

    first2 = 0;

    items = signal<DotCMSContentlet[]>([]);
    loading = signal<boolean>(false);
    selectedContentlet = signal<DotCMSContentlet | null>(null);

    // New signals for managing state
    currentPath = signal<string>('/*');
    baseTypes = signal<string>('');

    // Tree related properties
    files = signal<TreeNode[]>([]);
    selectedFile = signal<TreeNode | null>(null);

    // Breadcrumb items
    breadcrumbItems = signal<MenuItem[]>([]);
    breadcrumbHome = signal<MenuItem>({
        icon: 'pi pi-home',
        command: () => {
            this.router.navigate(['/drive'], {
                queryParams: {},
                replaceUrl: true
            });
            this.currentPath.set('/*');
            this.breadcrumbItems.set([]);
            this.selectedFile.set(null);
            this.initializeFileTree();
        }
    });

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

    constructor() {
        // Effect to load content whenever path or baseTypes change
        // TODO: Too much vibe coding here.
        effect(() => {
            this.loadContent();
        }, { allowSignalWrites: true });
    }

    ngOnInit() {
        // Initialize with path from URL, then subscribe to future changes
        const initialPath = this.getCurrentPathFromUrl();
        if (initialPath) {
            this.navigateToPathFromUrl(initialPath);
        } else {
            this.initializeFileTree();
            // Initial content will load via the effect
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

        // Update path signal which triggers the content loading via effect
        this.currentPath.set(`${path}/*`);
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

    onSearch(formData: { searchQuery: string; selectedTypes: { name: string; value: number }[] }): void {
        const types = formData.selectedTypes.map(item => `basetype:${item.value}`);
        const query = `+(${types.join(' ')})`;

        // Update the baseTypes signal which will trigger content loading via effect
        this.baseTypes.set(query);
    }

    onPageChange2(event) {
        console.log(event)

    }

    private loadContent(): void {
        this.loading.set(true);

        // Use the signal values
        const params: queryEsParams = {
            query: `+path:${this.currentPath()} ${this.baseTypes()}`,
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
            this.currentPath.set('/*');

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
            this.currentPath.set(`${currentPath}/*`);
        } else {
            this.currentPath.set('/*');
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

        // Update path signal which will trigger content loading
        this.currentPath.set(`${path}/*`);
        this.updateBrowserUrl(path);
    }
}
