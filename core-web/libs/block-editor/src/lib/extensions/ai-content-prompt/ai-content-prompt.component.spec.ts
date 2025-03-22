import { Spectator, createComponentFactory } from '@ngneat/spectator/jest';

import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';

import { ConfirmationService } from 'primeng/api';

import { DotAiService, DotMessageService } from '@dotcms/data-access';

import { AIContentPromptComponent } from './ai-content-prompt.component';
import { AiContentPromptStore } from './store/ai-content-prompt.store';

describe('AIContentPromptComponent', () => {
    let spectator: Spectator<AIContentPromptComponent>;
    const createComponent = createComponentFactory({
        component: AIContentPromptComponent,
        providers: [
            AiContentPromptStore,
            DotMessageService,
            DotAiService,
            ConfirmationService,
            provideHttpClient(),
            provideHttpClientTesting()
        ]
    });

    beforeEach(() => (spectator = createComponent()));

    it('should create', () => {
        expect(spectator.component).toBeTruthy();
    });
});
