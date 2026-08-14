import { Subject } from 'rxjs';

import { DatePipe } from '@angular/common';
import { Component, computed, debounced, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { ButtonModule } from 'primeng/button';
import { DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';
import { IconFieldModule } from 'primeng/iconfield';
import { InputIconModule } from 'primeng/inputicon';
import { InputTextModule } from 'primeng/inputtext';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { TooltipModule } from 'primeng/tooltip';
import type { TreeNodeExpandEvent, TreeNodeSelectEvent } from 'primeng/types/tree';

import { catchError, map, switchMap, take, tap } from 'rxjs/operators';

import {
    DotExperimentsService,
    DotHttpErrorManagerService,
    DotPageBrowserFolder,
    DotPageBrowserPage,
    DotPageBrowserState,
    DotPagesBrowserService
} from '@dotcms/data-access';
import {
    createLoadMoreTreeNode,
    DOT_FOLDER_TREE_PAGE_SIZE,
    DotExperimentStatus,
    isTreeNodeContentData,
    isTreeNodeLoadMoreData,
    TreeNodeItem
} from '@dotcms/dotcms-models';
import { GlobalStore } from '@dotcms/store';
import { DotFolderTreeComponent, DotMessagePipe } from '@dotcms/ui';

import {
    DotExperimentsSelectPageDialogData,
    SelectPageDialogViewRow
} from './dot-experiments-select-page-dialog.models';

import { SEARCH_DEBOUNCE_MS } from '../../../shared/constants';
import { SelectPageDialogRowState, TagSeverity } from '../../../shared/models';

/** Path of the site root, and the key of the tree's root node. */
const SITE_ROOT_PATH = '/';

/** How many characters of a template identifier the Template column shows. */
const TEMPLATE_ID_PREVIEW_LENGTH = 8;

/** i18n key of the tooltip on rows that already host a non-archived experiment. */
const ROW_DISABLED_TOOLTIP_KEY = 'experiments.configure.select-page.row.disabled.tooltip';

/** i18n key of the State column's tag, per publication state. */
const STATE_LABEL_KEYS: Record<DotPageBrowserState, string> = {
    [DotPageBrowserState.PUBLISHED]: 'experiments.configure.select-page.state.published',
    [DotPageBrowserState.CHANGED]: 'experiments.configure.select-page.state.changed',
    [DotPageBrowserState.DRAFT]: 'experiments.configure.select-page.state.draft',
    [DotPageBrowserState.ARCHIVED]: 'experiments.configure.select-page.state.archived'
};

/** Severity of the State column's tag, per publication state. */
const STATE_SEVERITIES: Record<DotPageBrowserState, TagSeverity> = {
    [DotPageBrowserState.PUBLISHED]: 'success',
    [DotPageBrowserState.CHANGED]: 'warn',
    [DotPageBrowserState.DRAFT]: 'info',
    [DotPageBrowserState.ARCHIVED]: 'secondary'
};

/**
 * The shared row contract only distinguishes live / working / draft, so the two states that mean
 * "there is no live version" both narrow to `draft`.
 */
const ROW_STATES: Record<DotPageBrowserState, SelectPageDialogRowState> = {
    [DotPageBrowserState.PUBLISHED]: 'live',
    [DotPageBrowserState.CHANGED]: 'working',
    [DotPageBrowserState.DRAFT]: 'draft',
    [DotPageBrowserState.ARCHIVED]: 'draft'
};

/**
 * Picks the page an experiment runs on.
 *
 * A folder tree scopes the listing and a text box narrows it further; pages that already host a
 * non-archived experiment are listed but cannot be picked, so the reason they are unavailable is
 * visible instead of the page silently missing.
 *
 * Opened with `DialogService.open()` and closed with the picked row, or `undefined` on cancel.
 * The caller owns the dialog chrome (title, close button, 900×560 sizing) — this component renders
 * only the dialog body.
 */
@Component({
    selector: 'dot-experiments-select-page-dialog',
    imports: [
        DatePipe,
        ButtonModule,
        IconFieldModule,
        InputIconModule,
        InputTextModule,
        TableModule,
        TagModule,
        TooltipModule,
        DotFolderTreeComponent,
        DotMessagePipe
    ],
    templateUrl: './dot-experiments-select-page-dialog.component.html',
    // None of the three are `providedIn: 'root'`, so the dialog owns their lifetime. It is opened
    // outside the Configure screen's injector, so it cannot inherit the screen's instances.
    providers: [DotPagesBrowserService, DotExperimentsService],
    host: { class: 'flex flex-col h-full min-h-0' }
})
export class DotExperimentsSelectPageDialogComponent {
    /** Folder tree, rooted at the site. */
    readonly $folderNodes = signal<TreeNodeItem[]>([]);

    /** Folder whose pages the table lists. */
    readonly $selectedFolder = signal<TreeNodeItem | null>(null);

    /** Row the radio column has selected, or `null` while nothing is picked. */
    readonly $selectedRow = signal<SelectPageDialogViewRow | null>(null);

    readonly $isLoadingFolders = signal(false);
    readonly $isLoadingPages = signal(false);

    readonly #ref = inject(DynamicDialogRef);
    readonly #config = inject<DynamicDialogConfig<DotExperimentsSelectPageDialogData>>(
        DynamicDialogConfig<DotExperimentsSelectPageDialogData>
    );
    readonly #globalStore = inject(GlobalStore);
    readonly #pagesBrowserService = inject(DotPagesBrowserService);
    readonly #experimentsService = inject(DotExperimentsService);
    readonly #httpErrorManagerService = inject(DotHttpErrorManagerService);
    readonly #destroyRef = inject(DestroyRef);

    /**
     * Site being browsed, resolved once. A modal cannot outlive a site switch, so re-reading it
     * would only add reactivity nothing can trigger.
     */
    readonly #site = ((): { id: string; hostname: string } | null => {
        const data = this.#config.data;
        const currentSite = this.#globalStore.siteDetails();
        const id = data?.hostId ?? currentSite?.identifier;
        const hostname = data?.hostname ?? currentSite?.hostname;

        return id && hostname ? { id, hostname } : null;
    })();

    /** Every folder node by its path, so a "load more" click can find the level to append to. */
    readonly #folderNodesByPath = new Map<string, TreeNodeItem>();

    /** Requested folder listings. `switchMap` drops the response of a folder the user left. */
    readonly #pageRequests = new Subject<string>();

    /** Pages of the selected folder, before the search term narrows them. */
    readonly #pages = signal<DotPageBrowserPage[]>([]);

    /** Pages already hosting a non-archived experiment. Resolved once, from a single request. */
    readonly #excludedPageIds = signal<ReadonlySet<string>>(new Set());

    /** Raw search box text. */
    readonly $searchTerm = signal('');

    readonly #debouncedSearchTerm = debounced(() => this.$searchTerm(), SEARCH_DEBOUNCE_MS);

    /**
     * Rows the table renders.
     *
     * The search runs here rather than as another request: `GET /api/v1/page/search` has no
     * free-text parameter, so the term would be applied client-side either way — this only avoids
     * re-fetching a folder the browser already has.
     */
    readonly $rows = computed<SelectPageDialogViewRow[]>(() => {
        const term = this.#debouncedSearchTerm.value().trim().toLowerCase();
        const excludedPageIds = this.#excludedPageIds();

        return this.#pages()
            .filter((page) => this.#matchesTerm(page, term))
            .map((page) => this.#toViewRow(page, excludedPageIds));
    });

    /**
     * Path of the folder being listed, shown above the table.
     *
     * `{hostname} /` at the site root, `{hostname} / {folder path}` below it.
     */
    readonly $breadcrumb = computed<string>(() => {
        const hostname = this.#site?.hostname ?? '';
        const path = this.#selectedFolderPath();

        return path === SITE_ROOT_PATH ? `${hostname} /` : `${hostname} / ${trimSlashes(path)}`;
    });

    /** True until a row is picked, which is the only thing the confirm button waits for. */
    readonly $isConfirmDisabled = computed<boolean>(() => this.$selectedRow() === null);

    constructor() {
        this.#watchPageRequests();
        this.#loadExcludedPageIds();
        this.#initFolderTree();
    }

    onSearchInput(event: Event): void {
        this.$searchTerm.set((event.target as HTMLInputElement).value);
    }

    onClearSearch(): void {
        this.$searchTerm.set('');
    }

    /** Lists the selected folder's pages. Keeps the current pick, which the footer still names. */
    onFolderSelect(event: TreeNodeSelectEvent): void {
        const node = event.node as TreeNodeItem;

        this.$selectedFolder.set(node);
        this.#pageRequests.next(pathOf(node) ?? SITE_ROOT_PATH);
    }

    /** Loads a level the first time it is opened; re-opening it uses what is already there. */
    onFolderExpand(event: TreeNodeExpandEvent): void {
        const node = event.node as TreeNodeItem;

        if (node.children?.length) {
            return;
        }

        this.#loadFolderChildren(node);
    }

    /** Appends the next page of a level that was truncated. */
    onLoadMoreFolders(node: TreeNodeItem): void {
        const data = node.data;

        if (!data || !isTreeNodeLoadMoreData(data) || !data.path) {
            return;
        }

        const parent = this.#folderNodesByPath.get(data.path);

        if (parent) {
            this.#loadFolderChildren(parent, data.nextPage ?? 1);
        }
    }

    onSelectionChange(row: SelectPageDialogViewRow | null): void {
        this.$selectedRow.set(row);
    }

    onCancel(): void {
        this.#ref.close();
    }

    onConfirm(): void {
        const row = this.$selectedRow();

        if (row) {
            this.#ref.close(row);
        }
    }

    #selectedFolderPath(): string {
        return pathOf(this.$selectedFolder()) ?? SITE_ROOT_PATH;
    }

    #watchPageRequests(): void {
        const site = this.#site;

        if (!site) {
            return;
        }

        this.#pageRequests
            .pipe(
                tap(() => this.$isLoadingPages.set(true)),
                switchMap((path) =>
                    this.#pagesBrowserService.searchPages({ hostname: site.hostname, path }).pipe(
                        catchError((error) => {
                            this.#httpErrorManagerService.handle(error);

                            return [[] as DotPageBrowserPage[]];
                        })
                    )
                ),
                takeUntilDestroyed(this.#destroyRef)
            )
            .subscribe((pages) => {
                this.#pages.set(pages);
                this.$isLoadingPages.set(false);
            });
    }

    /**
     * One request covers every row the dialog can ever render, however deep the user browses, so
     * the greyed-out set is built up front instead of per folder.
     */
    #loadExcludedPageIds(): void {
        this.#experimentsService
            .getAllUnfiltered()
            .pipe(
                map(
                    (experiments) =>
                        new Set(
                            experiments
                                .filter(({ status }) => status !== DotExperimentStatus.ARCHIVED)
                                .map(({ pageId }) => pageId)
                        )
                ),
                catchError((error) => {
                    this.#httpErrorManagerService.handle(error);

                    return [new Set<string>()];
                }),
                take(1),
                takeUntilDestroyed(this.#destroyRef)
            )
            .subscribe((pageIds) => this.#excludedPageIds.set(pageIds));
    }

    #initFolderTree(): void {
        const site = this.#site;

        if (!site) {
            return;
        }

        const root: TreeNodeItem = {
            key: SITE_ROOT_PATH,
            label: site.hostname,
            data: {
                type: 'site',
                id: site.id,
                path: SITE_ROOT_PATH,
                hostname: site.hostname
            },
            expanded: true,
            leaf: false,
            children: []
        };

        this.#folderNodesByPath.set(SITE_ROOT_PATH, root);
        this.$folderNodes.set([root]);
        this.$selectedFolder.set(root);

        this.#loadFolderChildren(root);
        this.#pageRequests.next(SITE_ROOT_PATH);
    }

    #loadFolderChildren(node: TreeNodeItem, page = 1): void {
        const site = this.#site;
        const path = pathOf(node);

        if (!site || !path) {
            return;
        }

        this.$isLoadingFolders.set(true);

        this.#pagesBrowserService
            .getFolderChildren({
                siteId: site.id,
                hostname: site.hostname,
                path,
                page,
                perPage: DOT_FOLDER_TREE_PAGE_SIZE
            })
            .pipe(
                catchError((error) => {
                    this.#httpErrorManagerService.handle(error);

                    return [{ folders: [], totalFolders: 0, page, perPage: 0 }];
                }),
                take(1),
                takeUntilDestroyed(this.#destroyRef)
            )
            .subscribe(({ folders, totalFolders }) => {
                this.#appendFolderChildren(path, folders, totalFolders, page);
                this.$isLoadingFolders.set(false);
            });
    }

    /**
     * Adds a level's page of children under its parent.
     *
     * The nodes are mutated in place because PrimeNG holds on to the node objects it rendered;
     * replacing the top-level array is what tells change detection to look again.
     */
    #appendFolderChildren(
        parentPath: string,
        folders: DotPageBrowserFolder[],
        totalFolders: number,
        page: number
    ): void {
        const parent = this.#folderNodesByPath.get(parentPath);

        if (!parent) {
            return;
        }

        const loaded = [
            ...withoutLoadMore(parent.children),
            ...folders.map((folder) => this.#createFolderNode(folder))
        ];
        const remaining = totalFolders - loaded.length;

        parent.children =
            remaining > 0
                ? [
                      ...loaded,
                      createLoadMoreTreeNode({
                          levelKey: parentPath,
                          nextPage: page + 1,
                          remaining,
                          path: parentPath,
                          hostname: this.#site?.hostname
                      })
                  ]
                : loaded;
        parent.leaf = loaded.length === 0;

        this.$folderNodes.update((nodes) => [...nodes]);
    }

    #createFolderNode(folder: DotPageBrowserFolder): TreeNodeItem {
        const node: TreeNodeItem = {
            // The full path doubles as the label: `dotFolderName` renders its last segment, and
            // the path is what identifies the level everywhere else.
            key: folder.path,
            label: folder.path,
            data: {
                type: 'folder',
                id: folder.id,
                path: folder.path,
                hostname: folder.hostname
            },
            leaf: !folder.hasChildren,
            children: []
        };

        this.#folderNodesByPath.set(folder.path, node);

        return node;
    }

    #matchesTerm({ title, url }: DotPageBrowserPage, term: string): boolean {
        if (!term) {
            return true;
        }

        return title.toLowerCase().includes(term) || url.toLowerCase().includes(term);
    }

    #toViewRow(
        page: DotPageBrowserPage,
        excludedPageIds: ReadonlySet<string>
    ): SelectPageDialogViewRow {
        const isDisabled = excludedPageIds.has(page.identifier);

        return {
            pageId: page.identifier,
            title: page.title,
            url: page.path || page.url,
            template: page.templateId,
            templateLabel: shortenTemplateId(page.templateId),
            modDate: toTimestamp(page.modDate),
            state: ROW_STATES[page.state],
            pageState: page.state,
            stateLabelKey: STATE_LABEL_KEYS[page.state],
            stateSeverity: STATE_SEVERITIES[page.state],
            disabled: isDisabled,
            disabledTooltipKey: isDisabled ? ROW_DISABLED_TOOLTIP_KEY : null
        };
    }
}

/** Path a site or folder node stands for; `null` for the synthetic "load more" node. */
function pathOf(node: TreeNodeItem | null): string | null {
    const data = node?.data;

    return data && isTreeNodeContentData(data) ? data.path : null;
}

function withoutLoadMore(nodes: TreeNodeItem[] | undefined): TreeNodeItem[] {
    return (nodes ?? []).filter((node) => !(node.data && isTreeNodeLoadMoreData(node.data)));
}

function trimSlashes(path: string): string {
    return path.replace(/^\/+|\/+$/g, '');
}

/**
 * `modDate` arrives either as an epoch or as a date string depending on how the endpoint
 * serialized it; `0` means neither parsed, and the Modified column renders nothing for it.
 */
function toTimestamp(modDate: string): number {
    const epoch = Number(modDate);

    if (modDate && Number.isFinite(epoch)) {
        return epoch;
    }

    const parsed = Date.parse(modDate);

    return Number.isNaN(parsed) ? 0 : parsed;
}

function shortenTemplateId(templateId: string): string {
    if (templateId.length <= TEMPLATE_ID_PREVIEW_LENGTH) {
        return templateId;
    }

    return `${templateId.slice(0, TEMPLATE_ID_PREVIEW_LENGTH)}…`;
}
