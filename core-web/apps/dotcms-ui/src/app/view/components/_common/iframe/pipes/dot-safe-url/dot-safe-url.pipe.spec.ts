import { createPipeFactory, mockProvider, SpectatorPipe, SpyObject } from '@ngneat/spectator';

import { DomSanitizer } from '@angular/platform-browser';
import { ActivatedRoute } from '@angular/router';

import { DotRouterService } from '@dotcms/data-access';
import { MockDotRouterService } from '@dotcms/utils-testing';

import { DotSafeUrlPipe } from './dot-safe-url.pipe';

describe('DotSafeUrlPipe', () => {
    let spectator: SpectatorPipe<DotSafeUrlPipe>;
    let sanitizer: SpyObject<DomSanitizer>;
    const fakeActivatedRoute = {
        snapshot: {
            queryParams: {
                filter: 'test',
                sort: 'asc'
            }
        }
    };

    const URL_WITH_PARAMS =
        '?in_frame=true&frame=detailFrame&container=true&angularCurrentPortlet=123-567&filter=test&sort=asc';
    const URL_EMPTY = '';

    const createPipe = createPipeFactory({
        pipe: DotSafeUrlPipe,
        providers: [
            mockProvider(DomSanitizer, {
                bypassSecurityTrustResourceUrl: jasmine.createSpy('bypassSecurityTrustResourceUrl')
            }),
            { provide: DotRouterService, useClass: MockDotRouterService },
            { provide: ActivatedRoute, useValue: fakeActivatedRoute }
        ],
        detectChanges: false
    });

    it('should return url correctly including params', () => {
        spectator = createPipe(`{{ value | dotSafeUrl }}`, {
            hostProps: { value: 'test' }
        });
        sanitizer = spectator.inject(DomSanitizer);
        const sanitizedUrl = 'sanitized-test-url';
        sanitizer.bypassSecurityTrustResourceUrl.and.returnValue(sanitizedUrl);
        spectator.detectChanges();
        expect(sanitizer.bypassSecurityTrustResourceUrl).toHaveBeenCalledWith(
            `test${URL_WITH_PARAMS}`
        );
        expect(spectator.element.textContent).toBe(sanitizedUrl);
    });

    it('should return empty url', () => {
        spectator = createPipe(`{{ value | dotSafeUrl }}`, {
            hostProps: { value: '' }
        });
        sanitizer = spectator.inject(DomSanitizer);
        sanitizer.bypassSecurityTrustResourceUrl.and.returnValue(URL_EMPTY);
        spectator.detectChanges();
        expect(sanitizer.bypassSecurityTrustResourceUrl).toHaveBeenCalledWith(URL_EMPTY);
        expect(spectator.element.textContent).toBe(URL_EMPTY);
    });
});
