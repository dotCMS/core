import { Route } from '@angular/router';

import { DotPushPublishEnvironmentsResolver } from '@dotcms/ui';

import { dotExperimentsPortletRoutes } from './lib.routes';

describe('dotExperimentsPortletRoutes', () => {
    const listRoute = dotExperimentsPortletRoutes.find((route) => route.path === '') as Route;

    it('should expose the list route', () => {
        expect(listRoute).toBeDefined();
        expect(listRoute.loadComponent).toBeDefined();
    });

    it('should not wire the screens owned by follow-up issues', () => {
        // `new`, `:id/configuration` and `:id/results` land with #36990+. Until then an
        // unimplemented deep link must fall through rather than resolve to a blank screen.
        const paths = dotExperimentsPortletRoutes.map((route) => route.path);

        expect(paths).toEqual(['']);
    });

    describe('resolvers', () => {
        it('should resolve the push publish environments', () => {
            expect(listRoute.resolve?.['pushPublishEnvironments']).toBe(
                DotPushPublishEnvironmentsResolver
            );
        });

        it('should provide every class resolver it references', () => {
            // `DotPushPublishEnvironmentsResolver` is `@Injectable()` with no `providedIn`, so
            // a `resolve` entry without a matching `providers` entry compiles and builds fine
            // and then throws NG0201 the moment a user opens the portlet.
            const provided = new Set(listRoute.providers ?? []);
            const referenced = Object.values(listRoute.resolve ?? {});

            expect(referenced.length).toBeGreaterThan(0);
            referenced.forEach((resolver) => expect(provided.has(resolver)).toBe(true));
        });
    });
});
