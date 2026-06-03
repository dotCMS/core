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
    DotSpinnerComponent,
    PrincipalConfiguration
} from '@dotcms/ui';
import { buildCurlSnippet, buildFetchSnippet, getDownloadLink } from '@dotcms/utils';

import { DotVelocityPlaygroundStore } from './store/dot-velocity-playground.store';

import {
    formatHistoryLabel,
    getDownloadParams,
    VELOCITY_HELP_EXAMPLES
} from '../dot-velocity-playground.utils';
import {
    ensureVelocityLanguageRegistered,
    VELOCITY_LANGUAGE_ID,
    VELOCITY_THEME_ID
} from '../monaco/register-velocity';

@Component({
    selector: 'dot-velocity-playground-page',
    imports: [
        FormsModule,
        MonacoEditorModule,
        SplitterModule,
        ButtonModule,
        CheckboxModule,
        SelectModule,
        TooltipModule,
        MessageModule,
        MenuModule,
        PanelModule,
        PopoverModule,
        DotEmptyContainerComponent,
        DotSpinnerComponent,
        DotMessagePipe
    ],
    providers: [DotVelocityPlaygroundStore, DotClipboardUtil],
    templateUrl: './dot-velocity-playground-page.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    host: { class: 'flex flex-col h-full min-h-0 bg-white' }
})
export class DotVelocityPlaygroundPageComponent {
    // 1. Dependency Injection
    readonly store = inject(DotVelocityPlaygroundStore);
    readonly #messageService = inject(DotMessageService);
    readonly #globalMessage = inject(DotGlobalMessageService);
    readonly #clipboard = inject(DotClipboardUtil);
    readonly #document = inject(DOCUMENT);
    readonly #monacoLoader = inject(MonacoEditorLoaderService);

    // Memoized i18n fallback used by every history-label render.
    readonly #emptyHistoryLabel = this.#messageService.get('velocityPlayground.history.empty');

    // 2. State signals (viewChild signals + local state) — $ prefix
    readonly $helpPopover = viewChild.required<Popover>('helpPopoverEl');
    readonly $exportMenu = viewChild<Menu>('exportMenu');
    readonly $historyOpen = signal(false);

    // 3. Computed signals — $ prefix
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
            label: formatHistoryLabel(entry, this.#emptyHistoryLabel),
            value: entry
        }))
    );

    // 4. Template-bound static configuration
    readonly ComponentStatus = ComponentStatus;
    readonly splitterPt = { root: { class: 'border-0! rounded-none! flex-1 min-h-0' } };
    readonly helpExamples = VELOCITY_HELP_EXAMPLES;

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

    readonly emptyOutputConfig: PrincipalConfiguration = {
        title: this.#messageService.get('velocityPlayground.output.empty'),
        subtitle: this.#messageService.get('velocityPlayground.output.empty.hint'),
        icon: 'pi-search'
    };

    // 5. Lifecycle
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

    // 6. Public methods
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
        this.$helpPopover().hide();
    }

    copyToClipboard(value: unknown): void {
        this.#copy(String(value ?? ''));
    }

    toggleExportMenu(event: MouseEvent): void {
        this.$exportMenu()?.toggle(event);
    }

    downloadOutput(): void {
        const { ext, mime } = getDownloadParams(this.store.outputContentType());
        const blob = new Blob([this.store.output()], { type: mime });
        const link = getDownloadLink(blob, `velocity-output.${ext}`);
        this.#document.body.appendChild(link);
        link.click();
        this.#document.body.removeChild(link);
    }

    // 7. Private helpers
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
}
