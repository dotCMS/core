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
});
