import { SpectatorService, createServiceFactory, mockProvider } from '@openng/spectator/vitest';
import { vi } from 'vitest';

import {
    DotContentSearchService,
    DotContentTypeService,
    DotMessageService
} from '@dotcms/data-access';

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
        vi.useRealTimers();
    });

    it('runs the initial (empty-query) search immediately on open', async () => {
        const search = vi.fn().mockResolvedValue([item('A')]);

        service.openAsyncSubmenu(search, vi.fn());

        expect(search).toHaveBeenCalledTimes(1);
        expect(search).toHaveBeenCalledWith('');

        await flush();

        expect(service.items()).toEqual([item('A')]);
        expect(service.isLoading()).toBe(false);
    });

    it('debounces re-queries and searches only the latest query', () => {
        vi.useFakeTimers();
        const search = vi.fn().mockResolvedValue([]);
        service.openAsyncSubmenu(search, vi.fn());
        search.mockClear();

        service.filterItems('bl');
        service.filterItems('blo');
        service.filterItems('blog');

        vi.advanceTimersByTime(250);

        expect(search).toHaveBeenCalledTimes(1);
        expect(search).toHaveBeenCalledWith('blog');
    });

    it('does not re-search when the query is unchanged (dedupe)', () => {
        vi.useFakeTimers();
        const search = vi.fn().mockResolvedValue([]);
        service.openAsyncSubmenu(search, vi.fn());
        search.mockClear();

        service.filterItems('blog');
        service.filterItems('blog');

        vi.advanceTimersByTime(250);

        expect(search).toHaveBeenCalledTimes(1);
    });

    it('drops a stale response that resolves after a newer query (token guard)', async () => {
        vi.useFakeTimers();
        const older = defer<BlockItem[]>();
        const newer = defer<BlockItem[]>();
        const search = vi
            .fn()
            .mockResolvedValueOnce([]) // initial '' search from open
            .mockReturnValueOnce(older.promise) // 'a'
            .mockReturnValueOnce(newer.promise); // 'ab'

        service.openAsyncSubmenu(search, vi.fn());
        await flush();

        service.filterItems('a');
        vi.advanceTimersByTime(250);
        service.filterItems('ab');
        vi.advanceTimersByTime(250);

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
        vi.useFakeTimers();
        const pending = defer<BlockItem[]>();
        const search = vi.fn().mockReturnValueOnce(pending.promise);

        service.openAsyncSubmenu(search, vi.fn());
        service.close();

        pending.resolve([item('late')]);
        await flush();

        expect(service.items()).toEqual([]);
        expect(service.isOpen()).toBe(false);
    });
});

/**
 * Allowed Blocks gating of the AI slash entries.
 *
 * The Settings tab emits `aiContentPrompt` / `aiImagePrompt` (`getEditorBlockOptions()` maps the
 * option ids from `suggestion.utils.ts`), but the catalog consulted `aiContent` / `aiImage`. The
 * result was perverse: ticking "AI Content" in Allowed Blocks is precisely what hid AI Content,
 * because ticking anything restricts the field and the mismatched key then evaluates false. Not
 * ticking it hid it too — on a restricted field no configuration showed it at all (#37601,
 * defect C).
 *
 * Unlike `youtube`, the resolution here is NOT to ungate: these two ARE producible, so the gate is
 * legitimate and only the key was wrong. Enforced by `capability-keys.i1.spec.ts` (the key must be
 * producible) and `capability-keys.i2.spec.ts` (the producible option must have a consumer).
 */
describe('SlashMenuService — AI entries honour Allowed Blocks (#37601)', () => {
    let allowedBlocks: string[] | undefined;
    let aiInstalled: boolean;

    const createService = createServiceFactory({
        service: SlashMenuService,
        providers: [
            {
                provide: EditorStore,
                useValue: {
                    languageId: () => 1,
                    allowedContentTypes: () => '',
                    aiInstalled: () => aiInstalled,
                    isAllowed: (block: string) => !allowedBlocks || allowedBlocks.includes(block)
                }
            },
            mockProvider(EditorPopoverService),
            mockProvider(EditorModalService),
            mockProvider(DotContentTypeService),
            mockProvider(DotContentSearchService),
            mockProvider(DotMessageService, { get: (key: string) => key })
        ]
    });

    const blockNamesFor = (blocks: string[] | undefined, installed = true) => {
        allowedBlocks = blocks;
        aiInstalled = installed;

        return createService()
            .service.filterItems('')
            .map((item) => item.blockName);
    };

    it('shows both AI entries when the field allows them', () => {
        const names = blockNamesFor(['bulletList', 'aiContentPrompt', 'aiImagePrompt']);

        expect(names).toContain('aiContentPrompt');
        expect(names).toContain('aiImagePrompt');
    });

    it('hides both when the field is restricted and does not allow them', () => {
        // The gate still works — it is simply reading the key the Settings tab actually writes.
        const names = blockNamesFor(['bulletList']);

        expect(names).not.toContain('aiContentPrompt');
        expect(names).not.toContain('aiImagePrompt');
    });

    it('shows both on an unrestricted field, as before', () => {
        const names = blockNamesFor(undefined);

        expect(names).toContain('aiContentPrompt');
        expect(names).toContain('aiImagePrompt');
    });

    it('keeps the dotAI availability check winning over Allowed Blocks', () => {
        // Allowed explicitly, but the plugin is not installed: still hidden. `aiInstalled()` is a
        // separate and correct gate and this change must not weaken it.
        const names = blockNamesFor(['aiContentPrompt', 'aiImagePrompt'], false);

        expect(names).not.toContain('aiContentPrompt');
        expect(names).not.toContain('aiImagePrompt');
    });
});
