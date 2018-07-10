import { ConnectionBackend, Response, ResponseOptions } from '@angular/http';
import { MockBackend } from '@angular/http/testing';
import { RouterTestingModule } from '@angular/router/testing';

import { LoginService } from 'dotcms-js/dotcms-js';

import { DOTTestBed } from '../../../test/dot-test-bed';
import { DotRenderHTMLService, DotRenderPageOptions } from './dot-render-html.service';
import { DotRenderedPage } from '../../../portlets/dot-edit-page/shared/models/dot-rendered-page.model';
import { LoginServiceMock } from '../../../test/login-service.mock';
import { mockDotRenderedPage } from '../../../test/dot-rendered-page.mock';
import { PageMode } from '../../../portlets/dot-edit-page/shared/models/page-mode.enum';

describe('DotRenderHTMLService', () => {
    let editPageService: DotRenderHTMLService;
    let backend: MockBackend;
    let lastConnection;
    let injector;

    beforeEach(() => {
        lastConnection = [];

        injector = DOTTestBed.configureTestingModule({
            providers: [DotRenderHTMLService, {
                provide: LoginService,
                useClass: LoginServiceMock
            }],
            imports: [RouterTestingModule]
        });

        editPageService = injector.get(DotRenderHTMLService);

        backend = injector.get(ConnectionBackend) as MockBackend;
        backend.connections.subscribe((connection: any) => {
            lastConnection.push(connection);
        });
    });

    it('should get a rendered page in edit mode', () => {
        let result: DotRenderedPage;
        editPageService.getEdit('about-us').subscribe((renderedPage: DotRenderedPage) => (result = renderedPage));

        lastConnection[0].mockRespond(
            new Response(
                new ResponseOptions({
                    body: {
                        entity: mockDotRenderedPage
                    }
                })
            )
        );
        expect(lastConnection[0].request.url).toContain('/api/v1/page/render/about-us?mode=EDIT_MODE');
        expect(result).toEqual(mockDotRenderedPage);
    });


    it('should get a rendered page in preview mode', () => {
        let result: DotRenderedPage;
        editPageService.getPreview('about-us').subscribe((renderedPage: DotRenderedPage) => (result = renderedPage));

        lastConnection[0].mockRespond(
            new Response(
                new ResponseOptions({
                    body: {
                        entity: mockDotRenderedPage
                    }
                })
            )
        );
        expect(lastConnection[0].request.url).toContain('/api/v1/page/render/about-us?mode=PREVIEW_MODE');
        expect(result).toEqual(mockDotRenderedPage);
    });

    it('should get a rendered page in live mode', () => {
        let result: DotRenderedPage;
        editPageService.getLive('about-us').subscribe((renderedPage: DotRenderedPage) => result = renderedPage);

        lastConnection[0].mockRespond(new Response(new ResponseOptions({
            body: {
                entity: mockDotRenderedPage
            }
        })));
        expect(lastConnection[0].request.url).toContain('/api/v1/page/render/about-us?mode=ADMIN_MODE');
        expect(result).toEqual(mockDotRenderedPage);
    });

    it('should get a rendered page in specific mode', () => {
        let result: DotRenderedPage;
        const param: DotRenderPageOptions = {
            url: 'about-us',
            mode: PageMode.EDIT
        };
        editPageService.get(param).subscribe((renderedPage: DotRenderedPage) => (result = renderedPage));

        lastConnection[0].mockRespond(
            new Response(
                new ResponseOptions({
                    body: {
                        entity: mockDotRenderedPage
                    }
                })
            )
        );
        expect(lastConnection[0].request.url).toContain('/api/v1/page/render/about-us?mode=EDIT_MODE');
        expect(result).toEqual(mockDotRenderedPage);
    });

    it('should get a rendered page in default mode', () => {
        let result: DotRenderedPage;
        const param: DotRenderPageOptions = {
            url: 'about-us',
            viewAs: {
                language_id: 2
            }
        };
        editPageService.get(param).subscribe((renderedPage: DotRenderedPage) => (result = renderedPage));

        lastConnection[0].mockRespond(
            new Response(
                new ResponseOptions({
                    body: {
                        entity: mockDotRenderedPage
                    }
                })
            )
        );
        expect(lastConnection[0].request.url).toContain('/api/v1/page/render/about-us?language_id=2');
        expect(result).toEqual(mockDotRenderedPage);
    });

    it('should get a rendered page in specific mode and language', () => {
        let result: DotRenderedPage;
        const param: DotRenderPageOptions = {
            url: 'about-us',
            viewAs: {
                language_id: 2
            },
            mode: PageMode.EDIT
        };
        editPageService.get(param).subscribe((renderedPage: DotRenderedPage) => (result = renderedPage));

        lastConnection[0].mockRespond(
            new Response(
                new ResponseOptions({
                    body: {
                        entity: mockDotRenderedPage
                    }
                })
            )
        );
        expect(lastConnection[0].request.url).toContain('/api/v1/page/render/about-us?mode=EDIT_MODE&language_id=2');
        expect(result).toEqual(mockDotRenderedPage);
    });
});
