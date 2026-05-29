import { MonacoEditorLoaderService, MonacoEditorModule } from '@materia-ui/ngx-monaco-editor';

import { DOCUMENT } from '@angular/common';
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
import { Menu, MenuModule } from 'primeng/menu';
import { MessageModule } from 'primeng/message';
import { PanelModule } from 'primeng/panel';
import { Popover, PopoverModule } from 'primeng/popover';
import { SelectModule } from 'primeng/select';
import { SplitterModule } from 'primeng/splitter';
import { TagModule } from 'primeng/tag';
import { TooltipModule } from 'primeng/tooltip';

import { filter, take } from 'rxjs/operators';

import { DotGlobalMessageService, DotMessageService } from '@dotcms/data-access';
import { ComponentStatus } from '@dotcms/dotcms-models';
import {
    DOT_MONACO_BASE_OPTIONS,
    DOT_MONACO_RAW_OPTIONS,
    DotClipboardUtil,
    DotEmptyContainerComponent,
    DotMessagePipe,
    PrincipalConfiguration
} from '@dotcms/ui';
import { buildCurlSnippet, buildFetchSnippet, getDownloadLink } from '@dotcms/utils';

import { DotVelocityPlaygroundStore } from './store/dot-velocity-playground.store';

import {
    ensureVelocityLanguageRegistered,
    VELOCITY_LANGUAGE_ID,
    VELOCITY_THEME_ID
} from '../monaco/register-velocity';
import { DotVelocityPlaygroundService } from '../services/dot-velocity-playground.service';

interface VelocityHelpExample {
    title: string;
    code: string;
    description?: string;
}

@Component({
    selector: 'dot-velocity-playground-page',
    imports: [
        FormsModule,
        MonacoEditorModule,
        SplitterModule,
        ButtonModule,
        CheckboxModule,
        SelectModule,
        TagModule,
        TooltipModule,
        MessageModule,
        MenuModule,
        PanelModule,
        PopoverModule,
        DotEmptyContainerComponent,
        DotMessagePipe
    ],
    providers: [DotVelocityPlaygroundStore, DotVelocityPlaygroundService, DotClipboardUtil],
    templateUrl: './dot-velocity-playground-page.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    host: { class: 'flex flex-col h-full min-h-0 bg-white' }
})
export class DotVelocityPlaygroundPageComponent {
    readonly store = inject(DotVelocityPlaygroundStore);
    readonly #messageService = inject(DotMessageService);
    readonly #globalMessage = inject(DotGlobalMessageService);
    readonly #clipboard = inject(DotClipboardUtil);
    readonly #document = inject(DOCUMENT);
    readonly #monacoLoader = inject(MonacoEditorLoaderService);

    constructor() {
        // Register the Velocity language + custom theme as soon as Monaco's AMD
        // loader is ready, before any <ngx-monaco-editor> instance is created.
        // Without this, the editor mounts referencing `dot-velocity-dark` which
        // doesn't exist yet and Monaco silently falls back to the default light
        // theme.
        this.#monacoLoader.isMonacoLoaded$
            .pipe(
                filter((isLoaded) => isLoaded),
                take(1)
            )
            .subscribe(() => ensureVelocityLanguageRegistered());
    }

    readonly helpPopover = viewChild.required<Popover>('helpPopoverEl');
    readonly exportMenu = viewChild<Menu>('exportMenu');

    readonly exportItems: MenuItem[] = [
        {
            label: this.#messageService.get('velocityPlayground.copy.curl'),
            command: () => this.#copyAs('curl')
        },
        {
            label: this.#messageService.get('velocityPlayground.copy.fetch'),
            command: () => this.#copyAs('fetch')
        }
    ];

    readonly ComponentStatus = ComponentStatus;

    readonly splitterPt = { root: { class: 'border-0! rounded-none! flex-1 min-h-0' } };

    readonly $historyOpen = signal(false);

    readonly $editorOptions = computed(() => ({
        ...DOT_MONACO_BASE_OPTIONS,
        theme: VELOCITY_THEME_ID,
        language: VELOCITY_LANGUAGE_ID,
        wordWrap: this.store.wrapCode() ? 'on' : 'off'
    }));

    readonly $outputOptions = computed(() => ({
        ...DOT_MONACO_RAW_OPTIONS,
        theme: VELOCITY_THEME_ID,
        language: this.store.outputContentType(),
        wordWrap: this.store.wrapCode() ? 'on' : 'off',
        readOnly: true
    }));

    readonly $historyOptions = computed(() =>
        this.store.history().map((entry) => ({
            label: this.#formatHistoryLabel(entry),
            value: entry
        }))
    );

    readonly emptyOutputConfig: PrincipalConfiguration = {
        title: this.#messageService.get('velocityPlayground.output.empty.title'),
        subtitle: this.#messageService.get('velocityPlayground.output.empty'),
        icon: 'pi-play'
    };

    readonly helpExamples: VelocityHelpExample[] = [
        {
            title: 'velocityPlayground.help.example.contentSnapshot',
            description: 'velocityPlayground.help.example.contentSnapshot.desc',
            code: '#set($types = ["htmlpageasset","webPageContent","FileAsset","persona","Vanityurl"])\nContent live on $host.hostname:\n#foreach($t in $types)\n  #set($n = $dotcontent.pull("+contentType:$t +live:true +conhost:$host.identifier", 1000, "modDate").size())\n  - $t: $n\n#end'
        },
        {
            title: 'velocityPlayground.help.example.pullPages',
            description: 'velocityPlayground.help.example.pullPages.desc',
            code: '#set($pages = $dotcontent.pull("+contentType:htmlpageasset +live:true +conhost:$host.identifier", 10, "modDate desc"))\nFound $pages.size() page(s):\n#foreach($page in $pages)\n  - $page.title  ($page.pageUrl)\n#end'
        },
        {
            title: 'velocityPlayground.help.example.transformToJsonApi',
            description: 'velocityPlayground.help.example.transformToJsonApi.desc',
            code: '#set($pages = $dotcontent.pull("+contentType:htmlpageasset +live:true +conhost:$host.identifier", 10, "modDate desc"))\n#set($items = [])\n#foreach($p in $pages)\n  #set($entry = {\n    "id":      $p.identifier,\n    "title":   $p.title,\n    "url":     $p.pageUrl,\n    "modDate": $date.format("yyyy-MM-dd\'T\'HH:mm:ssZ", $p.modDate)\n  })\n  $items.add($entry)\n#end\n$dotJSON.put("site", $host.hostname)\n$dotJSON.put("count", $items.size())\n$dotJSON.put("items", $items)'
        },
        {
            title: 'velocityPlayground.help.example.pullFiles',
            description: 'velocityPlayground.help.example.pullFiles.desc',
            code: '#set($files = $dotcontent.pull("+contentType:FileAsset +live:true +conhost:$host.identifier", 5, "modDate desc"))\n#foreach($f in $files)\n  - $f.fileName  ($f.fileSize bytes, $f.mimeType)\n#end'
        }
    ];

    onEditorInit(): void {
        ensureVelocityLanguageRegistered();
    }

    onRun(): void {
        if (!this.store.canRun()) return;
        this.store.runScript();
    }

    onHistoryChange(entry: string | null): void {
        if (entry == null) return;
        this.store.selectHistoryEntry(entry);
    }

    onSplitterResize(event: { sizes: number[] }): void {
        const [left, right] = event.sizes;
        if (typeof left === 'number' && typeof right === 'number') {
            this.store.setSplitterRatio([left, right]);
        }
    }

    useExample(code: string): void {
        this.store.setCode(code);
        this.helpPopover().hide();
    }

    copyToClipboard(value: unknown): void {
        this.#copy(String(value ?? ''));
    }

    toggleExportMenu(event: MouseEvent): void {
        this.exportMenu()?.toggle(event);
    }

    downloadOutput(): void {
        const contentType = this.store.outputContentType();
        const ext = contentType === 'plaintext' ? 'txt' : contentType;
        const mime =
            contentType === 'plaintext'
                ? 'text/plain'
                : contentType === 'json'
                  ? 'application/json'
                  : 'application/xml';

        const blob = new Blob([this.store.output()], { type: mime });
        const link = getDownloadLink(blob, `velocity-output.${ext}`);
        this.#document.body.appendChild(link);
        link.click();
        this.#document.body.removeChild(link);
    }

    #copyAs(format: 'curl' | 'fetch'): void {
        const path = '/api/vtl/dynamic/';
        const origin = this.#document.defaultView?.location.origin ?? '';
        const body = { velocity: this.store.code() };
        const snippet =
            format === 'curl'
                ? buildCurlSnippet({ url: `${origin}${path}`, body })
                : buildFetchSnippet({ url: path, body });
        this.#copy(snippet);
    }

    async #copy(text: string): Promise<void> {
        const ok = await this.#clipboard.copy(text);
        if (!ok) this.#globalMessage.error();
    }

    #formatHistoryLabel(entry: string): string {
        const compact = entry.replace(/\s+/g, ' ').trim();
        return compact.length > 60
            ? `${compact.slice(0, 60)}…`
            : compact || this.#messageService.get('velocityPlayground.history.empty');
    }
}
