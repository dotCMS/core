import { byTestId, createComponentFactory, mockProvider, Spectator } from '@openng/spectator/jest';

import { DotMessageService } from '@dotcms/data-access';

import DotAiImageComponent from './dot-ai-image.component';

import { DotAiStore } from '../../store/dot-ai.store';

const image = (overrides = {}) => ({
    response: 'abc123',
    tempFileName: 'cat.png',
    originalPrompt: 'a cat',
    revisedPrompt: 'a photorealistic cat',
    published: false,
    ...overrides
});

describe('DotAiImageComponent', () => {
    let spectator: Spectator<DotAiImageComponent>;

    const storeMock = {
        image: jest.fn().mockReturnValue(null),
        imageUrl: jest.fn().mockReturnValue(null),
        imageGenerating: jest.fn().mockReturnValue(false),
        imageSaving: jest.fn().mockReturnValue(false),
        imageOrientation: jest.fn().mockReturnValue('1024x1024'),
        isConfigured: jest.fn().mockReturnValue(true),
        generateImage: jest.fn(),
        saveImage: jest.fn(),
        setOrientation: jest.fn()
    };

    const createComponent = createComponentFactory({
        component: DotAiImageComponent,
        componentProviders: [{ provide: DotAiStore, useValue: storeMock }],
        providers: [mockProvider(DotMessageService)],
        shallow: true
    });

    beforeEach(() => {
        jest.clearAllMocks();
        storeMock.image.mockReturnValue(null);
        storeMock.imageUrl.mockReturnValue(null);
        storeMock.imageGenerating.mockReturnValue(false);
        storeMock.isConfigured.mockReturnValue(true);
        spectator = createComponent();
    });

    const withImage = (overrides = {}) => {
        storeMock.image.mockReturnValue(image(overrides));
        storeMock.imageUrl.mockReturnValue('/dA/abc123/asset.png');
        spectator = createComponent();
    };

    it('should show the empty state before generating', () => {
        expect(spectator.query(byTestId('dotai-image-empty'))).toBeTruthy();
    });

    it('should show a placeholder while generating', () => {
        storeMock.imageGenerating.mockReturnValue(true);
        spectator = createComponent();

        expect(spectator.query(byTestId('dotai-image-loading'))).toBeTruthy();
    });

    it('should show the provider rewritten prompt (FR-039)', () => {
        withImage();

        expect(spectator.query(byTestId('dotai-image-revised'))).toHaveText('a photorealistic cat');
    });

    it('should offer download as a same-origin anchor, available before any save (FR-038)', () => {
        withImage();
        const link = spectator.query(byTestId('dotai-image-download')) as HTMLAnchorElement;

        expect(link.getAttribute('href')).toBe('/dA/abc123/asset.png');
        expect(link.getAttribute('download')).toBe('cat.png');
    });

    it('should disable Save once published rather than allowing a second publish', () => {
        withImage({ published: true });

        expect(
            spectator.query(byTestId('dotai-image-save'))?.querySelector('button')?.disabled
        ).toBe(true);
        expect(spectator.query(byTestId('dotai-image-published'))).toBeTruthy();
    });

    it('should not generate on an empty prompt', () => {
        spectator.click(
            spectator.query(byTestId('dotai-image-generate'))?.querySelector('button') as Element
        );

        expect(storeMock.generateImage).not.toHaveBeenCalled();
    });
});
