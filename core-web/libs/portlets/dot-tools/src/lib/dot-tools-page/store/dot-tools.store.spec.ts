import { createServiceFactory, mockProvider, SpectatorService } from '@openng/spectator/vitest';
import { of, Subject, throwError } from 'rxjs';
import { Mock, Mocked, vi } from 'vitest';

import { HttpErrorResponse } from '@angular/common/http';

import { DotHttpErrorManagerService } from '@dotcms/data-access';

import { DotToolsStore } from './dot-tools.store';

import { CATALOG_INITIAL_LIMIT, CATALOG_LOAD_MORE_STEP } from '../../constants/dot-tools.constants';
import { DotToolsCatalogEntry, DotToolsSection } from '../../models/dot-tools.models';
import { DotToolsService } from '../../services/dot-tools.service';

const MOCK_SECTIONS: DotToolsSection[] = [
    {
        id: 'site',
        name: 'Site',
        icon: 'language',
        tabOrder: 0,
        portletIds: ['pages', 'browser'],
        portletTitles: ['Pages', 'Browser']
    },
    {
        id: 'content',
        name: 'Content',
        icon: 'article',
        tabOrder: 1,
        portletIds: ['blogs', 'events', 'c_press-releases'],
        portletTitles: ['Blogs', 'Events', 'Press Releases']
    },
    { id: 'empty', name: 'Empty', icon: 'folder', tabOrder: 2, portletIds: [], portletTitles: [] }
];

const MOCK_CATALOG: DotToolsCatalogEntry[] = [
    { id: 'blogs', title: 'Blogs', isCustom: false },
    { id: 'browser', title: 'Browser', isCustom: false },
    { id: 'c_press-releases', title: 'Press Releases', isCustom: true },
    { id: 'events', title: 'Events', isCustom: false },
    { id: 'pages', title: 'Pages', isCustom: false }
];

describe('DotToolsStore', () => {
    let spectator: SpectatorService<InstanceType<typeof DotToolsStore>>;
    let store: InstanceType<typeof DotToolsStore>;
    let service: Mocked<DotToolsService>;
    let httpErrorManager: Mocked<DotHttpErrorManagerService>;

    const createService = createServiceFactory({
        service: DotToolsStore,
        providers: [
            mockProvider(DotToolsService, {
                getSections: vi.fn().mockReturnValue(of(MOCK_SECTIONS)),
                getCatalog: vi.fn().mockReturnValue(of(MOCK_CATALOG)),
                createSection: vi.fn().mockReturnValue(
                    of<DotToolsSection>({
                        id: 'new-section',
                        name: 'New Section',
                        icon: 'widgets',
                        tabOrder: 99,
                        portletIds: [],
                        portletTitles: []
                    })
                ),
                updateSection: vi.fn().mockReturnValue(
                    of<DotToolsSection>({
                        id: 'site',
                        name: 'Renamed Site',
                        icon: 'public',
                        tabOrder: 0,
                        portletIds: ['pages', 'browser'],
                        portletTitles: ['Pages', 'Browser']
                    })
                ),
                // Delete, reorder, and setSectionTools now return the full
                // section list per the spec on PR #37645.
                deleteSection: vi.fn().mockReturnValue(of(MOCK_SECTIONS)),
                reorderSections: vi.fn().mockReturnValue(of(MOCK_SECTIONS)),
                setSectionTools: vi.fn().mockReturnValue(of(MOCK_SECTIONS)),
                createCustomTool: vi.fn().mockReturnValue(
                    of<DotToolsCatalogEntry>({
                        id: 'c_new-tool',
                        title: 'New Tool',
                        isCustom: true
                    })
                ),
                updateCustomTool: vi.fn().mockReturnValue(
                    of<DotToolsCatalogEntry>({
                        id: 'c_press-releases',
                        title: 'Press Releases v2',
                        isCustom: true
                    })
                ),
                deleteCustomTool: vi.fn().mockReturnValue(of(undefined))
            }),
            mockProvider(DotHttpErrorManagerService)
        ]
    });

    beforeEach(() => {
        spectator = createService();
        store = spectator.service;
        service = spectator.inject(DotToolsService) as Mocked<DotToolsService>;
        httpErrorManager = spectator.inject(
            DotHttpErrorManagerService
        ) as Mocked<DotHttpErrorManagerService>;
        // mockProvider creates each spy once at factory time, so call
        // history carries between specs — reset the ones with per-test
        // assertions.
        (service.setSectionTools as Mock).mockClear();
    });

    describe('initial load', () => {
        it('populates sections sorted by tabOrder and selects the first one', () => {
            expect(store.status()).toBe('loaded');
            expect(store.sections().map((s) => s.id)).toEqual(['site', 'content', 'empty']);
            expect(store.selectedSectionId()).toBe('site');
        });

        it('resolves selected section tools from the catalog in stored order', () => {
            expect(store.selectedSectionTools().map((t) => t.id)).toEqual(['pages', 'browser']);
        });

        it('exposes the catalog and its filtered/paginated derivations', () => {
            expect(store.catalog()).toHaveLength(MOCK_CATALOG.length);
            expect(store.paginatedCatalog()).toHaveLength(MOCK_CATALOG.length);
            expect(store.hasMoreCatalog()).toBe(false);
        });
    });

    describe('computeds', () => {
        it('selectedSectionToolIds is a Set of the selected section portletIds', () => {
            const ids = store.selectedSectionToolIds();
            expect(ids.has('pages')).toBe(true);
            expect(ids.has('blogs')).toBe(false);
        });

        it('filteredCatalog matches against title, case-insensitive', () => {
            store.setCatalogFilter('BLOG');
            expect(store.filteredCatalog().map((e) => e.id)).toEqual(['blogs']);
        });

        it('toolSectionCount returns how many sections reference a tool', () => {
            const count = store.toolSectionCount();
            expect(count('pages')).toBe(1); // in Site
            expect(count('blogs')).toBe(1); // in Content
            expect(count('nowhere')).toBe(0);
        });

        it('showEmptySections toggles only after load with zero sections', () => {
            expect(store.showEmptySections()).toBe(false);
            (service.getSections as Mock).mockReturnValueOnce(of([]));
            (service.getCatalog as Mock).mockReturnValueOnce(of([]));
            store.loadAll();
            expect(store.showEmptySections()).toBe(true);
            expect(store.showLoading()).toBe(false);
        });

        it('showError flips on load failure', () => {
            (service.getSections as Mock).mockReturnValueOnce(throwError(() => new Error('boom')));
            store.loadAll();
            expect(store.showError()).toBe(true);
            expect(httpErrorManager.handle).toHaveBeenCalled();
        });

        it('loadAll skips the global handler on a 404 so the retry card is not covered', () => {
            (service.getSections as Mock).mockReturnValueOnce(
                throwError(() => new HttpErrorResponse({ status: 404 }))
            );
            store.loadAll();
            expect(store.showError()).toBe(true);
            expect(httpErrorManager.handle).not.toHaveBeenCalled();
        });
    });

    describe('selection + pagination', () => {
        it('selectSection updates id and resets the catalog limit', () => {
            store.setCatalogFilter(''); // reset side-effect free
            store.loadMoreCatalog();
            expect(store.catalogLimit()).toBe(CATALOG_INITIAL_LIMIT + CATALOG_LOAD_MORE_STEP);

            store.selectSection('content');

            expect(store.selectedSectionId()).toBe('content');
            expect(store.catalogLimit()).toBe(CATALOG_INITIAL_LIMIT);
        });

        it('setCatalogFilter resets the catalog limit', () => {
            store.loadMoreCatalog();
            store.setCatalogFilter('anything');
            expect(store.catalogLimit()).toBe(CATALOG_INITIAL_LIMIT);
        });

        it('loadMoreCatalog increments by CATALOG_LOAD_MORE_STEP', () => {
            const start = store.catalogLimit();
            store.loadMoreCatalog();
            expect(store.catalogLimit()).toBe(start + CATALOG_LOAD_MORE_STEP);
        });
    });

    describe('section mutations', () => {
        // Server is authoritative on the shape of the resulting section list
        // (all mutation endpoints return the full list). These tests verify
        // the store's outward-facing contract: it calls the right service
        // method with the right args and commits whatever the server returned.

        it('createSection calls the service and appends the returned section', () => {
            store.createSection({ name: 'New Section', icon: 'widgets' }).subscribe();
            expect(service.createSection).toHaveBeenCalledWith({
                name: 'New Section',
                icon: 'widgets'
            });
            // Mock returns { id: 'new-section', ... } — store appends and selects it.
            expect(store.sections().at(-1)?.id).toBe('new-section');
            expect(store.selectedSectionId()).toBe('new-section');
        });

        it('updateSection calls the service and patches name/icon from the response', () => {
            store.updateSection('site', { name: 'Renamed Site', icon: 'public' }).subscribe();
            expect(service.updateSection).toHaveBeenCalledWith('site', {
                name: 'Renamed Site',
                icon: 'public'
            });
            const site = store.sections().find((s) => s.id === 'site');
            expect(site?.name).toBe('Renamed Site');
            expect(site?.icon).toBe('public');
        });

        it('deleteSection calls the service and commits the returned list', () => {
            const remaining = MOCK_SECTIONS.filter((s) => s.id !== 'site');
            (service.deleteSection as Mock).mockReturnValueOnce(of(remaining));

            store.deleteSection('site');

            expect(service.deleteSection).toHaveBeenCalledWith('site');
            expect(store.sections().map((s) => s.id)).toEqual(['content', 'empty']);
            expect(store.selectedSectionId()).toBe('content');
        });

        it('reorderSections calls the service with the id list and commits the response', () => {
            const reordered = [
                { ...MOCK_SECTIONS[1], tabOrder: 0 },
                { ...MOCK_SECTIONS[2], tabOrder: 1 },
                { ...MOCK_SECTIONS[0], tabOrder: 2 }
            ];
            (service.reorderSections as Mock).mockReturnValueOnce(of(reordered));

            store.reorderSections(['content', 'empty', 'site']);

            expect(service.reorderSections).toHaveBeenCalledWith(['content', 'empty', 'site']);
            expect(store.sections().map((s) => s.id)).toEqual(['content', 'empty', 'site']);
        });

        it('reverts to loaded on delete error and does not remove the section', () => {
            (service.deleteSection as Mock).mockReturnValueOnce(
                throwError(() => new Error('nope'))
            );
            store.deleteSection('site');
            expect(store.sections().some((s) => s.id === 'site')).toBe(true);
            expect(store.status()).toBe('loaded');
            expect(httpErrorManager.handle).toHaveBeenCalled();
        });

        it('createSection rethrows and resets status on error (no global handler)', () => {
            (service.createSection as Mock).mockReturnValueOnce(
                throwError(() => new HttpErrorResponse({ status: 400 }))
            );
            let caught: unknown = null;
            store
                .createSection({ name: 'Dup', icon: 'widgets' })
                .subscribe({ error: (err) => (caught = err) });
            expect(caught).toBeInstanceOf(HttpErrorResponse);
            expect(store.status()).toBe('loaded');
            expect(httpErrorManager.handle).not.toHaveBeenCalled();
        });

        it('updateSection rethrows and resets status on error (no global handler)', () => {
            (service.updateSection as Mock).mockReturnValueOnce(
                throwError(() => new HttpErrorResponse({ status: 400 }))
            );
            let caught: unknown = null;
            store
                .updateSection('site', { name: 'Dup', icon: 'public' })
                .subscribe({ error: (err) => (caught = err) });
            expect(caught).toBeInstanceOf(HttpErrorResponse);
            expect(store.status()).toBe('loaded');
            expect(httpErrorManager.handle).not.toHaveBeenCalled();
        });

        it('reorderSections rolls the optimistic patch back when the write fails', () => {
            const beforeIds = store.sections().map((s) => s.id);
            (service.reorderSections as Mock).mockReturnValueOnce(
                throwError(() => new Error('server down'))
            );

            store.reorderSections(['content', 'empty', 'site']);

            // The optimistic patch is reverted, order matches pre-call state.
            expect(store.sections().map((s) => s.id)).toEqual(beforeIds);
            expect(store.status()).toBe('loaded');
            expect(httpErrorManager.handle).toHaveBeenCalled();
        });
    });

    describe('section-tool mutations', () => {
        // All three actions ultimately call setSectionTools(sectionId, portletIds).
        // The store commits whatever the server returned; we assert the wire
        // call has the correct args.

        it('toggleToolInSelectedSection adds an unchecked tool', () => {
            store.toggleToolInSelectedSection('blogs'); // Site does not have blogs
            expect(service.setSectionTools).toHaveBeenCalledWith('site', [
                'pages',
                'browser',
                'blogs'
            ]);
        });

        it('toggleToolInSelectedSection removes a checked tool', () => {
            store.toggleToolInSelectedSection('pages'); // Site has pages
            expect(service.setSectionTools).toHaveBeenCalledWith('site', ['browser']);
        });

        it('removeToolFromSelectedSection drops the tool from portletIds', () => {
            store.removeToolFromSelectedSection('pages');
            expect(service.setSectionTools).toHaveBeenCalledWith('site', ['browser']);
        });

        it('reorderSelectedSectionTools writes the new order', () => {
            store.reorderSelectedSectionTools(['browser', 'pages']);
            expect(service.setSectionTools).toHaveBeenCalledWith('site', ['browser', 'pages']);
        });

        it('serializes concurrent tool writes so an in-flight PUT blocks the next one', () => {
            // Hold the first response in a Subject so the second toggle fires
            // while the first PUT is still in flight. With rxMethod +
            // concatMap, the second service call must not reach the wire
            // until the first subject completes.
            const first = new Subject<DotToolsSection[]>();
            const second = new Subject<DotToolsSection[]>();
            (service.setSectionTools as Mock)
                .mockReturnValueOnce(first.asObservable())
                .mockReturnValueOnce(second.asObservable());

            // Click: add 'blogs' to site → PUT #1 fires.
            store.toggleToolInSelectedSection('blogs');
            // Click: add 'c_press-releases' → should enqueue, not fire yet.
            store.toggleToolInSelectedSection('c_press-releases');

            expect(service.setSectionTools).toHaveBeenCalledTimes(1);
            expect(service.setSectionTools).toHaveBeenNthCalledWith(1, 'site', [
                'pages',
                'browser',
                'blogs'
            ]);

            // Resolve PUT #1 — queue drains the next call.
            first.next(MOCK_SECTIONS);
            first.complete();

            expect(service.setSectionTools).toHaveBeenCalledTimes(2);
            // The queued call read from the optimistic state (which already
            // held 'blogs'), so the full-replace list carries both tools.
            expect(service.setSectionTools).toHaveBeenNthCalledWith(2, 'site', [
                'pages',
                'browser',
                'blogs',
                'c_press-releases'
            ]);
        });
    });

    describe('custom tool mutations', () => {
        it('createCustomTool appends to the catalog and keeps it sorted', () => {
            store
                .createCustomTool({
                    portletId: 'c_new-tool',
                    portletName: 'New Tool',
                    baseTypes: [],
                    contentTypes: [],
                    dataViewMode: 'list'
                })
                .subscribe();
            const titles = store.catalog().map((entry) => entry.title);
            expect(titles).toContain('New Tool');
            // Alphabetical: 'Blogs' < 'Browser' < 'Events' < 'New Tool' < 'Pages' < 'Press Releases'
            expect(titles).toEqual([...titles].sort((a, b) => a.localeCompare(b)));
        });

        it('createCustomTool rethrows and resets status on error', () => {
            (service.createCustomTool as Mock).mockReturnValueOnce(
                throwError(() => new HttpErrorResponse({ status: 400 }))
            );
            let caught: unknown = null;
            store
                .createCustomTool({
                    portletId: 'c_err',
                    portletName: 'Err',
                    baseTypes: [],
                    contentTypes: [],
                    dataViewMode: 'list'
                })
                .subscribe({ error: (err) => (caught = err) });
            expect(caught).toBeInstanceOf(HttpErrorResponse);
            expect(store.status()).toBe('loaded');
            // Error path intentionally does not route through the handler —
            // the dialog owns inline error UX. See the dialog spec.
            expect(httpErrorManager.handle).not.toHaveBeenCalled();
        });

        it('deleteCustomTool cascades: removes from catalog AND from every section that referenced it', () => {
            expect(store.sections().find((s) => s.id === 'content')?.portletIds).toContain(
                'c_press-releases'
            );

            store.deleteCustomTool('c_press-releases');

            expect(store.catalog().some((e) => e.id === 'c_press-releases')).toBe(false);
            expect(store.sections().find((s) => s.id === 'content')?.portletIds).not.toContain(
                'c_press-releases'
            );
        });

        it('updateCustomTool patches title and re-sorts the catalog', () => {
            store
                .updateCustomTool({
                    portletId: 'c_press-releases',
                    portletName: 'Press Releases v2',
                    baseTypes: [],
                    contentTypes: [],
                    dataViewMode: 'list'
                })
                .subscribe();
            const entry = store.catalog().find((e) => e.id === 'c_press-releases');
            expect(entry?.title).toBe('Press Releases v2');
        });
    });
});
