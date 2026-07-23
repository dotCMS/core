import { MonacoEditorModule } from '@materia-ui/ngx-monaco-editor';

import { DecimalPipe, DOCUMENT, SlicePipe } from '@angular/common';
import {
    ChangeDetectionStrategy,
    Component,
    computed,
    inject,
    signal,
    viewChild
} from '@angular/core';
import { FormsModule } from '@angular/forms';

import { MenuItem } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { CheckboxModule } from 'primeng/checkbox';
import { InputTextModule } from 'primeng/inputtext';
import { Menu, MenuModule } from 'primeng/menu';
import { MessageModule } from 'primeng/message';
import { PanelModule } from 'primeng/panel';
import { Popover, PopoverModule } from 'primeng/popover';
import { SkeletonModule } from 'primeng/skeleton';
import { SplitterModule } from 'primeng/splitter';
import { TableModule } from 'primeng/table';
import { TabsModule } from 'primeng/tabs';
import { TagModule } from 'primeng/tag';
import { TooltipModule } from 'primeng/tooltip';

import {
    DotCurrentUserService,
    DotEsSearchService,
    DotGlobalMessageService,
    DotMessageService
} from '@dotcms/data-access';
import { ComponentStatus, DotContentState } from '@dotcms/dotcms-models';
import {
    DOT_MONACO_BASE_OPTIONS,
    DOT_MONACO_RAW_OPTIONS,
    DotClipboardUtil,
    DotContentletStatusBadgeComponent,
    DotEmptyContainerComponent,
    DotMessagePipe,
    PrincipalConfiguration
} from '@dotcms/ui';
import { buildCurlSnippet, buildFetchSnippet, getDownloadLink } from '@dotcms/utils';

import { DotEsSearchStore, ESSearchActiveTab, MAX_HITS } from './store/dot-es-search.store';

const VALID_TABS = new Set<ESSearchActiveTab>(['results', 'raw', 'aggregations', 'suggestions']);

interface ParsedBucket {
    key: string;
    docCount: number;
    subCols: string[];
    subValues: Record<string, string | number>;
}

export interface ParsedAggregation {
    id: string;
    type: string;
    name: string;
    isBucket: boolean;
    buckets: ParsedBucket[];
    value: number | null;
    sumOtherDocCount: number;
}

interface SuggestionOption {
    text: string;
    score: number;
    freq: number;
}

interface SuggestionTerm {
    original: string;
    options: SuggestionOption[];
}

export interface ParsedSuggester {
    name: string;
    terms: SuggestionTerm[];
}

const BUCKET_RESERVED_KEYS = new Set(['key', 'key_as_string', 'doc_count']);

const QUERY_EDITOR_OPTIONS = {
    ...DOT_MONACO_BASE_OPTIONS,
    language: 'json'
} as const;

@Component({
    selector: 'dot-es-search-page',
    imports: [
        DecimalPipe,
        SlicePipe,
        FormsModule,
        MonacoEditorModule,
        SplitterModule,
        TabsModule,
        TableModule,
        ButtonModule,
        CheckboxModule,
        InputTextModule,
        TooltipModule,
        MenuModule,
        PanelModule,
        PopoverModule,
        SkeletonModule,
        MessageModule,
        TagModule,
        DotContentletStatusBadgeComponent,
        DotEmptyContainerComponent,
        DotMessagePipe
    ],
    providers: [DotEsSearchStore, DotEsSearchService, DotCurrentUserService, DotClipboardUtil],
    templateUrl: './dot-es-search-page.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    host: { class: 'flex flex-col h-full min-h-0 bg-white' }
})
export class DotEsSearchPageComponent {
    readonly store = inject(DotEsSearchStore);
    readonly #messageService = inject(DotMessageService);
    readonly #document = inject(DOCUMENT);
    readonly #globalMessage = inject(DotGlobalMessageService);
    readonly #clipboard = inject(DotClipboardUtil);

    readonly exportMenu = viewChild<Menu>('exportMenu');
    readonly helpPopover = viewChild.required<Popover>('helpPopoverEl');

    readonly $queryEditorOptions = computed(() => ({
        ...QUERY_EDITOR_OPTIONS,
        wordWrap: this.store.wrapCode() ? 'on' : 'off'
    }));
    readonly RAW_EDITOR_OPTIONS = DOT_MONACO_RAW_OPTIONS;
    readonly MAX_HITS = MAX_HITS;

    readonly ComponentStatus = ComponentStatus;

    readonly splitterPt = { root: { class: 'border-0! rounded-none!' } };
    readonly tabPanelsPt = { root: { class: 'flex-1 min-h-0 overflow-auto p-0!' } };
    readonly tabPanelPt = { root: { class: 'h-full p-0!' } };

    readonly $paramsOpen = signal(true);
    readonly $hasEditorErrors = signal(false);

    readonly noHitsConfig: PrincipalConfiguration = {
        title: this.#messageService.get('esSearch.results.noHits'),
        subtitle: this.#messageService.get('esSearch.results.noHits.subtitle'),
        icon: 'search',
        iconStyle: 'material-symbols-rounded'
    };

    readonly $parsedAggregations = computed<ParsedAggregation[]>(() => {
        const raw = this.store.aggregations();
        if (!raw) return [];
        return Object.entries(raw).map(([key, value]) => this.parseAggregation(key, value));
    });

    readonly $parsedSuggestions = computed<ParsedSuggester[]>(() => {
        const raw = this.store.suggestions();
        if (!raw) return [];
        return Object.entries(raw).map(([name, entries]) => ({
            name,
            terms: (entries as Array<{ text: string; options: SuggestionOption[] }>)
                .filter((e) => e.options.length > 0)
                .map((e) => ({ original: e.text, options: e.options }))
        }));
    });

    readonly exportItems: MenuItem[] = [
        {
            label: this.#messageService.get('esSearch.copy.curl'),
            command: () => this.copyAs('curl')
        },
        {
            label: this.#messageService.get('esSearch.copy.fetch'),
            command: () => this.copyAs('fetch')
        }
    ];

    readonly helpExamples: { title: string; query: string; description?: string }[] = [
        {
            title: 'esSearch.help.example.matchAll',
            query: '{\n  "query": {\n    "match_all": {}\n  }\n}'
        },
        {
            title: 'esSearch.help.example.fullText',
            query: '{\n  "query": {\n    "query_string": {\n      "query": "contentType:Blog"\n    }\n  },\n  "size": 20\n}'
        },
        {
            title: 'esSearch.help.example.filterByTypeAndLang',
            query: '{\n  "query": {\n    "bool": {\n      "must": [\n        { "term": { "contentType": "Blog" } },\n        { "term": { "languageId": 1 } }\n      ]\n    }\n  },\n  "size": 20\n}'
        },
        {
            title: 'esSearch.help.example.filterByDate',
            query: '{\n  "query": {\n    "bool": {\n      "filter": [\n        { "range": { "modDate": { "gte": "now-30d/d", "lte": "now" } } }\n      ]\n    }\n  },\n  "sort": [{ "modDate": "desc" }],\n  "size": 20\n}'
        },
        {
            title: 'esSearch.help.example.filterByBaseType',
            query: '{\n  "query": {\n    "bool": {\n      "must": [\n        { "term": { "contentType": "FileAsset" } },\n        { "term": { "live": true } }\n      ]\n    }\n  },\n  "size": 20\n}'
        },
        {
            title: 'esSearch.help.example.withAggregation',
            description: 'esSearch.help.example.withAggregation.desc',
            query: '{\n  "query": { "match_all": {} },\n  "aggs": {\n    "terms#by_type": {\n      "terms": { "field": "contentType", "size": 10 }\n    }\n  },\n  "size": 0\n}'
        },
        {
            title: 'esSearch.help.example.suggestions',
            description: 'esSearch.help.example.suggestions.desc',
            query: '{\n  "query": { "match_all": {} },\n  "suggest": {\n    "title-suggest": {\n      "text": "blag",\n      "term": {\n        "field": "title"\n      }\n    }\n  },\n  "size": 5\n}'
        },
        {
            title: 'esSearch.help.example.complexBool',
            description: 'esSearch.help.example.complexBool.desc',
            query: '{\n  "query": {\n    "bool": {\n      "must": [\n        { "query_string": { "query": "contentType:Blog AND title:dotCMS AND +languageId:1 AND +live:true", "default_operator": "AND", "analyze_wildcard": true } }\n      ],\n      "filter": [\n        { "range": { "modDate": { "gte": "now-90d/d", "lte": "now" } } },\n        { "terms": { "contentType": ["Blog", "Documentation", "News", "Product", "LandingPage"] } }\n      ],\n      "must_not": [\n        { "term": { "deleted": true } },\n        { "term": { "working": false } }\n      ]\n    }\n  },\n  "sort": [\n    { "modDate": { "order": "desc", "unmapped_type": "date" } },\n    { "title.dotraw": { "order": "asc", "unmapped_type": "keyword" } }\n  ],\n  "aggs": {\n    "terms#by_type": { "terms": { "field": "contentType", "size": 10 } },\n    "terms#by_lang": { "terms": { "field": "languageId", "size": 5 } }\n  },\n  "highlight": {\n    "fields": { "title": {}, "body": { "fragment_size": 150, "number_of_fragments": 3 } },\n    "pre_tags": ["<mark>"],\n    "post_tags": ["</mark>"]\n  },\n  "from": 0,\n  "size": 20\n}'
        }
    ];

    onQueryChange(value: string): void {
        this.store.setQuery(value);
        if (!value.trim()) {
            this.$hasEditorErrors.set(false);
            return;
        }
        try {
            JSON.parse(value);
            this.$hasEditorErrors.set(false);
        } catch {
            this.$hasEditorErrors.set(true);
        }
    }

    onTabChange(value: string): void {
        if (VALID_TABS.has(value as ESSearchActiveTab)) {
            this.store.setActiveTab(value as ESSearchActiveTab);
        }
    }

    toggleExportMenu(event: MouseEvent): void {
        this.exportMenu()?.toggle(event);
    }

    onRun(): void {
        if (this.$hasEditorErrors() || !this.store.query()) return;
        this.store.runSearch();
    }

    useExample(query: string): void {
        this.store.setQuery(query);
        this.helpPopover().hide();
    }

    copyToClipboard(value: unknown): void {
        this.#copy(String(value ?? ''));
    }

    downloadRawJson(): void {
        const blob = new Blob([this.store.rawJson()], { type: 'application/json' });
        const link = getDownloadLink(blob, 'es-search-results.json');
        this.#document.body.appendChild(link);
        link.click();
        this.#document.body.removeChild(link);
    }

    async #copy(text: string): Promise<void> {
        const ok = await this.#clipboard.copy(text);
        if (!ok) this.#globalMessage.error();
    }

    asContentState(contentlet: Record<string, unknown>): DotContentState {
        return {
            live: (contentlet['live'] as DotContentState['live']) ?? false,
            working: (contentlet['working'] as DotContentState['working']) ?? false,
            hasLiveVersion:
                (contentlet['hasLiveVersion'] as DotContentState['hasLiveVersion']) ?? false,
            archived: contentlet['archived'] as DotContentState['archived'],
            deleted: contentlet['deleted'] as DotContentState['deleted']
        };
    }

    private splitAggKey(key: string): { type: string; name: string } {
        const idx = key.indexOf('#');
        if (idx === -1) return { type: '', name: key };
        return { type: key.slice(0, idx), name: key.slice(idx + 1) };
    }

    private parseAggregation(key: string, value: unknown): ParsedAggregation {
        const { type, name } = this.splitAggKey(key);
        const agg = value as Record<string, unknown>;
        const rawBuckets = agg['buckets'] as unknown[] | undefined;

        if (Array.isArray(rawBuckets)) {
            const subCols =
                rawBuckets.length > 0
                    ? Object.keys(rawBuckets[0] as object).filter(
                          (k) => !BUCKET_RESERVED_KEYS.has(k)
                      )
                    : [];
            const buckets: ParsedBucket[] = rawBuckets.map((b) => {
                const bucket = b as Record<string, unknown>;
                const subValues: Record<string, string | number> = {};
                for (const col of subCols) {
                    const sub = bucket[col] as Record<string, unknown> | undefined;
                    subValues[col] =
                        sub != null && 'value' in sub
                            ? (sub['value'] as number)
                            : ((sub?.['doc_count'] as number) ?? String(sub));
                }
                return {
                    key: String(bucket['key_as_string'] ?? bucket['key']),
                    docCount: Number(bucket['doc_count']),
                    subCols,
                    subValues
                };
            });
            return {
                id: key,
                type,
                name,
                isBucket: true,
                buckets,
                value: null,
                sumOtherDocCount: Number(agg['sum_other_doc_count'] ?? 0)
            };
        }

        return {
            id: key,
            type,
            name,
            isBucket: false,
            buckets: [],
            value: agg['value'] != null ? Number(agg['value']) : null,
            sumOtherDocCount: 0
        };
    }

    private copyAs(format: 'curl' | 'fetch'): void {
        const qs = this.buildApiQueryString();
        const path = `/api/es/search${qs ? '?' + qs : ''}`;
        const origin = this.#document.defaultView?.location.origin ?? '';
        const body = this.parseQueryBody();
        const snippet =
            format === 'curl'
                ? buildCurlSnippet({ url: `${origin}${path}`, body })
                : buildFetchSnippet({ url: path, body });
        this.#copy(snippet);
    }

    private parseQueryBody(): unknown {
        try {
            return JSON.parse(this.store.query());
        } catch {
            return {};
        }
    }

    private buildApiQueryString(): string {
        const { live, userid } = this.store.params();
        const qs = new URLSearchParams();
        // depth=1 mirrors the fixed value sent by DotEsSearchService
        qs.set('depth', '1');
        if (live) qs.set('live', 'true');
        if (userid) qs.set('userid', userid);
        return qs.toString();
    }
}
