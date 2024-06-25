import { byTestId, createComponentFactory, Spectator } from '@ngneat/spectator';
import { of } from 'rxjs';

import { ConfirmationService } from 'primeng/api';
import { Dialog } from 'primeng/dialog';

import {
    PromptType,
    AIImagePrompt,
    DotAIImageOrientation,
    DotGeneratedAIImage
} from '@dotcms/dotcms-models';

import { AIImagePromptComponent } from './ai-image-prompt.component';
import { DotAiImagePromptStore } from './ai-image-prompt.store';
import { AiImagePromptFormComponent } from './components/ai-image-prompt-form/ai-image-prompt-form.component';

describe('AIImagePromptComponent', () => {
    let spectator: Spectator<AIImagePromptComponent>;
    let store: DotAiImagePromptStore;
    let confirmationService: ConfirmationService;

    const imagesMock: DotGeneratedAIImage[] = [
        { name: 'image1', url: 'image_url' },
        { name: 'image2', url: 'image_url_2' }
    ] as unknown as DotGeneratedAIImage[];

    const createComponent = createComponentFactory({
        component: AIImagePromptComponent,
        providers: [
            {
                provide: DotAiImagePromptStore,
                useValue: {
                    vm$: of({
                        showDialog: true,
                        isLoading: false,
                        images: imagesMock,
                        galleryActiveIndex: 0,
                        orientation: DotAIImageOrientation.VERTICAL
                    }),
                    generateImage: jasmine.createSpy('generateImage'),
                    hideDialog: jasmine.createSpy('hideDialog'),
                    patchState: jasmine.createSpy('patchState'),
                    cleanError: jasmine.createSpy('cleanError')
                }
            }
        ]
    });

    beforeEach(() => {
        spectator = createComponent();
        store = spectator.inject(DotAiImagePromptStore);
        confirmationService = spectator.inject(ConfirmationService);
    });

    it('should hide dialog', () => {
        const dialog = spectator.query(Dialog);
        dialog.onHide.emit('true');
        expect(store.hideDialog).toHaveBeenCalled();
    });

    it('should generate image', () => {
        const promptForm = spectator.query(AiImagePromptFormComponent);
        const formMock: AIImagePrompt = {
            text: 'Test',
            type: PromptType.INPUT,
            size: DotAIImageOrientation.VERTICAL
        };
        promptForm.valueChange.emit(formMock);
        promptForm.generate.emit();

        expect(store.generateImage).toHaveBeenCalledWith();
    });

    it('should inset image', () => {
        const submitBtn = spectator.query(byTestId('submit-btn'));

        spectator.click(submitBtn);
        expect(store.patchState).toHaveBeenCalledWith({
            selectedImage: imagesMock[0]
        });
    });

    it('should clear error on hide confirm', () => {
        const dialog = spectator.query(Dialog);
        dialog.onHide.emit('true');
        expect(store.cleanError).toHaveBeenCalled();
    });

    it('should call confirm dialog when try to close dialog', () => {
        const closeBtn = spectator.query(byTestId('close-btn'));
        jest.spyOn(confirmationService, 'confirm');

        spectator.click(closeBtn);
        expect(confirmationService.confirm).toHaveBeenCalled();
    });
});
