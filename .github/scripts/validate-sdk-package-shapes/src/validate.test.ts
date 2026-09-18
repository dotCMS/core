import { validateSdkLibPackageJson, validateExamplePackageJson } from './validate';

describe('validateSdkLibPackageJson', () => {
    it('flags a floating peerDependencies value (latest/next/*)', () => {
        const pkg = {
            name: '@dotcms/experiments',
            peerDependencies: {
                '@dotcms/client': 'latest',
                '@dotcms/react': 'next',
                '@dotcms/uve': '*',
                '@dotcms/types': '0.0.0'
            }
        };

        const violations = validateSdkLibPackageJson(pkg, 'experiments');

        expect(violations).toEqual(
            expect.arrayContaining([
                expect.stringContaining('@dotcms/client'),
                expect.stringContaining('@dotcms/react'),
                expect.stringContaining('@dotcms/uve')
            ])
        );
        expect(violations.some((v) => v.includes('@dotcms/types'))).toBe(false);
    });

    it('passes when every peerDependencies entry uses the 0.0.0 sentinel', () => {
        const pkg = {
            name: '@dotcms/experiments',
            peerDependencies: {
                '@dotcms/client': '0.0.0',
                '@dotcms/react': '0.0.0',
                '@dotcms/uve': '0.0.0',
                '@dotcms/types': '0.0.0'
            }
        };

        expect(validateSdkLibPackageJson(pkg, 'experiments')).toEqual([]);
    });

    it('flags @dotcms/client or @dotcms/uve reintroduced in dependencies for react/angular/vue/analytics', () => {
        const pkg = {
            name: '@dotcms/react',
            dependencies: {
                '@dotcms/uve': 'latest',
                '@dotcms/client': 'latest',
                '@tinymce/tinymce-react': '6.2.1'
            },
            devDependencies: { '@dotcms/types': 'latest' }
        };

        const violations = validateSdkLibPackageJson(pkg, 'react');

        expect(violations).toEqual(
            expect.arrayContaining([
                expect.stringContaining('@dotcms/uve'),
                expect.stringContaining('@dotcms/client')
            ])
        );
    });

    it('does not flag dependencies/devDependencies "latest" for packages other than the sibling-dependency guard (masked at publish)', () => {
        const pkg = {
            name: '@dotcms/react',
            dependencies: { '@tinymce/tinymce-react': '6.2.1' },
            devDependencies: { '@dotcms/types': 'latest' }
        };

        expect(validateSdkLibPackageJson(pkg, 'react')).toEqual([]);
    });

    it('does not apply the dependencies structural guard to packages other than react/angular/vue/analytics', () => {
        // client/uve only ever have @dotcms/types in devDependencies — never a dependencies-field concern
        const pkg = { name: '@dotcms/client', devDependencies: { '@dotcms/types': 'latest' } };

        expect(validateSdkLibPackageJson(pkg, 'client')).toEqual([]);
    });
});

describe('validateExamplePackageJson', () => {
    it('flags "next" on any branch', () => {
        const pkg = { dependencies: { '@dotcms/client': 'next', '@dotcms/uve': 'next' } };

        const violations = validateExamplePackageJson(pkg, 'main');

        expect(violations.length).toBeGreaterThan(0);
    });

    it('flags "*" on any branch', () => {
        const pkg = { dependencies: { '@dotcms/client': '*' } };

        expect(validateExamplePackageJson(pkg, 'main').length).toBeGreaterThan(0);
    });

    it('flags "latest" on a non-main (LTS/release) branch', () => {
        const pkg = { dependencies: { '@dotcms/client': 'latest' } };

        const violations = validateExamplePackageJson(pkg, 'release-25.07.10_lts_v12');

        expect(violations.length).toBeGreaterThan(0);
    });

    it('does NOT flag "latest" on main — intentional per ADR-0019 Evergreen alignment', () => {
        const pkg = {
            dependencies: { '@dotcms/client': 'latest', '@dotcms/uve': 'latest' }
        };

        expect(validateExamplePackageJson(pkg, 'main')).toEqual([]);
    });

    it('passes an exact pinned version on any branch', () => {
        const pkg = { dependencies: { '@dotcms/client': '1.2.0' } };

        expect(validateExamplePackageJson(pkg, 'release-25.07.10_lts_v12')).toEqual([]);
        expect(validateExamplePackageJson(pkg, 'main')).toEqual([]);
    });

    it('does NOT flag "latest" on master — cicd_1-pr.yml accepts PRs to main OR master', () => {
        const pkg = { dependencies: { '@dotcms/client': 'latest' } };

        expect(validateExamplePackageJson(pkg, 'master')).toEqual([]);
    });

    it.each(['^26.9.3-1', '~1.2.0', '>=1.0.0', '<2.0.0', '1.2.x', '1.0.0 - 2.0.0', '^1 || ^2'])(
        'flags the range "%s" on a release branch — a range drifts forward just like "latest"',
        (range) => {
            const pkg = { dependencies: { '@dotcms/client': range } };

            const violations = validateExamplePackageJson(pkg, 'release-25.07.10_lts_v12');

            expect(violations).toEqual([expect.stringContaining(range)]);
        }
    );

    it('passes an exact prerelease pin on a release branch', () => {
        // The shape deploy-javascript-sdk/action.yml writes: normalized CalVer, and the
        // `-next.<run>` form cicd_3-trunk.yml produces.
        const pkg = {
            dependencies: { '@dotcms/client': '26.8.3-1', '@dotcms/uve': '1.2.0-next.2632' }
        };

        expect(validateExamplePackageJson(pkg, 'release-26.08.03_lts_v1')).toEqual([]);
    });

    it('still allows ranges on trunk — only release branches require an exact pin', () => {
        const pkg = { dependencies: { '@dotcms/client': '^1.2.0' } };

        expect(validateExamplePackageJson(pkg, 'main')).toEqual([]);
    });

    it('ignores non-@dotcms dependencies on release branches', () => {
        const pkg = { dependencies: { next: '^14.0.0', react: '18.2.0' } };

        expect(validateExamplePackageJson(pkg, 'release-25.07.10_lts_v12')).toEqual([]);
    });
});
