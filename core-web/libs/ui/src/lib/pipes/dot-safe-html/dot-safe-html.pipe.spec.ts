import { DomSanitizer } from '@angular/platform-browser';

import { DotSafeHtmlPipe } from './dot-safe-html.pipe';

describe('SafeHtmlPipe', () => {
    let pipe: DotSafeHtmlPipe;
    let sanitizer: DomSanitizer;

    beforeEach(() => {
        sanitizer = {
            bypassSecurityTrustHtml: jest.fn()
        } as unknown as DomSanitizer;
        pipe = new DotSafeHtmlPipe(sanitizer);
    });

    it('should create an instance', () => {
        expect(pipe).toBeTruthy();
    });

    it('should call DomSanitizer.bypassSecurityTrustHtml with value', () => {
        const value = '<p>Hello World</p>';
        pipe.transform(value);
        expect(sanitizer.bypassSecurityTrustHtml).toHaveBeenCalledWith(value);
    });
});
