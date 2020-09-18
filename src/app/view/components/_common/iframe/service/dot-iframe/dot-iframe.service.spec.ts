import { DotIframeService } from './dot-iframe.service';
import { DotUiColorsService } from '@services/dot-ui-colors/dot-ui-colors.service';
import { async, TestBed } from '@angular/core/testing';
import { DotRouterService } from '@services/dot-router/dot-router.service';
import { Injectable } from '@angular/core';
import { BaseRequestOptions, ConnectionBackend, Http, RequestOptions } from '@angular/http';
import { MockBackend } from '@angular/http/testing';
import { CoreWebService } from 'dotcms-js';
import { CoreWebServiceMock } from 'projects/dotcms-js/src/lib/core/core-web.service.mock';

@Injectable()
export class DotRouterServiceMock {
    isCustomPortlet(portletId: string): boolean {
        return portletId === 'c_testing';
    }
}

describe('DotIframeService', () => {
    let service: DotIframeService;

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [
                DotIframeService,
                DotUiColorsService,
                { provide: DotRouterService, useClass: DotRouterServiceMock },
                { provide: ConnectionBackend, useClass: MockBackend },
                { provide: CoreWebService, useClass: CoreWebServiceMock },
                { provide: RequestOptions, useClass: BaseRequestOptions },
                Http
            ]
        });

        service = TestBed.get(DotIframeService);
    });

    it(
        'should trigger reload action',
        async(() => {
            service.reloaded().subscribe(res => {
                expect(res).toEqual({ name: 'reload' });
            });

            service.reload();
        })
    );

    it(
        'should trigger reload colors action',
        async(() => {
            service.reloadedColors().subscribe(res => {
                expect(res).toEqual({ name: 'colors' });
            });

            service.reloadColors();
        })
    );

    it('should trigger ran action', () => {
        service.ran().subscribe(res => {
            expect(res).toEqual({ name: 'functionName' });
        });

        service.run({ name: 'functionName' });
    });

    describe('reload portlet data', () => {
        beforeEach(() => {
            spyOn(service, 'run');
        });

        it('should reload data for content', () => {
            service.reloadData('content');

            expect(service.run).toHaveBeenCalledWith({ name: 'doSearch' });
        });

        it('should reload data for custom content portlet', () => {
            service.reloadData('c_testing');

            expect(service.run).toHaveBeenCalledWith({ name: 'doSearch' });
        });

        it('should reload data for vanity-urls', () => {
            service.reloadData('vanity-urls');

            expect(service.run).toHaveBeenCalledWith({ name: 'doSearch' });
        });

        it('should reload data for site-browser', () => {
            service.reloadData('site-browser');

            expect(service.run).toHaveBeenCalledWith({ name: 'reloadContent' });
        });

        it('should reload data for sites', () => {
            service.reloadData('sites');

            expect(service.run).toHaveBeenCalledWith({ name: 'refreshHostTable' });
        });

        it('should reload data for worflow', () => {
            service.reloadData('workflow');

            expect(service.run).toHaveBeenCalledWith({ name: 'doFilter' });
        });
    });
});
