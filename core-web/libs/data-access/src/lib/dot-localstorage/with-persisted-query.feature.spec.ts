import { patchState, signalStore, withMethods, withState } from '@ngrx/signals';
import { createServiceFactory, SpectatorService } from '@openng/spectator/jest';

import { buildPersistedQueryKey } from './dot-persisted-query.utils';
import { withPersistedQuery } from './with-persisted-query.feature';

interface TestState {
    query: string;
}

// Minimal harness store that composes withPersistedQuery so we can exercise
// hydration, debounced writes, and clearPersistedQuery in isolation.
const TestStore = signalStore(
    { providedIn: 'root' },
    withState<TestState>({ query: '' }),
    withPersistedQuery({ portletKey: 'test-portlet', field: 'query' }),
    withMethods((store) => ({
        setQuery(query: string): void {
            patchState(store, { query });
        }
    }))
);

describe('withPersistedQuery', () => {
    const STORAGE_KEY = buildPersistedQueryKey('test-portlet');
    let spectator: SpectatorService<InstanceType<typeof TestStore>>;

    const createStore = createServiceFactory({
        service: TestStore
    });

    beforeEach(() => {
        window.localStorage.clear();
        jest.useFakeTimers();
    });

    afterEach(() => {
        jest.useRealTimers();
        window.localStorage.clear();
    });

    describe('hydration on init', () => {
        it('restores the query from localStorage when a value exists', () => {
            window.localStorage.setItem(STORAGE_KEY, JSON.stringify('previous query'));

            spectator = createStore();
            spectator.flushEffects();

            expect(spectator.service.query()).toBe('previous query');
        });

        it('leaves the initial state untouched when storage is empty', () => {
            spectator = createStore();
            spectator.flushEffects();

            expect(spectator.service.query()).toBe('');
        });

        it('falls back to the initial state when the stored payload is corrupt', () => {
            window.localStorage.setItem(STORAGE_KEY, '{not-json');

            spectator = createStore();
            spectator.flushEffects();

            expect(spectator.service.query()).toBe('');
        });
    });

    describe('debounced write', () => {
        it('persists the latest value after debounceMs elapses', () => {
            spectator = createStore();
            spectator.flushEffects();

            spectator.service.setQuery('draft');
            spectator.flushEffects();

            expect(window.localStorage.getItem(STORAGE_KEY)).toBeNull();

            jest.advanceTimersByTime(300);

            expect(window.localStorage.getItem(STORAGE_KEY)).toBe(JSON.stringify('draft'));
        });

        it('only writes the latest value when changes fire faster than the debounce window', () => {
            spectator = createStore();
            spectator.flushEffects();

            spectator.service.setQuery('first');
            spectator.flushEffects();
            jest.advanceTimersByTime(100);

            spectator.service.setQuery('second');
            spectator.flushEffects();
            jest.advanceTimersByTime(100);

            spectator.service.setQuery('third');
            spectator.flushEffects();
            jest.advanceTimersByTime(300);

            expect(window.localStorage.getItem(STORAGE_KEY)).toBe(JSON.stringify('third'));
        });
    });

    describe('clearPersistedQuery', () => {
        it('resets the state to empty and removes the stored entry', () => {
            window.localStorage.setItem(STORAGE_KEY, JSON.stringify('stored'));

            spectator = createStore();
            spectator.flushEffects();

            expect(spectator.service.query()).toBe('stored');

            spectator.service.clearPersistedQuery();

            expect(spectator.service.query()).toBe('');
            expect(window.localStorage.getItem(STORAGE_KEY)).toBeNull();
        });
    });
});
