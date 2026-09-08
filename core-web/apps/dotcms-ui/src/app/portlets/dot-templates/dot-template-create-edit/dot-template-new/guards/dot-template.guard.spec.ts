import { TestBed } from '@angular/core/testing';
import { Route, UrlSegment } from '@angular/router';

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

    /** `canLoad` declares this parameter `_route` and never reads it. */
    const UNUSED_ROUTE = null as unknown as Route;

    it('should return true when path is /advanced', () => {
        const segment = new UrlSegment('advanced', {});
        expect(guard.canLoad(UNUSED_ROUTE, [segment])).toBe(true);
    });

    it('should return true when path is /designer', () => {
        const segment = new UrlSegment('designer', {});
        expect(guard.canLoad(UNUSED_ROUTE, [segment])).toBe(true);
    });

    it('should return false and redirect with invalid path', () => {
        const segment = new UrlSegment('xxxx', {});
        expect(guard.canLoad(UNUSED_ROUTE, [segment])).toBe(false);
        expect(dotRouterService.gotoPortlet).toHaveBeenCalledWith('templates');
        expect(dotRouterService.gotoPortlet).toHaveBeenCalledTimes(1);
    });
});
