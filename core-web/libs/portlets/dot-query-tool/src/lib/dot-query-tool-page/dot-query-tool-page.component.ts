import { MonacoEditorModule } from '@materia-ui/ngx-monaco-editor';

import { DOCUMENT, Location } from '@angular/common';
import {
    ChangeDetectionStrategy,
    Component,
    effect,
    inject,
    OnInit,
    signal,
    untracked,
    viewChild
} from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
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

import { take } from 'rxjs/operators';

import {
    DotContentletEditUrlService,
    DotCurrentUserService,
    DotGlobalMessageService,
    DotMessageService,
    DotPropertiesService
} from '@dotcms/data-access';
import { ComponentStatus, DotCMSContentlet, FeaturedFlags } from '@dotcms/dotcms-models';
import {
    DotEditContentSidePanelComponent,
    EditContentDialogData
} from '@dotcms/edit-content';
import {
    DOT_MONACO_BASE_OPTIONS,
    DOT_MONACO_RAW_OPTIONS,
    DotClipboardUtil,
    DotEmptyContainerComponent,
    DotMessagePipe,
    PrincipalConfiguration
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
        DotMessagePipe,
        DotEditContentSidePanelComponent
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
    readonly #dotPropertiesService = inject(DotPropertiesService);

    #lastSyncedUrl: string | null = null;

    /**
     * Feature flag gating the side panel. When off, editing a result opens the editor in a new
     * browser tab (the previous behavior); when on, results that resolve to the new content
     * editor open in the in-page side panel instead. Defaults to `false` until it resolves.
     */
    readonly $sidePanelEnabled = toSignal(
        this.#dotPropertiesService.getFeatureFlag(
            FeaturedFlags.FEATURE_FLAG_EDIT_CONTENT_SIDE_PANEL
        ),
        { initialValue: false }
    );

    /** Content shown in the Edit Content side panel, or `null` when it is closed. */
    readonly $editPanelRequest = signal<EditContentDialogData | null>(null);

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

    readonly splitterPt = { root: { class: 'border-0! rounded-none!' } };
    readonly tabPanelsPt = { root: { class: 'flex-1 min-h-0 overflow-auto p-0!' } };
    readonly tabPanelPt = { root: { class: 'h-full p-0!' } };

    readonly $paramsOpen = signal(true);

    readonly noHitsConfig: PrincipalConfiguration = {
        title: this.#messageService.get('queryTool.results.noHits'),
        subtitle: this.#messageService.get('queryTool.results.noHits.subtitle'),
        icon: 'pi-search'
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

        this.store.setQuery(query);
        this.store.setOffset(offset);
        this.store.setLimit(limit);
        this.store.setSort(sort);
        this.store.setUserId(userId);

        if (query.trim()) {
            this.store.runSearch();
        }
    }

    onQueryChange(value: string): void {
        this.store.setQuery(value);
    }

    onTabChange(value: string): void {
        if (VALID_TABS.has(value as QueryToolActiveTab)) {
            this.store.setActiveTab(value as QueryToolActiveTab);
        }
    }

    onRun(): void {
        if (!this.store.query().trim()) return;
        this.store.resetOffset();
        this.store.runSearch();
    }

    onResultClick(contentlet: DotCMSContentlet, event: MouseEvent): void {
        event.preventDefault();

        // Side panel enabled: resolve first (no intermediate tab). New-editor content opens in
        // the in-page panel; legacy-editor and HTMLPAGE content still open in a new tab. For a
        // cached content type the resolver answers synchronously, so the fallback `window.open`
        // stays within the click gesture and the popup blocker accepts it.
        if (this.$sidePanelEnabled()) {
            this.#editUrlResolver
                .resolveEditUrl(contentlet)
                .pipe(take(1))
                .subscribe({
                    next: (url) => {
                        if (this.#isNewEditorUrl(url)) {
                            this.$editPanelRequest.set({
                                mode: 'edit',
                                contentletInode: contentlet.inode,
                                identifier: contentlet.identifier,
                                title: contentlet.title
                            });

                            return;
                        }

                        window.open(url, '_blank');
                    },
                    error: () => this.#globalMessage.error()
                });

            return;
        }

        // Side panel disabled: open the placeholder synchronously so the popup blocker accepts it,
        // then assign the resolved URL once DotContentletEditUrlService returns. The resolver may
        // answer synchronously (HTMLPAGE / cached content type) — that's fine, subscribe still
        // delivers on the same tick.
        const placeholder = window.open('about:blank', '_blank');
        if (!placeholder) return;
        this.#editUrlResolver
            .resolveEditUrl(contentlet)
            .pipe(take(1))
            .subscribe({
                next: (url) => (placeholder.location.href = url),
                error: () => {
                    placeholder.close();
                    this.#globalMessage.error();
                }
            });
    }

    /** Re-runs the search after a save so the results table reflects the edited content. */
    onEditPanelSaved(): void {
        this.store.runSearch();
    }

    /** Clears the side panel request, closing the panel. */
    onEditPanelClosed(): void {
        this.$editPanelRequest.set(null);
    }

    /**
     * True when `url` targets the new content editor (`/dotAdmin/#/content/<inode>`). The legacy
     * editor (`/c/content/`) and the page editor (`/edit-page/`) resolve to different prefixes, so
     * only the new editor is eligible for the in-page side panel.
     */
    #isNewEditorUrl(url: string): boolean {
        return url.startsWith('/dotAdmin/#/content/');
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
