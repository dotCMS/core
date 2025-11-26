import { createPipeFactory, mockProvider, SpectatorPipe, SpyObject } from '@ngneat/spectator/jest';

import { DomSanitizer } from '@angular/platform-browser';

import { DotSafeHtmlPipe } from './dot-safe-html.pipe';

describe('DotSafeHtmlPipe', () => {
    let spectator: SpectatorPipe<DotSafeHtmlPipe>;
    let sanitizer: SpyObject<DomSanitizer>;
    const safeHtml = 'safeHtml: <p>Hello World</p>';

    const createPipe = createPipeFactory({
        pipe: DotSafeHtmlPipe,
        providers: [
            mockProvider(DomSanitizer, {
                bypassSecurityTrustHtml: jest.fn().mockReturnValue(safeHtml)
            })
        ]
    });

    it('should call DomSanitizer.bypassSecurityTrustHtml with value', () => {
        const value = '<p>Hello World</p>';

        spectator = createPipe(`{{ value | safeHtml }}`, {
            hostProps: { value }
        });
        sanitizer = spectator.inject(DomSanitizer);
        spectator.detectChanges();
        expect(sanitizer.bypassSecurityTrustHtml).toHaveBeenCalledWith(value);
        expect(spectator.element.textContent).toBe(safeHtml);
    });
});
