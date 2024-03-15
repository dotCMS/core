import { byTestId, createComponentFactory, Spectator } from '@ngneat/spectator';

import { HttpClientTestingModule } from '@angular/common/http/testing';

import { Galleria, GalleriaModule } from 'primeng/galleria';

import { AiImagePromptGalleryComponent } from './ai-image-prompt-gallery.component';

import { DotGeneratedAIImage } from '../../../../shared/services/dot-ai/dot-ai.models';

describe('AiImagePromptGalleryComponent', () => {
    let spectator: Spectator<AiImagePromptGalleryComponent>;

    const createComponent = createComponentFactory({
        component: AiImagePromptGalleryComponent,
        imports: [HttpClientTestingModule, GalleriaModule]
    });

    const imagesMock: DotGeneratedAIImage[] = [
        {
            response: {
                contentlet: { assetVersion: 'image_url' }
            }
        } as unknown as DotGeneratedAIImage,
        {
            response: { assetVersion: 'image_url_2' }
        } as unknown as DotGeneratedAIImage
    ];

    beforeEach(() => {
        spectator = createComponent();
    });

    it('should display placeholder when isLoading is false and images is empty', () => {
        spectator.setInput({
            isLoading: false,
            images: []
        });
        spectator.detectChanges();
        const placeholderElement = spectator.query(byTestId('ai-image-gallery__placeholder'));
        expect(placeholderElement).toBeTruthy();
    });

    it('should display skeleton when isLoading is true', () => {
        spectator.setInput({
            isLoading: true,
            images: []
        });
        spectator.detectChanges();

        const skeletonElement = spectator.query(byTestId('ai-image-gallery_skeleton'));
        const skeletonElementCount = spectator.query(byTestId('ai-image-gallery__count-skeleton'));

        expect(skeletonElement).toBeTruthy();
        expect(skeletonElementCount).toBeTruthy();
    });

    it('should display Galleria when isLoading is false and images is not empty', () => {
        spectator.setInput({
            isLoading: false,
            images: imagesMock
        });
        spectator.detectChanges();
        const galleriaElement = spectator.query('p-galleria');
        expect(galleriaElement).toBeTruthy();
    });

    it('should emit activeIndexChange event when galleria active index changes', () => {
        const emitterSpy = jest.spyOn(spectator.component.activeIndexChange, 'emit');

        spectator.setInput({
            isLoading: false,
            images: imagesMock
        });
        spectator.detectChanges();
        const galleria = spectator.query(Galleria);

        galleria.activeIndexChange.emit(1);

        expect(emitterSpy).toHaveBeenCalledWith(1);
    });
});
