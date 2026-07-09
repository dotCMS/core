import { SpectatorService, createServiceFactory, mockProvider } from '@ngneat/spectator/jest';

import { DotContentSearchService, DotContentTypeService, DotMessageService } from '@dotcms/data-access';

import { SlashMenuService } from './slash-menu.service';
import { BlockItem } from './slash-menu.types';

import { EditorModalService } from '../../services/editor-modal.service';
import { EditorPopoverService } from '../../services/editor-popover.service';
import { EditorStore } from '../../store/editor.store';

const item = (label: string): BlockItem => ({ label, description: '', icon: '', keywords: [] });

/** A promise whose resolution is controlled by the test, to exercise out-of-order responses. */
function defer<T>() {
    let resolve!: (value: T) => void;
    const promise = new Promise<T>((res) => {
        resolve = res;
    });

    return { promise, resolve };
}

const flush = () => Promise.resolve();

describe('SlashMenuService — async sub-menu search', () => {
    let spectator: SpectatorService<SlashMenuService>;
    let service: SlashMenuService;

    const createService = createServiceFactory({
        service: SlashMenuService,
        providers: [
            // Real NgZone (its run() executes synchronously here). A mock breaks Angular's
            // change-detection scheduler, which subscribes to NgZone's observables at setup.
            {
                provide: EditorStore,
                useValue: {
                    languageId: () => 1,
                    allowedContentTypes: () => '',
                    aiInstalled: () => false
                }
            },
            mockProvider(EditorPopoverService),
            mockProvider(EditorModalService),
            mockProvider(DotContentTypeService),
            mockProvider(DotContentSearchService),
            mockProvider(DotMessageService, { get: (key: string) => key })
        ]
    });

    beforeEach(() => {
        spectator = createService();
        service = spectator.service;
    });

    afterEach(() => {
        jest.useRealTimers();
    });

    it('runs the initial (empty-query) search immediately on open', async () => {
        const search = jest.fn().mockResolvedValue([item('A')]);

        service.openAsyncSubmenu(search, jest.fn());

        expect(search).toHaveBeenCalledTimes(1);
        expect(search).toHaveBeenCalledWith('');

        await flush();

        expect(service.items()).toEqual([item('A')]);
        expect(service.isLoading()).toBe(false);
    });

    it('debounces re-queries and searches only the latest query', () => {
        jest.useFakeTimers();
        const search = jest.fn().mockResolvedValue([]);
        service.openAsyncSubmenu(search, jest.fn());
        search.mockClear();

        service.filterItems('bl');
        service.filterItems('blo');
        service.filterItems('blog');

        jest.advanceTimersByTime(250);

        expect(search).toHaveBeenCalledTimes(1);
        expect(search).toHaveBeenCalledWith('blog');
    });

    it('does not re-search when the query is unchanged (dedupe)', () => {
        jest.useFakeTimers();
        const search = jest.fn().mockResolvedValue([]);
        service.openAsyncSubmenu(search, jest.fn());
        search.mockClear();

        service.filterItems('blog');
        service.filterItems('blog');

        jest.advanceTimersByTime(250);

        expect(search).toHaveBeenCalledTimes(1);
    });

    it('drops a stale response that resolves after a newer query (token guard)', async () => {
        jest.useFakeTimers();
        const older = defer<BlockItem[]>();
        const newer = defer<BlockItem[]>();
        const search = jest
            .fn()
            .mockResolvedValueOnce([]) // initial '' search from open
            .mockReturnValueOnce(older.promise) // 'a'
            .mockReturnValueOnce(newer.promise); // 'ab'

        service.openAsyncSubmenu(search, jest.fn());
        await flush();

        service.filterItems('a');
        jest.advanceTimersByTime(250);
        service.filterItems('ab');
        jest.advanceTimersByTime(250);

        // Newer query resolves first and wins.
        newer.resolve([item('AB')]);
        await flush();
        expect(service.items()).toEqual([item('AB')]);

        // Older (stale) query resolves last — must be ignored, not overwrite the newer rows.
        older.resolve([item('A')]);
        await flush();
        expect(service.items()).toEqual([item('AB')]);
    });

    it('ignores a search result that resolves after the menu closed', async () => {
        jest.useFakeTimers();
        const pending = defer<BlockItem[]>();
        const search = jest.fn().mockReturnValueOnce(pending.promise);

        service.openAsyncSubmenu(search, jest.fn());
        service.close();

        pending.resolve([item('late')]);
        await flush();

        expect(service.items()).toEqual([]);
        expect(service.isOpen()).toBe(false);
    });
});
