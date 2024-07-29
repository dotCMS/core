import { it, describe, expect } from '@jest/globals';
import { Observable } from 'rxjs';

import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, Router, RouterStateSnapshot } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';

import { editEmaGuard } from './edit-ema.guard';

describe('EditEmaGuard', () => {
    let router: Router;

    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    const state: RouterStateSnapshot = {} as any;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [RouterTestingModule],
            providers: [
                {
                    provide: Router,
                    useValue: {
                        navigate: jest.fn(),
                        createUrlTree: jest.fn().mockReturnValue('this.is.a.url.tree.mock')
                    }
                }
            ]
        });

        router = TestBed.inject(Router);
    });

    it('should just return true when queryParams are complete', () => {
        const route: ActivatedRouteSnapshot = {
            firstChild: {
                url: [{ path: 'content' }]
            },
            queryParams: {
                url: '/some-url',
                'com.dotmarketing.persona.id': 'modes.persona.no.persona',
                language_id: 1
            }
            // eslint-disable-next-line @typescript-eslint/no-explicit-any
        } as any;

        const didEnteredPortlet = TestBed.runInInjectionContext(
            () => editEmaGuard(route, state) as boolean
        );

        expect(router.navigate).not.toHaveBeenCalled();
        expect(didEnteredPortlet).toBe(true);
    });

    it('should just return true when the url has an "index" in the url', () => {
        const route: ActivatedRouteSnapshot = {
            firstChild: {
                url: [{ path: 'content' }]
            },
            queryParams: {
                url: '/im-just-a-cool-index-index',
                'com.dotmarketing.persona.id': 'modes.persona.no.persona',
                language_id: 1
            }
            // eslint-disable-next-line @typescript-eslint/no-explicit-any
        } as any;

        const didEnteredPortlet = TestBed.runInInjectionContext(
            () => editEmaGuard(route, state) as boolean
        );

        expect(router.navigate).not.toHaveBeenCalled();
        expect(didEnteredPortlet).toBe(true);
    });

    it('should just return true when the url has an "index-something" in the url', () => {
        const route: ActivatedRouteSnapshot = {
            firstChild: {
                url: [{ path: 'content' }]
            },
            queryParams: {
                url: '/im-just-a-cool-index-index/index-something',
                'com.dotmarketing.persona.id': 'modes.persona.no.persona',
                language_id: 1
            }
            // eslint-disable-next-line @typescript-eslint/no-explicit-any
        } as any;

        const didEnteredPortlet = TestBed.runInInjectionContext(
            () => editEmaGuard(route, state) as boolean
        );

        expect(router.navigate).not.toHaveBeenCalled();
        expect(didEnteredPortlet).toBe(true);
    });

    it('should navigate to "edit-page" and sanitize url', () => {
        const route: ActivatedRouteSnapshot = {
            firstChild: {
                url: [{ path: 'content' }]
            },
            queryParams: { url: '/some-url/with-index/index' }
            // eslint-disable-next-line @typescript-eslint/no-explicit-any
        } as any;

        TestBed.runInInjectionContext(() => editEmaGuard(route, state) as Observable<boolean>);

        expect(router.navigate).toHaveBeenCalledWith(['/edit-page/content'], {
            queryParams: {
                'com.dotmarketing.persona.id': 'modes.persona.no.persona',
                language_id: 1,
                url: 'some-url/with-index'
            },
            replaceUrl: true
        });
    });

    it('should navigate to "edit-page" and sanitize url when the url is "/"', () => {
        const route: ActivatedRouteSnapshot = {
            firstChild: {
                url: [{ path: 'content' }]
            },
            queryParams: { url: '/' }
            // eslint-disable-next-line @typescript-eslint/no-explicit-any
        } as any;

        TestBed.runInInjectionContext(() => editEmaGuard(route, state) as Observable<boolean>);

        expect(router.navigate).toHaveBeenCalledWith(['/edit-page/content'], {
            queryParams: {
                'com.dotmarketing.persona.id': 'modes.persona.no.persona',
                language_id: 1,
                url: 'index'
            },
            replaceUrl: true
        });
    });
});
