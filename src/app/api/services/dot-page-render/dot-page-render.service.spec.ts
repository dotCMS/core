import { ConnectionBackend, ResponseOptions, Response } from '@angular/http';
import { MockBackend } from '@angular/http/testing';
import { RouterTestingModule } from '@angular/router/testing';

import { LoginService } from 'dotcms-js';

import { DotPageMode } from '@portlets/dot-edit-page/shared/models/dot-page-mode.enum';
import { DotPageRenderService } from './dot-page-render.service';
import { DOTTestBed } from '@tests/dot-test-bed';
import { LoginServiceMock } from '@tests/login-service.mock';
import { mockDotPersona } from '@tests/dot-persona.mock';
import { mockDotDevices } from '@tests/dot-device.mock';
import { DotPageRender } from '@portlets/dot-edit-page/shared/models';
import { mockDotRenderedPage } from '@tests/dot-page-render.mock';

const url = 'about-us';

describe('DotPageRenderService', () => {
    let service: DotPageRenderService;
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

        service = injector.get(DotPageRenderService);

        backend = injector.get(ConnectionBackend) as MockBackend;
        backend.connections.subscribe((connection: any) => {
            lastConnection.push(connection);
        });
    });

    it('should return entity', () => {
        let result: DotPageRender;

        service.get({ url }).subscribe((res: DotPageRender) => {
            result = res;
        });
        lastConnection[0].mockRespond(
            new Response(
                new ResponseOptions({
                    body: {
                        entity: mockDotRenderedPage
                    }
                })
            )
        );

        expect(result).toEqual(result);
    });

    it('should get a page with just the url', () => {
        service.get({ url }).subscribe();
        expect(lastConnection[0].request.url).toBe(`v1/page/render/${url}`);
    });

    it('should get a page with just the mode', () => {
        service.get({ url, mode: DotPageMode.LIVE }).subscribe();
        expect(lastConnection[0].request.url).toBe(`v1/page/render/${url}?mode=ADMIN_MODE`);
    });

    describe('view as', () => {
        it('should get a page with just the language', () => {
            service
                .get({
                    url,
                    viewAs: {
                        language: 3
                    }
                })
                .subscribe();

            expect(lastConnection[0].request.url).toBe(`v1/page/render/${url}?language_id=3`);
        });

        it('should get a page with just the device', () => {
            service
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

            expect(lastConnection[0].request.url).toBe(`v1/page/render/${url}?device_inode=1234`);
        });

        it('should get a page with just the device', () => {
            service
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

            expect(lastConnection[0].request.url).toBe(
                `v1/page/render/${url}?com.dotmarketing.persona.id=6789`
            );
        });

        it('should get a page with all params and preserve render options over extraParams', () => {
            service
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
                }, {language_id: '1'})
                .subscribe();

            expect(lastConnection[0].request.url).toBe(
                `v1/page/render/${url}?language_id=3&com.dotmarketing.persona.id=6789&device_inode=1234`
            );
        });
    });
});
