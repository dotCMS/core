import { TestBed } from '@angular/core/testing';
import { UrlSegment } from '@angular/router';

import { DotRouterService } from '@dotcms/data-access';

import { DotTemplateGuard } from './dot-template.guard';

describe('DotTemplateGuard', () => {
    let guard: DotTemplateGuard;
    let dotRouterService: DotRouterService;

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [
                DotTemplateGuard,
                {
                    provide: DotRouterService,
                    useValue: {
                        gotoPortlet: jest.fn()
                    }
                }
            ]
        });
        guard = TestBed.inject(DotTemplateGuard);
        dotRouterService = TestBed.inject(DotRouterService);
    });

    it('should return true when path is /advanced', () => {
        const segment = new UrlSegment('advanced', null);
        expect(guard.canLoad(null, [segment])).toBe(true);
    });

    it('should return true when path is /designer', () => {
        const segment = new UrlSegment('designer', null);
        expect(guard.canLoad(null, [segment])).toBe(true);
    });

    it('should return false and redirect with invalid path', () => {
        const segment = new UrlSegment('xxxx', null);
        expect(guard.canLoad(null, [segment])).toBe(false);
        expect(dotRouterService.gotoPortlet).toHaveBeenCalledWith('templates');
        expect(dotRouterService.gotoPortlet).toHaveBeenCalledTimes(1);
    });
});
