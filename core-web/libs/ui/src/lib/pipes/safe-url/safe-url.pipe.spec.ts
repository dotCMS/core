import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';

import { SafeUrlPipe } from './safe-url.pipe';

describe('SafeUrlPipe', () => {
    let pipe: SafeUrlPipe;
    let sanitizer: DomSanitizer;

    beforeEach(() => {
        sanitizer = {
            bypassSecurityTrustResourceUrl: jest.fn()
        } as unknown as DomSanitizer;
        pipe = new SafeUrlPipe(sanitizer);
    });

    it('should transform URL to a safe resource URL', () => {
        const url = 'http://example.com';
        const safeUrl: SafeResourceUrl = 'safeUrl: http://example.com';

        (sanitizer.bypassSecurityTrustResourceUrl as jest.Mock).mockReturnValue(safeUrl);

        const transformedUrl = pipe.transform(url);

        expect(transformedUrl).toBe(safeUrl);
        expect(sanitizer.bypassSecurityTrustResourceUrl).toHaveBeenCalledWith(url);
    });
    it('should transform URL to a safe resource URL when the URL is an instance of String', () => {
        const url = new String('http://example.com');
        const safeUrl: SafeResourceUrl = 'safeUrl: http://example.com';

        (sanitizer.bypassSecurityTrustResourceUrl as jest.Mock).mockReturnValue(safeUrl);

        const transformedUrl = pipe.transform(url);

        expect(transformedUrl).toBe(safeUrl);
        expect(sanitizer.bypassSecurityTrustResourceUrl).toHaveBeenCalledWith(url.toString());
    });
});
