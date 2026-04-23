import { MonacoEditorModule } from '@materia-ui/ngx-monaco-editor';

import { DecimalPipe } from '@angular/common';
import {
    ChangeDetectionStrategy,
    Component,
    DestroyRef,
    computed,
    inject,
    signal,
    viewChild
} from '@angular/core';
import { FormsModule } from '@angular/forms';

import { MenuItem } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { Menu, MenuModule } from 'primeng/menu';
import { PanelModule } from 'primeng/panel';
import { Popover, PopoverModule } from 'primeng/popover';
import { SelectModule } from 'primeng/select';
import { SkeletonModule } from 'primeng/skeleton';
import { SplitterModule } from 'primeng/splitter';
import { TableModule } from 'primeng/table';
import { TabsModule } from 'primeng/tabs';
import { TagModule } from 'primeng/tag';
import { ToggleSwitchModule } from 'primeng/toggleswitch';
import { ToolbarModule } from 'primeng/toolbar';
import { TooltipModule } from 'primeng/tooltip';

import { DotEsSearchService, DotMessageService } from '@dotcms/data-access';
import { DotContentState } from '@dotcms/dotcms-models';
import {
    DotContentletStatusChipComponent,
    DotEmptyContainerComponent,
    DotMessagePipe,
    PrincipalConfiguration
} from '@dotcms/ui';

import { DotEsSearchStore, EsSearchActiveTab } from './store/dot-es-search.store';

// Monaco is loaded as a global by ngx-monaco-editor
declare const monaco: {
    editor: {
        onDidChangeMarkers: (listener: (uris: unknown[]) => void) => { dispose(): void };
        getModelMarkers: (filter: { resource: unknown }) => { severity: number }[];
    };
};

const MONACO_SEVERITY_ERROR = 8;

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
    theme: 'vs',
    language: 'json',
    minimap: { enabled: false },
    lineNumbers: 'on',
    scrollBeyondLastLine: false,
    automaticLayout: true,
    fontSize: 13,
    fontFamily: 'JetBrains Mono, Fira Code, Consolas, monospace'
};

const RAW_EDITOR_OPTIONS = {
    ...QUERY_EDITOR_OPTIONS,
    readOnly: true,
    lineNumbers: 'off'
};

@Component({
    selector: 'dot-es-search-page',
    standalone: true,
    imports: [
        DecimalPipe,
        FormsModule,
        MonacoEditorModule,
        SplitterModule,
        TabsModule,
        TableModule,
        ButtonModule,
        SelectModule,
        InputTextModule,
        ToggleSwitchModule,
        ToolbarModule,
        TooltipModule,
        MenuModule,
        PanelModule,
        PopoverModule,
        SkeletonModule,
        TagModule,
        DotContentletStatusChipComponent,
        DotEmptyContainerComponent,
        DotMessagePipe
    ],
    providers: [DotEsSearchStore, DotEsSearchService],
    templateUrl: './dot-es-search-page.component.html',
    styleUrl: './dot-es-search-page.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush,
    host: { class: 'flex flex-col h-full min-h-0 bg-white' }
})
export class DotEsSearchPageComponent {
    readonly store = inject(DotEsSearchStore);
    private readonly messageService = inject(DotMessageService);
    private readonly destroyRef = inject(DestroyRef);

    readonly exportMenu = viewChild.required<Menu>('exportMenu');
    readonly helpPopover = viewChild.required<Popover>('helpPopoverEl');

    readonly QUERY_EDITOR_OPTIONS = QUERY_EDITOR_OPTIONS;
    readonly RAW_EDITOR_OPTIONS = RAW_EDITOR_OPTIONS;

    readonly paramsOpen = signal(true);
    readonly hasEditorErrors = signal(false);

    readonly depthOptions = [
        { label: '0', value: 0 },
        { label: '1', value: 1 },
        { label: '2', value: 2 },
        { label: '3', value: 3 }
    ];

    readonly noHitsConfig: PrincipalConfiguration = {
        title: this.messageService.get('esSearch.results.noHits'),
        subtitle: this.messageService.get('esSearch.results.noHits.subtitle'),
        icon: 'pi-search'
    };

    readonly parsedAggregations = computed<ParsedAggregation[]>(() => {
        const raw = this.store.aggregations();
        if (!raw) return [];
        return Object.entries(raw).map(([key, value]) => this.parseAggregation(key, value));
    });

    readonly parsedSuggestions = computed<ParsedSuggester[]>(() => {
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
            label: this.messageService.get('esSearch.export.json'),
            command: () => this.exportAs('json')
        },
        {
            label: this.messageService.get('esSearch.export.csv'),
            command: () => this.exportAs('csv')
        }
    ];

    readonly helpExamples = [
        {
            title: 'esSearch.help.example.matchAll',
            query: '{ "query": { "match_all": {} } }'
        },
        {
            title: 'esSearch.help.example.fullText',
            query: '{\n  "query": {\n    "query_string": {\n      "query": "contentType:Blog"\n    }\n  }\n}'
        },
        {
            title: 'esSearch.help.example.withAggregation',
            query: '{\n  "query": { "match_all": {} },\n  "aggs": {\n    "by_type": {\n      "terms": { "field": "contentType" }\n    }\n  },\n  "size": 0\n}'
        },
        {
            title: 'esSearch.help.example.sorted',
            query: '{\n  "query": {\n    "term": { "contentType": "Blog" }\n  },\n  "sort": [{ "modDate": "desc" }],\n  "from": 0,\n  "size": 20\n}'
        }
    ];

    onEditorInit(editor: { getModel(): { uri: unknown } | null }): void {
        const model = editor.getModel();
        if (!model) return;

        const disposable = monaco.editor.onDidChangeMarkers(() => {
            const markers = monaco.editor.getModelMarkers({ resource: model.uri });
            this.hasEditorErrors.set(markers.some((m) => m.severity === MONACO_SEVERITY_ERROR));
        });

        this.destroyRef.onDestroy(() => disposable.dispose());
    }

    onTabChange(value: string): void {
        this.store.setActiveTab(value as EsSearchActiveTab);
    }

    toggleExportMenu(event: MouseEvent): void {
        this.exportMenu().toggle(event);
    }

    onRun(): void {
        this.store.runSearch();
    }

    useExample(query: string): void {
        this.store.setQuery(query);
        this.helpPopover().hide();
    }

    copyQuery(query: string): void {
        navigator.clipboard.writeText(query);
    }

    copyToClipboard(value: unknown): void {
        navigator.clipboard.writeText(String(value ?? ''));
    }

    asContentState(contentlet: Record<string, unknown>): DotContentState {
        return contentlet as unknown as DotContentState;
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

    private exportAs(format: 'json' | 'csv'): void {
        const hits = this.store.hits();
        if (!hits.length) return;

        let content: string;
        let mimeType: string;
        let filename: string;
        const date = new Date().toISOString().slice(0, 10);

        if (format === 'json') {
            content = JSON.stringify(hits, null, 2);
            mimeType = 'application/json';
            filename = `es-search-export-${date}.json`;
        } else {
            const sources = hits.map((h) => h._source);
            const keys = [...new Set(sources.flatMap(Object.keys))];
            const rows = sources.map((s) => keys.map((k) => JSON.stringify(s[k] ?? '')).join(','));
            content = [keys.join(','), ...rows].join('\n');
            mimeType = 'text/csv;charset=utf-8;';
            filename = `es-search-export-${date}.csv`;
        }

        const blob = new Blob([content], { type: mimeType });
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = filename;
        a.click();
        URL.revokeObjectURL(url);
    }
}
