import { MonacoEditorModule } from '@materia-ui/ngx-monaco-editor';
import { EMPTY } from 'rxjs';

import { DOCUMENT, Location } from '@angular/common';
import {
    ChangeDetectionStrategy,
    Component,
    DestroyRef,
    effect,
    inject,
    OnInit,
    signal,
    untracked,
    viewChild
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';

import { MenuItem, MessageService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { InputNumberModule } from 'primeng/inputnumber';
import { InputTextModule } from 'primeng/inputtext';
import { Menu, MenuModule } from 'primeng/menu';
import { MessageModule } from 'primeng/message';
import { PanelModule } from 'primeng/panel';
import { Popover, PopoverModule } from 'primeng/popover';
import { SkeletonModule } from 'primeng/skeleton';
import { SplitterModule } from 'primeng/splitter';
import { TableModule } from 'primeng/table';
import { TabsModule } from 'primeng/tabs';
import { TooltipModule } from 'primeng/tooltip';

import { catchError, take } from 'rxjs/operators';

import {
    DotContentletEditUrlService,
    DotContentSearchService,
    DotCurrentUserService,
    DotGlobalMessageService,
    DotMessageService
} from '@dotcms/data-access';
import { ComponentStatus, DotCMSContentlet } from '@dotcms/dotcms-models';
import { DotEditContentRequest, injectEditContentPanel } from '@dotcms/edit-content';
import {
    DOT_MONACO_BASE_OPTIONS,
    DOT_MONACO_RAW_OPTIONS,
    DotClipboardUtil,
    DotEmptyContainerComponent,
    DotMessagePipe,
    DotMonacoRunShortcutEditor,
    getDotMonacoRunShortcutLabel,
    PrincipalConfiguration,
    registerDotMonacoRunShortcut
} from '@dotcms/ui';
import { buildCurlSnippet, buildFetchSnippet, getDownloadLink } from '@dotcms/utils';

import {
    DEFAULT_LIMIT,
    DEFAULT_OFFSET,
    DotQueryToolStore,
    MAX_RESULTS
} from './store/dot-query-tool.store';

import { QueryToolActiveTab, QueryToolHelpExample } from '../models/dot-query-tool.models';

const VALID_TABS = new Set<QueryToolActiveTab>(['results', 'raw']);
const SEARCH_ENDPOINT = '/api/v1/content/_search';

const QUERY_EDITOR_OPTIONS = {
    ...DOT_MONACO_BASE_OPTIONS,
    language: 'plaintext',
    wordWrap: 'on'
} as const;

@Component({
    selector: 'dot-query-tool-page',
    imports: [
        FormsModule,
        MonacoEditorModule,
        SplitterModule,
        PanelModule,
        InputNumberModule,
        InputTextModule,
        ButtonModule,
        TooltipModule,
        TabsModule,
        TableModule,
        SkeletonModule,
        MessageModule,
        MenuModule,
        PopoverModule,
        DotEmptyContainerComponent,
        DotMessagePipe
    ],
    // MessageService: the Edit Content side panel's editor injects PrimeNG's MessageService for
    // inline toasts; provide it here (as Content Drive's shell does) so the panel works in-page.
    providers: [DotQueryToolStore, DotCurrentUserService, DotClipboardUtil, MessageService],
    templateUrl: './dot-query-tool-page.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    host: { class: 'flex flex-col h-full min-h-0 bg-white' }
})
export class DotQueryToolPageComponent implements OnInit {
    readonly store = inject(DotQueryToolStore);
    readonly #messageService = inject(DotMessageService);
    readonly #globalMessage = inject(DotGlobalMessageService);
    readonly #clipboard = inject(DotClipboardUtil);
    readonly #document = inject(DOCUMENT);
    readonly #router = inject(Router);
    readonly #route = inject(ActivatedRoute);
    readonly #location = inject(Location);
    readonly #editUrlResolver = inject(DotContentletEditUrlService);
    readonly #contentSearch = inject(DotContentSearchService);
    readonly #destroyRef = inject(DestroyRef);

    /** Keyboard shortcut shown in the Run button tooltip (`⌘ + Enter` / `Ctrl + Enter`). */
    readonly runShortcutLabel = getDotMonacoRunShortcutLabel(this.#document.defaultView?.navigator);

    #lastSyncedUrl: string | null = null;

    /** The content open in the side panel, or `null` when it is closed. */
    readonly $editRequest = signal<DotEditContentRequest | null>(null);

    /**
     * The side panel, open while {@link $editRequest} is set, in whichever editor the content's type
     * uses (#38303). Every save reruns the search; a close reruns it too and clears the request.
     */
    readonly #editPanel = injectEditContentPanel(this.$editRequest, {
        saved: () => this.onEditPanelSaved(),
        closed: () => this.onEditPanelClosed()
    });

    /**
     * Whether the last `editContent` URL write reflected an open panel — so a close writes with
     * replaceState instead of a history push whose Back would resurrect the just-removed param.
     */
    #editContentUrlWasSet = false;

    /**
     * Reflects the open edit panel in the URL (`?editContent=<identifier>`) so it is shareable,
     * refresh-safe, and dismissible with browser Back (see the popstate listener in ngOnInit).
     * Written with `Location.go` (a history push) — NOT `replaceState` like the search-param sync
     * above — precisely so Back has an entry to pop. `merge` preserves the query-tool params.
     */
    // eslint-disable-next-line no-unused-private-class-members -- effect() runs for its side effects
    readonly #editContentUrlEffect = effect(() => {
        const request = this.$editRequest();

        untracked(() =>
            this.#syncEditContentParam(
                request?.mode === 'edit' ? (request.identifier ?? null) : null
            )
        );
    });

    // Mirrors store state into the address bar via Location.replaceState, but only
    // once the search settles (LOADED or ERROR). Tying the sync to `status` instead
    // of the inputs means typing in the param fields never touches the URL — only
    // an actually-executed search updates the shareable address bar — and the
    // user's browser back stack is never polluted (replaceState, not pushState).
    readonly updateQueryParamsEffect = effect(() => {
        const status = this.store.status();
        if (status !== ComponentStatus.LOADED && status !== ComponentStatus.ERROR) return;
        // Read inputs untracked so this effect re-runs only on status transitions.
        untracked(() => {
            const queryParams: Record<string, string | number | null> = {
                q: this.store.query() || null,
                offset: this.store.offset() !== DEFAULT_OFFSET ? this.store.offset() : null,
                limit: this.store.limit() !== DEFAULT_LIMIT ? this.store.limit() : null,
                sort: this.store.sort() || null,
                userId: this.store.userId() || null
            };
            const url = this.#router
                .createUrlTree([], {
                    relativeTo: this.#route,
                    queryParams,
                    queryParamsHandling: 'merge'
                })
                .toString();
            if (url === this.#lastSyncedUrl || url === this.#router.url) return;
            this.#lastSyncedUrl = url;
            this.#location.replaceState(url);
        });
    });

    readonly $helpPopover = viewChild.required<Popover>('helpPopoverEl');
    readonly $exportMenu = viewChild<Menu>('exportMenu');

    readonly QUERY_EDITOR_OPTIONS = QUERY_EDITOR_OPTIONS;
    readonly RAW_EDITOR_OPTIONS = DOT_MONACO_RAW_OPTIONS;
    readonly ComponentStatus = ComponentStatus;
    readonly MAX_RESULTS = MAX_RESULTS;
    readonly DEFAULT_LIMIT = DEFAULT_LIMIT;
    readonly DEFAULT_OFFSET = DEFAULT_OFFSET;

    readonly splitterPt = { root: { class: 'border-0! rounded-none!' }, panel: {} };
    readonly tabPanelsPt = { root: { class: 'flex-1 min-h-0 overflow-auto p-0!' } };
    readonly tabPanelPt = { root: { class: 'h-full p-0!' } };

    readonly $paramsOpen = signal(true);

    readonly noHitsConfig: PrincipalConfiguration = {
        title: this.#messageService.get('queryTool.results.noHits'),
        subtitle: this.#messageService.get('queryTool.results.noHits.subtitle'),
        icon: 'search',
        iconStyle: 'material-symbols-rounded'
    };

    readonly exportItems: MenuItem[] = [
        {
            label: this.#messageService.get('queryTool.share.url'),
            command: () => this.#copyShareUrl()
        },
        {
            label: this.#messageService.get('queryTool.share.curl'),
            command: () => this.#copyAs('curl')
        },
        {
            label: this.#messageService.get('queryTool.share.fetch'),
            command: () => this.#copyAs('fetch')
        }
    ];

    readonly helpExamples: QueryToolHelpExample[] = [
        {
            title: 'queryTool.help.example.live.title',
            description: 'queryTool.help.example.live.description',
            query: '+contentType:htmlpageasset +live:true +languageId:1'
        },
        {
            title: 'queryTool.help.example.recent.title',
            description: 'queryTool.help.example.recent.description',
            query: '+contentType:fileAsset +deleted:false +modDate:[20250101 TO 20991231]'
        },
        {
            title: 'queryTool.help.example.wildcard.title',
            description: 'queryTool.help.example.wildcard.description',
            query: '+title:*demo* +working:true'
        },
        {
            title: 'queryTool.help.example.everything.title',
            description: 'queryTool.help.example.everything.description',
            query: '+languageId:1 +deleted:false'
        }
    ];

    ngOnInit(): void {
        const params = this.#route.snapshot.queryParamMap;
        const query = params.get('q') ?? '';
        const offset = this.#parseInt(params.get('offset'), DEFAULT_OFFSET);
        const limit = this.#parseInt(params.get('limit'), DEFAULT_LIMIT);
        const sort = params.get('sort') ?? '';
        const userId = params.get('userId') ?? '';

        // Only write `query` when the URL actually carries `q`. The store's `withPersistedQuery`
        // restored the user's last query in its own onInit, which runs before this — writing the
        // absent param's '' over it is what made Query Tool lose the query on every return to the
        // portlet while ES Search and Velocity Playground kept theirs (neither reads URL params).
        // `has` rather than a truthiness check, so an explicit `?q=` in a shared link still clears
        // the editor instead of silently resurrecting the previous query.
        if (params.has('q')) {
            this.store.setQuery(query);
        }

        this.store.setOffset(offset);
        this.store.setLimit(limit);
        this.store.setSort(sort);
        this.store.setUserId(userId);

        // Auto-run only for a deep link. A restored query is left in the editor unexecuted, as in
        // the sibling dev tools — returning to the portlet should not fire a search by itself.
        if (query.trim()) {
            this.store.runSearch();
        }

        // Shareable deep-link: `?editContent=<identifier>` reopens the edit panel on load.
        const editContent = params.get('editContent');
        if (editContent) {
            this.#resolveAndOpenByIdentifier(editContent);
        }

        // Browser Back/Forward: nothing else reacts to the `editContent` param (written via
        // Location.go, not the router). When Back removes/changes it while the panel is open, route
        // the close through the panel's unsaved-changes guard — a direct clear would tear the editor
        // down and discard unsaved edits silently.
        const locationSubscription = this.#location.subscribe((event) => {
            const backParams = new URLSearchParams(event.url?.split('?')[1] ?? '');
            const editContentParam = backParams.get('editContent');
            const request = this.$editRequest();

            if (request?.mode === 'edit' && request.identifier !== editContentParam) {
                // Restore the param so the URL matches the still-open panel while the guard decides.
                const restored = this.#router
                    .createUrlTree([], {
                        relativeTo: this.#route,
                        queryParams: { editContent: request.identifier },
                        queryParamsHandling: 'merge'
                    })
                    .toString();
                this.#location.replaceState(restored);
                this.#editPanel.requestClose();
            }
        });
        this.#destroyRef.onDestroy(() => locationSubscription.unsubscribe());
    }

    onQueryChange(value: string): void {
        this.store.setQuery(value);
    }

    onTabChange(value: string | number | undefined): void {
        if (typeof value === 'string' && VALID_TABS.has(value as QueryToolActiveTab)) {
            this.store.setActiveTab(value as QueryToolActiveTab);
        }
    }

    onRun(): void {
        if (!this.store.query().trim()) return;
        this.store.resetOffset();
        this.store.runSearch();
    }

    /**
     * Binds `Cmd/Ctrl + Enter` inside the query editor to the same action as the Run button.
     * The shortcut is ignored while a search is already in flight, where the Run button is
     * disabled too.
     *
     * @param editor the Monaco editor instance emitted by `ngx-monaco-editor`
     */
    onQueryEditorInit(editor: DotMonacoRunShortcutEditor): void {
        registerDotMonacoRunShortcut(editor, () => this.onRun(), {
            label: this.#messageService.get('queryTool.action.run'),
            canRun: () => !this.store.isLoading()
        });
    }

    /**
     * Opens a result for editing (#38303, R13). A page opens the page editor in a new tab, as
     * before; any other result opens in the side panel over the results, in whichever editor its
     * type uses, which the opener decides.
     *
     * @param contentlet The result whose title was clicked.
     * @param event The click, whose default navigation is cancelled.
     */
    onResultClick(contentlet: DotCMSContentlet, event: MouseEvent): void {
        event.preventDefault();

        if (contentlet.baseType === 'HTMLPAGE') {
            this.#editUrlResolver
                .resolveEditUrl(contentlet)
                .pipe(take(1))
                .subscribe({
                    next: (url) => window.open(url, '_blank'),
                    error: () => this.#globalMessage.error()
                });

            return;
        }

        this.$editRequest.set(toEditRequest(contentlet));
    }

    /** Re-runs the search after a save so the results table reflects the edited content. */
    onEditPanelSaved(): void {
        this.store.runSearch();
    }

    /**
     * The panel closed itself: rerun the search, since a move or a translation saved inside the
     * legacy editor reports no save (FR-017), and clear the request.
     */
    onEditPanelClosed(): void {
        this.store.runSearch();
        this.$editRequest.set(null);
    }

    /**
     * Writes/clears `editContent` (merged with the query-tool params). Pushes when opening the
     * panel (so browser Back can pop it) but replaces when closing — a push on close would leave a
     * phantom history entry whose Back puts the removed param back with no panel rendered.
     */
    #syncEditContentParam(identifier: string | null): void {
        const url = this.#router
            .createUrlTree([], {
                relativeTo: this.#route,
                queryParams: { editContent: identifier },
                queryParamsHandling: 'merge'
            })
            .toString();

        if (url !== this.#location.path(true)) {
            if (identifier === null && this.#editContentUrlWasSet) {
                this.#location.replaceState(url);
            } else {
                this.#location.go(url);
            }
        }

        this.#editContentUrlWasSet = identifier !== null;
    }

    /**
     * Resolves a shared `?editContent=` identifier to its working contentlet and opens the panel on
     * it, with the same request a click on that result would open.
     *
     * @param identifier The identifier the link names.
     */
    #resolveAndOpenByIdentifier(identifier: string): void {
        this.#contentSearch
            .get<{ jsonObjectView: { contentlets: DotCMSContentlet[] } }>({
                query: `+identifier:${identifier} +working:true`,
                limit: 1
            })
            .pipe(
                catchError(() => EMPTY),
                takeUntilDestroyed(this.#destroyRef)
            )
            .subscribe((entity) => {
                const contentlet = entity?.jsonObjectView?.contentlets?.[0];
                if (!contentlet?.inode) {
                    return;
                }

                this.$editRequest.set(toEditRequest({ ...contentlet, identifier }));
            });
    }

    useExample(query: string): void {
        this.store.setQuery(query);
        this.$helpPopover().hide();
    }

    copyToClipboard(value: unknown): void {
        this.#copy(String(value ?? ''));
    }

    downloadRawJson(): void {
        const blob = new Blob([this.store.rawJson()], { type: 'application/json' });
        const link = getDownloadLink(blob, 'query-tool-results.json');
        this.#document.body.appendChild(link);
        link.click();
        this.#document.body.removeChild(link);
    }

    toggleExportMenu(event: MouseEvent): void {
        this.$exportMenu()?.toggle(event);
    }

    #copyShareUrl(): void {
        const href = this.#document.defaultView?.location.href;
        if (href) this.#copy(href);
    }

    #copyAs(format: 'curl' | 'fetch'): void {
        const body = this.store.apiRequestBody();
        if (format === 'curl') {
            const origin = this.#document.defaultView?.location.origin ?? '';
            this.#copy(buildCurlSnippet({ url: `${origin}${SEARCH_ENDPOINT}`, body }));
        } else {
            this.#copy(buildFetchSnippet({ url: SEARCH_ENDPOINT, body }));
        }
    }

    async #copy(text: string): Promise<void> {
        const ok = await this.#clipboard.copy(text);
        if (!ok) this.#globalMessage.error();
    }

    #parseInt(value: string | null, fallback: number): number {
        if (!value) return fallback;
        const n = Number.parseInt(value, 10);
        return Number.isFinite(n) ? n : fallback;
    }
}

/**
 * The edit request for a search result. The content type is the result's own variable, so the
 * opener picks the editor without fetching the contentlet first.
 *
 * @param contentlet The result to edit.
 */
function toEditRequest({
    inode,
    identifier,
    contentType,
    languageId,
    title
}: DotCMSContentlet): DotEditContentRequest {
    return { mode: 'edit', inode, identifier, contentType, languageId, title };
}
