import { createServiceFactory, mockProvider, SpectatorService } from '@openng/spectator/jest';
import { of, throwError } from 'rxjs';

import { DotHttpErrorManagerService } from '@dotcms/data-access';

import { DotToolsStore } from './dot-tools.store';

import { CATALOG_INITIAL_LIMIT, CATALOG_LOAD_MORE_STEP } from '../../constants/dot-tools.constants';
import { DotToolsCatalogEntry, DotToolsSection } from '../../models/dot-tools.models';
import { DotToolsService } from '../../services/dot-tools.service';

const MOCK_SECTIONS: DotToolsSection[] = [
    { id: 'site', name: 'Site', icon: 'language', tabOrder: 0, portletIds: ['pages', 'browser'] },
    {
        id: 'content',
        name: 'Content',
        icon: 'article',
        tabOrder: 1,
        portletIds: ['blogs', 'events', 'c_press-releases']
    },
    { id: 'empty', name: 'Empty', icon: 'folder', tabOrder: 2, portletIds: [] }
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
    let service: jest.Mocked<DotToolsService>;
    let httpErrorManager: jest.Mocked<DotHttpErrorManagerService>;

    const createService = createServiceFactory({
        service: DotToolsStore,
        providers: [
            mockProvider(DotToolsService, {
                getSections: jest.fn().mockReturnValue(of(MOCK_SECTIONS)),
                getCatalog: jest.fn().mockReturnValue(of(MOCK_CATALOG)),
                createSection: jest.fn().mockReturnValue(
                    of<DotToolsSection>({
                        id: 'new-section',
                        name: 'New Section',
                        icon: 'widgets',
                        tabOrder: 99,
                        portletIds: []
                    })
                ),
                updateSection: jest.fn().mockReturnValue(
                    of<DotToolsSection>({
                        id: 'site',
                        name: 'Renamed Site',
                        icon: 'public',
                        tabOrder: 0,
                        portletIds: ['pages', 'browser']
                    })
                ),
                deleteSection: jest.fn().mockReturnValue(of(undefined)),
                reorderSections: jest.fn().mockReturnValue(of(undefined)),
                setSectionTools: jest.fn().mockReturnValue(of(undefined)),
                createCustomTool: jest.fn().mockReturnValue(
                    of<DotToolsCatalogEntry>({
                        id: 'c_new-tool',
                        title: 'New Tool',
                        isCustom: true
                    })
                ),
                updateCustomTool: jest.fn().mockReturnValue(
                    of<DotToolsCatalogEntry>({
                        id: 'c_press-releases',
                        title: 'Press Releases v2',
                        isCustom: true
                    })
                ),
                deleteCustomTool: jest.fn().mockReturnValue(of(undefined))
            }),
            mockProvider(DotHttpErrorManagerService)
        ]
    });

    beforeEach(() => {
        spectator = createService();
        store = spectator.service;
        service = spectator.inject(DotToolsService) as jest.Mocked<DotToolsService>;
        httpErrorManager = spectator.inject(
            DotHttpErrorManagerService
        ) as jest.Mocked<DotHttpErrorManagerService>;
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
            (service.getSections as jest.Mock).mockReturnValueOnce(of([]));
            (service.getCatalog as jest.Mock).mockReturnValueOnce(of([]));
            store.loadAll();
            expect(store.showEmptySections()).toBe(true);
            expect(store.showLoading()).toBe(false);
        });

        it('showError flips on load failure', () => {
            (service.getSections as jest.Mock).mockReturnValueOnce(
                throwError(() => new Error('boom'))
            );
            store.loadAll();
            expect(store.showError()).toBe(true);
            expect(httpErrorManager.handle).toHaveBeenCalled();
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
        it('createSection appends and selects the new section', () => {
            store.createSection({ name: 'New Section', icon: 'widgets' });
            expect(store.sections().at(-1)?.id).toBe('new-section');
            expect(store.selectedSectionId()).toBe('new-section');
        });

        it('updateSection patches name and icon in place', () => {
            store.updateSection('site', { name: 'Renamed Site', icon: 'public' });
            const site = store.sections().find((s) => s.id === 'site');
            expect(site?.name).toBe('Renamed Site');
            expect(site?.icon).toBe('public');
        });

        it('deleteSection removes it and re-selects the next survivor', () => {
            expect(store.selectedSectionId()).toBe('site');
            store.deleteSection('site');
            expect(store.sections().map((s) => s.id)).toEqual(['content', 'empty']);
            expect(store.selectedSectionId()).toBe('content');
        });

        it('reorderSections rewrites tabOrder from the given id list', () => {
            store.reorderSections(['content', 'empty', 'site']);
            expect(store.sections().map((s) => s.id)).toEqual(['content', 'empty', 'site']);
            expect(store.sections().map((s) => s.tabOrder)).toEqual([0, 1, 2]);
        });

        it('reverts to loaded on delete error and does not remove the section', () => {
            (service.deleteSection as jest.Mock).mockReturnValueOnce(
                throwError(() => new Error('nope'))
            );
            store.deleteSection('site');
            expect(store.sections().some((s) => s.id === 'site')).toBe(true);
            expect(store.status()).toBe('loaded');
            expect(httpErrorManager.handle).toHaveBeenCalled();
        });
    });

    describe('section-tool mutations', () => {
        it('toggleToolInSelectedSection adds an unchecked tool', () => {
            store.toggleToolInSelectedSection('blogs'); // Site does not have blogs
            const site = store.sections().find((s) => s.id === 'site');
            expect(site?.portletIds).toContain('blogs');
        });

        it('toggleToolInSelectedSection removes a checked tool', () => {
            store.toggleToolInSelectedSection('pages'); // Site has pages
            const site = store.sections().find((s) => s.id === 'site');
            expect(site?.portletIds).not.toContain('pages');
        });

        it('removeToolFromSelectedSection drops the tool from portletIds', () => {
            store.removeToolFromSelectedSection('pages');
            const site = store.sections().find((s) => s.id === 'site');
            expect(site?.portletIds).toEqual(['browser']);
        });

        it('reorderSelectedSectionTools writes the new order', () => {
            store.reorderSelectedSectionTools(['browser', 'pages']);
            const site = store.sections().find((s) => s.id === 'site');
            expect(site?.portletIds).toEqual(['browser', 'pages']);
        });
    });

    describe('custom tool mutations', () => {
        it('createCustomTool appends to the catalog and keeps it sorted', () => {
            store.createCustomTool({
                portletId: 'c_new-tool',
                portletName: 'New Tool',
                baseTypes: [],
                contentTypes: [],
                dataViewMode: 'List'
            });
            const titles = store.catalog().map((entry) => entry.title);
            expect(titles).toContain('New Tool');
            // Alphabetical: 'Blogs' < 'Browser' < 'Events' < 'New Tool' < 'Pages' < 'Press Releases'
            expect(titles).toEqual([...titles].sort((a, b) => a.localeCompare(b)));
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
            store.updateCustomTool({
                portletId: 'c_press-releases',
                portletName: 'Press Releases v2',
                baseTypes: [],
                contentTypes: [],
                dataViewMode: 'List'
            });
            const entry = store.catalog().find((e) => e.id === 'c_press-releases');
            expect(entry?.title).toBe('Press Releases v2');
        });
    });
});
