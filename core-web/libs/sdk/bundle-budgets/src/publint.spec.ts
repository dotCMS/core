import { publint } from 'publint';
import { formatMessage } from 'publint/utils';

import { readFileSync } from 'node:fs';
import { join } from 'node:path';

import { SDK_DIST } from './bundle-probe.ts';

/**
 * Runs publint against every built package.
 *
 * publint checks the things a bundle probe cannot see: whether the exports map resolves,
 * whether declared files were actually emitted, and whether a package's module type matches
 * the code it ships. It caught two real defects when it was introduced — `types` shadowed by
 * an earlier condition in the Nx-generated maps, and `@dotcms/analytics` shipping ESM with no
 * `"type": "module"`, which made Node reparse every file on import.
 *
 * Errors and warnings both fail. That matters: the analytics defect above was reported as a
 * *warning* (`pkg.main is written in ESM, but is interpreted as CJS`), so gating on errors
 * alone would have let exactly the bug that justified this tool straight through.
 *
 * Known findings we have consciously accepted are listed in ACCEPTED, each with a reason.
 * Suggestions never fail.
 */

/** publint codes we have looked at and decided not to act on. */
const ACCEPTED: Record<string, string> = {
    // Every package in the repo points at the monorepo root with a #main fragment. Cosmetic,
    // repo-wide, and not something this PR should change unilaterally.
    INVALID_REPOSITORY_VALUE: 'repo-wide convention, tracked separately',
    // Fixing this means emitting .d.mts alongside .d.ts and splitting `types` per condition.
    // Real, but a build change wider than this work — the types still resolve today.
    EXPORTS_TYPES_INVALID_FORMAT: 'needs .d.mts output; types resolve correctly today'
};
describe('publint', () => {
    const PACKAGES = ['react', 'client', 'uve', 'types', 'analytics'];

    describe.each(PACKAGES)('@dotcms/%s', (name) => {
        test('should publish a valid package', async () => {
            const pkgDir = join(SDK_DIST, name);
            const pkg = JSON.parse(readFileSync(join(pkgDir, 'package.json'), 'utf-8'));

            const { messages } = await publint({ pkgDir, level: 'suggestion' });
            const format = (message: (typeof messages)[number]) =>
                formatMessage(message, pkg) ?? message.code;

            const blocking = messages.filter(
                (message) =>
                    (message.type === 'error' || message.type === 'warning') &&
                    !(message.code in ACCEPTED)
            );

            expect(
                blocking.map(format),
                `publint flagged @dotcms/${name}. If a finding is genuinely acceptable, add its ` +
                    'code to ACCEPTED with the reason rather than widening the filter.'
            ).toEqual([]);
        });
    });
});
