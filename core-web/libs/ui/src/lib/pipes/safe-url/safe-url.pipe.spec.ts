import { createPipeFactory, mockProvider, SpectatorPipe, SpyObject } from '@ngneat/spectator/jest';

import { DomSanitizer } from '@angular/platform-browser';

import { SafeUrlPipe } from './safe-url.pipe';

describe('SafeUrlPipe', () => {
    let spectator: SpectatorPipe<SafeUrlPipe>;
    let sanitizer: SpyObject<DomSanitizer>;
    const safeUrl = 'safeUrl: http://example.com';

    const createPipe = createPipeFactory({
        pipe: SafeUrlPipe,
        providers: [
            mockProvider(DomSanitizer, {
                bypassSecurityTrustResourceUrl: jest.fn().mockReturnValue(safeUrl)
            })
        ]
    });

    it('should transform URL to a safe resource URL (string)', () => {
        const url = 'http://example.com';
        const safeUrl = 'safeUrl: http://example.com';
        spectator = createPipe(`{{ url | safeUrl }}`, {
            hostProps: { url }
        });
        sanitizer = spectator.inject(DomSanitizer);
        spectator.detectChanges();
        expect(sanitizer.bypassSecurityTrustResourceUrl).toHaveBeenCalledWith(url);
        expect(spectator.element.textContent).toBe(safeUrl);
    });

    it('should transform URL to a safe resource URL (String object)', () => {
        const url = new String('http://example.com');
        const safeUrl = 'safeUrl: http://example.com';
        spectator = createPipe(`{{ url | safeUrl }}`, {
            hostProps: { url }
        });
        sanitizer = spectator.inject(DomSanitizer);
        spectator.detectChanges();
        expect(sanitizer.bypassSecurityTrustResourceUrl).toHaveBeenCalledWith(url.toString());
        expect(spectator.element.textContent).toBe(safeUrl);
    });
});
