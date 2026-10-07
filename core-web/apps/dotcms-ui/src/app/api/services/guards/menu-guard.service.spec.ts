import { mockProvider } from '@openng/spectator/vitest';
import { of as observableOf, throwError } from 'rxjs';
import { vi } from 'vitest';

import { HttpErrorResponse } from '@angular/common/http';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { Injectable, signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import {
    ActivatedRouteSnapshot,
    Router,
    RouterStateSnapshot,
    UrlTree,
    provideRouter
} from '@angular/router';

import {
    DotContentletService,
    DotHttpErrorManagerService,
    DotRouterService,
    DotSessionStorageService,
    DotSiteService
} from '@dotcms/data-access';
import { DotCMSContentlet, DotPagination, DotSite } from '@dotcms/dotcms-models';
import { GlobalStore } from '@dotcms/store';

import { MenuGuardService } from './menu-guard.service';

import { DotNavigationService } from '../../../view/components/dot-navigation/services/dot-navigation.service';
import { DotMenuService } from '../dot-menu.service';

@Injectable()
class MockDotMenuService {
    isPortletInMenu() {
        //
    }
}

@Injectable()
class MockDotNavigationService {
    goToFirstPortlet = vi.fn();
}

describe('ValidMenuGuardService', () => {
    let menuGuardService: MenuGuardService;
    let dotMenuService: DotMenuService;
    let dotNavigationService: DotNavigationService;
    let dotRouterService: DotRouterService;
    let mockRouterStateSnapshot: RouterStateSnapshot;
    let mockActivatedRouteSnapshot: ActivatedRouteSnapshot;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [HttpClientTestingModule],
            providers: [
                MenuGuardService,
                provideRouter([]),
                {
                    provide: DotRouterService,
                    useValue: {
                        getPortletId: () => 'test',
                        isJSPPortletURL: () => false
                    }
                },
                { provide: DotMenuService, useClass: MockDotMenuService },
                {
                    provide: DotNavigationService,
                    useClass: MockDotNavigationService
                },
                mockProvider(DotSessionStorageService),
                mockProvider(DotContentletService),
                mockProvider(GlobalStore, {
                    siteDetails: signal({
                        identifier: 'demo-id',
                        hostname: 'demo.dotcms.com'
                    } as DotSite),
                    switchCurrentSite: vi.fn()
                }),
                mockProvider(DotSiteService, {
                    getSites: vi.fn().mockReturnValue(
                        observableOf({
                            sites: [
                                { identifier: 'other-prefix-id', hostname: 'other.com.ar' },
                                { identifier: 'other-id', hostname: 'other.com' }
                            ] as DotSite[],
                            pagination: {}
                        })
                    )
                }),
                mockProvider(DotHttpErrorManagerService, {
                    handle: vi.fn().mockReturnValue(observableOf({ redirected: false }))
                })
            ]
        });

        menuGuardService = TestBed.inject(MenuGuardService);
        dotMenuService = TestBed.inject(DotMenuService);
        dotRouterService = TestBed.inject(DotRouterService);
        dotNavigationService = TestBed.inject(DotNavigationService);
        mockRouterStateSnapshot = { toString: vi.fn() } as unknown as RouterStateSnapshot;
        mockActivatedRouteSnapshot = { toString: vi.fn() } as unknown as ActivatedRouteSnapshot;
    });

    it('should allow access to Menu Portlets', () => {
        let result: boolean | UrlTree;
        mockRouterStateSnapshot.url = '/test';
        vi.spyOn(dotMenuService, 'isPortletInMenu').mockReturnValue(observableOf(true));
        menuGuardService
            .canActivate(mockActivatedRouteSnapshot, mockRouterStateSnapshot)
            .subscribe((res) => (result = res));
        expect(dotMenuService.isPortletInMenu).toHaveBeenCalledWith('test', false);
        expect(dotMenuService.isPortletInMenu).toHaveBeenCalledTimes(1);
        expect(result).toBe(true);
    });

    it('should prevent access to Menu Portlets', () => {
        let result: boolean | UrlTree;
        mockRouterStateSnapshot.url = '/test';
        vi.spyOn(dotMenuService, 'isPortletInMenu').mockReturnValue(observableOf(false));
        menuGuardService
            .canActivate(mockActivatedRouteSnapshot, mockRouterStateSnapshot)
            .subscribe((res) => (result = res));
        expect(dotMenuService.isPortletInMenu).toHaveBeenCalledWith('test', false);
        expect(dotMenuService.isPortletInMenu).toHaveBeenCalledTimes(1);
        expect(dotNavigationService.goToFirstPortlet).toHaveBeenCalled();
        expect(result).toBe(false);
    });

    it('should allow children access to Menu Portlets', () => {
        let result: boolean | UrlTree;
        mockRouterStateSnapshot.url = '/test';
        vi.spyOn(dotMenuService, 'isPortletInMenu').mockReturnValue(observableOf(true));
        menuGuardService
            .canActivateChild(mockActivatedRouteSnapshot, mockRouterStateSnapshot)
            .subscribe((res) => (result = res));
        expect(dotMenuService.isPortletInMenu).toHaveBeenCalledWith('test', false);
        expect(dotMenuService.isPortletInMenu).toHaveBeenCalledTimes(1);
        expect(result).toBe(true);
    });

    it('should prevent children access to Menu Portlets', () => {
        let result: boolean | UrlTree;
        mockRouterStateSnapshot.url = '/test';
        vi.spyOn(dotMenuService, 'isPortletInMenu').mockReturnValue(observableOf(false));
        menuGuardService
            .canActivateChild(mockActivatedRouteSnapshot, mockRouterStateSnapshot)
            .subscribe((res) => (result = res));
        expect(dotMenuService.isPortletInMenu).toHaveBeenCalledWith('test', false);
        expect(dotMenuService.isPortletInMenu).toHaveBeenCalledTimes(1);
        expect(dotNavigationService.goToFirstPortlet).toHaveBeenCalled();
        expect(result).toBe(false);
    });

    describe('JSPPortlet', () => {
        beforeEach(() => {
            vi.spyOn(dotRouterService, 'isJSPPortletURL').mockReturnValue(true);
            mockRouterStateSnapshot.url = '/c/test';
        });

        it('should allow children access to Menu Portlets if JSPPortlet is in menu', () =>
            new Promise<void>((done) => {
                const spy = vi
                    .spyOn(dotMenuService, 'isPortletInMenu')
                    .mockReturnValue(observableOf(true));
                menuGuardService
                    .canActivateChild(mockActivatedRouteSnapshot, mockRouterStateSnapshot)
                    .subscribe((res) => {
                        expect(res).toBe(true);
                        done();
                    });
                expect(spy).toHaveBeenCalledWith('test', true);
                expect(spy).toHaveBeenCalledTimes(1);
                expect(dotNavigationService.goToFirstPortlet).not.toHaveBeenCalled();
            }));

        it('should prevent children access to Menu Portlets if JSPPortlet is in menu', () =>
            new Promise<void>((done) => {
                const spy = vi
                    .spyOn(dotMenuService, 'isPortletInMenu')
                    .mockReturnValue(observableOf(false));
                menuGuardService
                    .canActivateChild(mockActivatedRouteSnapshot, mockRouterStateSnapshot)
                    .subscribe((res) => {
                        expect(res).toBe(false);
                        done();
                    });
                expect(spy).toHaveBeenCalledWith('test', true);
                expect(spy).toHaveBeenCalledTimes(1);
                expect(dotNavigationService.goToFirstPortlet).toHaveBeenCalled();
            }));
    });
    describe('Content Drive fallback', () => {
        let router: Router;

        const givenMenu = (portletIds: string[]) =>
            vi
                .spyOn(dotMenuService, 'isPortletInMenu')
                .mockImplementation((id: string) => observableOf(portletIds.includes(id)));

        const runGuard = (url: string, portletId: string) => {
            let result: boolean | UrlTree | undefined;
            vi.spyOn(dotRouterService, 'getPortletId').mockReturnValue(portletId);
            vi.spyOn(dotRouterService, 'isJSPPortletURL').mockReturnValue(true);
            mockRouterStateSnapshot.url = url;
            menuGuardService
                .canActivateChild(mockActivatedRouteSnapshot, mockRouterStateSnapshot)
                .subscribe((res) => (result = res));

            return result;
        };

        const serialize = (result: boolean | UrlTree | undefined) =>
            result instanceof UrlTree ? router.serializeUrl(result) : result;

        beforeEach(() => {
            router = TestBed.inject(Router);
        });

        it('should redirect Content Search to Content Drive when only Content Drive is in the menu', () => {
            givenMenu(['content-drive']);

            expect(serialize(runGuard('/c/content', 'content'))).toBe('/content-drive');
            expect(dotNavigationService.goToFirstPortlet).not.toHaveBeenCalled();
        });

        it('should carry the Content Search content type filter over to Content Drive', () => {
            givenMenu(['content-drive']);

            expect(serialize(runGuard('/c/content?filter=Banner', 'content'))).toBe(
                '/content-drive?filters=contentType:Banner'
            );
        });

        it('should clear the variant when redirecting to Content Drive', () => {
            givenMenu(['content-drive']);

            runGuard('/c/content', 'content');

            expect(TestBed.inject(DotSessionStorageService).removeVariantId).toHaveBeenCalled();
        });

        it('should drop Content Search params Content Drive has no equivalent for', () => {
            givenMenu(['content-drive']);

            expect(serialize(runGuard('/c/content?mId=abcd', 'content'))).toBe('/content-drive');
        });

        it('should redirect Site Browser to Content Drive when only Content Drive is in the menu', () => {
            givenMenu(['content-drive']);

            expect(serialize(runGuard('/c/site-browser', 'site-browser'))).toBe('/content-drive');
            expect(dotNavigationService.goToFirstPortlet).not.toHaveBeenCalled();
        });

        describe('Site Browser folder links', () => {
            it('should open the folder in Content Drive without the host when it is the current site', () => {
                givenMenu(['content-drive']);

                const result = runGuard(
                    '/c/site-browser?path=%2F%2Fdemo.dotcms.com%2Fapplication%2Fcontainers%2Fdefault%2F',
                    'site-browser'
                ) as UrlTree;

                expect(result.queryParams).toEqual({ path: '/application/containers/default/' });
                expect(TestBed.inject(GlobalStore).switchCurrentSite).not.toHaveBeenCalled();
            });

            it('should open a folder path that has no host on the current site', () => {
                givenMenu(['content-drive']);

                const result = runGuard(
                    '/c/site-browser?path=%2Fapplication%2Fthemes%2Ftravel%2F',
                    'site-browser'
                ) as UrlTree;

                expect(result.queryParams).toEqual({ path: '/application/themes/travel/' });
                expect(TestBed.inject(DotSiteService).getSites).not.toHaveBeenCalled();
            });

            it('should switch to the folder host first when it is another site', () => {
                givenMenu(['content-drive']);

                const result = runGuard(
                    '/c/site-browser?path=%2F%2Fother.com%2Fapplication%2Ftemplates%2Fmain%2F',
                    'site-browser'
                ) as UrlTree;

                expect(TestBed.inject(DotSiteService).getSites).toHaveBeenCalledWith({
                    filter: 'other.com'
                });
                expect(TestBed.inject(GlobalStore).switchCurrentSite).toHaveBeenCalledWith(
                    'other-id'
                );
                expect(result.queryParams).toEqual({ path: '/application/templates/main/' });
            });

            it('should open Content Drive at the root when the folder host cannot be found', () => {
                vi.spyOn(TestBed.inject(DotSiteService), 'getSites').mockReturnValue(
                    observableOf({ sites: [] as DotSite[], pagination: {} as DotPagination })
                );
                givenMenu(['content-drive']);

                expect(
                    serialize(
                        runGuard(
                            '/c/site-browser?path=%2F%2Fgone.com%2Fapplication%2F',
                            'site-browser'
                        )
                    )
                ).toBe('/content-drive');
                expect(TestBed.inject(GlobalStore).switchCurrentSite).not.toHaveBeenCalled();
            });
        });

        it('should keep Content Search when it is still in the menu', () => {
            const spy = givenMenu(['content', 'content-drive']);

            expect(runGuard('/c/content?filter=Banner', 'content')).toBe(true);
            expect(spy).not.toHaveBeenCalledWith('content-drive', expect.anything());
        });

        it('should keep Site Browser when it is still in the menu', () => {
            givenMenu(['site-browser', 'content-drive']);

            expect(runGuard('/c/site-browser', 'site-browser')).toBe(true);
        });

        it('should send the user to the first portlet when Content Drive is not in the menu either', () => {
            givenMenu([]);

            expect(runGuard('/c/content', 'content')).toBe(false);
            expect(dotNavigationService.goToFirstPortlet).toHaveBeenCalled();
        });

        describe('edit links', () => {
            let dotContentletService: DotContentletService;

            beforeEach(() => {
                dotContentletService = TestBed.inject(DotContentletService);
                vi.spyOn(dotContentletService, 'getContentletByInode').mockReturnValue(
                    observableOf({
                        inode: 'inode-123',
                        identifier: 'identifier-456',
                        languageId: 2
                    } as DotCMSContentlet)
                );
            });

            it('should open the content in Content Drive by identifier and language', () => {
                givenMenu(['content-drive']);

                expect(serialize(runGuard('/c/content/inode-123', 'content'))).toBe(
                    '/content-drive?editContent=identifier-456&editContentLang=2'
                );
                expect(dotContentletService.getContentletByInode).toHaveBeenCalledWith('inode-123');
                expect(dotNavigationService.goToFirstPortlet).not.toHaveBeenCalled();
            });

            // The `CD_` hand-off is gone (#37759, FR-026): an old link's `CD_` params are dropped
            // like any other param Content Drive does not know.
            it('should drop the old CD_ params the link carries', () => {
                givenMenu(['content-drive']);

                const result = runGuard(
                    '/c/content/inode-123?CD_path=%2Fimages&CD_filters=baseType:1&variantName=DEFAULT',
                    'content'
                ) as UrlTree;

                expect(result.queryParams).toEqual({
                    editContent: 'identifier-456',
                    editContentLang: '2'
                });
            });

            it('should open Content Drive and report the error when the lookup fails', () => {
                const error = new HttpErrorResponse({ status: 404 });
                const dotHttpErrorManagerService = TestBed.inject(DotHttpErrorManagerService);
                vi.spyOn(dotContentletService, 'getContentletByInode').mockReturnValue(
                    throwError(() => error)
                );
                givenMenu(['content-drive']);

                expect(serialize(runGuard('/c/content/inode-123', 'content'))).toBe(
                    '/content-drive'
                );
                expect(dotHttpErrorManagerService.handle).toHaveBeenCalledWith(error);
            });

            it('should stop without a second navigation when the error handler already navigated', () => {
                vi.spyOn(dotContentletService, 'getContentletByInode').mockReturnValue(
                    throwError(() => new HttpErrorResponse({ status: 401 }))
                );
                vi.spyOn(TestBed.inject(DotHttpErrorManagerService), 'handle').mockReturnValue(
                    observableOf({ redirected: true, status: 401 })
                );
                givenMenu(['content-drive']);

                expect(runGuard('/c/content/inode-123', 'content')).toBe(false);
                expect(dotNavigationService.goToFirstPortlet).not.toHaveBeenCalled();
            });

            it('should not look the content up when Content Search is still in the menu', () => {
                givenMenu(['content', 'content-drive']);

                expect(runGuard('/c/content/inode-123', 'content')).toBe(true);
                expect(dotContentletService.getContentletByInode).not.toHaveBeenCalled();
            });

            it('should not look the content up when Content Drive is not in the menu', () => {
                givenMenu([]);

                expect(runGuard('/c/content/inode-123', 'content')).toBe(false);
                expect(dotContentletService.getContentletByInode).not.toHaveBeenCalled();
                expect(dotNavigationService.goToFirstPortlet).toHaveBeenCalled();
            });
        });

        describe('create links', () => {
            it('should open the create flow in Content Drive for the content type', () => {
                givenMenu(['content-drive']);

                expect(serialize(runGuard('/c/content/new/webPageContent', 'content'))).toBe(
                    '/content-drive?createContent=webPageContent'
                );
                expect(dotNavigationService.goToFirstPortlet).not.toHaveBeenCalled();
            });

            it('should drop the old CD_ params and the legacy folder inode', () => {
                givenMenu(['content-drive']);

                const result = runGuard(
                    '/c/content/new/Blog?CD_path=%2Fblog&folder=folder-inode',
                    'content'
                ) as UrlTree;

                expect(result.queryParams).toEqual({ createContent: 'Blog' });
            });

            it('should open Content Drive when no content type is given', () => {
                givenMenu(['content-drive']);

                expect(serialize(runGuard('/c/content/new', 'content'))).toBe('/content-drive');
            });
        });

        // Content Drive opens legacy-editor content in its own panel now, so it never links to the
        // Content Search route and there is no loop to prevent. Part 1's loop check is gone, and
        // links fired from inside Content Drive are redirected like any other (#37759, FR-027).
        describe('when leaving Content Drive', () => {
            beforeEach(() => {
                vi.spyOn(router, 'url', 'get').mockReturnValue('/content-drive?path=%2Fimages');
            });

            it('should redirect an edit link back into Content Drive like any other', () => {
                const dotContentletService = TestBed.inject(DotContentletService);
                vi.spyOn(dotContentletService, 'getContentletByInode').mockReturnValue(
                    observableOf({
                        inode: 'inode-123',
                        identifier: 'identifier-456',
                        languageId: 2
                    } as DotCMSContentlet)
                );
                givenMenu(['content-drive']);

                expect(serialize(runGuard('/c/content/inode-123', 'content'))).toBe(
                    '/content-drive?editContent=identifier-456&editContentLang=2'
                );
                expect(dotContentletService.getContentletByInode).toHaveBeenCalledWith('inode-123');
                expect(dotNavigationService.goToFirstPortlet).not.toHaveBeenCalled();
            });

            it('should redirect a create link like any other', () => {
                givenMenu(['content-drive']);

                const result = runGuard('/c/content/new/Blog', 'content') as UrlTree;

                expect(result.queryParams).toEqual({ createContent: 'Blog' });
                expect(dotNavigationService.goToFirstPortlet).not.toHaveBeenCalled();
            });

            it('should still redirect a Content Search listing link', () => {
                givenMenu(['content-drive']);

                expect(serialize(runGuard('/c/content', 'content'))).toBe('/content-drive');
            });
        });

        it('should not redirect other portlets missing from the menu', () => {
            givenMenu(['content-drive']);

            expect(runGuard('/c/workflow', 'workflow')).toBe(false);
            expect(dotNavigationService.goToFirstPortlet).toHaveBeenCalled();
        });
    });
});
