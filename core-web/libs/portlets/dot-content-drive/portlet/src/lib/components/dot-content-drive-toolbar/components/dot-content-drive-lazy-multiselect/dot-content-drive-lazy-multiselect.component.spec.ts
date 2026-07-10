import { byTestId, createComponentFactory, Spectator } from '@openng/spectator/jest';
import { of, throwError } from 'rxjs';

import { DotMessageService } from '@dotcms/data-access';
import { MockDotMessageService } from '@dotcms/utils-testing';

import {
    DotContentDriveLazyMultiselectComponent,
    DotLazyMultiselectLoader,
    DotLazyMultiselectOption
} from './dot-content-drive-lazy-multiselect.component';

import { DEBOUNCE_TIME } from '../../../../shared/constants';

const page = (options: DotLazyMultiselectOption[], hasMore = false) => of({ options, hasMore });

describe('DotContentDriveLazyMultiselectComponent', () => {
    let spectator: Spectator<DotContentDriveLazyMultiselectComponent>;
    let loadPage: jest.Mock;

    const createComponent = createComponentFactory({
        component: DotContentDriveLazyMultiselectComponent,
        providers: [
            {
                provide: DotMessageService,
                useValue: new MockDotMessageService({})
            }
        ],
        detectChanges: false
    });

    const build = (loader: DotLazyMultiselectLoader, selectedValues: string[] = []) => {
        loadPage = loader as jest.Mock;
        spectator = createComponent({
            props: { loadPage, selectedValues } as never
        });
        spectator.detectChanges();
    };

    afterEach(() => jest.clearAllMocks());

    it('should load the first page on init', () => {
        build(jest.fn().mockReturnValue(page([{ label: 'A', value: 'a' }])));

        expect(loadPage).toHaveBeenCalledWith({ page: 1, perPage: 20, filter: '' });
    });

    describe('search', () => {
        beforeEach(() => jest.useFakeTimers());
        afterEach(() => jest.useRealTimers());

        it('should reload from the first page with the typed filter (debounced)', () => {
            build(jest.fn().mockReturnValue(page([{ label: 'A', value: 'a' }], true)));
            loadPage.mockClear();

            const input = spectator.query(byTestId('lazy-multiselect-search')) as HTMLInputElement;
            spectator.typeInElement('ang', input);
            jest.advanceTimersByTime(DEBOUNCE_TIME);

            expect(loadPage).toHaveBeenCalledWith({ page: 1, perPage: 20, filter: 'ang' });
        });
    });

    describe('infinite scroll', () => {
        it('should prefetch the next page when the scroller reaches the current one', () => {
            build(jest.fn().mockReturnValue(page([{ label: 'A', value: 'a' }], true)));
            loadPage.mockClear();

            spectator.triggerEventHandler('p-listbox', 'onLazyLoad', { last: 20 });

            expect(loadPage).toHaveBeenCalledWith({ page: 2, perPage: 20, filter: '' });
        });

        it('should not load more once the loader reports no further pages', () => {
            build(jest.fn().mockReturnValue(page([{ label: 'A', value: 'a' }], false)));
            loadPage.mockClear();

            spectator.triggerEventHandler('p-listbox', 'onLazyLoad', { last: 20 });

            expect(loadPage).not.toHaveBeenCalled();
        });
    });

    describe('selection', () => {
        it('should emit the chosen options (value + label) on change', () => {
            build(
                jest.fn().mockReturnValue(
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
            build(jest.fn().mockReturnValue(page([{ label: 'Angular', value: 'a' }])));
            const emitted: DotLazyMultiselectOption[][] = [];
            spectator.component.selectionChange.subscribe((value) => emitted.push(value));

            spectator.triggerEventHandler('p-listbox', 'onChange', { value: ['unknown'] });

            expect(emitted).toEqual([[{ label: 'unknown', value: 'unknown' }]]);
        });

        it('should keep an earlier page label after a search reset', () => {
            jest.useFakeTimers();
            build(
                jest
                    .fn()
                    .mockReturnValueOnce(page([{ label: 'Angular', value: 'a' }]))
                    .mockReturnValue(page([{ label: 'Backend', value: 'b' }]))
            );

            // Search resets the option list to a page that no longer contains 'a'.
            const input = spectator.query(byTestId('lazy-multiselect-search')) as HTMLInputElement;
            spectator.typeInElement('b', input);
            jest.advanceTimersByTime(DEBOUNCE_TIME);

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
            jest.useRealTimers();
        });
    });

    describe('error handling', () => {
        it('should stop loading (and paging) when a page request fails', () => {
            build(jest.fn().mockReturnValue(throwError(() => new Error('boom'))));

            expect(spectator.component.$state.loading()).toBe(false);
            expect(spectator.component.$state.canLoadMore()).toBe(false);
        });
    });
});
