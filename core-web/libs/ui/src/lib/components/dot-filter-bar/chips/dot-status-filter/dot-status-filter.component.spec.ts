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
            spectator.component['onSelectionChange']([CONTENT_STATUS.ARCHIVED]);
            spectator.detectChanges();

            expect(patchFilters).toHaveBeenCalledWith({
                [STATUS_FILTER_KEY]: [CONTENT_STATUS.ARCHIVED]
            });
        });

        it('should remove the key rather than write an empty selection', () => {
            storedValue.set([CONTENT_STATUS.LOCKED]);
            spectator.detectChanges();

            spectator.component['onSelectionChange']([]);
            spectator.detectChanges();

            expect(removeFilter).toHaveBeenCalledWith(STATUS_FILTER_KEY);
        });

        it('should combine selections rather than replace them one at a time', () => {
            // OR-combined: more boxes means more content, same as content types and locales.
            spectator.component['onSelectionChange']([
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

    // ── Restored from the spec deleted with `dot-content-drive-status-filter` (#6d in review).
    // Each of these guarded something that fails silently, which is why they are back rather than
    // treated as covered by the selection tests above.

    describe('rendering', () => {
        beforeEach(() => {
            spectator = createComponent();
            spectator.detectChanges();
        });

        it('should render the chip', () => {
            expect(spectator.query(byTestId('status-filter-chip'))).toBeTruthy();
        });

        it('should render every status option once the panel is open', () => {
            openPanel();

            expect(offeredOptions()).toEqual([
                CONTENT_STATUS.ARCHIVED,
                CONTENT_STATUS.UNPUBLISHED,
                CONTENT_STATUS.LOCKED
            ]);
        });

        it('should carry an accessible label on the chip', () => {
            // The chip's visible text is a bare title; without this a screen reader announces a
            // button with no indication of what it filters.
            expect(
                spectator.query(byTestId('status-filter-chip'))?.getAttribute('aria-label')
            ).toBeTruthy();
        });

        it('should show a restored selection as chip values', () => {
            storedValue.set([CONTENT_STATUS.ARCHIVED]);
            spectator = createComponent();
            spectator.detectChanges();

            expect(spectator.query(DotChipFilterComponent)?.selections()).toEqual(['Archived']);
        });
    });

    it('should select when the LABEL is clicked, not just the checkbox', () => {
        // Regression guard: the listbox had [ngModel] but no (ngModelChange), so clicking the row
        // or its label did nothing and only the checkbox itself responded. The checkbox is
        // presentation only (`pointer-events-none`), so the row IS the click target.
        spectator = createComponent();
        spectator.detectChanges();
        openPanel();

        spectator
            .query(`[data-testid="status-option-${CONTENT_STATUS.ARCHIVED}"]`)
            ?.querySelector('dot-filter-list-item')
            ?.dispatchEvent(new MouseEvent('click', { bubbles: true }));
        spectator.detectChanges();

        expect(patchFilters).toHaveBeenCalledWith({
            [STATUS_FILTER_KEY]: [CONTENT_STATUS.ARCHIVED]
        });
    });

    /**
     * The write-back is a cycle: the chip writes through the facade, the facade's value feeds
     * `$selection`, and `$selection` is what triggers the write. The suites above hand back a
     * value the writes never reach, so that cycle cannot occur there — a passing mocked test says
     * nothing about it.
     *
     * These use a **stateful** facade whose `getFilterValue` reflects what was written, which is
     * the whole feedback path this component owns. (The deleted spec reached for Content Drive's
     * real store; a shared chip cannot import a portlet's, and it does not need to — what the
     * surface does with the write afterwards is covered by each facade's own conformance suite.)
     */
    describe('the write-back cycle, against a facade that answers with what it stored', () => {
        let bag: ReturnType<typeof signal<Record<string, string | string[]>>>;
        let writes: number;

        const statefulFacade = (): DotFilterFacade => ({
            getFilterValue: (key: string) => bag()[key],
            patchFilters: (patch) => {
                writes++;
                bag.update((current) => ({ ...current, ...patch }));
            },
            removeFilter: (key: string) => {
                writes++;
                bag.update((current) => {
                    const next = { ...current };
                    delete next[key];

                    return next;
                });
            },
            clearFilters: jest.fn(),
            $hasNonDefaultFilters: signal(false)
        });

        const createStateful = createComponentFactory({
            component: DotStatusFilterComponent,
            providers: [
                {
                    provide: DotMessageService,
                    useValue: new MockDotMessageService({
                        'content-drive.status-filter.archived': 'Archived',
                        'content-drive.status-filter.locked': 'Locked'
                    })
                }
            ]
        });

        beforeEach(() => {
            bag = signal<Record<string, string | string[]>>({});
            writes = 0;
            spectator = createStateful({
                providers: [{ provide: DOT_FILTER_FACADE, useFactory: statefulFacade }]
            });
            spectator.detectChanges();
        });

        /** Runs change detection until the graph stops producing work. */
        const settle = () => {
            spectator.detectChanges();
            spectator.detectChanges();
            spectator.detectChanges();

            return writes;
        };

        it('should persist a selection and then stop writing', () => {
            spectator.component.$selection.set([CONTENT_STATUS.ARCHIVED]);
            const afterSelection = settle();

            expect(bag()[STATUS_FILTER_KEY]).toEqual([CONTENT_STATUS.ARCHIVED]);
            // Termination is the property, not a write count: each write feeds the signal that
            // triggered it, so without value equality on `$selection` this climbs forever.
            expect(settle()).toBe(afterSelection);
        });

        it('should remove the key when the selection is emptied, and then stop writing', () => {
            spectator.component.$selection.set([CONTENT_STATUS.LOCKED]);
            settle();
            expect(bag()[STATUS_FILTER_KEY]).toEqual([CONTENT_STATUS.LOCKED]);

            spectator.component.$selection.set([]);
            const afterClearing = settle();

            expect(bag()[STATUS_FILTER_KEY]).toBeUndefined();
            expect(settle()).toBe(afterClearing);
        });

        it('should be inert on mount, when nothing was ever selected', () => {
            // The sync runs once on mount with an empty selection, which calls `removeFilter` for
            // a key that is not set. Both real surfaces no-op that — Content Drive guards on
            // `if (removedFilter)`, the picker on `if (!(filter in filters))` — so it costs
            // nothing. Pinned because if either guard went, mounting the chip would reset the
            // editor's page on every load, invisibly.
            expect(bag()[STATUS_FILTER_KEY]).toBeUndefined();
            expect(Object.keys(bag())).toEqual([]);
        });

        it('should not write again when an unrelated filter changes', () => {
            spectator.component.$selection.set([CONTENT_STATUS.ARCHIVED]);
            spectator.detectChanges();
            const afterSelection = writes;

            bag.update((current) => ({ ...current, title: 'something' }));
            spectator.detectChanges();

            // The selection is unchanged, and `$selection` compares by value, so the sync is inert.
            // A second write here would reset the surface's paging for nothing.
            expect(writes).toBe(afterSelection);
        });
    });
});
