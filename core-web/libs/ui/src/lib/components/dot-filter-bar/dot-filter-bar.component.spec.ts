import { byTestId, createHostFactory, SpectatorHost } from '@openng/spectator/jest';

import { signal, WritableSignal } from '@angular/core';

import { DotMessageService } from '@dotcms/data-access';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotFilterBarComponent } from './dot-filter-bar.component';
import { DOT_FILTER_FACADE, DotFilterFacade } from './filter-facade.token';

describe('DotFilterBarComponent', () => {
    let spectator: SpectatorHost<DotFilterBarComponent>;
    let hasNonDefaultFilters: WritableSignal<boolean>;
    const clearFilters = jest.fn();

    const createHost = createHostFactory({
        component: DotFilterBarComponent,
        providers: [
            {
                provide: DotMessageService,
                useValue: new MockDotMessageService({
                    'content-drive.filters.clear-all': 'Clear all'
                })
            }
        ]
    });

    const setup = (nonDefault: boolean, content = '<span data-testid="a-chip"></span>') => {
        hasNonDefaultFilters = signal(nonDefault);

        const facade: DotFilterFacade = {
            getFilterValue: jest.fn(() => undefined),
            patchFilters: jest.fn(),
            removeFilter: jest.fn(),
            clearFilters,
            $hasNonDefaultFilters: hasNonDefaultFilters
        };

        spectator = createHost(`<dot-filter-bar>${content}</dot-filter-bar>`, {
            providers: [{ provide: DOT_FILTER_FACADE, useValue: facade }]
        });
        spectator.detectChanges();
    };

    afterEach(() => jest.clearAllMocks());

    describe('projection', () => {
        it('should render whatever chips it is given', () => {
            setup(false);

            expect(spectator.query(byTestId('a-chip'))).toBeTruthy();
        });

        it('should render them in the order they were projected', () => {
            // The bar owns no chip registry — the rendered order IS the surface's template order,
            // which is what lets Content Drive project its portlet-local chips into a shared row.
            setup(
                false,
                '<span data-filter-chip="sharedAssets"></span><span data-filter-chip="status"></span>'
            );

            const chips = Array.from(spectator.element.querySelectorAll('[data-filter-chip]')).map(
                (element) => element.getAttribute('data-filter-chip')
            );

            expect(chips).toEqual(['sharedAssets', 'status']);
        });

        it('should not require any chips at all', () => {
            setup(false, '');

            expect(spectator.query(byTestId('dot-filter-bar'))).toBeTruthy();
        });
    });

    describe('clear all', () => {
        it('should not offer it while everything is at its default', () => {
            // The seeded defaults are always present, so a bar that counted filter keys would
            // offer to clear a surface nobody has filtered.
            setup(false);

            expect(spectator.query(byTestId('clear-all-filters'))).toBeFalsy();
        });

        it('should offer it once something differs from the default', () => {
            setup(true);

            expect(spectator.query(byTestId('clear-all-filters'))).toBeTruthy();
        });

        it('should appear and disappear as the facade reports changes', () => {
            setup(false);

            hasNonDefaultFilters.set(true);
            spectator.detectChanges();
            expect(spectator.query(byTestId('clear-all-filters'))).toBeTruthy();

            hasNonDefaultFilters.set(false);
            spectator.detectChanges();
            expect(spectator.query(byTestId('clear-all-filters'))).toBeFalsy();
        });

        it('should clear through the facade when clicked', () => {
            setup(true);

            spectator.click(
                spectator.query(byTestId('clear-all-filters'))?.querySelector('button') as Element
            );

            expect(clearFilters).toHaveBeenCalledTimes(1);
        });
    });

    describe('layout', () => {
        it('should wrap rather than overflow, so a long chip row stays reachable', () => {
            // FR-016. The dialog the picker lives in is narrower than Content Drive's page, and a
            // row that overflowed would put chips out of reach with no horizontal scroll.
            setup(false);

            expect(spectator.query(byTestId('dot-filter-bar'))?.className).toContain('flex-wrap');
        });
    });
});
