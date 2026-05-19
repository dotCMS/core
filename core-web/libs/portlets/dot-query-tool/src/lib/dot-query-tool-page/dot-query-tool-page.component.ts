import { MonacoEditorModule } from '@materia-ui/ngx-monaco-editor';

import { DOCUMENT } from '@angular/common';
import {
    ChangeDetectionStrategy,
    Component,
    computed,
    inject,
    OnInit,
    signal,
    viewChild
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';


import { ButtonModule } from 'primeng/button';
import { InputNumberModule } from 'primeng/inputnumber';
import { InputTextModule } from 'primeng/inputtext';
import { PanelModule } from 'primeng/panel';
import { Popover, PopoverModule } from 'primeng/popover';
import { SkeletonModule } from 'primeng/skeleton';
import { SplitterModule } from 'primeng/splitter';
import { TableModule } from 'primeng/table';
import { TabsModule } from 'primeng/tabs';
import { TooltipModule } from 'primeng/tooltip';

import { take } from 'rxjs/operators';

import {
    DotContentTypeService,
    DotCurrentUserService,
    DotGlobalMessageService,
    DotMessageService
} from '@dotcms/data-access';
import {
    ComponentStatus,
    DotCMSBaseTypesContentTypes,
    DotCMSContentlet,
    FeaturedFlags
} from '@dotcms/dotcms-models';
import { DotEmptyContainerComponent, DotMessagePipe, PrincipalConfiguration } from '@dotcms/ui';

import { DEFAULT_LIMIT, DEFAULT_OFFSET, DotQueryToolStore } from './store/dot-query-tool.store';

import { QueryToolActiveTab, QueryToolHelpExample } from '../models/dot-query-tool.models';

const VALID_TABS = new Set<QueryToolActiveTab>(['results', 'raw']);

const QUERY_EDITOR_OPTIONS = {
    theme: 'vs',
    language: 'plaintext',
    minimap: { enabled: false },
    lineNumbers: 'on',
    scrollBeyondLastLine: false,
    automaticLayout: true,
    fontSize: 13,
    fontFamily: 'JetBrains Mono, Fira Code, Consolas, monospace',
    wordWrap: 'on'
};

const RAW_EDITOR_OPTIONS = {
    ...QUERY_EDITOR_OPTIONS,
    language: 'json',
    readOnly: true,
    lineNumbers: 'off'
};

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
        PopoverModule,
        DotEmptyContainerComponent,
        DotMessagePipe
    ],
    providers: [DotQueryToolStore, DotCurrentUserService, DotContentTypeService],
    templateUrl: './dot-query-tool-page.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    host: { class: 'flex flex-col h-full min-h-0 bg-white' }
})
export class DotQueryToolPageComponent implements OnInit {
    readonly store = inject(DotQueryToolStore);
    readonly #messageService = inject(DotMessageService);
    readonly #globalMessage = inject(DotGlobalMessageService);
    readonly #document = inject(DOCUMENT);
    readonly #router = inject(Router);
    readonly #route = inject(ActivatedRoute);
    readonly #contentTypeService = inject(DotContentTypeService);

    readonly helpPopover = viewChild.required<Popover>('helpPopoverEl');

    readonly QUERY_EDITOR_OPTIONS = QUERY_EDITOR_OPTIONS;
    readonly RAW_EDITOR_OPTIONS = RAW_EDITOR_OPTIONS;
    readonly ComponentStatus = ComponentStatus;

    readonly splitterPt = { root: { class: 'border-0! rounded-none!' } };
    readonly tabPanelsPt = { root: { class: 'flex-1 min-h-0 overflow-auto p-0!' } };
    readonly tabPanelPt = { root: { class: 'h-full p-0!' } };

    readonly $paramsOpen = signal(true);

    readonly noHitsConfig: PrincipalConfiguration = {
        title: this.#messageService.get('queryTool.results.noHits'),
        subtitle: this.#messageService.get('queryTool.results.noHits.subtitle'),
        icon: 'pi-search'
    };

    readonly $rangeLabel = computed(() => {
        const from = this.store.showingFrom();
        const to = this.store.showingTo();
        const total = this.store.resultsSize();
        return this.#messageService.get(
            'queryTool.results.showing',
            String(from),
            String(to),
            String(total)
        );
    });

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
        const offset = this.parseInt(params.get('offset'), DEFAULT_OFFSET);
        const limit = this.parseInt(params.get('limit'), DEFAULT_LIMIT);
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
        this.syncUrl();
        this.store.runSearch();
    }

    onResultClick(contentlet: DotCMSContentlet, event: MouseEvent): void {
        event.preventDefault();
        const pageUrl = this.buildPageEditUrl(contentlet);
        if (pageUrl) {
            window.open(pageUrl, '_blank');
            return;
        }
        const placeholder = window.open('about:blank', '_blank');
        if (!placeholder) return;
        this.#contentTypeService
            .getContentType(contentlet.contentType)
            .pipe(take(1))
            .subscribe({
                next: (contentType) => {
                    const useNewEditor =
                        !!contentType?.metadata?.[
                            FeaturedFlags.FEATURE_FLAG_CONTENT_EDITOR2_ENABLED
                        ];
                    placeholder.location.href = useNewEditor
                        ? `/dotAdmin/#/content/${contentlet.inode}`
                        : `/dotAdmin/#/c/content/${contentlet.inode}`;
                },
                error: () => {
                    placeholder.close();
                    this.#globalMessage.error();
                }
            });
    }

    useExample(query: string): void {
        this.store.setQuery(query);
        this.helpPopover().hide();
    }

    copyQuery(query: string): void {
        navigator.clipboard.writeText(query).catch(() => this.#globalMessage.error());
    }

    copyToClipboard(value: unknown): void {
        navigator.clipboard.writeText(String(value ?? '')).catch(() => this.#globalMessage.error());
    }

    downloadRawJson(): void {
        const a = this.#document.createElement('a');
        a.href = `data:application/json;charset=utf-8,${encodeURIComponent(this.store.rawJson())}`;
        a.download = 'query-tool-results.json';
        this.#document.body.appendChild(a);
        a.click();
        this.#document.body.removeChild(a);
    }

    private syncUrl(): void {
        const userId = this.store.userId();
        this.#router.navigate([], {
            relativeTo: this.#route,
            queryParams: {
                q: this.store.query() || null,
                offset: this.store.offset() || null,
                limit: this.store.limit() !== DEFAULT_LIMIT ? this.store.limit() : null,
                sort: this.store.sort() || null,
                userId: this.store.isAdmin() && userId ? userId : null
            },
            queryParamsHandling: 'merge',
            replaceUrl: true
        });
    }

    private parseInt(value: string | null, fallback: number): number {
        if (!value) return fallback;
        const n = Number.parseInt(value, 10);
        return Number.isFinite(n) ? n : fallback;
    }

    private buildPageEditUrl(contentlet: DotCMSContentlet): string | null {
        if (contentlet.baseType !== DotCMSBaseTypesContentTypes.HTMLPAGE) return null;
        const url = (contentlet['urlMap'] as string) || (contentlet['url'] as string);
        if (!url) return null;
        const params = new URLSearchParams({
            url,
            language_id: String(contentlet.languageId ?? 1),
            mId: 'edit'
        });
        return `/dotAdmin/#/edit-page/content?${params.toString()}`;
    }
}
