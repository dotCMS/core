import { DOTTestBed } from '../../../../../../test/dot-test-bed';
import { DotIframeService } from './dot-iframe.service';
import { async } from '@angular/core/testing';

describe('DotIframeService', () => {
    beforeEach(() => {
        this.injector = DOTTestBed.resolveAndCreate([DotIframeService]);
        this.service = this.injector.get(DotIframeService);
    });

    it('should trigger reload action', async(() => {
        this.service.reloaded().subscribe(res => {
            expect(res).toBe('reload');
        });

        this.service.reload();
    }));

    it('should trigger ran action', () => {
        this.service.ran().subscribe(res => {
            expect(res).toBe('functionName');
        });

        this.service.run('functionName');
    });

    describe('reload portlet data', () => {
        beforeEach(() => {
            spyOn(this.service, 'run');
        });

        it('should reload data for content', () => {
            this.service.reloadData('content');

            expect(this.service.run).toHaveBeenCalledWith('doSearch');
        });

        it('should reload data for vanity-urls', () => {
            this.service.reloadData('vanity-urls');

            expect(this.service.run).toHaveBeenCalledWith('doSearch');
        });

        it('should reload data for site-browser', () => {
            this.service.reloadData('site-browser');

            expect(this.service.run).toHaveBeenCalledWith('reloadContent');
        });

        it('should reload data for sites', () => {
            this.service.reloadData('sites');

            expect(this.service.run).toHaveBeenCalledWith('refreshHostTable');
        });

        it('should reload data for worflow', () => {
            this.service.reloadData('workflow');

            expect(this.service.run).toHaveBeenCalledWith('doFilter');
        });
    });
});
