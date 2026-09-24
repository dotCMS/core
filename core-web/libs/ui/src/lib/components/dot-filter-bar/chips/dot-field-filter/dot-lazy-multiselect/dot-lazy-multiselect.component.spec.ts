import { byTestId, createComponentFactory, Spectator } from '@openng/spectator/vitest';
import { of, throwError } from 'rxjs';
import { Mock, vi } from 'vitest';

import { HttpErrorResponse } from '@angular/common/http';

import { DotMessageService } from '@dotcms/data-access';
import { MockDotMessageService } from '@dotcms/utils-testing';

import {
    DotLazyMultiselectComponent,
    DotLazyMultiselectLoader,
    DotLazyMultiselectOption
} from './dot-lazy-multiselect.component';

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
        it('should prefetch the next page when the scroller reaches the current one', () => {
            build(vi.fn().mockReturnValue(page([{ label: 'A', value: 'a' }], true)));
            loadPage.mockClear();

            spectator.triggerEventHandler('p-listbox', 'onLazyLoad', { last: 20 });

            expect(loadPage).toHaveBeenCalledWith({ page: 2, perPage: 20, filter: '' });
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
});
