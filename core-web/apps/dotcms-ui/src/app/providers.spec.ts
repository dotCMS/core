import { readdirSync, readFileSync, statSync } from 'node:fs';
import { resolve } from 'node:path';

import { DOT_MENU_ROUTES_REGISTRAR } from '@dotcms/store';

import { ENV_PROVIDERS } from './providers';
import { SharedModule } from './shared/shared.module';
import { DotNavigationService } from './view/components/dot-navigation/services/dot-navigation.service';

// Vitest runs from the project root (apps/dotcms-ui), where the app sources live under src/app.
const appDir = `${resolve(process.cwd(), 'src/app')}/`;
const read = (relativePath: string): string => readFileSync(`${appDir}${relativePath}`, 'utf8');

/** Every non-spec TypeScript file of the app, as [relative path, source]. */
const productionSources = (dir = appDir): [string, string][] =>
    readdirSync(dir).flatMap((entry) => {
        const full = `${dir}${entry}`;
        if (statSync(full).isDirectory()) {
            return productionSources(`${full}/`);
        }

        return full.endsWith('.ts') && !full.endsWith('.spec.ts')
            ? [[full.replace(appDir, ''), readFileSync(full, 'utf8')] as [string, string]]
            : [];
    });

/**
 * One instance of each menu service, on the root injector (issue #38078). `DotMenuService` is
 * `providedIn: 'root'` in `@dotcms/data-access`; `DotNavigationService` is registered once in
 * `providers.ts`. Registering them again on a module or a component creates a second orchestrator
 * that subscribes to the router, the websocket and the auth stream and writes to the same store.
 */
describe('menu service providers', () => {
    it('registers DotNavigationService exactly once in the root providers', () => {
        expect(ENV_PROVIDERS.filter((provider) => provider === DotNavigationService)).toHaveLength(
            1
        );
    });

    it('does not register DotNavigationService again in SharedModule.forRoot()', () => {
        expect(SharedModule.forRoot().providers).not.toContain(DotNavigationService);
    });

    it('does not provide a DotMenuService of the app: the client lives in data-access', () => {
        const names = ENV_PROVIDERS.filter((provider) => typeof provider === 'function').map(
            (provider) => (provider as { name: string }).name
        );

        expect(names).not.toContain('DotMenuService');
    });

    it('binds the routes registrar the menu feature calls after every load', () => {
        const binding = ENV_PROVIDERS.find(
            (provider) =>
                typeof provider === 'object' &&
                provider !== null &&
                (provider as { provide?: unknown }).provide === DOT_MENU_ROUTES_REGISTRAR
        );

        expect(binding).toBeDefined();
    });

    it.each([
        'portlets/shared/dot-content-types-listing/dot-content-types.component.ts',
        'portlets/shared/dot-content-types-edit/dot-content-types-edit.module.ts'
    ])('%s does not re-provide the menu or navigation service', (file) => {
        const source = read(file);

        expect(source).not.toMatch(/\bDotNavigationService\b/);
        expect(source).not.toMatch(/\bDotMenuService\b/);
    });

    it('has no copy of the legacy menu service in the app', () => {
        const offenders = productionSources()
            .filter(([, source]) => /api\/services\/dot-menu\.service['"]/.test(source))
            .map(([file]) => file);

        expect(offenders).toEqual([]);
    });
});

/**
 * Dynamic routes register in one place: the routes registrar the menu feature calls after each
 * load. The separate initializer was never called by production code and is gone.
 */
describe('dynamic route registration', () => {
    it('has no production code that imports DynamicRouteInitializerService', () => {
        const offenders = productionSources()
            .filter(
                ([file, source]) =>
                    /dynamic-route-initializer\.service/.test(source) &&
                    !file.includes('dynamic-route-initializer.service.ts')
            )
            .map(([file]) => file);

        expect(offenders).toEqual([]);
    });

    it('no longer ships the DynamicRouteInitializerService file', () => {
        expect(() => read('api/services/dynamic-route-initializer.service.ts')).toThrow();
    });
});
