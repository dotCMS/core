import {
    byTestId,
    createComponentFactory,
    mockProvider,
    Spectator,
    SpyObject
} from '@ngneat/spectator/jest';
import { patchState } from '@ngrx/signals';
import { Observable, of } from 'rxjs';

import { provideHttpClient } from '@angular/common/http';

import { ConfirmationService } from 'primeng/api';
import { DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';

import { DotAiService } from '@dotcms/data-access';
import {
    AIImagePrompt,
    DotAIImageContent,
    DotAIImageOrientation,
    PromptType
} from '@dotcms/dotcms-models';

import { DotAIImagePromptComponent } from './ai-image-prompt.component';
import { AiImagePromptFormComponent } from './components/ai-image-prompt-form/ai-image-prompt-form.component';
import { DotAiImagePromptStore } from './store/ai-image-prompt.store';
import { MOCK_AI_IMAGE_CONTENT, MOCK_GENERATED_AI_IMAGE } from './utils/mocks';

const mockDotAiService = {
    generateAndPublishImage: (_prompt: string, _size?: string): Observable<DotAIImageContent> =>
        of(MOCK_AI_IMAGE_CONTENT)
};

describe('DotAIImagePromptComponent', () => {
    let spectator: Spectator<DotAIImagePromptComponent>;
    let store: InstanceType<typeof DotAiImagePromptStore>;
    let dynamicDialogRef: SpyObject<DynamicDialogRef>;
    let dotAiService: SpyObject<DotAiService>;
    let confirmationService: SpyObject<ConfirmationService>;

    const createComponent = createComponentFactory({
        component: DotAIImagePromptComponent,
        componentProviders: [
            DotAiImagePromptStore,
            mockProvider(DynamicDialogRef),
            mockProvider(DynamicDialogConfig),
            ConfirmationService
        ],
        providers: [provideHttpClient(), { provide: DotAiService, useValue: mockDotAiService }]
    });

    beforeEach(() => {
        spectator = createComponent();
        store = spectator.inject(DotAiImagePromptStore, true);
        dynamicDialogRef = spectator.inject(DynamicDialogRef, true);
        dotAiService = spectator.inject(DotAiService, true);
        confirmationService = spectator.inject(ConfirmationService, true);

        jest.spyOn(dotAiService, 'generateAndPublishImage');
    });

    it('should create', () => {
        expect(spectator.component).toBeTruthy();
    });

    it('should generate image', () => {
        const generateImageSpy = jest.spyOn(store, 'generateImage');

        const formMock: AIImagePrompt = {
            text: 'Test',
            type: PromptType.INPUT,
            size: DotAIImageOrientation.VERTICAL
        };
        patchState(store, { formValue: formMock });

        spectator.triggerEventHandler(AiImagePromptFormComponent, 'generate', null);
        expect(generateImageSpy).toHaveBeenCalled();
        expect(dotAiService.generateAndPublishImage).toHaveBeenCalledWith(
            'Test',
            DotAIImageOrientation.VERTICAL
        );
    });

    it('should inset an image', async () => {
        patchState(store, { images: [MOCK_GENERATED_AI_IMAGE] });
        spectator.detectChanges();

        const submitBtn = spectator.query(byTestId('submit-btn'));

        spectator.click(submitBtn);
        await spectator.fixture.whenStable();
        expect(dynamicDialogRef.close).toHaveBeenCalled();
    });

    it('should call confirm dialog when try to close dialog', async () => {
        patchState(store, { images: [MOCK_GENERATED_AI_IMAGE] });
        const confirmSpy = jest.spyOn(confirmationService, 'confirm');
        spectator.detectChanges();

        const closeBtn = spectator.query(byTestId('close-btn'));

        spectator.click(closeBtn);
        await spectator.fixture.whenStable();
        expect(confirmSpy).toHaveBeenCalled();
    });
});
