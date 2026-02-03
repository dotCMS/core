import { TestBed } from '@angular/core/testing';
import {
    ActivatedRouteSnapshot,
    Router,
    RouterStateSnapshot,
    UrlSegment,
    UrlTree
} from '@angular/router';

import { editEmaGuard } from './edit-ema.guard';

import { DEFAULT_PERSONA, PERSONA_KEY } from '../../shared/consts';

describe('editEmaGuard', () => {
    let mockCreateUrlTree: jest.Mock;
    let mockRouterStateSnapshot: RouterStateSnapshot;

    beforeEach(() => {
        mockCreateUrlTree = jest.fn().mockReturnValue({} as UrlTree);
        mockRouterStateSnapshot = {
            url: '/test'
        } as RouterStateSnapshot;

        TestBed.configureTestingModule({
            providers: [
                {
                    provide: Router,
                    useValue: {
                        createUrlTree: mockCreateUrlTree
                    }
                }
            ]
        });
    });

    /**
     * Helper to call the guard within an injection context
     */
    const runGuard = (route: ActivatedRouteSnapshot, state: RouterStateSnapshot) => {
        return TestBed.runInInjectionContext(() => editEmaGuard(route, state));
    };

    afterEach(() => {
        jest.clearAllMocks();
    });

    describe('when all required query parameters are present', () => {
        it('should return true and not redirect', () => {
            const route = createMockRoute({
                queryParams: {
                    url: '/test-page',
                    language_id: '1',
                    [PERSONA_KEY]: DEFAULT_PERSONA.identifier
                },
                firstChild: createMockRoute({
                    url: [{ path: 'experiments' } as UrlSegment]
                })
            });

            const result = runGuard(route, mockRouterStateSnapshot);

            expect(result).toBe(true);
            expect(mockCreateUrlTree).not.toHaveBeenCalled();
        });

        it('should return true even when no child route exists', () => {
            const route = createMockRoute({
                queryParams: {
                    url: '/test-page',
                    language_id: '1',
                    [PERSONA_KEY]: DEFAULT_PERSONA.identifier
                },
                firstChild: null
            });

            const result = runGuard(route, mockRouterStateSnapshot);

            expect(result).toBe(true);
            expect(mockCreateUrlTree).not.toHaveBeenCalled();
        });
    });

    describe('when query parameters are missing', () => {
        it('should redirect with default values when language_id is missing', () => {
            const childRoute = createMockRoute({
                url: [{ path: 'experiments' } as UrlSegment]
            });

            const route = createMockRoute({
                queryParams: {
                    url: '/test-page',
                    [PERSONA_KEY]: DEFAULT_PERSONA.identifier
                },
                firstChild: childRoute
            });

            const result = runGuard(route, mockRouterStateSnapshot);

            expect(result).not.toBe(true);
            expect(mockCreateUrlTree).toHaveBeenCalledWith(['/edit-page/experiments'], {
                queryParams: {
                    url: '/test-page',
                    [PERSONA_KEY]: DEFAULT_PERSONA.identifier,
                    language_id: '1'
                }
            });
        });

        it('should redirect with default values when url is missing', () => {
            const childRoute = createMockRoute({
                url: [{ path: 'experiments' } as UrlSegment]
            });

            const route = createMockRoute({
                queryParams: {
                    language_id: '1',
                    [PERSONA_KEY]: DEFAULT_PERSONA.identifier
                },
                firstChild: childRoute
            });

            const result = runGuard(route, mockRouterStateSnapshot);

            expect(result).not.toBe(true);
            expect(mockCreateUrlTree).toHaveBeenCalledWith(['/edit-page/experiments'], {
                queryParams: {
                    language_id: '1',
                    [PERSONA_KEY]: DEFAULT_PERSONA.identifier,
                    url: '/'
                }
            });
        });

        it('should redirect with default values when persona.id is missing', () => {
            const childRoute = createMockRoute({
                url: [{ path: 'experiments' } as UrlSegment]
            });

            const route = createMockRoute({
                queryParams: {
                    url: '/test-page',
                    language_id: '1'
                },
                firstChild: childRoute
            });

            const result = runGuard(route, mockRouterStateSnapshot);

            expect(result).not.toBe(true);
            expect(mockCreateUrlTree).toHaveBeenCalledWith(['/edit-page/experiments'], {
                queryParams: {
                    url: '/test-page',
                    language_id: '1',
                    [PERSONA_KEY]: DEFAULT_PERSONA.identifier
                }
            });
        });

        it('should redirect with all default values when all params are missing', () => {
            const childRoute = createMockRoute({
                url: [{ path: 'experiments' } as UrlSegment]
            });

            const route = createMockRoute({
                queryParams: {},
                firstChild: childRoute
            });

            const result = runGuard(route, mockRouterStateSnapshot);

            expect(result).not.toBe(true);
            expect(mockCreateUrlTree).toHaveBeenCalledWith(['/edit-page/experiments'], {
                queryParams: {
                    url: '/',
                    language_id: '1',
                    [PERSONA_KEY]: DEFAULT_PERSONA.identifier
                }
            });
        });

        it('should preserve existing query params when adding missing ones', () => {
            const childRoute = createMockRoute({
                url: [{ path: 'experiments' } as UrlSegment]
            });

            const route = createMockRoute({
                queryParams: {
                    url: '/test-page',
                    language_id: '1',
                    customParam: 'customValue',
                    anotherParam: 'anotherValue'
                },
                firstChild: childRoute
            });

            // Remove persona.id to trigger redirect
            delete route.queryParams[PERSONA_KEY];

            const result = runGuard(route, mockRouterStateSnapshot);

            expect(result).not.toBe(true);
            expect(mockCreateUrlTree).toHaveBeenCalledWith(['/edit-page/experiments'], {
                queryParams: {
                    url: '/test-page',
                    language_id: '1',
                    customParam: 'customValue',
                    anotherParam: 'anotherValue',
                    [PERSONA_KEY]: DEFAULT_PERSONA.identifier
                }
            });
        });
    });

    describe('when url parameter is empty or whitespace', () => {
        it('should treat empty string url as missing and use default', () => {
            const childRoute = createMockRoute({
                url: [{ path: 'experiments' } as UrlSegment]
            });

            const route = createMockRoute({
                queryParams: {
                    url: '',
                    language_id: '1',
                    [PERSONA_KEY]: DEFAULT_PERSONA.identifier
                },
                firstChild: childRoute
            });

            const result = runGuard(route, mockRouterStateSnapshot);

            expect(result).not.toBe(true);
            expect(mockCreateUrlTree).toHaveBeenCalledWith(['/edit-page/experiments'], {
                queryParams: {
                    url: '/',
                    language_id: '1',
                    [PERSONA_KEY]: DEFAULT_PERSONA.identifier
                }
            });
        });

        it('should treat whitespace-only url as missing and use default', () => {
            const childRoute = createMockRoute({
                url: [{ path: 'experiments' } as UrlSegment]
            });

            const route = createMockRoute({
                queryParams: {
                    url: '   ',
                    language_id: '1',
                    [PERSONA_KEY]: DEFAULT_PERSONA.identifier
                },
                firstChild: childRoute
            });

            const result = runGuard(route, mockRouterStateSnapshot);

            expect(result).not.toBe(true);
            expect(mockCreateUrlTree).toHaveBeenCalledWith(['/edit-page/experiments'], {
                queryParams: {
                    url: '/',
                    language_id: '1',
                    [PERSONA_KEY]: DEFAULT_PERSONA.identifier
                }
            });
        });

        it('should accept url with only whitespace padding', () => {
            const childRoute = createMockRoute({
                url: [{ path: 'experiments' } as UrlSegment]
            });

            const route = createMockRoute({
                queryParams: {
                    url: '  /test-page  ',
                    language_id: '1',
                    [PERSONA_KEY]: DEFAULT_PERSONA.identifier
                },
                firstChild: childRoute
            });

            const result = runGuard(route, mockRouterStateSnapshot);

            // Should not redirect because trimmed url has content
            expect(result).toBe(true);
            expect(mockCreateUrlTree).not.toHaveBeenCalled();
        });
    });

    describe('when handling nested child routes', () => {
        it('should preserve full nested path in redirect', () => {
            // Route structure: edit-page -> experiments -> PAGE_ID -> EXP_ID -> configuration
            const configurationRoute = createMockRoute({
                url: [{ path: 'configuration' } as UrlSegment],
                firstChild: null
            });

            const expIdRoute = createMockRoute({
                url: [{ path: 'EXP_ID' } as UrlSegment],
                firstChild: configurationRoute
            });

            const pageIdRoute = createMockRoute({
                url: [{ path: 'PAGE_ID' } as UrlSegment],
                firstChild: expIdRoute
            });

            const experimentsRoute = createMockRoute({
                url: [{ path: 'experiments' } as UrlSegment],
                firstChild: pageIdRoute
            });

            const route = createMockRoute({
                queryParams: {
                    url: '/test-page'
                },
                firstChild: experimentsRoute
            });

            const result = runGuard(route, mockRouterStateSnapshot);

            expect(result).not.toBe(true);
            expect(mockCreateUrlTree).toHaveBeenCalledWith(
                ['/edit-page/experiments/PAGE_ID/EXP_ID/configuration'],
                {
                    queryParams: {
                        url: '/test-page',
                        language_id: '1',
                        [PERSONA_KEY]: DEFAULT_PERSONA.identifier
                    }
                }
            );
        });

        it('should handle single-level child route', () => {
            const childRoute = createMockRoute({
                url: [{ path: 'experiments' } as UrlSegment],
                firstChild: null
            });

            const route = createMockRoute({
                queryParams: {
                    url: '/test-page'
                },
                firstChild: childRoute
            });

            const result = runGuard(route, mockRouterStateSnapshot);

            expect(result).not.toBe(true);
            expect(mockCreateUrlTree).toHaveBeenCalledWith(['/edit-page/experiments'], {
                queryParams: {
                    url: '/test-page',
                    language_id: '1',
                    [PERSONA_KEY]: DEFAULT_PERSONA.identifier
                }
            });
        });

        it('should handle child route with multiple URL segments at same level', () => {
            const childRoute = createMockRoute({
                url: [{ path: 'experiments' } as UrlSegment, { path: 'PAGE_ID' } as UrlSegment],
                firstChild: null
            });

            const route = createMockRoute({
                queryParams: {
                    url: '/test-page'
                },
                firstChild: childRoute
            });

            const result = runGuard(route, mockRouterStateSnapshot);

            expect(result).not.toBe(true);
            expect(mockCreateUrlTree).toHaveBeenCalledWith(['/edit-page/experiments/PAGE_ID'], {
                queryParams: {
                    url: '/test-page',
                    language_id: '1',
                    [PERSONA_KEY]: DEFAULT_PERSONA.identifier
                }
            });
        });
    });

    describe('edge cases', () => {
        it('should return true when no child path exists even if params are missing', () => {
            const route = createMockRoute({
                queryParams: {},
                firstChild: null
            });

            const result = runGuard(route, mockRouterStateSnapshot);

            // When there's no childPath, guard returns true even if params are missing
            // This is the current behavior based on the condition: didQueryParamsGetCompleted && childPath
            expect(result).toBe(true);
            expect(mockCreateUrlTree).not.toHaveBeenCalled();
        });

        it('should handle falsy query param values correctly', () => {
            const childRoute = createMockRoute({
                url: [{ path: 'experiments' } as UrlSegment]
            });

            const route = createMockRoute({
                queryParams: {
                    url: '/test-page',
                    language_id: null,
                    [PERSONA_KEY]: undefined
                },
                firstChild: childRoute
            });

            const result = runGuard(route, mockRouterStateSnapshot);

            expect(result).not.toBe(true);
            expect(mockCreateUrlTree).toHaveBeenCalledWith(['/edit-page/experiments'], {
                queryParams: {
                    url: '/test-page',
                    language_id: '1',
                    [PERSONA_KEY]: DEFAULT_PERSONA.identifier
                }
            });
        });

        it('should handle zero as a valid language_id value', () => {
            const childRoute = createMockRoute({
                url: [{ path: 'experiments' } as UrlSegment]
            });

            const route = createMockRoute({
                queryParams: {
                    url: '/test-page',
                    language_id: '0',
                    [PERSONA_KEY]: DEFAULT_PERSONA.identifier
                },
                firstChild: childRoute
            });

            // Zero is truthy, so should not trigger redirect
            const result = runGuard(route, mockRouterStateSnapshot);

            expect(result).toBe(true);
            expect(mockCreateUrlTree).not.toHaveBeenCalled();
        });
    });
});

/**
 * Helper function to create a mock ActivatedRouteSnapshot with the specified properties.
 */
function createMockRoute(
    config: Partial<ActivatedRouteSnapshot> & {
        queryParams?: Record<string, string | number | null | undefined>;
        url?: UrlSegment[];
        firstChild?: ActivatedRouteSnapshot | null;
    }
): ActivatedRouteSnapshot {
    return {
        queryParams: config.queryParams || {},
        url: config.url || [],
        firstChild: config.firstChild ?? null,
        params: {},
        data: {},
        fragment: null,
        outlet: 'primary',
        component: null,
        routeConfig: null,
        root: {} as ActivatedRouteSnapshot,
        parent: null,
        children: [],
        pathFromRoot: [],
        paramMap: new Map(),
        queryParamMap: new Map(),
        title: null,
        toString: jest.fn()
    } as unknown as ActivatedRouteSnapshot;
}
