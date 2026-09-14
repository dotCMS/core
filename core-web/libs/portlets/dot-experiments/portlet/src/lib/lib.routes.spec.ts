import { Route, UrlSegment } from '@angular/router';

import { DotExperimentsService } from '@dotcms/data-access';
import { ExperimentsConfigProperties } from '@dotcms/dotcms-models';
import { DotExperimentsConfigResolver } from '@dotcms/portlets/dot-experiments/data-access';
import { dotAnalyticsHealthCheckResolver, DotPushPublishEnvironmentsResolver } from '@dotcms/ui';

import { dotExperimentsPortletRoutes, experimentsConfigureMatcher } from './lib.routes';

const segmentsOf = (...paths: string[]): UrlSegment[] =>
    paths.map((path) => new UrlSegment(path, {}));

describe('dotExperimentsPortletRoutes', () => {
    const listRoute = dotExperimentsPortletRoutes.find((route) => route.path === '') as Route;
    const configureRoute = dotExperimentsPortletRoutes.find((route) => !!route.matcher) as Route;
    const resultsRoute = dotExperimentsPortletRoutes.find(
        (route) => route.path === ':experimentId/results'
    ) as Route;

    it('should expose the list route', () => {
        expect(listRoute).toBeDefined();
        expect(listRoute.loadComponent).toBeDefined();
    });

    it('should reach the Configure screen through the matcher, not a path', () => {
        // `new` and `:experimentId/configuration` are deliberately one route config: creating a
        // draft swaps one URL for the other, and a second config would tear the shell — and with
        // it the store holding the just-created experiment — down mid-swap.
        expect(configureRoute).toBeDefined();
        expect(configureRoute.matcher).toBe(experimentsConfigureMatcher);
        expect(configureRoute.loadComponent).toBeDefined();
        expect(configureRoute.path).toBeUndefined();
    });

    it('should expose the Results screen on a plain path', () => {
        // Nothing swaps this URL mid-screen, so it needs none of the matcher the Configure screen
        // exists for: one experiment, one path, on every status (AC1).
        expect(resultsRoute).toBeDefined();
        expect(resultsRoute.loadComponent).toBeDefined();
        expect(resultsRoute.matcher).toBeUndefined();
    });

    it('should wire the three screens the portlet owns', () => {
        expect(dotExperimentsPortletRoutes).toHaveLength(3);
    });

    describe('resolvers', () => {
        it('should resolve the push publish environments on both screens', () => {
            expect(listRoute.resolve?.['pushPublishEnvironments']).toBe(
                DotPushPublishEnvironmentsResolver
            );
            // The Configure header's kebab offers Push Publish, so it needs them the same way
            // the list's row menu does.
            expect(configureRoute.resolve?.['pushPublishEnvironments']).toBe(
                DotPushPublishEnvironmentsResolver
            );
        });

        it('should resolve the experiments configuration on the Configure screen', () => {
            expect(configureRoute.resolve?.['config']).toBe(DotExperimentsConfigResolver);
        });

        it('should ask for the duration bounds the Scheduling card is limited by', () => {
            expect(configureRoute.data?.['experimentsConfigProps']).toEqual([
                ExperimentsConfigProperties.EXPERIMENTS_MIN_DURATION,
                ExperimentsConfigProperties.EXPERIMENTS_MAX_DURATION
            ]);
        });

        it('should resolve the analytics health status on the Results screen', () => {
            // A plain resolve, not a guard: it reports rather than redirects, so a misconfigured
            // analytics app takes out this screen only and the list stays reachable (AC22).
            expect(resultsRoute.resolve?.['healthStatus']).toBe(dotAnalyticsHealthCheckResolver);
        });

        it('should provide the service the health resolver injects', () => {
            // The resolver is a standalone `ResolveFn` and needs no provider of its own, but it
            // runs in the route's injector — where `DotExperimentsService`, `@Injectable()` with
            // no `providedIn`, has to exist before the screen does.
            expect(resultsRoute.providers).toContain(DotExperimentsService);
        });

        it('should leave the list ungated by the analytics health check', () => {
            expect(listRoute.resolve?.['healthStatus']).toBeUndefined();
        });

        it.each([
            ['list', () => listRoute],
            ['configure', () => configureRoute]
        ])('should provide every class resolver the %s route references', (_name, routeOf) => {
            // Both resolvers are `@Injectable()` with no `providedIn`, so a `resolve` entry
            // without a matching `providers` entry compiles and builds fine and then throws
            // NG0201 the moment a user opens the screen.
            const route = routeOf();
            const provided = new Set(route.providers ?? []);
            const referenced = Object.values(route.resolve ?? {});

            expect(referenced.length).toBeGreaterThan(0);
            referenced.forEach((resolver) => expect(provided.has(resolver)).toBe(true));
        });
    });

    describe('route reuse', () => {
        it('should let the Configure shell survive the new → configuration swap', () => {
            // Route `data` is inherited, so the mount point in `app.routes.ts` must not force
            // `reuseRoute: false` on this subtree: recreating the shell would drop the debounced
            // autosaves and the experiment the store holds in memory.
            expect(configureRoute.data?.['reuseRoute']).toBe(true);
        });
    });
});

describe('experimentsConfigureMatcher', () => {
    it('should match the creation screen without exposing an experimentId', () => {
        const segments = segmentsOf('new');

        const match = experimentsConfigureMatcher(segments);

        expect(match?.consumed).toEqual(segments);
        // Its absence is what tells the store it is on the creation screen.
        expect(match?.posParams).toBeUndefined();
    });

    it('should match an existing experiment and expose its id', () => {
        const segments = segmentsOf('abc', 'configuration');

        const match = experimentsConfigureMatcher(segments);

        expect(match?.consumed).toEqual(segments);
        expect(match?.posParams?.['experimentId']).toBe(segments[0]);
    });

    it.each([
        ['the list', segmentsOf()],
        ['the Results screen, which matches on its own path', segmentsOf('abc', 'results')],
        ['a deeper unknown URL', segmentsOf('abc', 'configuration', 'extra')]
    ])('should fall through to %s', (_name, segments) => {
        expect(experimentsConfigureMatcher(segments)).toBeNull();
    });
});
