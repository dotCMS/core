import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';

import { ButtonModule } from 'primeng/button';
import { TextareaModule } from 'primeng/textarea';

import { map, take } from 'rxjs/operators';

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
        maxRetries: 3,
        rolePrompt: 'You are dotCMSbot, an AI assistant to help content creators.',
        textPrompt: 'Use Descriptive writing style.'
    },
    embeddings: {
        provider: 'openai',
        apiKey: 'sk-...',
        model: 'text-embedding-ada-002',
        listenerIndexer: { default: 'blog,news,webPageContent' }
    },
    image: {
        provider: 'openai',
        apiKey: 'sk-...',
        model: 'dall-e-3',
        size: '1792x1024',
        imagePrompt: 'Use 16:9 aspect ratio.'
    }
};

@Component({
    selector: 'dot-ai-config-detail',
    templateUrl: './dot-ai-config-detail.component.html',
    styleUrls: ['./dot-ai-config-detail.component.scss'],
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
    private cdr = inject(ChangeDetectorRef);

    app: DotApp;
    configJson = '';
    saving = false;
    readonly exampleJson = JSON.stringify(EXAMPLE_CONFIG, null, 2);

    ngOnInit(): void {
        this.route.data
            .pipe(
                map((x) => x?.data),
                take(1)
            )
            .subscribe((app: DotApp) => {
                this.app = app;
            });

        this.dotAiService
            .getConfig()
            .pipe(take(1))
            .subscribe({
                next: (config) => {
                    if (config?.providerConfig) {
                        try {
                            this.configJson = JSON.stringify(
                                JSON.parse(config.providerConfig),
                                null,
                                2
                            );
                        } catch {
                            this.configJson = config.providerConfig;
                        }
                        this.cdr.detectChanges();
                    }
                }
            });
    }

    onSubmit(): void {
        this.saving = true;
        this.dotAiService
            .saveConfig(this.configJson)
            .pipe(take(1))
            .subscribe({
                next: () => {
                    this.saving = false;
                    this.dotMessageDisplayService.push({
                        life: 3000,
                        message: this.dotMessageService.get('dot.common.message.saved'),
                        severity: DotMessageSeverity.SUCCESS,
                        type: DotMessageType.SIMPLE_MESSAGE
                    });
                },
                error: (err) => {
                    this.saving = false;
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
        this.dotRouterService.goToAppsConfiguration(this.app.key);
    }
}
