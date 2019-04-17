import { DOTTestBed } from '../../../../../../test/dot-test-bed';
import { DotIframeService } from './dot-iframe.service';
import { DotUiColorsService } from '@services/dot-ui-colors/dot-ui-colors.service';
import { async } from '@angular/core/testing';
import { DotRouterService } from '@services/dot-router/dot-router.service';
import { Injectable } from '@angular/core';

@Injectable()
export class DotRouterServiceMock {
    isCustomPortlet(portletId: string): boolean {
        return portletId === 'c_testing';
    }
}

describe('DotIframeService', () => {
    let service: DotIframeService;
    const dotRouterService: DotRouterServiceMock = new DotRouterServiceMock();

    beforeEach(() => {
        const injector = DOTTestBed.resolveAndCreate([
            DotIframeService,
            DotUiColorsService,
            { provide: DotRouterService, useValue: dotRouterService }
        ]);
        service = injector.get(DotIframeService);
    });

    it('should trigger reload action', async(() => {
        service.reloaded().subscribe((res) => {
            expect(res).toBe('reload');
        });

        service.reload();
    }));

    it('should trigger reload colors action', async(() => {
        service.reloadedColors().subscribe((res) => {
            expect(res).toBe('colors');
        });

        service.reloadColors();
    }));

    it('should trigger ran action', () => {
        service.ran().subscribe((res) => {
            expect(res).toBe('functionName');
        });

        service.run('functionName');
    });

    describe('reload portlet data', () => {
        beforeEach(() => {
            spyOn(service, 'run');
        });

        it('should reload data for content', () => {
            service.reloadData('content');

            expect(service.run).toHaveBeenCalledWith('doSearch');
        });

        it('should reload data for custom content portlet', () => {
            service.reloadData('c_testing');

            expect(service.run).toHaveBeenCalledWith('doSearch');
        });

        it('should reload data for vanity-urls', () => {
            service.reloadData('vanity-urls');

            expect(service.run).toHaveBeenCalledWith('doSearch');
        });

        it('should reload data for site-browser', () => {
            service.reloadData('site-browser');

            expect(service.run).toHaveBeenCalledWith('reloadContent');
        });

        it('should reload data for sites', () => {
            service.reloadData('sites');

            expect(service.run).toHaveBeenCalledWith('refreshHostTable');
        });

        it('should reload data for worflow', () => {
            service.reloadData('workflow');

            expect(service.run).toHaveBeenCalledWith('doFilter');
        });
    });
});
