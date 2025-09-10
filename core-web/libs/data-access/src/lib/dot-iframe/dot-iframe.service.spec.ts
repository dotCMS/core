import { createServiceFactory, SpectatorService, mockProvider } from '@ngneat/spectator/jest';

import { Injectable } from '@angular/core';

import { CoreWebService } from '@dotcms/dotcms-js';

import { DotIframeService } from './dot-iframe.service';

import { DotRouterService } from '../dot-router/dot-router.service';

@Injectable()
export class DotRouterServiceMock {
    isCustomPortlet(portletId: string): boolean {
        return portletId === 'c_testing';
    }
}

describe('DotIframeService', () => {
    let spectator: SpectatorService<DotIframeService>;
    let service: DotIframeService;

    const createService = createServiceFactory({
        service: DotIframeService,
        providers: [
            {
                provide: DotRouterService,
                useClass: DotRouterServiceMock
            },
            mockProvider(CoreWebService)
        ]
    });

    beforeEach(() => {
        spectator = createService();
        service = spectator.service;
    });

    it('should trigger reload action', (done) => {
        service.reloaded().subscribe((res) => {
            expect(res).toEqual({ name: 'reload' });
            done();
        });

        service.reload();
    });

    it('should trigger reload colors action', (done) => {
        service.reloadedColors().subscribe((res) => {
            expect(res).toEqual({ name: 'colors' });
            done();
        });

        service.reloadColors();
    });

    it('should trigger ran action', (done) => {
        service.ran().subscribe((res) => {
            expect(res).toEqual({ name: 'functionName' });
            done();
        });

        service.run({ name: 'functionName' });
    });

    describe('reload portlet data', () => {
        beforeEach(() => {
            jest.spyOn(service, 'run');
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

        it('should reload data for workflow', () => {
            service.reloadData('workflow');

            expect(service.run).toHaveBeenCalledWith({ name: 'doFilter' });
        });
    });
});
