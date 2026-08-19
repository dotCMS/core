import { byTestId, createComponentFactory, Spectator } from '@openng/spectator/jest';

import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';

import { DotMessageService } from '@dotcms/data-access';
import { ComponentStatus, TreeNodeItem } from '@dotcms/dotcms-models';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotAssetPickerSidebarComponent } from './dot-asset-picker-sidebar.component';

import { DotFolderTreeComponent } from '../../../dot-folder-tree/dot-folder-tree.component';
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

const LOAD_MORE_NODE: TreeNodeItem = {
    key: 'load-more:sites',
    label: '',
    type: 'load-more',
    data: { type: 'load-more', id: 'load-more:sites', nextPage: 2 }
};

/** Only the slice of the store the sidebar reads. Signals, so `computed` reacts. */
const createMockStore = () => ({
    folders: signal<TreeNodeItem[]>([SITE_ROOT, OTHER_SITE_ROOT, LOAD_MORE_NODE]),
    selectedNode: signal<TreeNodeItem | null>(SITE_ROOT),
    foldersStatus: signal<ComponentStatus>(ComponentStatus.LOADED),
    treeSearch: signal(''),
    selectNode: jest.fn(),
    expandNode: jest.fn(),
    loadMore: jest.fn(),
    setTreeSearch: jest.fn()
});

describe('DotAssetPickerSidebarComponent', () => {
    let spectator: Spectator<DotAssetPickerSidebarComponent>;
    let store: ReturnType<typeof createMockStore>;

    const createComponent = createComponentFactory({
        component: DotAssetPickerSidebarComponent,
        providers: [{ provide: DotMessageService, useValue: new MockDotMessageService({}) }],
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

    it('should render every site the store reports as a tree root', () => {
        // The regression this guards: the sidebar used to show one synthetic root for the single
        // configured site, so only that site was reachable.
        expect(tree()?.$folders()).toEqual([SITE_ROOT, OTHER_SITE_ROOT, LOAD_MORE_NODE]);
    });

    it('should render a search box over the tree', () => {
        expect(spectator.query(byTestId('asset-picker-tree-search'))).toBeTruthy();
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
