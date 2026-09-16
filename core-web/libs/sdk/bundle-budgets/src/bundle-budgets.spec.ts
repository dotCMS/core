import { readFileSync } from 'node:fs';
import { join } from 'node:path';


import { modulesMatching, probe, ProbeResult, SDK_DIST, stageSdkPackages } from './bundle-probe.ts';
import { PROBES } from './probes.ts';

import budgets from '../budgets.json';

/**
 * Guards the consumption patterns from #37571 against the built packages in dist/.
 *
 * These assertions are about what a consumer actually downloads, which unit tests cannot
 * see: they run after the SDKs are built, bundle a one-line import of each public entry
 * point with esbuild, and check both what came along and how big it is.
 */
describe('SDK bundle budgets', () => {
    const results = new Map<string, ProbeResult>();

    beforeAll(async () => {
        stageSdkPackages([...new Set(PROBES.flatMap((spec) => spec.packages))]);

        for (const spec of PROBES) {
            results.set(spec.name, await probe(spec.name, spec.source));
        }
    });

    describe.each(PROBES)('$name', (spec) => {
        test('should not pull in unrelated modules', () => {
            const result = results.get(spec.name) as ProbeResult;
            const leaked = modulesMatching(result, spec.forbidden);

            expect(
                leaked,
                `${spec.name} shipped modules it should not:\n  ${leaked.join('\n  ')}`
            ).toEqual([]);
        });

        test('should stay within its gzip budget', () => {
            const result = results.get(spec.name) as ProbeResult;
            const budget = (budgets as Record<string, number>)[spec.name];

            expect(budget, `No budget declared for ${spec.name} in budgets.json`).toBeDefined();
            expect(
                result.gzipBytes,
                `${spec.name} is ${result.gzipBytes} bytes gzipped, over its ${budget} byte budget. ` +
                    'Reduce it, or raise the budget in budgets.json with a reason in the PR.'
            ).toBeLessThanOrEqual(budget);
        });

        test('should resolve to ESM artifacts, not a CommonJS bridge', () => {
            const result = results.get(spec.name) as ProbeResult;
            const cjs = result.modules.filter(
                (id) => id.includes('@dotcms/') && /\.cjs(\.|$)/.test(id)
            );

            expect(
                cjs,
                `${spec.name} resolved through CommonJS artifacts:\n  ${cjs.join('\n  ')}`
            ).toEqual([]);
        });
    });

    // The layout renders through CSS modules that are injected by JavaScript, so they are the
    // one thing in @dotcms/react that must survive tree-shaking. If the sideEffects allow-list
    // in rollup.migrated.config.js ever becomes a blanket `false`, the grid silently disappears
    // and every other assertion here still passes.
    test('should keep the layout stylesheets in a layout bundle', () => {
        const result = results.get('react-layout-only') as ProbeResult;
        const styles = modulesMatching(result, ['Row.module.css', 'Column.module.css']);

        expect(
            styles.length,
            'The layout bundle no longer contains the Row/Column stylesheets — check the ' +
                'sideEffects field in the published @dotcms/react package.json.'
        ).toBeGreaterThanOrEqual(2);
    });
});

describe('SDK export conditions', () => {
    const readPackageJson = (pkg: string) =>
        JSON.parse(readFileSync(join(SDK_DIST, pkg, 'package.json'), 'utf-8'));

    describe.each(['client', 'uve', 'types', 'react', 'analytics'])('@dotcms/%s', (pkg) => {
        test('should resolve `import` to an ESM artifact', () => {
            const { exports } = readPackageJson(pkg);

            for (const [subpath, conditions] of Object.entries(exports)) {
                if (subpath === './package.json' || typeof conditions !== 'object') {
                    continue;
                }

                const target = (conditions as Record<string, string>).import;

                expect(target, `${pkg} ${subpath} declares no import condition`).toBeDefined();
                expect(
                    target,
                    `${pkg} ${subpath} resolves ESM imports to ${target}. Nx's *.cjs.mjs bridge ` +
                        'is backed by a single non-analysable CommonJS file and cannot be tree-shaken.'
                ).not.toMatch(/\.cjs\.mjs$/);
            }
        });

        test('should declare `types` before any other condition', () => {
            const { exports } = readPackageJson(pkg);

            for (const [subpath, conditions] of Object.entries(exports)) {
                if (subpath === './package.json' || typeof conditions !== 'object') {
                    continue;
                }

                const keys = Object.keys(conditions as Record<string, string>);

                if (!keys.includes('types')) {
                    continue;
                }

                expect(
                    keys[0],
                    `${pkg} ${subpath} lists conditions as [${keys.join(', ')}]. Resolvers take the ` +
                        'first match, so anything before `types` makes the type declarations unreachable.'
                ).toBe('types');
            }
        });

        test('should declare sideEffects', () => {
            const pkgJson = readPackageJson(pkg);

            expect(
                pkgJson.sideEffects,
                `${pkg} publishes no sideEffects field, so bundlers must assume every module has ` +
                    'side effects and keep it.'
            ).toBeDefined();
        });
    });
});
