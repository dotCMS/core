import { of, throwError } from 'rxjs';

import { TestBed } from '@angular/core/testing';

import { DotPropertiesService } from '@dotcms/data-access';
import { FEATURE_FLAG_NOT_FOUND, FeaturedFlags } from '@dotcms/dotcms-models';

import { readExperimentsPortletSwitch } from './experiments-portlet-switch.util';

/**
 * The entry-point switch's read contract (#37005, FR-012, FR-013, FR-015, SC-002).
 *
 * Three properties are asserted here and nowhere else, because getting any of them wrong is
 * invisible in the UI until a customer reports it:
 *
 * 1. **Only an explicit `true` turns it on.** A failed read, an absent key and any other value all
 *    resolve to `false`, which is the legacy behaviour FR-015 requires. The absent key is the one
 *    that needs the assertion most: the shared normaliser behind `getFreshFeatureFlag` maps a
 *    missing key to *enabled*, for flags that ship on, so reading through it would have switched
 *    the entry point over on a response that simply did not carry this flag.
 * 2. **Nothing is cached.** `getFeatureFlag` memoizes for the life of the SPA session, so an
 *    operator flipping the switch from Maintenance would not see it until a hard reload — SC-002
 *    gives them one minute without a restart.
 * 3. **It reads its own key**, never the visitor-facing experiments kill-switch.
 */
describe('readExperimentsPortletSwitch', () => {
    let propertiesService: jest.Mocked<
        Pick<DotPropertiesService, 'getKey' | 'getFreshFeatureFlag' | 'getFeatureFlag'>
    >;

    beforeEach(() => {
        propertiesService = {
            getKey: jest.fn(),
            getFreshFeatureFlag: jest.fn(),
            getFeatureFlag: jest.fn()
        } as unknown as jest.Mocked<
            Pick<DotPropertiesService, 'getKey' | 'getFreshFeatureFlag' | 'getFeatureFlag'>
        >;

        TestBed.configureTestingModule({
            providers: [{ provide: DotPropertiesService, useValue: propertiesService }]
        });
    });

    const read = () => TestBed.runInInjectionContext(() => readExperimentsPortletSwitch());

    /**
     * The endpoint answers strings; a boolean is accepted too, since the resource coerces some.
     *
     * Asserted synchronously rather than through `done`: `of(...)` emits on subscribe, and
     * `it.each` does not hand the callback a `done` argument.
     */
    it.each([
        ['true', true],
        [true, true],
        ['false', false],
        [false, false],
        ['TRUE', false],
        ['yes', false]
    ])('should resolve %p as %p', (value, expected) => {
        propertiesService.getKey.mockReturnValue(of(value));

        let resolved: boolean | undefined;
        read().subscribe((value) => (resolved = value));

        expect(resolved).toBe(expected);
    });

    /**
     * FR-015, the case the previous reader got wrong.
     *
     * `normalizeFlagValue` maps `FEATURE_FLAG_NOT_FOUND` to `true`, because most flags ship on and
     * a build that has not declared one yet should still show the feature. This one ships off, so
     * the same mapping would have turned the entry point on for any response missing the key. The
     * stock build cannot produce that — the property is declared, whitelisted and boolean-typed —
     * but the guarantee should not rest on config wiring.
     */
    it('should resolve false when the response does not carry the key', (done) => {
        propertiesService.getKey.mockReturnValue(of(FEATURE_FLAG_NOT_FOUND));

        read().subscribe((value) => {
            expect(value).toBe(false);
            done();
        });
    });

    it('should read FEATURE_FLAG_EXPERIMENTS_PORTLET, not the experiments kill-switch', (done) => {
        propertiesService.getKey.mockReturnValue(of('false'));

        read().subscribe(() => {
            expect(propertiesService.getKey).toHaveBeenCalledWith(
                FeaturedFlags.FEATURE_FLAG_EXPERIMENTS_PORTLET
            );
            expect(propertiesService.getKey).not.toHaveBeenCalledWith(
                FeaturedFlags.LOAD_FRONTEND_EXPERIMENTS
            );
            done();
        });
    });

    // FR-015. The failure must be swallowed into `false`, not propagated: an editor whose config
    // read failed still gets a working Experiments navigation item, pointing at the legacy screens.
    it('should resolve false when the read fails, rather than erroring', (done) => {
        propertiesService.getKey.mockReturnValue(throwError(() => new Error('config read failed')));

        read().subscribe({
            next: (value) => {
                expect(value).toBe(false);
                done();
            },
            error: () => done.fail('the switch read must not propagate the error (FR-015)')
        });
    });

    // SC-002. Asserting the *absence* of the memoizing readers as well as the presence of the raw
    // one: a helper that called both would pass a presence-only assertion while still serving a
    // stale value from the cache.
    it('should read uncached so an operator flip lands on the next gesture', (done) => {
        propertiesService.getKey.mockReturnValue(of('false'));

        read().subscribe(() => {
            expect(propertiesService.getKey).toHaveBeenCalledTimes(1);
            expect(propertiesService.getFeatureFlag).not.toHaveBeenCalled();
            done();
        });
    });
});
