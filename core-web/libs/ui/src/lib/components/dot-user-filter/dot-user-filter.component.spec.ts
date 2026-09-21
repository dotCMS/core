import {
    byTestId,
    createComponentFactory,
    mockProvider,
    Spectator
} from '@openng/spectator/vitest';
import { NEVER, of } from 'rxjs';
import { vi } from 'vitest';

import { DotMessageService, DotUserSearchService } from '@dotcms/data-access';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotUserFilterComponent } from './dot-user-filter.component';

const directoryPage = (userIds: string[], totalEntries: number) => ({
    entity: userIds.map((userId) => ({ userId, fullName: `Name ${userId}` })),
    pagination: { currentPage: 1, perPage: 20, totalEntries }
});

describe('DotUserFilterComponent', () => {
    let spectator: Spectator<DotUserFilterComponent>;
    let searchService: DotUserSearchService;

    const createComponent = createComponentFactory({
        component: DotUserFilterComponent,
        providers: [
            mockProvider(DotUserSearchService, {
                searchPage: vi.fn().mockReturnValue(of(directoryPage(['u1', 'u2'], 2))),
                resolveNames: vi.fn().mockReturnValue(of({}))
            }),
            {
                provide: DotMessageService,
                useValue: new MockDotMessageService({
                    'dot.common.remove': 'Remove',
                    'content-drive.chip-filter.overflow-label': '{0} and {1} more',
                    'experiments.list.filter.created-by': 'Created By',
                    'experiments.list.filter.all': 'All'
                })
            }
        ],
        detectChanges: false
    });

    const open = (): void => {
        spectator.click(spectator.query(byTestId('user-filter-chip')) as HTMLElement);
        spectator.detectChanges();
    };

    beforeEach(() => {
        // Cast because the inputs are aliased signal inputs, which Spectator types by property
        // name rather than alias.
        spectator = createComponent({
            props: { title: 'Created By', emptyLabel: 'All' } as never
        });
        searchService = spectator.inject(DotUserSearchService);

        // Re-armed per test, not once in the factory. The factory's `vi.fn()`s are shared across
        // every test in the file and this suite does not restore mocks, so a `mockReturnValue` set
        // inside one test leaks into the next and a call count never goes back to zero.
        (searchService.searchPage as ReturnType<typeof vi.fn>)
            .mockReset()
            .mockReturnValue(of(directoryPage(['u1', 'u2'], 2)));
        (searchService.resolveNames as ReturnType<typeof vi.fn>)
            .mockReset()
            .mockReturnValue(of({}));

        spectator.detectChanges();
    });

    /**
     * The component owns the LOADER, not the paging. Scrolling, the page counter, cancellation
     * and the stop at the last page all live in the shared lazy multi-select, which has its own
     * specs. What is ours is the loader contract: the params it sends, the shape it returns, and
     * how it decides whether another page exists.
     */
    describe('the loader handed to the option list', () => {
        const load = (page = 1, filter = '') =>
            spectator.component.loadPage({ page, perPage: 20, filter });

        it('should ask the directory for the requested page and term', () => {
            load(2, 'jane').subscribe();

            expect(searchService.searchPage).toHaveBeenCalledWith({
                page: 2,
                perPage: 20,
                filter: 'jane'
            });
        });

        it('should map rows to options keyed by user id', () => {
            let page: { options: { value: string; label: string }[]; hasMore: boolean } | undefined;
            load().subscribe((result) => (page = result));

            // Value is the id because that is what the filter matches on and what the address
            // stores; the label is only ever shown.
            expect(page?.options).toEqual([
                { value: 'u1', label: 'Name u1' },
                { value: 'u2', label: 'Name u2' }
            ]);
        });

        it('should report more pages while the total exceeds what has been served', () => {
            (searchService.searchPage as ReturnType<typeof vi.fn>).mockReturnValue(
                of(directoryPage(['u1', 'u2'], 57))
            );

            let page: { hasMore: boolean } | undefined;
            load(1).subscribe((result) => (page = result));

            expect(page?.hasMore).toBe(true);
        });

        it('should report no more pages once the total has been served', () => {
            // 2 of 2 in hand: this is what stops the scroll asking for ever (FR-011).
            let page: { hasMore: boolean } | undefined;
            load(1).subscribe((result) => (page = result));

            expect(page?.hasMore).toBe(false);
        });

        it('should not load anything before the popover is opened', () => {
            // The virtual scroller measures a zero-height viewport if created while hidden, and
            // renders nothing at all (FR-016).
            expect(searchService.searchPage).not.toHaveBeenCalled();
        });

        it('should show no counts beside the options', () => {
            open();

            spectator.queryAll(byTestId('lazy-multiselect-option')).forEach((option) => {
                expect(option.textContent).not.toMatch(/\d/);
            });
        });
    });

    describe('the selection', () => {
        it('should emit the selected ids, not the labels', () => {
            const emitted = vi.fn();
            spectator.output('selectionChange').subscribe(emitted);

            spectator.component.onSelectionChange([
                { value: 'u1', label: 'Name u1' },
                { value: 'u2', label: 'Name u2' }
            ]);

            expect(emitted).toHaveBeenCalledWith(['u1', 'u2']);
        });

        it('should clear the whole selection from the chip remove control', () => {
            const emitted = vi.fn();
            spectator.setInput('selected', ['u1', 'u2']);
            spectator.output('selectionChange').subscribe(emitted);

            spectator.component.onRemoveAll();

            expect(emitted).toHaveBeenCalledWith([]);
        });

        it('should keep a selected user ticked no matter what the visible page holds', () => {
            // The values handed to the option list come straight off the input, so a search that
            // replaces every visible row cannot un-tick a selection: there is no local copy to
            // fall out of step. Searching itself belongs to the shared list and is tested there.
            spectator.setInput('selected', ['u1']);
            spectator.detectChanges();

            expect(spectator.component.$selectedValues()).toContain('u1');
        });
    });

    describe('the chip label', () => {
        it('should read the empty label while nothing is selected', () => {
            expect(spectator.query(byTestId('chip-empty-label'))?.textContent).toContain('All');
        });

        it('should resolve ids arriving from the address into names', () => {
            (searchService.resolveNames as ReturnType<typeof vi.fn>).mockReturnValue(
                of({ u1: 'Jane Doe' })
            );

            spectator.setInput('selected', ['u1']);
            spectator.detectChanges();

            expect(searchService.resolveNames).toHaveBeenCalledWith(['u1']);
            expect(spectator.query(byTestId('chip-values'))?.textContent).toContain('Jane Doe');
        });

        it('should show a loading state while the names are being resolved', () => {
            // NEVER, not an error: `throwError` emits synchronously on subscribe, so the flag
            // would already be back down by the time this assertion runs. An observable that
            // never settles is the only way to observe the in-flight state at all.
            (searchService.resolveNames as ReturnType<typeof vi.fn>).mockReturnValue(NEVER);

            spectator.setInput('selected', ['u1']);

            // Asserted before the resolution settles: the chip says it is working rather than
            // rendering an id it is about to replace (FR-009e).
            expect(spectator.component.$resolving()).toBe(true);
        });

        it('should fall back to the id when a name cannot be resolved', () => {
            (searchService.resolveNames as ReturnType<typeof vi.fn>).mockReturnValue(
                of({ u1: 'u1' })
            );

            spectator.setInput('selected', ['u1']);
            spectator.detectChanges();

            expect(spectator.query(byTestId('chip-values'))?.textContent).toContain('u1');
        });
    });
});
