import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';

import { SafeUrlPipe } from './safe-url.pipe';

describe('SafeUrlPipe', () => {
    let pipe: SafeUrlPipe;
    let sanitizer: DomSanitizer;

    beforeEach(() => {
        sanitizer = jasmine.createSpyObj('DomSanitizer', ['bypassSecurityTrustResourceUrl']);
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        pipe = new SafeUrlPipe(sanitizer as any);
    });

    it('should transform URL to a safe resource URL', () => {
        const url = 'http://example.com';
        const safeUrl: SafeResourceUrl = 'safeUrl: http://example.com';

        (sanitizer.bypassSecurityTrustResourceUrl as jasmine.Spy).and.returnValue(safeUrl);

        const transformedUrl = pipe.transform(url);

        expect(transformedUrl).toBe(safeUrl);
        expect(sanitizer.bypassSecurityTrustResourceUrl).toHaveBeenCalledWith(url);
    });
});
