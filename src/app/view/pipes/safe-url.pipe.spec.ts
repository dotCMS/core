import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { SafePipe } from '@pipes/safe-url.pipe';
import { DOTTestBed } from '@tests/dot-test-bed';
import { ActivatedRoute } from '@angular/router';
import { DomSanitizer } from '@angular/platform-browser';
import { DotRouterService } from '@services/dot-router/dot-router.service';
import { TestBed } from '@angular/core/testing';

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

describe('SafePipe', () => {
    let activatedRoute: ActivatedRoute;
    let safePipe: SafePipe;
    let domSanitizer: DomSanitizer;
    let dotRouterService: DotRouterService;

    beforeEach(() => {
        DOTTestBed.configureTestingModule({
            declarations: [SafePipe],
            providers: [
                { provide: ActivatedRoute, useValue: fakeActivatedRoute },
                {
                    provide: DomSanitizer,
                    useValue: fakeDomSanitizer
                }
            ],
            imports: [BrowserAnimationsModule]
        });
        dotRouterService = TestBed.get(DotRouterService);
        activatedRoute = TestBed.get(ActivatedRoute);
        domSanitizer = TestBed.get(DomSanitizer);
        safePipe = new SafePipe(domSanitizer, dotRouterService, activatedRoute);
    });

    it('should return ulr correctly including params', () => {
        expect(safePipe.transform('test')).toEqual(`test${URL_WITH_PARAMS}`);
    });

    it('should return empty url', () => {
        expect(safePipe.transform('')).toEqual(URL_EMPTY);
    });
});
