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
});
