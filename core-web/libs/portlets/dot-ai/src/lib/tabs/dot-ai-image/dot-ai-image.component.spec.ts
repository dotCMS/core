import { byTestId, createComponentFactory, mockProvider, Spectator } from '@openng/spectator/jest';

import { By } from '@angular/platform-browser';

import { Tooltip } from 'primeng/tooltip';

import { DotMessageService } from '@dotcms/data-access';

import DotAiImageComponent from './dot-ai-image.component';

import { DotAiStore } from '../../store/dot-ai.store';

const image = (overrides = {}) => ({
    response: 'abc123',
    tempFileName: 'cat.png',
    originalPrompt: 'a cat',
    revisedPrompt: 'a photorealistic cat',
    published: false,
    size: '1792x1024',
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
        // Echoes the key: the icon buttons carry their name in aria-label and pTooltip,
        // and a mock returning undefined would leave both unset and untestable.
        providers: [mockProvider(DotMessageService, { get: (key: string) => key })],
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

    it('should replace Save with the published badge, not just disable it', () => {
        // There is no second publish to offer once the image is in the assets, so the button
        // gives up its slot rather than sitting there greyed out.
        withImage({ published: true });

        expect(spectator.query(byTestId('dotai-image-save'))).toBeFalsy();
        expect(spectator.query(byTestId('dotai-image-published'))).toBeTruthy();
    });

    it('should offer Save while the image is unpublished', () => {
        withImage({ published: false });

        expect(spectator.query(byTestId('dotai-image-save'))).toBeTruthy();
        expect(spectator.query(byTestId('dotai-image-published'))).toBeFalsy();
    });

    it('should keep Download available either way', () => {
        // Download needs no publish, so it must survive the swap.
        withImage({ published: true });
        expect(spectator.query(byTestId('dotai-image-download'))).toBeTruthy();

        withImage({ published: false });
        expect(spectator.query(byTestId('dotai-image-download'))).toBeTruthy();
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

        // jsdom's CSS implementation has no `aspect-ratio`, so assigning it is a silent
        // no-op and both `el.style.aspectRatio` and `DebugElement.styles` come back empty —
        // there is nothing in this environment to assert the binding against. The ratio the
        // frame actually renders at is covered in the browser instead; what is testable here
        // is the value the template is handed.
        const ratio = (size: string): string =>
            (spectator.component as unknown as { aspectRatio: (s: string) => string }).aspectRatio(
                size
            );

        it('should turn a stored size into the ratio the frame needs', () => {
            expect(ratio('1792x1024')).toBe('1792 / 1024');
            expect(ratio('1024x1792')).toBe('1024 / 1792');
            expect(ratio('1024x1024')).toBe('1024 / 1024');
        });

        it('should fall back to auto rather than collapse the frame', () => {
            // An unrecognised size degrades to the capped behaviour instead of a zero-height
            // frame, which is what `aspect-ratio: 0` or an empty value would give.
            for (const bad of ['whatever', '', 'x', '1792x', 'ax b']) {
                expect(ratio(bad)).toBe('auto');
            }
        });

        it('should keep the frame anchored to the picture, not the panel', () => {
            // The bar's `right-2 top-2` is relative to this element, so it only lands on the
            // corner you can see while the frame's box is the picture's box.
            const frame = spectator.query(byTestId('dotai-image-frame')) as HTMLElement;
            const bar = spectator.query(byTestId('dotai-image-save'))?.parentElement as HTMLElement;

            expect(frame.className).toContain('relative');
            expect(frame.className).toContain('max-h-full');
            expect(frame.className).toContain('max-w-full');
            // No explicit width or height: either one skews the box on one axis.
            expect(frame.className.split(/\s+/)).not.toContain('h-full');
            expect(frame.className.split(/\s+/)).not.toContain('w-full');
            expect(frame.contains(bar)).toBe(true);
        });

        it('should be a labelled group, so the icons are not the only cue', () => {
            const bar = spectator.query(byTestId('dotai-image-save'))?.parentElement as HTMLElement;

            expect(bar.getAttribute('role')).toBe('group');
            expect(bar.getAttribute('aria-label')).toBe('dotai.image.actions.aria');
            // The pill shape from the image editor's address bar.
            expect(bar.className).toContain('rounded-full');
        });

        /**
         * Tooltip text per control, read off the directive instances.
         *
         * `pTooltip` is a directive *input*, so it never lands as a DOM attribute, and
         * `ng-reflect-content` exists only in dev mode — asserting on either passes
         * vacuously whether or not a tooltip is wired.
         */
        const tooltips = (): Record<string, string> =>
            Object.fromEntries(
                spectator.debugElement
                    .queryAll(By.directive(Tooltip))
                    .map((node) => [
                        node.nativeElement.getAttribute('data-testid'),
                        node.injector.get(Tooltip).content
                    ])
            );

        it('should render both controls as icon buttons with a name and a tooltip', () => {
            // Icon-only, so the accessible name has to come from aria-label and the visible
            // affordance from the tooltip — an unlabelled glyph is neither (FR-056).
            for (const id of ['dotai-image-save', 'dotai-image-download']) {
                const el = spectator.query(byTestId(id)) as HTMLElement;
                const glyph = el.querySelector('.material-symbols-outlined');

                expect(glyph).toBeTruthy();
                // No visible label: the glyph is the only text in the control.
                expect(el.textContent?.trim()).toBe(glyph?.textContent?.trim());
                expect(el.getAttribute('aria-label')).toBeTruthy();
            }

            expect(tooltips()['dotai-image-save']).toBe('dotai.image.save');
            expect(tooltips()['dotai-image-download']).toBe('dotai.image.download');
        });

        it('should name the published badge for assistive tech, not just colour it', () => {
            // It stands in for a button, so it has to carry the same kind of name — a bare
            // green glyph says nothing to a screen reader.
            withImage({ published: true });
            const badge = spectator.query(byTestId('dotai-image-published')) as HTMLElement;

            expect(badge.getAttribute('role')).toBe('img');
            expect(badge.getAttribute('aria-label')).toBe('dotai.image.published');
            expect(badge.querySelector('.material-symbols-outlined')?.textContent?.trim()).toBe(
                'check_circle'
            );
            expect(tooltips()['dotai-image-published']).toBe('dotai.image.published');
        });

        it('should not underline the download link, which is shaped like a button', () => {
            const link = spectator.query(byTestId('dotai-image-download')) as HTMLElement;

            expect(link.className).toContain('p-button');
            // The bang is load-bearing, which is why it is asserted rather than just
            // `no-underline`: style.css's `a { @apply underline }` is unlayered, and an
            // unlayered declaration beats anything in @layer utilities whatever its
            // specificity. Plain `no-underline` shipped once and left the link underlined.
            expect(link.className).toContain('no-underline!');
        });
    });

    describe('the picture frame', () => {
        beforeEach(() => withImage());

        it('should let the picture fill the frame rather than size itself', () => {
            // `h-full w-auto` here was the original bug: the image took the panel's full
            // height and let its ratio pick the width, coming out 1817px wide inside a
            // 1344px frame. Sizing now belongs entirely to the frame, which carries the
            // ratio, so the image simply fills it.
            const image = spectator.query('p-image') as HTMLElement;
            // Tokenised, not a substring match: 'h-full' is a substring of 'max-h-full', so
            // `not.toContain` on the raw string passes for the very class it should reject.
            const classes = (image.getAttribute('imageclass') ?? '').split(/\s+/);

            expect(classes).toContain('h-full');
            expect(classes).toContain('w-full');
            expect(classes).not.toContain('w-auto');
        });

        it('should let the frame shrink-wrap the picture, so the bar lands on its corner', () => {
            const frame = spectator.query(byTestId('dotai-image-save'))?.parentElement
                ?.parentElement as HTMLElement;

            // No flex-1 and no h-full: either would stretch the frame past the picture.
            expect(frame.className).toContain('max-h-full');
            expect(frame.className).toContain('max-w-full');
            expect(frame.className).not.toContain('flex-1');
        });
    });

    describe('the composer', () => {
        it('should draw no box of its own, since dot-ai-prompt-input has one', () => {
            const composer = spectator.query('dot-ai-prompt-input')?.parentElement as HTMLElement;

            expect(composer.className).not.toContain('border-t');
            expect(composer.className).not.toContain('border-surface-200');
        });
    });

    describe('the generate button while generating', () => {
        it('should show the spinner and keep the label', () => {
            storeMock.imageGenerating.mockReturnValue(true);
            spectator = createComponent();
            const button = spectator.query(byTestId('dotai-image-generate')) as HTMLElement;

            // PrimeNG puts its spinner in the icon slot and leaves the label alone, so the
            // button does not change width while the request is out.
            expect(button.querySelector('.p-button-loading-icon')).toBeTruthy();
            expect(button.querySelector('button')?.disabled).toBe(true);
        });

        it('should show no spinner at rest', () => {
            expect(
                spectator
                    .query(byTestId('dotai-image-generate'))
                    ?.querySelector('.p-button-loading-icon')
            ).toBeFalsy();
        });
    });
});
