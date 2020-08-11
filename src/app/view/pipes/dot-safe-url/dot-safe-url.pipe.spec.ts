import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { DotSafeUrlPipe } from '@pipes/dot-safe-url/dot-safe-url.pipe';
import { ActivatedRoute } from '@angular/router';
import { DomSanitizer } from '@angular/platform-browser';
import { DotRouterService } from '@services/dot-router/dot-router.service';
import { TestBed } from '@angular/core/testing';
import { MockDotRouterService } from '@tests/dot-router-service.mock';

const fakeActivatedRoute = {
    snapshot: {
        queryParams: {
            filter: 'test',
            sort: 'asc'
        }
    }
};

const fakeDomSanitizer = {
    bypassSecurityTrustResourceUrl: param => param
};

const URL_WITH_PARAMS =
    '?in_frame=true&frame=detailFrame&container=true&angularCurrentPortlet=123-567&filter=test&sort=asc';
const URL_EMPTY = '';

describe('DotSafeUrlPipe', () => {
    let activatedRoute: ActivatedRoute;
    let safePipe: DotSafeUrlPipe;
    let domSanitizer: DomSanitizer;
    let dotRouterService: DotRouterService;

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [
                { provide: ActivatedRoute, useValue: fakeActivatedRoute },
                {
                    provide: DomSanitizer,
                    useValue: fakeDomSanitizer
                },
                { provide: DotRouterService, useClass: MockDotRouterService },
            ],
            imports: [BrowserAnimationsModule]
        }).compileComponents();
        dotRouterService = TestBed.get(DotRouterService);
        activatedRoute = TestBed.get(ActivatedRoute);
        domSanitizer = TestBed.get(DomSanitizer);
        safePipe = new DotSafeUrlPipe(domSanitizer, dotRouterService, activatedRoute);
    });

    it('should return ulr correctly including params', () => {
        expect(safePipe.transform('test')).toEqual(`test${URL_WITH_PARAMS}`);
    });

    it('should return empty url', () => {
        expect(safePipe.transform('')).toEqual(URL_EMPTY);
    });
});
