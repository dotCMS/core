import { byTestId, createComponentFactory, Spectator } from '@openng/spectator/jest';

import { signal } from '@angular/core';

import { DotMessageService } from '@dotcms/data-access';
import { MockDotMessageService } from '@dotcms/utils-testing';

import {
    SHARED_ASSETS_DISABLED_VALUE,
    SHARED_ASSETS_ENABLED_VALUE,
    SHARED_ASSETS_FILTER_KEY
} from './constants';
import { DotSharedAssetsFilterComponent } from './dot-shared-assets-filter.component';

import { DotChipFilterComponent } from '../../../dot-chip-filter/dot-chip-filter.component';
import { DOT_FILTER_FACADE, DotFilterFacade } from '../../filter-facade.token';

describe('DotSharedAssetsFilterComponent', () => {
    let spectator: Spectator<DotSharedAssetsFilterComponent>;

    /**
     * Backs the mocked `getFilterValue`. A signal, not a `mockReturnValue`: the component reads it
     * through a computed, so without a signal dependency there is nothing to invalidate and the
     * first value would be cached for the life of the test.
     */
    const storedValue = signal<string | undefined>(SHARED_ASSETS_ENABLED_VALUE);

    const patchFilters = jest.fn();
    const removeFilter = jest.fn();

    const facade: DotFilterFacade = {
        getFilterValue: jest.fn(() => storedValue()),
        patchFilters,
        removeFilter,
        clearFilters: jest.fn(),
        $hasNonDefaultFilters: signal(false)
    };

    const createComponent = createComponentFactory({
        component: DotSharedAssetsFilterComponent,
        providers: [
            { provide: DOT_FILTER_FACADE, useValue: facade },
            {
                provide: DotMessageService,
                useValue: new MockDotMessageService({
                    'content-drive.shared-assets-filter.title': 'Show Shared Assets',
                    'dot.common.remove': 'Remove'
                })
            }
        ]
    });

    const chip = () => spectator.query(DotChipFilterComponent);

    /** The chip is the whole control, so every interaction goes through its outputs. */
    const clickChip = () => {
        spectator.triggerEventHandler(DotChipFilterComponent, 'clicked', new Event('click'));
        spectator.detectChanges();
    };

    const removeChip = () => {
        spectator.triggerEventHandler(DotChipFilterComponent, 'removed', undefined);
        spectator.detectChanges();
    };

    const setStoredValue = (value: string | undefined) => {
        storedValue.set(value);
        spectator.detectChanges();
    };

    beforeEach(() => {
        storedValue.set(SHARED_ASSETS_ENABLED_VALUE);
        spectator = createComponent();
        spectator.detectChanges();
    });

    afterEach(() => jest.clearAllMocks());

    it('should render a toggle chip labelled from the message bundle', () => {
        expect(spectator.query(byTestId('shared-assets-filter-chip'))).toBeTruthy();
        expect(chip()?.mode()).toBe('toggle');
        expect(chip()?.title()).toBe('Show Shared Assets');
    });

    it('should identify itself for the canonical order check', () => {
        // The per-toolbar ordering test reads this attribute; without it the chip is invisible to
        // the assertion that both surfaces present their filters in the same order.
        // The attribute is a host binding, so it lives on the component's own element — a
        // descendant query would never see it.
        expect(spectator.element.getAttribute('data-filter-chip')).toBe('sharedAssets');
    });

    describe('state', () => {
        it('should read as on when the filter holds the enabled value', () => {
            setStoredValue(SHARED_ASSETS_ENABLED_VALUE);

            expect(chip()?.toggled()).toBe(true);
        });

        it('should read as on when the filter is absent, which is the default', () => {
            setStoredValue(undefined);

            expect(chip()?.toggled()).toBe(true);
        });

        it('should read as off only when the filter explicitly disables it', () => {
            setStoredValue(SHARED_ASSETS_DISABLED_VALUE);

            expect(chip()?.toggled()).toBe(false);
        });

        it('should read the state through the facade, not from any store', () => {
            expect(facade.getFilterValue).toHaveBeenCalledWith(SHARED_ASSETS_FILTER_KEY);
        });
    });

    describe('toggling', () => {
        it('should turn shared assets off when clicked while on', () => {
            setStoredValue(SHARED_ASSETS_ENABLED_VALUE);

            clickChip();

            expect(patchFilters).toHaveBeenCalledWith({
                [SHARED_ASSETS_FILTER_KEY]: SHARED_ASSETS_DISABLED_VALUE
            });
        });

        it('should turn shared assets back on when clicked while off', () => {
            setStoredValue(SHARED_ASSETS_DISABLED_VALUE);

            clickChip();

            expect(patchFilters).toHaveBeenCalledWith({
                [SHARED_ASSETS_FILTER_KEY]: SHARED_ASSETS_ENABLED_VALUE
            });
        });

        it('should write the on value rather than dropping the key', () => {
            // Content Drive's URL has to state the applied value rather than imply it by absence,
            // and a chip that removed the key instead would break that round-trip.
            setStoredValue(SHARED_ASSETS_DISABLED_VALUE);

            clickChip();

            expect(removeFilter).not.toHaveBeenCalled();
        });

        it('should turn shared assets off when the chip is removed', () => {
            setStoredValue(SHARED_ASSETS_ENABLED_VALUE);

            removeChip();

            expect(patchFilters).toHaveBeenCalledWith({
                [SHARED_ASSETS_FILTER_KEY]: SHARED_ASSETS_DISABLED_VALUE
            });
        });
    });
});
