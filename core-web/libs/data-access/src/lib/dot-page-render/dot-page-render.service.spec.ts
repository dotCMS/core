import { HttpTestingController, HttpClientTestingModule } from '@angular/common/http/testing';
import { TestBed, getTestBed } from '@angular/core/testing';

import { LoginService, CoreWebService, CoreWebServiceMock } from '@dotcms/dotcms-js';
import { DotPageRenderParameters, DotPageMode } from '@dotcms/dotcms-models';
import {
    LoginServiceMock,
    mockDotRenderedPage,
    mockDotDevices,
    mockDotPersona
} from '@dotcms/utils-testing';

import { DotPageRenderService } from './dot-page-render.service';
describe('DotPageRenderService', () => {
    let injector: TestBed;
    let dotPageRenderService: DotPageRenderService;
    let httpMock: HttpTestingController;
    const url = 'about-us';

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [HttpClientTestingModule],
            providers: [
                { provide: CoreWebService, useClass: CoreWebServiceMock },
                {
                    provide: LoginService,
                    useClass: LoginServiceMock
                },
                DotPageRenderService
            ]
        });
        injector = getTestBed();
        dotPageRenderService = injector.get(DotPageRenderService);
        httpMock = injector.get(HttpTestingController);
    });

    it('should check page permissions based on url', () => {
        dotPageRenderService.checkPermission({ url, language_id: '1' }).subscribe();
        const req = httpMock.expectOne(`v1/page/_check-permission`);
        expect(req.request.method).toBe('POST');
        expect(req.request.body).toEqual({ url, language_id: '1' });
        req.flush({
            entity: 'ok'
        });
    });

    it('should return entity', () => {
        dotPageRenderService.get({ url }).subscribe((res: DotPageRenderParameters) => {
            expect(res).toEqual(mockDotRenderedPage());
        });

        const req = httpMock.expectOne('v1/page/render/about-us?mode=PREVIEW_MODE');
        expect(req.request.method).toBe('GET');
        req.flush({ entity: mockDotRenderedPage() });
    });

    it('should get a page with just the url', () => {
        dotPageRenderService.get({ url }).subscribe();
        httpMock.expectOne('v1/page/render/about-us?mode=PREVIEW_MODE');
    });

    it('should get a page with just the mode', () => {
        dotPageRenderService.get({ url, mode: DotPageMode.LIVE }).subscribe();
        httpMock.expectOne(`v1/page/render/${url}?mode=ADMIN_MODE`);
    });

    describe('view as', () => {
        it('should get a page with just the language', () => {
            dotPageRenderService
                .get({
                    url,
                    viewAs: {
                        language: 3
                    }
                })
                .subscribe();
            httpMock.expectOne('v1/page/render/about-us?language_id=3&mode=PREVIEW_MODE');
        });

        it('should get a page with just the device', () => {
            dotPageRenderService
                .get({
                    url,
                    viewAs: {
                        device: {
                            ...mockDotDevices[0],
                            inode: '1234'
                        }
                    }
                })
                .subscribe();
            httpMock.expectOne('v1/page/render/about-us?device_inode=1234&mode=PREVIEW_MODE');
        });

        it('should get a page with just the device', () => {
            dotPageRenderService
                .get({
                    url,
                    viewAs: {
                        persona: {
                            ...mockDotPersona,
                            identifier: '6789'
                        }
                    }
                })
                .subscribe();
            httpMock.expectOne(
                'v1/page/render/about-us?com.dotmarketing.persona.id=6789&mode=PREVIEW_MODE'
            );
        });

        it('should get a page with all params and preserve render options over extraParams', () => {
            dotPageRenderService
                .get({
                    url,
                    viewAs: {
                        language: 3,
                        device: {
                            ...mockDotDevices[0],
                            inode: '1234'
                        },
                        persona: {
                            ...mockDotPersona,
                            identifier: '6789'
                        }
                    }
                })
                .subscribe();

            httpMock.expectOne(
                'v1/page/render/about-us?com.dotmarketing.persona.id=6789&device_inode=1234&language_id=3&mode=PREVIEW_MODE'
            );
        });
    });
});
