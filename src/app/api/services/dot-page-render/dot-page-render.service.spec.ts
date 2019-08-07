import { ConnectionBackend, Response, ResponseOptions } from '@angular/http';
import { MockBackend } from '@angular/http/testing';
import { RouterTestingModule } from '@angular/router/testing';

import { LoginService } from 'dotcms-js';

import { DOTTestBed } from '../../../test/dot-test-bed';
import { DotPageRenderService, DotPageRenderOptions } from './dot-page-render.service';
import { DotPageRender } from '../../../portlets/dot-edit-page/shared/models/dot-rendered-page.model';
import { LoginServiceMock } from '../../../test/login-service.mock';
import { mockDotRenderedPage } from '../../../test/dot-rendered-page.mock';
import { DotPageMode } from '../../../portlets/dot-edit-page/shared/models/dot-page-mode.enum';

describe('DotPageRenderService', () => {
    let editPageService: DotPageRenderService;
    let backend: MockBackend;
    let lastConnection;
    let injector;

    beforeEach(() => {
        lastConnection = [];

        injector = DOTTestBed.configureTestingModule({
            providers: [
                DotPageRenderService,
                {
                    provide: LoginService,
                    useClass: LoginServiceMock
                }
            ],
            imports: [RouterTestingModule]
        });

        editPageService = injector.get(DotPageRenderService);

        backend = injector.get(ConnectionBackend) as MockBackend;
        backend.connections.subscribe((connection: any) => {
            lastConnection.push(connection);
        });
    });

    it('should get a rendered page in edit mode with ViewAs params', () => {
        let result: DotPageRender;
        spyOn(editPageService, 'get').and.callThrough();

        const viewAs = {
            language: {
                country: 'United States',
                countryCode: 'US',
                id: 1,
                language: 'English',
                languageCode: 'en'
            },
            mode: 'EDIT_MODE'
        };

        editPageService
            .getEdit('about-us', viewAs)
            .subscribe((renderedPage: DotPageRender) => (result = renderedPage));

        lastConnection[0].mockRespond(
            new Response(
                new ResponseOptions({
                    body: {
                        entity: mockDotRenderedPage
                    }
                })
            )
        );

        const expectedResponse = {
            url: 'about-us',
            mode: 1,
            viewAs: {
                persona_id: null,
                language_id: viewAs.language.id,
                device_inode: null
            }
        };

        expect(lastConnection[0].request.url).toContain(
            'v1/page/render/about-us?mode=EDIT_MODE'
        );
        expect(result).toEqual(mockDotRenderedPage);
        expect(editPageService.get).toHaveBeenCalledWith(expectedResponse);
    });

    it('should get a rendered page in preview mode', () => {
        let result: DotPageRender;
        editPageService
            .getPreview('about-us')
            .subscribe((renderedPage: DotPageRender) => (result = renderedPage));

        lastConnection[0].mockRespond(
            new Response(
                new ResponseOptions({
                    body: {
                        entity: mockDotRenderedPage
                    }
                })
            )
        );
        expect(lastConnection[0].request.url).toContain(
            'v1/page/render/about-us?mode=PREVIEW_MODE'
        );
        expect(result).toEqual(mockDotRenderedPage);
    });

    it('should get a rendered page in live mode', () => {
        let result: DotPageRender;
        editPageService
            .getLive('about-us')
            .subscribe((renderedPage: DotPageRender) => (result = renderedPage));

        lastConnection[0].mockRespond(
            new Response(
                new ResponseOptions({
                    body: {
                        entity: mockDotRenderedPage
                    }
                })
            )
        );
        expect(lastConnection[0].request.url).toContain(
            'v1/page/render/about-us?mode=ADMIN_MODE'
        );
        expect(result).toEqual(mockDotRenderedPage);
    });

    it('should get a rendered page in specific mode', () => {
        let result: DotPageRender;
        const param: DotPageRenderOptions = {
            url: 'about-us',
            mode: DotPageMode.EDIT
        };
        editPageService
            .get(param)
            .subscribe((renderedPage: DotPageRender) => (result = renderedPage));

        lastConnection[0].mockRespond(
            new Response(
                new ResponseOptions({
                    body: {
                        entity: mockDotRenderedPage
                    }
                })
            )
        );
        expect(lastConnection[0].request.url).toContain(
            'v1/page/render/about-us?mode=EDIT_MODE'
        );
        expect(result).toEqual(mockDotRenderedPage);
    });

    it('should not crash when view as attribute is empty', () => {
        let result: DotPageRender;
        const param: DotPageRenderOptions = {
            url: 'about-us',
            viewAs: {
            }
        };
        editPageService
            .get(param)
            .subscribe((renderedPage: DotPageRender) => (result = renderedPage));

        lastConnection[0].mockRespond(
            new Response(
                new ResponseOptions({
                    body: {
                        entity: mockDotRenderedPage
                    }
                })
            )
        );
        expect(lastConnection[0].request.url).toContain(
            'v1/page/render/about-us'
        );
        expect(result).toEqual(mockDotRenderedPage);
    });

    it('should get a rendered page in default mode', () => {
        let result: DotPageRender;
        const param: DotPageRenderOptions = {
            url: 'about-us',
            viewAs: {
                language_id: 2
            }
        };
        editPageService
            .get(param)
            .subscribe((renderedPage: DotPageRender) => (result = renderedPage));

        lastConnection[0].mockRespond(
            new Response(
                new ResponseOptions({
                    body: {
                        entity: mockDotRenderedPage
                    }
                })
            )
        );
        expect(lastConnection[0].request.url).toContain(
            'v1/page/render/about-us?language_id=2'
        );
        expect(result).toEqual(mockDotRenderedPage);
    });

    it('should get a rendered page in specific mode and language', () => {
        let result: DotPageRender;
        const param: DotPageRenderOptions = {
            url: 'about-us',
            viewAs: {
                language_id: 2
            },
            mode: DotPageMode.EDIT
        };
        editPageService
            .get(param)
            .subscribe((renderedPage: DotPageRender) => (result = renderedPage));

        lastConnection[0].mockRespond(
            new Response(
                new ResponseOptions({
                    body: {
                        entity: mockDotRenderedPage
                    }
                })
            )
        );
        expect(lastConnection[0].request.url).toContain(
            'v1/page/render/about-us?mode=EDIT_MODE&language_id=2'
        );
        expect(result).toEqual(mockDotRenderedPage);
    });
});
