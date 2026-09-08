import { beforeEach, describe, expect, it, jest } from '@jest/globals';
import { byTestId, createComponentFactory, mockProvider, Spectator } from '@openng/spectator/jest';
import { of, throwError } from 'rxjs';

import { provideHttpClient } from '@angular/common/http';

import { AddToBundleService, DotCurrentUserService, DotMessageService } from '@dotcms/data-access';
import { DotBundle } from '@dotcms/dotcms-models';

import {
    DotContentDriveActionBundleTargetComponent,
    rememberLastBundleUsed,
    toBundle
} from './dot-content-drive-action-bundle-target.component';

const BUNDLES: DotBundle[] = [
    { id: 'bundle-1', name: 'Release 1' },
    { id: 'bundle-2', name: 'Release 2' }
];

describe('DotContentDriveActionBundleTargetComponent', () => {
    let spectator: Spectator<DotContentDriveActionBundleTargetComponent>;

    // A mutable mock rather than a per-test provider override: the module is already instantiated by
    // the time a test runs, so `createComponent({ providers })` would be refused.
    const getBundles = jest.fn();

    const createComponent = createComponentFactory({
        component: DotContentDriveActionBundleTargetComponent,
        providers: [
            provideHttpClient(),
            mockProvider(DotMessageService, {
                get: jest.fn().mockImplementation((key) => key as string)
            }),
            mockProvider(DotCurrentUserService)
        ],
        componentProviders: [mockProvider(AddToBundleService, { getBundles })],
        detectChanges: false
    });

    beforeEach(() => {
        sessionStorage.clear();
        getBundles.mockReturnValue(of(BUNDLES));
        spectator = createComponent({ props: { assetCount: 3 } });
        spectator.detectChanges();
    });

    it('should render the bundle select', () => {
        expect(spectator.query(byTestId('bundle-target-select'))).toBeTruthy();
    });

    it('should render no dialog of its own', () => {
        // The shell owns the one dialog; a second here would nest a modal inside it.
        expect(spectator.query('p-dialog')).toBeNull();
    });

    it('should load the current user unsent bundles', () => {
        expect(spectator.component['$bundles']()).toEqual(BUNDLES);
    });

    describe('choosing a bundle', () => {
        it('should emit an existing bundle as picked', () => {
            const emitted: (DotBundle | null)[] = [];
            spectator
                .output<DotBundle | null>('bundleChange')
                .subscribe((bundle) => emitted.push(bundle));

            spectator.component['onValueChange'](BUNDLES[1]);

            expect(emitted).toEqual([BUNDLES[1]]);
        });

        it('should turn a typed name into a new bundle', () => {
            // `editable` is what makes creation work: the endpoint creates a bundle when it can
            // resolve neither the id nor the name.
            const emitted: (DotBundle | null)[] = [];
            spectator
                .output<DotBundle | null>('bundleChange')
                .subscribe((bundle) => emitted.push(bundle));

            spectator.component['onValueChange']('Sprint 42');

            expect(emitted).toEqual([{ id: 'Sprint 42', name: 'Sprint 42' }]);
        });

        it('should emit nothing for a cleared select', () => {
            const emitted: (DotBundle | null)[] = [];
            spectator
                .output<DotBundle | null>('bundleChange')
                .subscribe((bundle) => emitted.push(bundle));

            spectator.component['onValueChange'](null);

            expect(emitted).toEqual([null]);
        });
    });

    describe('the remembered bundle', () => {
        it('should preselect the bundle last used when it is still unsent', () => {
            rememberLastBundleUsed({ id: 'bundle-2', name: 'Release 2' });

            spectator = createComponent({ props: { assetCount: 3 } });
            spectator.detectChanges();

            expect(spectator.component['$value']()).toEqual(BUNDLES[1]);
        });

        it('should match by name, since a bundle created by name stored its name as its id', () => {
            rememberLastBundleUsed({ id: 'Release 2', name: 'Release 2' });

            spectator = createComponent({ props: { assetCount: 3 } });
            spectator.detectChanges();

            expect(spectator.component['$value']()).toEqual(BUNDLES[1]);
        });

        it('should ignore a remembered bundle that is no longer in the list', () => {
            rememberLastBundleUsed({ id: 'gone', name: 'Already sent' });

            spectator = createComponent({ props: { assetCount: 3 } });
            spectator.detectChanges();

            expect(spectator.component['$value']()).toBeNull();
        });

        it('should survive corrupt storage', () => {
            sessionStorage.setItem('lastSelectedBundle', '{not json');

            spectator = createComponent({ props: { assetCount: 3 } });

            expect(() => spectator.detectChanges()).not.toThrow();
            expect(spectator.component['$value']()).toBeNull();
        });
    });

    describe('when the bundle list cannot be loaded', () => {
        it('should stay usable so a new bundle can still be named', () => {
            // Degrades to "new bundle only" rather than blocking the action behind an error.
            getBundles.mockReturnValue(throwError(() => new Error('boom')));

            spectator = createComponent({ props: { assetCount: 3 } });
            spectator.detectChanges();

            expect(spectator.component['$bundles']()).toEqual([]);
            expect(spectator.component['$loading']()).toBe(false);
            expect(spectator.query(byTestId('bundle-target-select'))).toBeTruthy();
        });
    });

    describe('the identifier collapse notice', () => {
        it('should stay hidden when no rows collapse', () => {
            expect(spectator.query(byTestId('bundle-target-collapsed'))).toBeNull();
        });

        it('should explain the smaller count when rows collapse', () => {
            // Said before the fact: the result reports the server's deduped count, and an
            // unexplained smaller number reads as a partial failure.
            spectator.setInput('collapsedCount', 2);
            spectator.detectChanges();

            expect(spectator.query(byTestId('bundle-target-collapsed'))).toBeTruthy();
        });
    });
});

describe('toBundle', () => {
    it('should pass an existing bundle through', () => {
        expect(toBundle(BUNDLES[0])).toEqual(BUNDLES[0]);
    });

    it('should make a typed name its own id', () => {
        expect(toBundle('Sprint 42')).toEqual({ id: 'Sprint 42', name: 'Sprint 42' });
    });

    it('should trim a typed name', () => {
        expect(toBundle('  Sprint 42  ')).toEqual({ id: 'Sprint 42', name: 'Sprint 42' });
    });

    it.each([
        ['null', null],
        ['unset', undefined],
        ['empty', ''],
        // Otherwise the endpoint would create a bundle actually named " ".
        ['whitespace only', '   ']
    ])('should return nothing for %s', (_label, value) => {
        expect(toBundle(value)).toBeNull();
    });

    it('should return nothing for a bundle with no name', () => {
        expect(toBundle({ id: 'x', name: '' })).toBeNull();
    });
});
