import { byTestId, createComponentFactory, Spectator } from '@openng/spectator/vitest';
import { of, throwError } from 'rxjs';
import { Mock, vi } from 'vitest';

import { HttpErrorResponse } from '@angular/common/http';

import { Listbox } from 'primeng/listbox';

import { DotMessageService } from '@dotcms/data-access';
import { MockDotMessageService } from '@dotcms/utils-testing';

import {
    DotLazyMultiselectComponent,
    DotLazyMultiselectLoader,
    DotLazyMultiselectOption
} from './dot-lazy-multiselect.component';

import { LISTBOX_OPTION_HEIGHT } from '../../../../../theme/theme.config';
import { FIELD_FILTER_DEBOUNCE_TIME } from '../constants';

const page = (options: DotLazyMultiselectOption[], hasMore = false) => of({ options, hasMore });

/** Shorter than `FIELD_FILTER_DEBOUNCE_TIME`, so a default-bound pipe cannot pass by accident. */
const SHORT_DEBOUNCE_MS = 100;

describe('DotLazyMultiselectComponent', () => {
    let spectator: Spectator<DotLazyMultiselectComponent>;
    let loadPage: Mock;

    const createComponent = createComponentFactory({
        component: DotLazyMultiselectComponent,
        providers: [
            {
                provide: DotMessageService,
                useValue: new MockDotMessageService({})
            }
        ],
        detectChanges: false
    });

    const build = (loader: DotLazyMultiselectLoader, selectedValues: string[] = []) => {
        loadPage = loader as Mock;
        spectator = createComponent({
            props: { loadPage, selectedValues } as never
        });
        spectator.detectChanges();
    };

    afterEach(() => vi.clearAllMocks());

    it('should load the first page on init', () => {
        build(vi.fn().mockReturnValue(page([{ label: 'A', value: 'a' }])));

        expect(loadPage).toHaveBeenCalledWith({ page: 1, perPage: 20, filter: '' });
    });

    describe('search', () => {
        beforeEach(() => vi.useFakeTimers());
        afterEach(() => vi.useRealTimers());

        it('should reload from the first page with the typed filter (debounced)', () => {
            build(vi.fn().mockReturnValue(page([{ label: 'A', value: 'a' }], true)));
            loadPage.mockClear();

            const input = spectator.query(byTestId('lazy-multiselect-search')) as HTMLInputElement;
            spectator.typeInElement('ang', input);
            vi.advanceTimersByTime(FIELD_FILTER_DEBOUNCE_TIME);

            expect(loadPage).toHaveBeenCalledWith({ page: 1, perPage: 20, filter: 'ang' });
        });

        /**
         * The override has to be honoured per emission, not read once while the component is
         * built. Signal inputs still hold their defaults during construction — Angular binds them
         * afterwards — so a `debounceTime(this.$debounceMs())` in the constructor silently pins
         * every consumer to the default, and `debounceTime` captures its argument once anyway.
         *
         * A test that only advances by the default cannot see any of that, which is how the
         * override shipped inert (#37307): the user filter asked for 300ms and waited 500.
         */
        it('should honour a shorter debounce than the default', () => {
            build(vi.fn().mockReturnValue(page([{ label: 'A', value: 'a' }], true)));
            spectator.setInput('debounceMs', SHORT_DEBOUNCE_MS);
            spectator.detectChanges();
            loadPage.mockClear();

            const input = spectator.query(byTestId('lazy-multiselect-search')) as HTMLInputElement;
            spectator.typeInElement('ang', input);
            vi.advanceTimersByTime(SHORT_DEBOUNCE_MS);

            expect(loadPage).toHaveBeenCalledWith({ page: 1, perPage: 20, filter: 'ang' });
        });

        it('should still wait out the shorter debounce rather than searching on every keystroke', () => {
            build(vi.fn().mockReturnValue(page([{ label: 'A', value: 'a' }], true)));
            spectator.setInput('debounceMs', SHORT_DEBOUNCE_MS);
            spectator.detectChanges();
            loadPage.mockClear();

            const input = spectator.query(byTestId('lazy-multiselect-search')) as HTMLInputElement;
            spectator.typeInElement('ang', input);
            vi.advanceTimersByTime(SHORT_DEBOUNCE_MS - 1);

            expect(loadPage).not.toHaveBeenCalled();
        });
    });

    describe('a failed page', () => {
        /**
         * The inline state tells the person looking at the panel; the output tells the surface,
         * which is the only thing that can route a 403 or a dead session to wherever that surface
         * reports errors. Both, because neither substitutes for the other (FR-012).
         */
        it('should report the failure outwards as well as inline', () => {
            const failure = new HttpErrorResponse({ status: 403 });
            const failed = vi.fn();

            // Subscribed before the first load runs: `build` renders, which fires `ngOnInit`, and
            // an output emitted then is gone by the time a later subscriber arrives.
            loadPage = vi.fn().mockReturnValue(throwError(() => failure));
            spectator = createComponent({ props: { loadPage, selectedValues: [] } as never });
            spectator.output('loadFailed').subscribe(failed);
            spectator.detectChanges();

            expect(failed).toHaveBeenCalledWith(failure);
        });

        it('should still show its own failed state', () => {
            build(
                vi.fn().mockReturnValue(throwError(() => new HttpErrorResponse({ status: 500 })))
            );

            expect(spectator.component['$state'].error()).toBe(true);
        });
    });

    describe('infinite scroll', () => {
        /** A full page of `count` options, values prefixed so pages don't collide. */
        const fullPage = (prefix: string, count = 20) =>
            Array.from({ length: count }, (_, i) => ({
                label: `${prefix}${i}`,
                value: `${prefix}${i}`
            }));

        it('should load the next page when the scroller reaches the last loaded row', () => {
            build(vi.fn().mockReturnValue(page(fullPage('p1-'), true)));
            loadPage.mockClear();

            spectator.triggerEventHandler('p-listbox', 'onLazyLoad', { last: 19 });

            expect(loadPage).toHaveBeenCalledWith({ page: 2, perPage: 20, filter: '' });
        });

        it('should not load more while the scroller is still above the last row', () => {
            build(vi.fn().mockReturnValue(page(fullPage('p1-'), true)));
            loadPage.mockClear();

            spectator.triggerEventHandler('p-listbox', 'onLazyLoad', { last: 10 });

            expect(loadPage).not.toHaveBeenCalled();
        });

        it('should keep loading while short pages leave the first screen part-empty', () => {
            // A loader that hides rows: 5, then nothing, then a full page — with more each time.
            build(
                vi
                    .fn()
                    .mockReturnValueOnce(page(fullPage('a', 5), true))
                    .mockReturnValueOnce(page([], true))
                    .mockReturnValueOnce(page(fullPage('c'), true))
            );

            expect(loadPage).toHaveBeenCalledTimes(3);
            expect(loadPage).toHaveBeenLastCalledWith({ page: 3, perPage: 20, filter: '' });
            expect(spectator.component['$state'].options()).toHaveLength(25);
        });

        it('should keep advancing past short pages as the admin scrolls', () => {
            build(
                vi
                    .fn()
                    .mockReturnValueOnce(page(fullPage('a'), true))
                    .mockReturnValueOnce(page(fullPage('b', 3), true))
                    .mockReturnValueOnce(page(fullPage('c', 3), true))
                    .mockReturnValue(page(fullPage('d', 3), false))
            );

            spectator.triggerEventHandler('p-listbox', 'onLazyLoad', { last: 19 });
            spectator.triggerEventHandler('p-listbox', 'onLazyLoad', { last: 22 });
            // 26 rows on three pages. Inferring the page from the row index gives
            // `ceil(25 / 20) + 1 = 3`, which is the page already loaded, so the list stalled here.
            spectator.triggerEventHandler('p-listbox', 'onLazyLoad', { last: 25 });

            expect(loadPage).toHaveBeenLastCalledWith({ page: 4, perPage: 20, filter: '' });
        });

        it('should not load more once the loader reports no further pages', () => {
            build(vi.fn().mockReturnValue(page([{ label: 'A', value: 'a' }], false)));
            loadPage.mockClear();

            spectator.triggerEventHandler('p-listbox', 'onLazyLoad', { last: 20 });

            expect(loadPage).not.toHaveBeenCalled();
        });
    });

    describe('selection', () => {
        it('should emit the chosen options (value + label) on change', () => {
            build(
                vi.fn().mockReturnValue(
                    page([
                        { label: 'Angular', value: 'a' },
                        { label: 'Backend', value: 'b' }
                    ])
                )
            );
            const emitted: DotLazyMultiselectOption[][] = [];
            spectator.component.selectionChange.subscribe((value) => emitted.push(value));

            spectator.triggerEventHandler('p-listbox', 'onChange', { value: ['a', 'b'] });

            expect(emitted).toEqual([
                [
                    { label: 'Angular', value: 'a' },
                    { label: 'Backend', value: 'b' }
                ]
            ]);
        });

        it('should fall back to the raw value as label for an unknown value', () => {
            build(vi.fn().mockReturnValue(page([{ label: 'Angular', value: 'a' }])));
            const emitted: DotLazyMultiselectOption[][] = [];
            spectator.component.selectionChange.subscribe((value) => emitted.push(value));

            spectator.triggerEventHandler('p-listbox', 'onChange', { value: ['unknown'] });

            expect(emitted).toEqual([[{ label: 'unknown', value: 'unknown' }]]);
        });

        it('should keep an earlier page label after a search reset', () => {
            vi.useFakeTimers();
            build(
                vi
                    .fn()
                    .mockReturnValueOnce(page([{ label: 'Angular', value: 'a' }]))
                    .mockReturnValue(page([{ label: 'Backend', value: 'b' }]))
            );

            // Search resets the option list to a page that no longer contains 'a'.
            const input = spectator.query(byTestId('lazy-multiselect-search')) as HTMLInputElement;
            spectator.typeInElement('b', input);
            vi.advanceTimersByTime(FIELD_FILTER_DEBOUNCE_TIME);

            const emitted: DotLazyMultiselectOption[][] = [];
            spectator.component.selectionChange.subscribe((value) => emitted.push(value));
            spectator.triggerEventHandler('p-listbox', 'onChange', { value: ['a', 'b'] });

            // 'a' keeps its label from the accumulated map, not a raw-value fallback.
            expect(emitted).toEqual([
                [
                    { label: 'Angular', value: 'a' },
                    { label: 'Backend', value: 'b' }
                ]
            ]);
            vi.useRealTimers();
        });
    });

    describe('error handling', () => {
        it('should stop loading (and paging) and flag a distinct error when a page request fails', () => {
            build(vi.fn().mockReturnValue(throwError(() => new Error('boom'))));

            expect(spectator.component.$state.loading()).toBe(false);
            expect(spectator.component.$state.canLoadMore()).toBe(false);
            expect(spectator.component.$state.error()).toBe(true);
            expect(spectator.query(byTestId('lazy-multiselect-error'))).toBeTruthy();
        });
    });

    describe('single selection', () => {
        const options = [
            { label: 'Angular', value: 'a' },
            { label: 'Backend', value: 'b' }
        ];

        const buildSingle = () => {
            build(vi.fn().mockReturnValue(page(options)));
            spectator.setInput('multiple' as never, false as never);
            spectator.detectChanges();
        };

        it('should be multiple, with checkboxes, by default', () => {
            build(vi.fn().mockReturnValue(page(options)));

            const listbox = spectator.query(Listbox) as Listbox;
            expect(listbox.multiple).toBe(true);
            expect(listbox.checkbox).toBe(true);
        });

        it('should drop the checkboxes and pick one when multiple is off', () => {
            buildSingle();

            const listbox = spectator.query(Listbox) as Listbox;
            expect(listbox.multiple).toBe(false);
            expect(listbox.checkbox).toBe(false);
        });

        it('should hand back the data a loaded option carried', () => {
            const record = { id: 'b', email: 'b@x' };
            build(vi.fn().mockReturnValue(page([{ label: 'Backend', value: 'b', data: record }])));
            spectator.setInput('multiple' as never, false as never);
            spectator.detectChanges();
            const emitted: DotLazyMultiselectOption[][] = [];
            spectator.component.selectionChange.subscribe((value) => emitted.push(value));

            spectator.triggerEventHandler('p-listbox', 'onChange', { value: 'b' });

            expect(emitted).toEqual([[{ label: 'Backend', value: 'b', data: record }]]);
        });

        it('should still emit an array holding the one pick', () => {
            buildSingle();
            const emitted: DotLazyMultiselectOption[][] = [];
            spectator.component.selectionChange.subscribe((value) => emitted.push(value));

            spectator.triggerEventHandler('p-listbox', 'onChange', { value: 'b' });
            spectator.triggerEventHandler('p-listbox', 'onChange', { value: null });

            expect(emitted).toEqual([[{ label: 'Backend', value: 'b' }], []]);
        });
    });

    describe('row customization', () => {
        it('should use the theme row height unless told otherwise', () => {
            build(vi.fn().mockReturnValue(page([])));

            expect((spectator.query(Listbox) as Listbox).virtualScrollItemSize).toBe(
                LISTBOX_OPTION_HEIGHT
            );

            spectator.setInput('itemSize' as never, 56 as never);
            spectator.detectChanges();

            expect((spectator.query(Listbox) as Listbox).virtualScrollItemSize).toBe(56);
        });

        it('should use the given key for the search placeholder', () => {
            spectator = createComponent({
                props: {
                    loadPage: vi.fn().mockReturnValue(page([])),
                    searchPlaceholderKey: 'users.search'
                } as never,
                providers: [
                    {
                        provide: DotMessageService,
                        useValue: new MockDotMessageService({ 'users.search': 'Search users' })
                    }
                ]
            });
            spectator.detectChanges();

            const input = spectator.query(byTestId('lazy-multiselect-search')) as HTMLInputElement;
            expect(input.placeholder).toBe('Search users');
            expect(input.getAttribute('aria-label')).toBe('Search users');
        });
    });
});
