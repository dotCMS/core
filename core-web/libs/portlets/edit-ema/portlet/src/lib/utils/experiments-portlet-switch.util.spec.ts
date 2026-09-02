import { of, throwError } from 'rxjs';

import { TestBed } from '@angular/core/testing';

import { DotPropertiesService } from '@dotcms/data-access';
import { FeaturedFlags } from '@dotcms/dotcms-models';

import { readExperimentsPortletSwitch } from './experiments-portlet-switch.util';

/**
 * The entry-point switch's read contract (#37005, FR-012, FR-013, FR-015, SC-002).
 *
 * Two properties are asserted here and nowhere else, because getting either wrong is invisible in
 * the UI until a customer reports it:
 *
 * 1. **A failed read resolves to `false`** — the legacy behaviour. `getFreshFeatureFlag` errors on
 *    a failed request rather than emitting, so without a `catchError` the navigation would break
 *    instead of falling back. FR-015 requires the safe side.
 * 2. **The uncached reader is used.** `getFeatureFlag` memoizes for the life of the SPA session, so
 *    an operator flipping the switch from Maintenance would not see it until a hard reload —
 *    SC-002 gives them one minute without a restart.
 */
describe('readExperimentsPortletSwitch', () => {
    let propertiesService: jest.Mocked<
        Pick<DotPropertiesService, 'getFreshFeatureFlag' | 'getFeatureFlag'>
    >;

    beforeEach(() => {
        propertiesService = {
            getFreshFeatureFlag: jest.fn(),
            getFeatureFlag: jest.fn()
        } as unknown as jest.Mocked<
            Pick<DotPropertiesService, 'getFreshFeatureFlag' | 'getFeatureFlag'>
        >;

        TestBed.configureTestingModule({
            providers: [{ provide: DotPropertiesService, useValue: propertiesService }]
        });
    });

    const read = () => TestBed.runInInjectionContext(() => readExperimentsPortletSwitch());

    it('should resolve true when the switch is on', (done) => {
        propertiesService.getFreshFeatureFlag.mockReturnValue(of(true));

        read().subscribe((value) => {
            expect(value).toBe(true);
            done();
        });
    });

    it('should resolve false when the switch is at its shipped default', (done) => {
        propertiesService.getFreshFeatureFlag.mockReturnValue(of(false));

        read().subscribe((value) => {
            expect(value).toBe(false);
            done();
        });
    });

    it('should read FEATURE_FLAG_EXPERIMENTS_PORTLET, not the experiments kill-switch', (done) => {
        propertiesService.getFreshFeatureFlag.mockReturnValue(of(false));

        read().subscribe(() => {
            expect(propertiesService.getFreshFeatureFlag).toHaveBeenCalledWith(
                FeaturedFlags.FEATURE_FLAG_EXPERIMENTS_PORTLET
            );
            expect(propertiesService.getFreshFeatureFlag).not.toHaveBeenCalledWith(
                FeaturedFlags.LOAD_FRONTEND_EXPERIMENTS
            );
            done();
        });
    });

    // FR-015. The failure must be swallowed into `false`, not propagated: an editor whose config
    // read failed still gets a working Experiments navigation item, pointing at the legacy screens.
    it('should resolve false when the read fails, rather than erroring', (done) => {
        propertiesService.getFreshFeatureFlag.mockReturnValue(
            throwError(() => new Error('config read failed'))
        );

        read().subscribe({
            next: (value) => {
                expect(value).toBe(false);
                done();
            },
            error: () => done.fail('the switch read must not propagate the error (FR-015)')
        });
    });

    // SC-002. Asserting the *absence* of the cached call as well as the presence of the fresh one:
    // a helper that called both would pass a presence-only assertion while still serving a stale
    // value from the cache.
    it('should use the uncached reader so an operator flip lands on the next gesture', (done) => {
        propertiesService.getFreshFeatureFlag.mockReturnValue(of(false));

        read().subscribe(() => {
            expect(propertiesService.getFreshFeatureFlag).toHaveBeenCalledTimes(1);
            expect(propertiesService.getFeatureFlag).not.toHaveBeenCalled();
            done();
        });
    });
});
