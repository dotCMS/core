import { byTestId, createComponentFactory, Spectator } from '@openng/spectator/jest';

import { signal } from '@angular/core';

import { DotMessageService } from '@dotcms/data-access';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { CONTENT_STATUS, STATUS_FILTER_KEY } from './constants';
import { DotStatusFilterComponent } from './dot-status-filter.component';

import { DotChipFilterComponent } from '../../../dot-chip-filter/dot-chip-filter.component';
import { DOT_FILTER_FACADE, DotFilterFacade } from '../../filter-facade.token';

describe('DotStatusFilterComponent', () => {
    let spectator: Spectator<DotStatusFilterComponent>;

    /** A signal, so the component's computed sees changes. */
    const storedValue = signal<string | string[] | undefined>(undefined);
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
        component: DotStatusFilterComponent,
        providers: [
            { provide: DOT_FILTER_FACADE, useValue: facade },
            {
                provide: DotMessageService,
                useValue: new MockDotMessageService({
                    'content-drive.status-filter.title': 'Status',
                    'content-drive.status-filter.archived': 'Archived',
                    'content-drive.status-filter.unpublished': 'Unpublished',
                    'content-drive.status-filter.locked': 'Locked',
                    'content-drive.status-filter.bound': 'Unavailable for published-only browsing'
                })
            }
        ]
    });

    const openPanel = () => {
        spectator.triggerEventHandler(DotChipFilterComponent, 'clicked', new Event('click'));
        spectator.detectChanges();
    };

    const offeredOptions = () =>
        Array.from(spectator.queryAll('[data-testid^="status-option-"]')).map((element) =>
            element.getAttribute('data-testid')?.replace('status-option-', '')
        );

    beforeEach(() => {
        storedValue.set(undefined);
    });

    afterEach(() => jest.clearAllMocks());

    it('should identify itself for the canonical order check', () => {
        spectator = createComponent();
        spectator.detectChanges();

        expect(spectator.element.getAttribute('data-filter-chip')).toBe('status');
    });

    describe('selection through the facade', () => {
        beforeEach(() => {
            spectator = createComponent();
            spectator.detectChanges();
        });

        it('should read the selection from the status filter key', () => {
            expect(facade.getFilterValue).toHaveBeenCalledWith(STATUS_FILTER_KEY);
        });

        it('should write the selection to the facade, not to any store', () => {
            spectator.component.onSelectionChange([CONTENT_STATUS.ARCHIVED]);
            spectator.detectChanges();

            expect(patchFilters).toHaveBeenCalledWith({
                [STATUS_FILTER_KEY]: [CONTENT_STATUS.ARCHIVED]
            });
        });

        it('should remove the key rather than write an empty selection', () => {
            storedValue.set([CONTENT_STATUS.LOCKED]);
            spectator.detectChanges();

            spectator.component.onSelectionChange([]);
            spectator.detectChanges();

            expect(removeFilter).toHaveBeenCalledWith(STATUS_FILTER_KEY);
        });

        it('should combine selections rather than replace them one at a time', () => {
            // OR-combined: more boxes means more content, same as content types and locales.
            spectator.component.onSelectionChange([
                CONTENT_STATUS.UNPUBLISHED,
                CONTENT_STATUS.LOCKED
            ]);
            spectator.detectChanges();

            expect(patchFilters).toHaveBeenCalledWith({
                [STATUS_FILTER_KEY]: [CONTENT_STATUS.UNPUBLISHED, CONTENT_STATUS.LOCKED]
            });
        });
    });

    describe('allowedOptions bound (FR-014d)', () => {
        it('should offer all three conditions when nothing is bound', () => {
            spectator = createComponent();
            spectator.setInput('allowedOptions', null);
            spectator.detectChanges();
            openPanel();

            expect(offeredOptions()).toEqual([
                CONTENT_STATUS.ARCHIVED,
                CONTENT_STATUS.UNPUBLISHED,
                CONTENT_STATUS.LOCKED
            ]);
        });

        it('should offer only what the bound admits', () => {
            // A picker pinned to published content can only honour Locked: neither Archived nor
            // Unpublished has a published version, so offering them would force the whole query
            // onto the working version and describe content by a version nobody asked for.
            spectator = createComponent();
            spectator.setInput('allowedOptions', [CONTENT_STATUS.LOCKED]);
            spectator.detectChanges();
            openPanel();

            expect(offeredOptions()).toEqual([CONTENT_STATUS.LOCKED]);
        });

        it('should say why the others are unavailable rather than just showing a shorter list', () => {
            // FR-014e: a silently shorter list reads as "these states do not exist", not as "how
            // this picker was opened rules them out".
            spectator = createComponent();
            spectator.setInput('allowedOptions', [CONTENT_STATUS.LOCKED]);
            spectator.detectChanges();
            openPanel();

            expect(spectator.query(byTestId('status-filter-bound-note'))).toBeTruthy();
        });

        it('should not show the note when nothing is bound', () => {
            spectator = createComponent();
            spectator.setInput('allowedOptions', null);
            spectator.detectChanges();
            openPanel();

            expect(spectator.query(byTestId('status-filter-bound-note'))).toBeFalsy();
        });

        it('should drop a stored selection the bound no longer admits', () => {
            // Restored or stale state must not keep applying a condition the control cannot offer,
            // or the request carries a filter the editor has no way to see or clear.
            storedValue.set([CONTENT_STATUS.ARCHIVED, CONTENT_STATUS.LOCKED]);
            spectator = createComponent();
            spectator.setInput('allowedOptions', [CONTENT_STATUS.LOCKED]);
            spectator.detectChanges();

            expect(patchFilters).toHaveBeenCalledWith({
                [STATUS_FILTER_KEY]: [CONTENT_STATUS.LOCKED]
            });
        });
    });
});
