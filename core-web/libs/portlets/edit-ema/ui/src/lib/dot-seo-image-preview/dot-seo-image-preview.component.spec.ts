import { byTestId, createComponentFactory, Spectator } from '@ngneat/spectator';

import { DotMessageService } from '@dotcms/data-access';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotSeoImagePreviewComponent } from './dot-seo-image-preview.component';

describe('DotSeoImagePreviewComponent', () => {
    let spectator: Spectator<DotSeoImagePreviewComponent>;

    const createComponent = createComponentFactory({
        component: DotSeoImagePreviewComponent,
        providers: [
            {
                provide: DotMessageService,
                useValue: new MockDotMessageService({
                    'seo.rules.media.preview.not.defined': 'Social Media Preview Image Not Defined!'
                })
            }
        ]
    });

    beforeEach(() => {
        spectator = createComponent();
    });

    it('should create', () => {
        expect(spectator.component).toBeTruthy();
    });

    it('should display an error message when noImageAvailable is true', () => {
        spectator.component.noImageAvailable = true;
        spectator.detectComponentChanges();

        const errorMessage = spectator.query(byTestId('seo-image-default'));
        expect(errorMessage).toBeTruthy();
    });

    it('should display an image when noImageAvailable is false', () => {
        spectator.component.noImageAvailable = false;
        spectator.setInput({
            image: 'sample-image-url.jpg'
        });
        spectator.detectComponentChanges();

        const imageElement = spectator.query(byTestId('seo-image-preview'));
        expect(imageElement).toBeTruthy();
        expect(imageElement.getAttribute('src')).toBe('sample-image-url.jpg');
    });

    it('should call onImageError() when the image fails to load', () => {
        const imageElement = spectator.query(byTestId('seo-image-preview'));

        spectator.dispatchFakeEvent(imageElement, 'error');
        spectator.detectComponentChanges();

        expect(spectator.component.noImageAvailable).toBe(true);
    });
});
