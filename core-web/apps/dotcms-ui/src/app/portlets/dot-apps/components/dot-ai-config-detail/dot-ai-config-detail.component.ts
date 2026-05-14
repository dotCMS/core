import { Component, DestroyRef, OnInit, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';

import { ButtonModule } from 'primeng/button';
import { TextareaModule } from 'primeng/textarea';

import { map } from 'rxjs/operators';

import {
    DotAiService,
    DotMessageDisplayService,
    DotMessageService,
    DotRouterService
} from '@dotcms/data-access';
import { DotApp, DotMessageSeverity, DotMessageType } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';

import { DotAppsConfigurationHeaderComponent } from '../dot-apps-configuration-detail/components/dot-apps-configuration-header/dot-apps-configuration-header.component';

const EXAMPLE_CONFIG = {
    chat: {
        provider: 'openai',
        apiKey: 'sk-...',
        model: 'gpt-4o',
        maxTokens: 16384,
        temperature: 1.0,
        maxRetries: 3
    },
    embeddings: {
        provider: 'openai',
        apiKey: 'sk-...',
        model: 'text-embedding-ada-002'
    },
    image: {
        provider: 'openai',
        apiKey: 'sk-...',
        model: 'gpt-image-1'
    },
    settings: {
        rolePrompt: 'You are dotCMSbot, an AI assistant to help content creators.',
        textPrompt: 'Use Descriptive writing style.',
        imagePrompt: 'Use 16:9 aspect ratio.',
        imageSize: '1792x1024',
        listenerIndexer: { default: 'blog,news,webPageContent' },
        completionRolePrompt: 'You are a helpful assistant with a descriptive writing style.',
        completionTextPrompt:
            'Answer this question\n"$!{prompt}?"\n\nby using only the information in the following text:\n"""\n$!{supportingContent} \n"""\n',
        embeddingsSearchThreshold: 0.25
    }
};

@Component({
    selector: 'dot-ai-config-detail',
    templateUrl: './dot-ai-config-detail.component.html',
    host: { class: 'flex h-full p-4 bg-gray-200 shadow-md' },
    imports: [
        FormsModule,
        ButtonModule,
        TextareaModule,
        DotAppsConfigurationHeaderComponent,
        DotMessagePipe
    ]
})
export class DotAiConfigDetailComponent implements OnInit {
    private route = inject(ActivatedRoute);
    private dotAiService = inject(DotAiService);
    private dotRouterService = inject(DotRouterService);
    private dotMessageDisplayService = inject(DotMessageDisplayService);
    private dotMessageService = inject(DotMessageService);
    private destroyRef = inject(DestroyRef);

    private readonly siteId = this.route.snapshot.paramMap.get('id') ?? undefined;

    readonly app = signal<DotApp | null>(null);
    readonly configJson = signal('');
    readonly saving = signal(false);
    readonly exampleJson = JSON.stringify(EXAMPLE_CONFIG, null, 2);

    ngOnInit(): void {
        this.route.data
            .pipe(
                map((x) => x?.data),
                takeUntilDestroyed(this.destroyRef)
            )
            .subscribe((app: DotApp) => {
                this.app.set(app);
            });

        this.dotAiService
            .getConfig(this.siteId)
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe({
                next: (config) => {
                    if (config?.providerConfig) {
                        try {
                            this.configJson.set(
                                JSON.stringify(JSON.parse(config.providerConfig), null, 2)
                            );
                        } catch {
                            this.configJson.set(config.providerConfig);
                        }
                    }
                },
                error: (err) => {
                    const detail =
                        err?.error?.error ?? err?.message ?? 'Failed to load AI configuration';
                    this.dotMessageDisplayService.push({
                        life: 5000,
                        message: detail,
                        severity: DotMessageSeverity.ERROR,
                        type: DotMessageType.SIMPLE_MESSAGE
                    });
                }
            });
    }

    onSubmit(): void {
        try {
            JSON.parse(this.configJson());
        } catch {
            this.dotMessageDisplayService.push({
                life: 5000,
                message: 'Invalid JSON — please check the provider configuration',
                severity: DotMessageSeverity.ERROR,
                type: DotMessageType.SIMPLE_MESSAGE
            });

            return;
        }

        this.saving.set(true);
        this.dotAiService
            .saveConfig(this.configJson(), this.siteId)
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe({
                next: () => {
                    this.saving.set(false);
                    this.dotMessageDisplayService.push({
                        life: 3000,
                        message: this.dotMessageService.get('dot.common.message.saved'),
                        severity: DotMessageSeverity.SUCCESS,
                        type: DotMessageType.SIMPLE_MESSAGE
                    });
                },
                error: (err) => {
                    this.saving.set(false);
                    const detail =
                        err?.error?.error ?? err?.message ?? 'Failed to save AI configuration';
                    this.dotMessageDisplayService.push({
                        life: 5000,
                        message: detail,
                        severity: DotMessageSeverity.ERROR,
                        type: DotMessageType.SIMPLE_MESSAGE
                    });
                }
            });
    }

    goToApps(): void {
        const key = this.app()?.key ?? 'dotAI';
        this.dotRouterService.goToAppsConfiguration(key);
    }
}
