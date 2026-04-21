import { MonacoEditorModule } from '@materia-ui/ngx-monaco-editor';

import { DecimalPipe, KeyValuePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject, signal, viewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { MenuItem } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { ChipModule } from 'primeng/chip';
import { InputTextModule } from 'primeng/inputtext';
import { Menu, MenuModule } from 'primeng/menu';
import { Popover, PopoverModule } from 'primeng/popover';
import { SelectModule } from 'primeng/select';
import { SkeletonModule } from 'primeng/skeleton';
import { SplitterModule } from 'primeng/splitter';
import { TableModule } from 'primeng/table';
import { TabsModule } from 'primeng/tabs';
import { ToggleSwitchModule } from 'primeng/toggleswitch';
import { TooltipModule } from 'primeng/tooltip';

import { DotEsSearchService, DotMessageService } from '@dotcms/data-access';
import { DotEmptyContainerComponent, DotMessagePipe } from '@dotcms/ui';

import { DotEsSearchStore, EsSearchActiveTab } from './store/dot-es-search.store';

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
        KeyValuePipe,
        MonacoEditorModule,
        SplitterModule,
        TabsModule,
        TableModule,
        ButtonModule,
        ChipModule,
        SelectModule,
        InputTextModule,
        ToggleSwitchModule,
        TooltipModule,
        PopoverModule,
        MenuModule,
        SkeletonModule,
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

    readonly exportMenu = viewChild.required<Menu>('exportMenu');
    readonly helpPopover = viewChild.required<Popover>('helpPopoverEl');

    readonly QUERY_EDITOR_OPTIONS = QUERY_EDITOR_OPTIONS;
    readonly RAW_EDITOR_OPTIONS = RAW_EDITOR_OPTIONS;

    readonly paramsOpen = signal(true);

    readonly depthOptions = [
        { label: '0', value: 0 },
        { label: '1', value: 1 },
        { label: '2', value: 2 },
        { label: '3', value: 3 }
    ];

    readonly exportItems: MenuItem[] = [
        {
            label: this.messageService.get('esSearch.export.json'),
            icon: 'pi pi-file',
            command: () => this.exportAs('json')
        },
        {
            label: this.messageService.get('esSearch.export.csv'),
            icon: 'pi pi-table',
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

    onTabChange(value: string): void {
        const tab = value as EsSearchActiveTab;
        this.store.setActiveTab(tab);

        if (tab === 'raw' && !this.store.rawResponse()) {
            this.store.loadRaw();
        }
    }

    onRun(): void {
        this.store.runSearch();
        this.store.setActiveTab('results');
    }

    toggleExportMenu(event: MouseEvent): void {
        this.exportMenu().toggle(event);
    }

    useExample(query: string): void {
        this.store.setQuery(query);
        this.helpPopover().hide();
    }

    copyQuery(query: string): void {
        navigator.clipboard.writeText(query);
    }

    getStatusSeverity(contentlet: Record<string, unknown>): string {
        const status = String(contentlet['contentletLocked'] ?? '');
        const map: Record<string, string> = {
            published: 'success',
            draft: 'warn',
            review: 'info',
            archived: 'secondary'
        };

        return map[status] ?? 'secondary';
    }

    getContentletStatus(contentlet: Record<string, unknown>): string {
        if (contentlet['live']) return 'Published';
        if (contentlet['working']) return 'Draft';

        return 'Archived';
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
