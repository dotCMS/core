import {
    byTestId,
    createComponentFactory,
    mockProvider,
    Spectator,
    SpyObject
} from '@openng/spectator/jest';
import { Subject, of } from 'rxjs';

import { Clipboard } from '@angular/cdk/clipboard';
import { fakeAsync, tick } from '@angular/core/testing';
import { By } from '@angular/platform-browser';

import { patchState } from '@ngrx/signals';
import { unprotected } from '@ngrx/signals/testing';

import { Popover } from 'primeng/popover';

import { DotHttpErrorManagerService, DotMessageService } from '@dotcms/data-access';
import { ComponentStatus, TreeNodeItem, DotPagination } from '@dotcms/dotcms-models';
import { DotBrowsingService } from '@dotcms/ui';

import { DotHostFolderFieldComponent } from './host-folder-field.component';

import { TREE_SELECT_MOCK, TREE_SELECT_SITES_MOCK } from '../../../../utils/mocks';
import {
    HostFolderFiledStore,
    SITE_PAGE_LIMIT,
    SITE_SEARCH_THRESHOLD
} from '../../store/host-folder-field.store';
import { MessageServiceMock } from '../../utils/mocks';

describe('DotHostFolderFieldComponent', () => {
    let spectator: Spectator<DotHostFolderFieldComponent>;
    let service: SpyObject<DotBrowsingService>;
    let clipboard: SpyObject<Clipboard>;
    let store: InstanceType<typeof HostFolderFiledStore>;

    const createSitesPageResponse = (sites: TreeNodeItem[], totalEntries?: number) => ({
        sites,
        pagination: {
            currentPage: 1,
            perPage: SITE_PAGE_LIMIT,
            totalEntries: totalEntries ?? sites.length
        }
    });

    const mockSitesPage = (sites: TreeNodeItem[], totalEntries?: number) => {
        service.getSitesPage.mockReturnValue(of(createSitesPageResponse(sites, totalEntries)));
    };

    const createComponent = createComponentFactory({
        component: DotHostFolderFieldComponent,
        providers: [
            HostFolderFiledStore,
            mockProvider(DotHttpErrorManagerService, {
                handle: jest.fn()
            }),
            mockProvider(DotBrowsingService, {
                getSitesPage: jest.fn(() => of(createSitesPageResponse(TREE_SELECT_SITES_MOCK))),
                resolveSiteByHostname: jest.fn((hostname: string) => {
                    const site =
                        TREE_SELECT_SITES_MOCK.find((item) => item.label === hostname) ??
                        TREE_SELECT_MOCK.find((item) => item.label === hostname);

                    return of(site ?? null);
                }),
                getCurrentSiteAsTreeNodeItem: jest.fn(),
                buildTreeByPaths: jest.fn(),
                searchFolders: jest.fn(() =>
                    of({
                        folders: [],
                        pagination: { currentPage: 1, perPage: 40, totalEntries: 0 }
                    })
                )
            }),
            mockProvider(Clipboard, {
                copy: jest.fn().mockReturnValue(true)
            }),
            { provide: DotMessageService, useValue: MessageServiceMock }
        ]
    });

    let overlayMock: { toggle: jest.Mock; hide: jest.Mock };

    const createToggleEvent = (offsetWidth = 320): Event => {
        const trigger = document.createElement('button');
        Object.defineProperty(trigger, 'offsetWidth', { value: offsetWidth });
        const event = new Event('click');
        Object.defineProperty(event, 'currentTarget', { value: trigger });

        return event;
    };

    beforeEach(() => {
        spectator = createComponent({ detectChanges: false });
        spectator.fixture.componentRef.setInput('hasError', false);
        spectator.fixture.componentRef.setInput('isRequired', false);
        spectator.detectChanges();
        service = spectator.inject(DotBrowsingService);
        clipboard = spectator.inject(Clipboard);
        store = spectator.component.store;

        // p-popover isn't rendered/attached in this unit test; stub it with a stable
        // reference so trigger interactions can be verified without a real overlay.
        overlayMock = { toggle: jest.fn(), hide: jest.fn() };
        Object.defineProperty(spectator.component, '$overlay', {
            value: () => overlayMock,
            writable: true
        });
    });

    it('should create the component', () => {
        expect(spectator.component).toBeTruthy();
        expect(store).toBeTruthy();
    });

    it('should load sites on writeValue', () => {
        service.getSitesPage.mockClear();

        spectator.component.writeValue('//demo.dotcms.com/system/');
        spectator.detectChanges();

        expect(service.getSitesPage).toHaveBeenCalled();
    });

    it('should not toggle the overlay when disabled', () => {
        spectator.component.setDisabledState(true);

        spectator.component.toggleOverlay(createToggleEvent());

        expect(overlayMock.toggle).not.toHaveBeenCalled();
    });

    it('should toggle the overlay when not disabled and set the overlay width to the trigger width', () => {
        const event = createToggleEvent(320);

        spectator.component.toggleOverlay(event);

        expect(overlayMock.toggle).toHaveBeenCalledWith(event, event.currentTarget);
        expect(spectator.component.$overlayWidth()).toBe('320px');
    });

    it('should select a site through the store', () => {
        jest.spyOn(store, 'selectSite');
        const site = TREE_SELECT_SITES_MOCK[0];

        spectator.component.onSiteSelect(site);

        expect(store.selectSite).toHaveBeenCalledWith(site);
    });

    it('should stage a folder selection through the store', () => {
        jest.spyOn(store, 'setPendingNode');
        const node = TREE_SELECT_MOCK[0].children[0];
        const event = { originalEvent: new Event('click'), node };

        spectator.component.onFolderSelect(event);

        expect(store.setPendingNode).toHaveBeenCalledWith(node);
    });

    it('should lazily expand a folder through the store', () => {
        jest.spyOn(store, 'expandNode');
        const node = TREE_SELECT_MOCK[0].children[0];
        const event = { originalEvent: new Event('click'), node };

        spectator.component.onFolderExpand(event);

        expect(store.expandNode).toHaveBeenCalledWith(event);
    });

    it('should forward the search input value to the store', () => {
        jest.spyOn(store, 'search');
        const input = document.createElement('input');
        input.value = 'foo';
        const event = { target: input } as unknown as Event;

        spectator.component.onSearchInput(event);

        expect(store.search).toHaveBeenCalledWith('foo');
    });

    it('should forward the sites search input value to the store', () => {
        jest.spyOn(store, 'filterSites');
        const input = document.createElement('input');
        input.value = 'demo';
        const event = { target: input } as unknown as Event;

        spectator.component.onSiteSearchInput(event);

        expect(store.filterSites).toHaveBeenCalledWith('demo');
    });

    it('should forward sites lazy-load events to the store when near the end of the list', () => {
        jest.spyOn(store, 'loadMoreSites');
        const sites = Array.from({ length: SITE_PAGE_LIMIT }, (_, index) => ({
            ...TREE_SELECT_SITES_MOCK[0],
            key: `site-${index}`,
            label: `site-${index}.dotcms.com`
        }));
        jest.spyOn(store, 'sites').mockReturnValue(sites);
        jest.spyOn(store, 'sitesPagination').mockReturnValue({
            page: 1,
            hasMore: true,
            loading: false,
            totalEntries: 100
        });

        spectator.component.onSitesLazyLoad({ first: 0, last: SITE_PAGE_LIMIT });

        expect(store.loadMoreSites).toHaveBeenCalled();
    });

    it('should not load more sites while pagination is loading', () => {
        jest.spyOn(store, 'loadMoreSites');
        jest.spyOn(store, 'sites').mockReturnValue(TREE_SELECT_SITES_MOCK);
        jest.spyOn(store, 'sitesPagination').mockReturnValue({
            page: 1,
            hasMore: true,
            loading: true,
            totalEntries: 100
        });

        spectator.component.onSitesLazyLoad({ first: 0, last: 2 });

        expect(store.loadMoreSites).not.toHaveBeenCalled();
    });

    it('should not load more sites when the viewport is not near the end', () => {
        jest.spyOn(store, 'loadMoreSites');
        jest.spyOn(store, 'sites').mockReturnValue(TREE_SELECT_SITES_MOCK);
        jest.spyOn(store, 'sitesPagination').mockReturnValue({
            page: 1,
            hasMore: true,
            loading: false,
            totalEntries: 100
        });

        spectator.component.onSitesLazyLoad({ first: 0, last: 0 });

        expect(store.loadMoreSites).not.toHaveBeenCalled();
    });

    describe('sites panel header', () => {
        beforeEach(fakeAsync(() => {
            tick();
        }));

        const createSite = (label: string): TreeNodeItem => ({
            key: label,
            label,
            data: {
                id: label,
                hostname: label,
                path: '',
                type: 'site'
            },
            expandedIcon: 'pi pi-folder-open',
            collapsedIcon: 'pi pi-folder'
        });

        const queryInOverlay = (testId: string): Element | null =>
            spectator.query(byTestId(testId)) ??
            document.querySelector(`[data-testid="${testId}"]`);

        const showSitesPanel = () => {
            const popoverDe = spectator.fixture.debugElement.query(By.directive(Popover));
            const popover = popoverDe.componentInstance as Popover;
            const trigger = document.createElement('button');
            const event = new Event('click');
            Object.defineProperty(event, 'currentTarget', { value: trigger });
            popover.show(event, trigger);
            spectator.detectChanges();
        };

        it('should show the Sites label when there are five or fewer sites', fakeAsync(() => {
            const sites = [createSite('site-1'), createSite('site-2'), createSite('site-3')];
            mockSitesPage(sites, 3);
            store.loadSites({ path: null, isRequired: false });
            tick();
            spectator.detectChanges();
            showSitesPanel();

            expect(queryInOverlay('host-folder-sites-search-input')).toBeNull();
            expect(spectator.query('[data-testid="host-folder-sites"]')).toHaveText('Sites');
        }));

        it('should show the Sites label when site count equals SITE_SEARCH_THRESHOLD', fakeAsync(() => {
            const sites = Array.from({ length: SITE_SEARCH_THRESHOLD }, (_, i) =>
                createSite(`site-${i + 1}`)
            );
            mockSitesPage(sites, SITE_SEARCH_THRESHOLD);
            store.loadSites({ path: null, isRequired: false });
            tick();
            spectator.detectChanges();
            showSitesPanel();

            expect(queryInOverlay('host-folder-sites-search-input')).toBeNull();
            expect(spectator.query('[data-testid="host-folder-sites"]')).toHaveText('Sites');
        }));

        it('should show the sites search input when there are more than five sites', fakeAsync(() => {
            const sites = Array.from({ length: SITE_SEARCH_THRESHOLD + 1 }, (_, i) =>
                createSite(`site-${i + 1}`)
            );
            mockSitesPage(sites, SITE_SEARCH_THRESHOLD + 1);
            store.loadSites({ path: null, isRequired: false });
            tick();
            expect(store.sites()).toHaveLength(SITE_SEARCH_THRESHOLD + 1);
            spectator.detectChanges();
            showSitesPanel();

            expect(queryInOverlay('host-folder-sites-search-input')).toBeTruthy();
            expect(spectator.query('[data-testid="host-folder-sites"]')).not.toHaveText('SITES');
        }));

        it('should show the empty state when the sites search has no matches', fakeAsync(() => {
            const sites = Array.from({ length: SITE_SEARCH_THRESHOLD + 1 }, (_, i) =>
                createSite(`site-${i + 1}`)
            );
            mockSitesPage(sites, SITE_SEARCH_THRESHOLD + 1);
            store.loadSites({ path: null, isRequired: false });
            tick();
            mockSitesPage([], 0);
            store.filterSites('no-match-zzzz');
            tick(300);
            spectator.detectChanges();
            showSitesPanel();

            expect(queryInOverlay('host-folder-sites-empty')).toBeTruthy();
            expect(queryInOverlay('host-folder-sites-search-input')).toBeTruthy();
        }));

        it('should keep the trigger label visible while filtering sites', fakeAsync(() => {
            mockSitesPage(TREE_SELECT_SITES_MOCK);
            store.loadSites({ path: 'demo.dotcms.com', isRequired: false });
            tick();
            spectator.detectChanges();

            mockSitesPage([TREE_SELECT_SITES_MOCK[0]], 1);
            store.filterSites('demo');
            tick();
            spectator.detectChanges();

            expect(spectator.query(byTestId('host-folder-trigger-skeleton'))).toBeNull();
            expect(spectator.query(byTestId('host-folder-trigger-label'))).toHaveText(
                'demo.dotcms.com'
            );
        }));
    });

    describe('folders panel empty states', () => {
        const queryInOverlay = (testId: string): Element | null =>
            spectator.query(byTestId(testId)) ??
            document.querySelector(`[data-testid="${testId}"]`);

        const showFoldersPanel = () => {
            const popoverDe = spectator.fixture.debugElement.query(By.directive(Popover));
            const popover = popoverDe.componentInstance as Popover;
            const trigger = document.createElement('button');
            const event = new Event('click');
            Object.defineProperty(event, 'currentTarget', { value: trigger });
            popover.show(event, trigger);
            spectator.detectChanges();
        };

        it('should hide the folder search and show the site empty state when the site has no folders', fakeAsync(() => {
            mockSitesPage(TREE_SELECT_SITES_MOCK);
            service.searchFolders.mockReturnValue(
                of({
                    folders: [],
                    pagination: { currentPage: 1, perPage: 40, totalEntries: 0 }
                })
            );
            store.loadSites({ path: null, isRequired: false });
            tick();
            store.selectSite(TREE_SELECT_SITES_MOCK[0]);
            tick();
            spectator.detectChanges();
            showFoldersPanel();

            expect(queryInOverlay('host-folder-search-input')).toBeNull();
            expect(queryInOverlay('host-folder-folders-empty')).toHaveText(
                'No folders in this site'
            );
        }));

        it('should show the folder search and search empty state when search has no matches', fakeAsync(() => {
            mockSitesPage(TREE_SELECT_SITES_MOCK);
            service.searchFolders.mockImplementation((params) => {
                if (params.recursive) {
                    return of({
                        folders: [],
                        pagination: { currentPage: 1, perPage: 40, totalEntries: 0 }
                    });
                }

                return of({
                    folders: [TREE_SELECT_MOCK[0].children[0]],
                    pagination: { currentPage: 1, perPage: 40, totalEntries: 1 }
                });
            });
            store.loadSites({ path: null, isRequired: false });
            tick();
            store.selectSite(TREE_SELECT_SITES_MOCK[0]);
            tick();
            store.search('no-match');
            tick(300);
            spectator.detectChanges();
            showFoldersPanel();

            expect(queryInOverlay('host-folder-search-input')).toBeTruthy();
            expect(queryInOverlay('host-folder-folders-empty')).toHaveText('No folders found');
        }));

        it('should show search result path on a second gray line as breadcrumb', fakeAsync(() => {
            const searchResult: TreeNodeItem = {
                key: 'ai',
                label: 'demo.dotcms.com/application/apivtl/ai/',
                data: {
                    id: 'ai',
                    hostname: 'demo.dotcms.com',
                    path: '/application/apivtl/ai/',
                    type: 'folder'
                },
                leaf: true
            };

            mockSitesPage(TREE_SELECT_SITES_MOCK);
            service.searchFolders.mockImplementation((params) => {
                if (params.recursive) {
                    return of({
                        folders: [searchResult],
                        pagination: { currentPage: 1, perPage: 40, totalEntries: 1 }
                    });
                }

                return of({
                    folders: [],
                    pagination: { currentPage: 1, perPage: 40, totalEntries: 0 }
                });
            });
            store.loadSites({ path: null, isRequired: false });
            tick();
            store.selectSite(TREE_SELECT_SITES_MOCK[0]);
            tick();
            store.search('ai');
            tick(300);
            spectator.detectChanges();
            showFoldersPanel();

            const pathEl = queryInOverlay('host-folder-search-result-path');
            expect(pathEl).toBeTruthy();
            expect(pathEl).toHaveText('demo.dotcms.com / application / apivtl / ai');
            expect(pathEl?.textContent).not.toMatch(/\(/);
            expect(pathEl).toHaveClass('text-surface-500');

            const nodeContent = document.querySelector('.p-tree-node-content');
            expect(nodeContent?.className).toContain('items-start');
            expect(document.querySelector('.p-tree-node-leaf')).toBeTruthy();
        }));

        it('should not show expand chevrons in search mode because results are flat leaves', fakeAsync(() => {
            const searchResult: TreeNodeItem = {
                key: 'containers',
                label: 'demo.dotcms.com/application/containers/',
                data: {
                    id: 'containers',
                    hostname: 'demo.dotcms.com',
                    path: '/application/containers/',
                    type: 'folder'
                },
                leaf: true
            };

            mockSitesPage(TREE_SELECT_SITES_MOCK);
            service.searchFolders.mockImplementation((params) => {
                if (params.recursive) {
                    return of({
                        folders: [searchResult],
                        pagination: { currentPage: 1, perPage: 40, totalEntries: 1 }
                    });
                }

                return of({
                    folders: [],
                    pagination: { currentPage: 1, perPage: 40, totalEntries: 0 }
                });
            });
            store.loadSites({ path: null, isRequired: false });
            tick();
            store.selectSite(TREE_SELECT_SITES_MOCK[0]);
            tick();
            store.search('containers');
            tick(300);
            spectator.detectChanges();
            showFoldersPanel();

            expect(document.querySelector('.p-tree-node-leaf')).toBeTruthy();
            expect(store.searchResults()[0]?.leaf).toBe(true);
        }));
    });

    describe('formatSearchNodePath', () => {
        it('should format nested folder path as breadcrumb with hostname', () => {
            const node: TreeNodeItem = {
                key: 'ai',
                label: 'demo.dotcms.com/application/apivtl/ai/',
                data: {
                    id: 'ai',
                    hostname: 'demo.dotcms.com',
                    path: '/application/apivtl/ai/',
                    type: 'folder'
                }
            };

            expect(spectator.component['formatSearchNodePath'](node)).toBe(
                'demo.dotcms.com / application / apivtl / ai'
            );
        });

        it('should return hostname only for site root path', () => {
            const node: TreeNodeItem = {
                key: 'root',
                label: 'demo.dotcms.com/',
                data: {
                    id: 'root',
                    hostname: 'demo.dotcms.com',
                    path: '/',
                    type: 'folder'
                }
            };

            expect(spectator.component['formatSearchNodePath'](node)).toBe('demo.dotcms.com');
        });
    });

    describe('onLoadMoreNode', () => {
        it('should load more root folders when the sentinel has no parent', () => {
            jest.spyOn(store, 'loadMore');
            const node = { key: 'load-more:root', type: 'load-more' as const };
            const event = new Event('click');
            jest.spyOn(event, 'stopPropagation');

            spectator.component.onLoadMoreNode(node, event);

            expect(event.stopPropagation).toHaveBeenCalled();
            expect(store.loadMore).toHaveBeenCalledWith(null);
        });

        it('should load more folders for the sentinel parent level', () => {
            jest.spyOn(store, 'loadMore');
            const parent = TREE_SELECT_MOCK[0];
            const node = { key: 'load-more:folder-1', type: 'load-more' as const, parent };
            const event = new Event('click');
            jest.spyOn(event, 'stopPropagation');

            spectator.component.onLoadMoreNode(node, event);

            expect(event.stopPropagation).toHaveBeenCalled();
            expect(store.loadMore).toHaveBeenCalledWith(parent);
        });

        it('should load more search results instead of folders when a search is active', () => {
            jest.spyOn(store, 'loadMoreSearchResults');
            jest.spyOn(store, 'loadMore');
            jest.spyOn(store, 'isSearching').mockReturnValue(true);
            const node = { key: 'load-more:search', type: 'load-more' as const };
            const event = new Event('click');
            jest.spyOn(event, 'stopPropagation');

            spectator.component.onLoadMoreNode(node, event);

            expect(event.stopPropagation).toHaveBeenCalled();
            expect(store.loadMoreSearchResults).toHaveBeenCalled();
            expect(store.loadMore).not.toHaveBeenCalled();
        });
    });

    it('should commit the pending selection and hide the overlay on Select', () => {
        jest.spyOn(store, 'commit');

        spectator.component.onSelect();

        expect(store.commit).toHaveBeenCalled();
        expect(overlayMock.hide).toHaveBeenCalled();
    });

    describe('onOverlayShow', () => {
        const stubFolderTree = (root: HTMLElement | undefined) => {
            Object.defineProperty(spectator.component, '$folderTree', {
                value: () => (root ? { el: { nativeElement: root } } : undefined),
                writable: true
            });
        };

        it('should open the overlay through the store', () => {
            jest.spyOn(store, 'openOverlay');

            spectator.component.onOverlayShow();

            expect(store.openOverlay).toHaveBeenCalled();
        });

        it('should schedule a scroll to the selected folder once the overlay renders', async () => {
            const node = TREE_SELECT_MOCK[0].children[0];
            store.setPendingNode(node);
            jest.spyOn(store, 'treeSelection').mockReturnValue(node);
            jest.spyOn(store, 'overlayOpen').mockReturnValue(true);

            const selectedElement = document.createElement('div');
            selectedElement.classList.add('p-tree-node-content', 'p-tree-node-selected');
            const scrollIntoViewSpy = jest.fn();
            selectedElement.scrollIntoView = scrollIntoViewSpy;

            const treeRoot = document.createElement('div');
            treeRoot.appendChild(selectedElement);
            stubFolderTree(treeRoot);

            spectator.component.onOverlayShow();
            spectator.detectChanges();
            await spectator.fixture.whenStable();

            expect(scrollIntoViewSpy).toHaveBeenCalledWith({ block: 'nearest' });
        });

        it('should not scroll when the overlay is not open', async () => {
            const node = TREE_SELECT_MOCK[0].children[0];
            store.setPendingNode(node);
            jest.spyOn(store, 'treeSelection').mockReturnValue(node);
            jest.spyOn(store, 'overlayOpen').mockReturnValue(false);
            const scrollIntoViewSpy = jest.spyOn(HTMLElement.prototype, 'scrollIntoView');

            spectator.component.onOverlayShow();
            spectator.detectChanges();
            await spectator.fixture.whenStable();

            expect(scrollIntoViewSpy).not.toHaveBeenCalled();
        });

        it('should not scroll when there is no selected folder', async () => {
            jest.spyOn(store, 'overlayOpen').mockReturnValue(true);
            jest.spyOn(store, 'treeSelection').mockReturnValue(null);
            const scrollIntoViewSpy = jest.spyOn(HTMLElement.prototype, 'scrollIntoView');

            spectator.component.onOverlayShow();
            spectator.detectChanges();
            await spectator.fixture.whenStable();

            expect(scrollIntoViewSpy).not.toHaveBeenCalled();
        });

        it('should retry on the next animation frame while folders are still loading', async () => {
            const node = TREE_SELECT_MOCK[0].children[0];
            store.setPendingNode(node);
            jest.spyOn(store, 'treeSelection').mockReturnValue(node);
            jest.spyOn(store, 'overlayOpen').mockReturnValue(true);
            jest.spyOn(store, 'foldersLoading').mockReturnValue(true);
            const rafSpy = jest.spyOn(window, 'requestAnimationFrame').mockImplementation(() => 0);

            spectator.component.onOverlayShow();
            spectator.detectChanges();
            await spectator.fixture.whenStable();

            expect(rafSpy).toHaveBeenCalled();
        });
    });

    describe('tree node expand loading', () => {
        const queryInOverlay = (testId: string): Element | null =>
            spectator.query(byTestId(testId)) ??
            document.querySelector(`[data-testid="${testId}"]`);

        const showFoldersPanel = () => {
            const popoverDe = spectator.fixture.debugElement.query(By.directive(Popover));
            const popover = popoverDe.componentInstance as Popover;
            const trigger = document.createElement('button');
            const event = new Event('click');
            Object.defineProperty(event, 'currentTarget', { value: trigger });
            popover.show(event, trigger);
            spectator.detectChanges();
        };

        it('should show a spinner on the toggler while a folder expand request is pending', fakeAsync(() => {
            const parentFolder: TreeNodeItem = {
                key: 'folder-parent',
                label: 'demo.dotcms.com/parent/',
                data: {
                    id: 'folder-parent',
                    hostname: 'demo.dotcms.com',
                    path: '/parent/',
                    type: 'folder'
                },
                leaf: false
            };
            const pending$ = new Subject<{
                folders: TreeNodeItem[];
                pagination: { currentPage: number; perPage: number; totalEntries: number };
            }>();

            mockSitesPage(TREE_SELECT_SITES_MOCK);
            service.searchFolders.mockImplementation((params) => {
                if (params.path === '/') {
                    return of({
                        folders: [parentFolder],
                        pagination: { currentPage: 1, perPage: 40, totalEntries: 1 }
                    });
                }

                return pending$.asObservable();
            });

            store.loadSites({ path: null, isRequired: false });
            tick();
            store.selectSite(TREE_SELECT_SITES_MOCK[0]);
            tick();
            spectator.detectChanges();
            showFoldersPanel();

            const folderNode = store.folders()[0];
            spectator.component.onFolderExpand({
                originalEvent: new Event('click'),
                node: folderNode
            });
            spectator.detectChanges();

            const tree = queryInOverlay('host-folder-tree');
            expect(tree?.querySelector('.pi-spinner')).toBeTruthy();
            expect(store.folders().find((item) => item.key === folderNode.key)?.loading).toBe(true);

            pending$.next({
                folders: [],
                pagination: { currentPage: 1, perPage: 40, totalEntries: 0 }
            });
            pending$.complete();
            spectator.detectChanges();

            expect(store.folders().find((item) => item.key === folderNode.key)?.loading).toBe(
                false
            );
            expect(tree?.querySelector('.pi-spinner')).toBeNull();
        }));
    });

    describe('loading states', () => {
        afterEach(() => {
            jest.restoreAllMocks();
            mockSitesPage(TREE_SELECT_SITES_MOCK);
        });

        it('should show a skeleton and spinner icon in the trigger while sites are loading', fakeAsync(() => {
            const sites$ = new Subject<{ sites: TreeNodeItem[]; pagination: DotPagination }>();
            service.getSitesPage.mockReturnValue(sites$.asObservable());

            patchState(unprotected(store), {
                confirmedNode: null,
                pendingNode: null,
                selectedSite: null,
                sitesStatus: ComponentStatus.INIT
            });

            store.loadSites({ path: 'demo.dotcms.com', isRequired: false });
            spectator.detectChanges();

            expect(store.showTriggerLoading()).toBe(true);
            expect(spectator.query(byTestId('host-folder-trigger-skeleton'))).toBeTruthy();
            expect(spectator.query(byTestId('host-folder-trigger-icon'))).toHaveClass('pi-spinner');
            expect(spectator.query(byTestId('host-folder-trigger-icon'))).toHaveClass('pi-spin');
            expect(spectator.query(byTestId('host-folder-trigger-label'))).not.toHaveText(
                'Select Host/Folder'
            );

            sites$.next(createSitesPageResponse(TREE_SELECT_SITES_MOCK));
            sites$.complete();
            tick();
            spectator.detectChanges();

            expect(spectator.query(byTestId('host-folder-trigger-skeleton'))).toBeNull();
        }));

        it('should show the sites loading state in the overlay', fakeAsync(() => {
            const queryInOverlay = (testId: string): Element | null =>
                spectator.query(byTestId(testId)) ??
                document.querySelector(`[data-testid="${testId}"]`);

            jest.spyOn(store, 'showSitesPanelLoading').mockReturnValue(true);
            jest.spyOn(store, 'filteredSites').mockReturnValue([]);
            spectator.detectChanges();

            const popoverDe = spectator.fixture.debugElement.query(By.directive(Popover));
            const popover = popoverDe.componentInstance as Popover;
            const trigger = document.createElement('button');
            const event = new Event('click');
            Object.defineProperty(event, 'currentTarget', { value: trigger });
            popover.show(event, trigger);
            spectator.detectChanges();

            expect(queryInOverlay('host-folder-sites-loading')).toBeTruthy();
            expect(queryInOverlay('host-folder-sites-loading')).toHaveText('Loading sites...');
        }));

        it('should show the folders loading state in the overlay', fakeAsync(() => {
            const queryInOverlay = (testId: string): Element | null =>
                spectator.query(byTestId(testId)) ??
                document.querySelector(`[data-testid="${testId}"]`);

            jest.spyOn(store, 'sitesLoading').mockReturnValue(false);
            jest.spyOn(store, 'showFoldersPanelLoading').mockReturnValue(true);
            jest.spyOn(store, 'showFolderSearch').mockReturnValue(false);
            jest.spyOn(store, 'displayedFolders').mockReturnValue([]);
            spectator.detectChanges();

            const popoverDe = spectator.fixture.debugElement.query(By.directive(Popover));
            const popover = popoverDe.componentInstance as Popover;
            const trigger = document.createElement('button');
            const event = new Event('click');
            Object.defineProperty(event, 'currentTarget', { value: trigger });
            popover.show(event, trigger);
            spectator.detectChanges();

            expect(queryInOverlay('host-folder-folders-loading')).toBeTruthy();
            expect(queryInOverlay('host-folder-folders-loading')).toHaveText('Loading folders...');
            expect(queryInOverlay('host-folder-search-input')).toBeNull();
        }));
    });

    it('should propagate the committed value through the form control accessor', () => {
        const onChange = jest.fn();
        const onTouched = jest.fn();
        spectator.component.registerOnChange(onChange);
        spectator.component.registerOnTouched(onTouched);

        const site = TREE_SELECT_SITES_MOCK[0];
        store.setPendingNode(site);
        store.commit();
        spectator.detectChanges();

        expect(onChange).toHaveBeenCalledWith('demo.dotcms.com:/');
        expect(onTouched).toHaveBeenCalled();
    });

    it('should show the full display path in the trigger label', () => {
        const node = TREE_SELECT_MOCK[0].children[0].children[0];
        store.setPendingNode(node);
        store.commit();
        spectator.detectChanges();

        expect(spectator.query(byTestId('host-folder-trigger-label'))).toHaveText(
            'demo.dotcms.com / level1 / child1'
        );
    });

    describe('copyPath', () => {
        beforeEach(() => {
            clipboard.copy.mockClear();
        });

        it('should copy the confirmed path to the clipboard', () => {
            const site = TREE_SELECT_SITES_MOCK[0];
            store.setPendingNode(site);
            store.commit();
            spectator.detectChanges();

            spectator.component.copyPath();

            expect(clipboard.copy).toHaveBeenCalledWith('//demo.dotcms.com/');
        });

        it('should do nothing when there is no confirmed selection', () => {
            jest.spyOn(store, 'copyPath').mockReturnValue('');

            spectator.component.copyPath();

            expect(clipboard.copy).not.toHaveBeenCalled();
        });

        it('should show a check icon after copying and revert after a delay', () => {
            jest.useFakeTimers();
            const site = TREE_SELECT_SITES_MOCK[0];
            store.setPendingNode(site);
            store.commit();
            spectator.detectChanges();

            spectator.component.copyPath();
            spectator.detectChanges();

            expect(spectator.query(byTestId('host-folder-copy-icon'))).toHaveText('check');

            jest.advanceTimersByTime(1500);
            spectator.detectChanges();

            expect(spectator.query(byTestId('host-folder-copy-icon'))).toHaveText('content_copy');
            jest.useRealTimers();
        });
    });
});
