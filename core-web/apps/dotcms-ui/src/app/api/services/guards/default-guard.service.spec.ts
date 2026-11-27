import { of, Subject } from 'rxjs';
import { Mock, vi } from 'vitest';

import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, Router, RouterStateSnapshot } from '@angular/router';

import { DotRouterService } from '@dotcms/data-access';
import { GlobalStore } from '@dotcms/store';

import { DefaultGuardService } from './default-guard.service';

import { DOTTestBed } from '../../../test/dot-test-bed';
import { DynamicRouteService } from '../dynamic-route.service';

describe('DefaultGuardService', () => {
    let guard: DefaultGuardService;
    let dotRouterService: DotRouterService;
    let router: Router;
    let dynamicRouteService: DynamicRouteService;
    let globalStore: { ensureLoaded: Mock };

    const mockRoute = {} as ActivatedRouteSnapshot;

    beforeEach(() => {
        DOTTestBed.configureTestingModule({
            providers: [
                DefaultGuardService,
                {
                    // The guard waits for the menu snapshot; loading it registers the plugin routes.
                    provide: GlobalStore,
                    useValue: { ensureLoaded: vi.fn().mockReturnValue(of(undefined)) }
                },
                {
                    provide: DynamicRouteService,
                    useValue: {
                        registerRoutesFromMenuItems: vi.fn().mockReturnValue(1),
                        isRouteRegistered: vi.fn()
                    }
                }
            ]
        });

        guard = TestBed.inject(DefaultGuardService);
        dotRouterService = TestBed.inject(DotRouterService);
        router = TestBed.inject(Router);
        dynamicRouteService = TestBed.inject(DynamicRouteService);
        globalStore = TestBed.inject(GlobalStore) as unknown as typeof globalStore;

        vi.spyOn(router, 'navigateByUrl').mockResolvedValue(true);
    });

    it('should redirect to dynamic route when URL matches a registered dynamic route', () =>
        new Promise<void>((done) => {
            (dynamicRouteService.isRouteRegistered as Mock).mockReturnValue(true);
            const state = { url: '/my-dynamic-portlet' } as RouterStateSnapshot;

            guard.canActivate(mockRoute, state).subscribe((result) => {
                expect(globalStore.ensureLoaded).toHaveBeenCalled();
                expect(dynamicRouteService.registerRoutesFromMenuItems).not.toHaveBeenCalled();
                expect(dynamicRouteService.isRouteRegistered).toHaveBeenCalledWith(
                    'my-dynamic-portlet'
                );
                expect(router.navigateByUrl).toHaveBeenCalledWith('/my-dynamic-portlet');
                expect(dotRouterService.goToMain).not.toHaveBeenCalled();
                expect(result).toBe(false);
                done();
            });
        }));

    it('should redirect to main when URL does not match a dynamic route', () =>
        new Promise<void>((done) => {
            (dynamicRouteService.isRouteRegistered as Mock).mockReturnValue(false);
            const state = { url: '/some-unknown-path' } as RouterStateSnapshot;

            guard.canActivate(mockRoute, state).subscribe((result) => {
                expect(globalStore.ensureLoaded).toHaveBeenCalled();
                expect(dynamicRouteService.registerRoutesFromMenuItems).not.toHaveBeenCalled();
                expect(dynamicRouteService.isRouteRegistered).toHaveBeenCalledWith(
                    'some-unknown-path'
                );
                expect(router.navigateByUrl).not.toHaveBeenCalled();
                expect(dotRouterService.goToMain).toHaveBeenCalled();
                expect(result).toBe(true);
                done();
            });
        }));

    it('decides only after the menu is ready, so the routes it registers already exist', () => {
        const ready = new Subject<void>();
        let result: boolean | undefined;
        globalStore.ensureLoaded.mockReturnValue(ready.asObservable());
        (dynamicRouteService.isRouteRegistered as Mock).mockReturnValue(true);

        guard
            .canActivate(mockRoute, { url: '/my-dynamic-portlet' } as RouterStateSnapshot)
            .subscribe((value) => (result = value));

        expect(dynamicRouteService.isRouteRegistered).not.toHaveBeenCalled();
        expect(result).toBeUndefined();

        ready.next();
        ready.complete();

        expect(dynamicRouteService.isRouteRegistered).toHaveBeenCalledWith('my-dynamic-portlet');
        expect(router.navigateByUrl).toHaveBeenCalledWith('/my-dynamic-portlet');
        expect(result).toBe(false);
    });
});
