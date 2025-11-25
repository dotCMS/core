import { byTestId, createComponentFactory, mockProvider, Spectator } from '@ngneat/spectator';
import { of } from 'rxjs';

import { HttpClientTestingModule } from '@angular/common/http/testing';
import { fakeAsync } from '@angular/core/testing';

import { ConfirmationService } from 'primeng/api';
import { Dialog } from 'primeng/dialog';

import {
    PromptType,
    AIImagePrompt,
    DotAIImageOrientation,
    DotGeneratedAIImage
} from '@dotcms/dotcms-models';

import { DotAIImagePromptComponent } from './ai-image-prompt.component';
import { DotAiImagePromptStore } from './ai-image-prompt.store';
import { AiImagePromptFormComponent } from './components/ai-image-prompt-form/ai-image-prompt-form.component';

describe('DotAIImagePromptComponent', () => {
    let spectator: Spectator<DotAIImagePromptComponent>;
    let store: DotAiImagePromptStore;

    const imagesMock: DotGeneratedAIImage[] = [
        { name: 'image1', url: 'image_url' },
        { name: 'image2', url: 'image_url_2' }
    ] as unknown as DotGeneratedAIImage[];

    const createComponent = createComponentFactory({
        component: DotAIImagePromptComponent,
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
                    cleanError: jasmine.createSpy('cleanError'),
                    setSelectedImage: jasmine.createSpy('setSelectedImage')
                }
            },
            mockProvider(ConfirmationService)
        ],
        imports: [HttpClientTestingModule]
    });

    beforeEach(() => {
        spectator = createComponent();
        store = spectator.inject(DotAiImagePromptStore);
    });

    it('should hide dialog', () => {
        const dialog = spectator.query(Dialog);
        dialog.onHide.emit('true');
        expect(store.hideDialog).toHaveBeenCalled();
    });

    it('should the modal have 1040px in maxWidth', () => {
        spectator.detectChanges();
        const dialog = spectator.query(Dialog);
        const width = dialog.style.width;
        const maxWidth = dialog.style.maxWidth;
        expect(width).toBe('90%');
        expect(maxWidth).toBe('1040px');
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

    it('should inset image', async () => {
        const submitBtn = spectator.query(byTestId('submit-btn'));

        spectator.click(submitBtn);
        await spectator.fixture.whenStable();
        expect(store.setSelectedImage).toHaveBeenCalled();
    });

    it('should clear error on hide confirm', () => {
        const dialog = spectator.query(Dialog);
        dialog.onHide.emit('true');
        expect(store.hideDialog).toHaveBeenCalled();
    });

    it('should call confirm dialog when try to close dialog', fakeAsync(() => {
        const closeBtn = spectator.query(byTestId('close-btn'));
        const spyCloseDialog = spyOn(spectator.component, 'closeDialog');

        spectator.click(closeBtn);
        spectator.tick();
        expect(spyCloseDialog).toHaveBeenCalled();
    }));
});
