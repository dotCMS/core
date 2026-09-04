import { byTestId, createComponentFactory, Spectator } from '@openng/spectator/jest';

import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';

import { DotMessageService } from '@dotcms/data-access';
import { ComponentStatus, TreeNodeItem } from '@dotcms/dotcms-models';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotAssetPickerSidebarComponent } from './dot-asset-picker-sidebar.component';

import { DotFolderSearchResultsComponent } from '../../../dot-folder-search-results/dot-folder-search-results.component';
import { DotFolderTreeComponent } from '../../../dot-folder-tree/dot-folder-tree.component';
import { DotSearchInputComponent } from '../../../dot-search-input/dot-search-input.component';
import { DotSiteComponent } from '../../../dot-site/dot-site.component';
import { DotAssetPickerStore } from '../../store/dot-asset-picker.store';

const SITE_ROOT: TreeNodeItem = {
    key: 'site-1',
    label: 'demo.dotcms.com',
    data: { type: 'site', id: 'site-1', hostname: 'demo.dotcms.com', path: '' },
    leaf: false
};

const OTHER_SITE_ROOT: TreeNodeItem = {
    key: 'site-2',
    label: 'blog.dotcms.com',
    data: { type: 'site', id: 'site-2', hostname: 'blog.dotcms.com', path: '' },
    leaf: false
};

const FOLDER_NODE: TreeNodeItem = {
    key: 'folder-1',
    label: 'demo.dotcms.com/activities/',
    data: {
        type: 'folder',
        id: 'folder-1',
        hostname: 'demo.dotcms.com',
        path: '/activities/'
    },
    leaf: true
};

const LOAD_MORE_NODE: TreeNodeItem = {
    key: 'load-more:sites',
    label: '',
    type: 'load-more',
    data: { type: 'load-more', id: 'load-more:sites', nextPage: 2 }
};

/** Only the slice of the store the sidebar reads. Signals, so `computed` reacts. */
const createMockStore = () => ({
    folders: signal<TreeNodeItem[]>([SITE_ROOT]),
    selectedNode: signal<TreeNodeItem | null>(SITE_ROOT),
    foldersStatus: signal(ComponentStatus.LOADED),
    folderSearch: signal(''),
    searchResults: signal<TreeNodeItem[] | null>(null),
    isSearchingFolders: signal(false),
    displayedResults: signal<TreeNodeItem[]>([]),
    showResultsEmptyState: signal(false),
    showRefineHint: signal(false),
    selectedResultKey: signal<string | null>(null),
    searchStatus: signal(ComponentStatus.INIT),
    browsingSite: signal<{ identifier: string; hostname: string } | undefined>({
        identifier: SITE_ROOT.data.id,
        hostname: SITE_ROOT.data.hostname
    }),
    selectNode: jest.fn(),
    expandNode: jest.fn(),
    loadMore: jest.fn(),
    setFolderSearch: jest.fn(),
    selectSearchResult: jest.fn(),
    setBrowsingSite: jest.fn()
});

describe('DotAssetPickerSidebarComponent', () => {
    let spectator: Spectator<DotAssetPickerSidebarComponent>;
    let store: ReturnType<typeof createMockStore>;

    const createComponent = createComponentFactory({
        component: DotAssetPickerSidebarComponent,
        providers: [
            {
                provide: DotMessageService,
                useValue: new MockDotMessageService({ 'dot.asset.picker.sidebar.tree.root': 'All' })
            }
        ],
        detectChanges: false
    });

    const tree = () => spectator.query(DotFolderTreeComponent);

    beforeEach(() => {
        store = createMockStore();

        TestBed.overrideComponent(DotAssetPickerSidebarComponent, {
            add: { providers: [{ provide: DotAssetPickerStore, useValue: store }] }
        });

        spectator = createComponent();
        spectator.detectChanges();
    });

    it('should root the tree at the browsed site alone', () => {
        // Inverted deliberately. This used to assert that every browsable site was a root — that
        // was how the editor changed site. The site is now chosen in the sidebar's own selector,
        // so a second root here would be a bug, not a feature.
        expect(tree()?.$folders()).toEqual([SITE_ROOT]);
    });

    it('should render a search box over the tree', () => {
        expect(spectator.query(byTestId('asset-picker-folder-search'))).toBeTruthy();
    });

    describe('site selector (US1)', () => {
        const selector = () => spectator.query(DotSiteComponent);

        it('should render a site selector', () => {
            expect(selector()).toBeTruthy();
        });

        it('should sit above the folder search, which sits above the tree', () => {
            // The order is the design's, and it is also the logic: the site scopes the search, and
            // the search replaces the tree. Reading it bottom-up would invert what depends on what.
            const host = spectator.element as HTMLElement;
            const positions = ['dot-site', 'dot-search-input', 'dot-folder-tree'].map((tag) =>
                Array.prototype.indexOf.call(host.querySelectorAll('*'), host.querySelector(tag))
            );

            expect(positions).toEqual([...positions].sort((a, b) => a - b));
            expect(positions.every((index) => index >= 0)).toBe(true);
        });

        it('should show the site the store says is being browsed', () => {
            expect(selector()?.value()).toBe(SITE_ROOT.data.id);
        });

        it('should hide System Host, which is not browsable', () => {
            expect(selector()?.showSystemHost()).toBe(false);
        });

        it('should carry the globe affordance from the design', () => {
            expect(selector()?.icon()).toBeTruthy();
        });

        it('should separate the dropdown from the trigger', () => {
            // Without this the overlay sits flush against the control and the two read as one box.
            expect(selector()?.panelStyleClass()).toBeTruthy();
        });

        it('should inset the three controls by the same amount, so they line up', () => {
            // The tree used to sit at a different inset from the two inputs above it, which made
            // the column read as two unrelated groups. Asserted on the class because there is no
            // visual-regression harness here.
            const host = spectator.element as HTMLElement;
            const inset = (selector: string) =>
                host.querySelector(selector)?.closest('[class*="px-"]')?.className;

            expect(inset('dot-site')).toContain('px-4');
            expect(inset('dot-search-input')).toContain('px-4');
            expect(host.querySelector('dot-folder-tree')?.className).toContain('px-4');
        });

        it('should ask the store to change site when one is picked', () => {
            // `siteChange`, not `onChange`: the store needs the hostname to build the tree root and
            // the asset path, and `onChange` carries only the identifier.
            selector()?.siteChange.emit({
                identifier: 'site-2',
                hostname: 'blog.dotcms.com',
                aliases: null,
                archived: false
            });

            expect(store.setBrowsingSite).toHaveBeenCalledWith({
                identifier: 'site-2',
                hostname: 'blog.dotcms.com'
            });
        });

        it('should ignore a cleared selection rather than un-scoping the picker', () => {
            // `DotSiteComponent` emits null when its value is cleared. The picker is always
            // browsing *somewhere*, so there is no such thing as "no site" to move to.
            selector()?.siteChange.emit(null);

            expect(store.setBrowsingSite).not.toHaveBeenCalled();
        });
    });

    describe('folder search (US2)', () => {
        const results = () => spectator.query(DotFolderSearchResultsComponent);

        const startSearching = (rows: TreeNodeItem[] = [FOLDER_NODE]) => {
            store.isSearchingFolders.set(true);
            store.displayedResults.set(rows);
            spectator.detectChanges();
        };

        it('should render the folder search wired to the store', () => {
            const input = spectator.query(DotSearchInputComponent);

            expect(input).toBeTruthy();

            input?.search.emit('images');

            expect(store.setFolderSearch).toHaveBeenCalledWith('images');
        });

        it('should show the tree and no results while no term is active', () => {
            expect(tree()).toBeTruthy();
            expect(results()).toBeNull();
        });

        it('should replace the tree with the flat results once a term is active', () => {
            // Replace, not stack: the design shows one or the other, and showing both would offer
            // two different answers to "where am I browsing".
            startSearching();

            expect(results()).toBeTruthy();
            expect(tree()).toBeNull();
        });

        it('should offer no load-more, so the picker cannot page a different query', () => {
            startSearching();

            expect(results()?.$loadMoreLabelKey()).toBe('');
        });

        it('should hand a picked result to the store', () => {
            startSearching();

            results()?.resultSelect.emit(FOLDER_NODE);

            expect(store.selectSearchResult).toHaveBeenCalledWith(FOLDER_NODE);
        });

        it('should mark the chosen row as selected', () => {
            startSearching();
            store.selectedResultKey.set(FOLDER_NODE.key ?? null);
            spectator.detectChanges();

            expect(results()?.$selectedKey()).toBe(FOLDER_NODE.key);
        });

        it('should render an empty state rather than a blank panel', () => {
            store.isSearchingFolders.set(true);
            store.displayedResults.set([]);
            store.showResultsEmptyState.set(true);
            spectator.detectChanges();

            expect(spectator.query(byTestId('asset-picker-search-empty'))).toBeTruthy();
        });

        it('should tell the editor to narrow the term when more matches exist', () => {
            startSearching();
            store.showRefineHint.set(true);
            spectator.detectChanges();

            expect(spectator.query(byTestId('asset-picker-search-refine'))).toBeTruthy();
        });

        it('should say the search failed rather than leaving the panel blank', () => {
            // FR-018: a failure and an empty result must be distinguishable. The empty state is
            // gated on LOADED, so on ERROR the sidebar fell through to a results list of `[]` and
            // rendered nothing at all — the exact bug this feature set out to remove.
            store.isSearchingFolders.set(true);
            store.displayedResults.set([]);
            store.showResultsEmptyState.set(false);
            store.searchStatus.set(ComponentStatus.ERROR);
            spectator.detectChanges();

            expect(spectator.query(byTestId('asset-picker-search-error'))).toBeTruthy();
        });

        it('should not show the empty state when the search failed', () => {
            // A failure and an empty result read differently; the old sidebar collapsed them.
            store.isSearchingFolders.set(true);
            store.displayedResults.set([]);
            store.showResultsEmptyState.set(false);
            store.searchStatus.set(ComponentStatus.ERROR);
            spectator.detectChanges();

            expect(spectator.query(byTestId('asset-picker-search-empty'))).toBeNull();
        });
    });

    describe('tree root (US3)', () => {
        const labels = () =>
            spectator.queryAll(byTestId('tree-node-label')).map((el) => el.textContent?.trim());

        beforeEach(() => {
            store.folders.set([{ ...SITE_ROOT, expanded: true, children: [FOLDER_NODE] }]);
            spectator.detectChanges();
        });

        it('should label the single root "All" rather than the hostname', () => {
            // The hostname already names the site in the selector above. Repeating it on the root
            // says the same thing twice and reads as if the root were a second site control.
            expect(labels()).toContain('All');
            expect(labels()).not.toContain('demo.dotcms.com');
        });

        it('should still render folder nodes by their own name', () => {
            // The `All` override must key on the node being a *site*, not replace every label.
            expect(labels()).toContain('activities');
        });

        it('should render exactly one root', () => {
            expect(tree()?.$folders()).toHaveLength(1);
        });

        it('should reveal the wording the root row displays, not the hostname (#37363)', () => {
            // FR-012: this row is the reason the tooltip reads its text from what was rendered.
            // `node.label` still carries `demo.dotcms.com`, but the row says "All".
            jest.useFakeTimers();

            const clip = spectator.queryAll(byTestId('tree-node-label-clip'))[0] as HTMLElement;
            Object.defineProperty(clip, 'offsetWidth', { value: 10, configurable: true });
            Object.defineProperty(clip, 'scrollWidth', { value: 400, configurable: true });

            clip.dispatchEvent(new MouseEvent('mouseenter'));
            spectator.detectChanges();
            jest.advanceTimersByTime(1000);

            const text = document.querySelector('.p-tooltip-text')?.textContent?.trim();

            expect(text).toBe('All');
            expect(text).not.toBe('demo.dotcms.com');

            document.querySelectorAll('.p-tooltip').forEach((node) => node.remove());
            jest.useRealTimers();
        });
    });

    describe('node interaction', () => {
        it('should hand a selected node to the store', () => {
            tree()?.onNodeSelect.emit({ originalEvent: new Event('click'), node: SITE_ROOT });

            expect(store.selectNode).toHaveBeenCalledWith(SITE_ROOT);
        });

        it('should ask the store to load children on expand', () => {
            tree()?.onNodeExpand.emit({ originalEvent: new Event('click'), node: OTHER_SITE_ROOT });

            expect(store.expandNode).toHaveBeenCalledWith(OTHER_SITE_ROOT);
        });

        it('should ask the store for the next page from a "Load more" sentinel', () => {
            tree()?.loadMore.emit(LOAD_MORE_NODE);

            expect(store.loadMore).toHaveBeenCalledWith(LOAD_MORE_NODE);
        });
    });

    describe('loading', () => {
        it('should not report loading once the tree has resolved', () => {
            expect(tree()?.$loading()).toBe(false);
        });

        it('should report loading while the store is fetching', () => {
            store.foldersStatus.set(ComponentStatus.LOADING);
            spectator.detectChanges();

            expect(tree()?.$loading()).toBe(true);
        });
    });
});
