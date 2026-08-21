import { forkJoin } from 'rxjs';

import {
    ChangeDetectionStrategy,
    Component,
    DestroyRef,
    OnInit,
    computed,
    effect,
    inject,
    signal,
    viewChild,
    viewChildren
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute } from '@angular/router';

import { ButtonModule } from 'primeng/button';

import { map } from 'rxjs/operators';

import {
    DotAiService,
    DotMessageDisplayService,
    DotMessageService,
    DotRouterService
} from '@dotcms/data-access';
import {
    DotApp,
    DotAiProviderMetadata,
    DotMessageSeverity,
    DotMessageType
} from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';
import { isEqual } from '@dotcms/utils';

import {
    DotAiCapabilityCardComponent,
    DotAiCapabilitySectionValue
} from './components/dot-ai-capability-card/dot-ai-capability-card.component';
import { DotAiSettingsCardComponent } from './components/dot-ai-settings-card/dot-ai-settings-card.component';
import { CAPABILITY_META } from './dot-ai-config.constants';

@Component({
    selector: 'dot-ai-config-detail',
    templateUrl: './dot-ai-config-detail.component.html',
    host: { class: 'flex h-full w-full flex-col overflow-hidden bg-white' },
    changeDetection: ChangeDetectionStrategy.OnPush,
    imports: [
        ButtonModule,
        DotAiCapabilityCardComponent,
        DotAiSettingsCardComponent,
        DotMessagePipe
    ]
})
export class DotAiConfigDetailComponent implements OnInit {
    private readonly route = inject(ActivatedRoute);
    private readonly dotAiService = inject(DotAiService);
    private readonly dotRouterService = inject(DotRouterService);
    private readonly dotMessageDisplayService = inject(DotMessageDisplayService);
    private readonly dotMessageService = inject(DotMessageService);
    private readonly destroyRef = inject(DestroyRef);

    readonly siteId = this.route.snapshot.paramMap.get('id') ?? undefined;

    readonly app = signal<DotApp | null>(null);
    readonly loading = signal(true);
    readonly loadFailed = signal(false);
    readonly saving = signal(false);
    readonly dirty = signal(false);

    readonly capabilityMeta = CAPABILITY_META;
    readonly initialSections = signal<Record<string, DotAiCapabilitySectionValue | null>>({});
    readonly initialSettings = signal<Record<string, unknown> | null>(null);
    readonly providers = signal<DotAiProviderMetadata[]>([]);

    /** The site this configuration applies to — already resolved by the route (see the
     *  `dotAiConfigDetailResolver`), just never surfaced in the redesigned page. */
    readonly siteName = computed(() => this.app()?.sites?.[0]?.name ?? null);

    private readonly capabilityCards = viewChildren(DotAiCapabilityCardComponent);
    private readonly settingsCard = viewChild(DotAiSettingsCardComponent);

    private savedPayload: Record<string, unknown> | null = null;
    private baselineCaptured = false;

    constructor() {
        effect(() => {
            const cards = this.capabilityCards();
            const settings = this.settingsCard();

            if (this.baselineCaptured || this.loading() || cards.length === 0 || !settings) {
                return;
            }

            this.baselineCaptured = true;
            this.savedPayload = this.buildCurrentPayload();
        });
    }

    ngOnInit(): void {
        this.route.data
            .pipe(
                map((x) => x?.data),
                takeUntilDestroyed(this.destroyRef)
            )
            .subscribe((app: DotApp) => this.app.set(app));

        forkJoin({
            providers: this.dotAiService.getProviders(),
            config: this.dotAiService.getConfig(this.siteId)
        })
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe({
                next: ({ providers, config }) => {
                    this.providers.set(providers);

                    let parsed: Record<string, unknown> = {};
                    if (config?.providerConfig) {
                        try {
                            parsed = JSON.parse(config.providerConfig);
                        } catch {
                            parsed = {};
                        }
                    }

                    this.initialSections.set({
                        chat: (parsed['chat'] as DotAiCapabilitySectionValue) ?? null,
                        embeddings: (parsed['embeddings'] as DotAiCapabilitySectionValue) ?? null,
                        image: (parsed['image'] as DotAiCapabilitySectionValue) ?? null
                    });
                    this.initialSettings.set(
                        (parsed['settings'] as Record<string, unknown>) ?? null
                    );
                    this.loading.set(false);
                },
                error: (err) => {
                    this.loading.set(false);
                    this.loadFailed.set(true);
                    this.showError(err, this.dotMessageService.get('apps.ai.error.load'));
                }
            });
    }

    onAnyChanged(): void {
        if (!this.baselineCaptured) {
            return;
        }

        this.dirty.set(!isEqual(this.buildCurrentPayload(), this.savedPayload));
    }

    cancel(): void {
        const key = this.app()?.key ?? 'dotAI';
        this.dotRouterService.goToAppsConfiguration(key);
    }

    save(): void {
        if (this.loadFailed()) {
            return;
        }

        const cards = this.capabilityCards();

        const invalidCard = cards.find((card) => !card.isValid());
        if (invalidCard) {
            invalidCard.markAllTouched();
            this.dotMessageDisplayService.push({
                life: 5000,
                message: this.dotMessageService.get('apps.ai.validation.required-fields'),
                severity: DotMessageSeverity.ERROR,
                type: DotMessageType.SIMPLE_MESSAGE
            });

            return;
        }

        const payload = this.buildCurrentPayload();

        this.saving.set(true);
        this.dotAiService
            .saveConfig(JSON.stringify(payload), this.siteId)
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe({
                next: () => {
                    this.saving.set(false);
                    this.savedPayload = payload;
                    this.dirty.set(false);
                    this.dotMessageDisplayService.push({
                        life: 3000,
                        message: this.dotMessageService.get('dot.common.message.saved'),
                        severity: DotMessageSeverity.SUCCESS,
                        type: DotMessageType.SIMPLE_MESSAGE
                    });
                },
                error: (err) => {
                    this.saving.set(false);
                    this.showError(err, this.dotMessageService.get('apps.ai.error.save'));
                }
            });
    }

    private buildCurrentPayload(): Record<string, unknown> {
        const payload: Record<string, unknown> = {};
        this.capabilityCards().forEach((card) => {
            const section = card.buildPayloadSection();
            if (section) {
                payload[card.meta().sectionKey] = section;
            }
        });

        const settings = this.settingsCard();
        if (settings) {
            payload['settings'] = settings.buildPayloadSection();
        }

        return payload;
    }

    private showError(err: unknown, fallback: string): void {
        const detail =
            (err as { error?: { error?: string }; message?: string })?.error?.error ??
            (err as { message?: string })?.message ??
            fallback;
        this.dotMessageDisplayService.push({
            life: 5000,
            message: detail,
            severity: DotMessageSeverity.ERROR,
            type: DotMessageType.SIMPLE_MESSAGE
        });
    }
}
