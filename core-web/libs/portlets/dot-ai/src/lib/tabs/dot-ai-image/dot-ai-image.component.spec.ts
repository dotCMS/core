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
        imageOrientation: jest.fn().mockReturnValue('1792x1024'),
        imageError: jest.fn().mockReturnValue(null),
        isConfigured: jest.fn().mockReturnValue(true),
        generateImage: jest.fn(),
        saveImage: jest.fn(),
        setOrientation: jest.fn(),
        dismissImageError: jest.fn()
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
        storeMock.imageError.mockReturnValue(null);
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

    it('should render a generate failure inline rather than swallowing it', () => {
        // The content service rejects with a string, which DotHttpErrorManagerService cannot
        // dispatch on — so before this the spinner just stopped and nothing said why.
        storeMock.imageError.mockReturnValue('content policy violation');
        spectator = createComponent();

        expect(spectator.query(byTestId('dotai-image-error'))).toBeTruthy();
    });

    it('should show a placeholder while generating', () => {
        storeMock.imageGenerating.mockReturnValue(true);
        spectator = createComponent();

        expect(spectator.query(byTestId('dotai-image-loading'))).toBeTruthy();
    });

    it('should not render the provider rewritten prompt', () => {
        // Removed on request; FR-039 was struck from the spec to match.
        withImage();

        expect(spectator.query(byTestId('dotai-image-revised'))).toBeFalsy();
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

    describe('the composer toolbar', () => {
        it('should place the size selector immediately before Generate', () => {
            // Both are projected into promptEnd, so DOM order is what puts the size to the
            // left of the action it applies to.
            const size = spectator.query(byTestId('dotai-image-orientation')) as Element;
            const generate = spectator.query(byTestId('dotai-image-generate')) as Element;

            expect(
                size.compareDocumentPosition(generate) & Node.DOCUMENT_POSITION_FOLLOWING
            ).toBeTruthy();
            expect(size.parentElement).toBe(generate.parentElement);
        });

        it('should not carry the replaces hint any more', () => {
            expect(spectator.query(byTestId('dotai-image-replaces-hint'))).toBeFalsy();
        });
    });

    describe('the image action bar', () => {
        beforeEach(() => withImage());

        it('should float over the top-right of the picture, not sit below it', () => {
            const bar = spectator.query(byTestId('dotai-image-save'))?.parentElement as HTMLElement;

            expect(bar.className).toContain('absolute');
            expect(bar.className).toContain('right-2');
            expect(bar.className).toContain('top-2');
            // The bar has to clear p-image's hover preview mask, which is absolute with no
            // z-index of its own; without this the mask swallows these clicks.
            expect(bar.className).toContain('z-20');
        });

        it('should keep the bar inside the frame that hugs the picture', () => {
            const frame = spectator.query(byTestId('dotai-image-result')) as HTMLElement;
            const bar = spectator.query(byTestId('dotai-image-save'))?.parentElement as HTMLElement;

            expect(frame.contains(bar)).toBe(true);
            // `absolute` needs a positioned ancestor or it escapes to the viewport; that
            // ancestor is the wrapper sized to the picture, so the bar lands on the picture's
            // corner rather than the panel's.
            expect(bar.parentElement?.className).toContain('relative');
        });

        it('should not underline the download link, which is shaped like a button', () => {
            // The global `a` rule underlines every link (style.css), which reads wrong here.
            const link = spectator.query(byTestId('dotai-image-download')) as HTMLElement;

            expect(link.className).toContain('no-underline');
            expect(link.className).toContain('p-button');
        });
    });

    describe('the composer', () => {
        it('should draw no box of its own, since dot-ai-prompt-input has one', () => {
            const composer = spectator.query('dot-ai-prompt-input')?.parentElement as HTMLElement;

            expect(composer.className).not.toContain('border-t');
            expect(composer.className).not.toContain('border-surface-200');
        });
    });
});
