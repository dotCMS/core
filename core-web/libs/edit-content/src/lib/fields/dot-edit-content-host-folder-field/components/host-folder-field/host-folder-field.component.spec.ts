import {
    byTestId,
    createComponentFactory,
    mockProvider,
    Spectator,
    SpyObject
} from '@openng/spectator/jest';
import { of } from 'rxjs';

import { Clipboard } from '@angular/cdk/clipboard';
import { fakeAsync, tick } from '@angular/core/testing';
import { By } from '@angular/platform-browser';

import { Popover } from 'primeng/popover';

import { DotHttpErrorManagerService, DotMessageService } from '@dotcms/data-access';
import { TreeNodeItem } from '@dotcms/dotcms-models';
import { DotBrowsingService } from '@dotcms/ui';

import { DotHostFolderFieldComponent } from './host-folder-field.component';

import { TREE_SELECT_MOCK, TREE_SELECT_SITES_MOCK } from '../../../../utils/mocks';
import { HostFolderFiledStore, SITE_SEARCH_THRESHOLD } from '../../store/host-folder-field.store';
import { MessageServiceMock } from '../../utils/mocks';

describe('DotHostFolderFieldComponent', () => {
    let spectator: Spectator<DotHostFolderFieldComponent>;
    let service: SpyObject<DotBrowsingService>;
    let clipboard: SpyObject<Clipboard>;
    let store: InstanceType<typeof HostFolderFiledStore>;

    const createComponent = createComponentFactory({
        component: DotHostFolderFieldComponent,
        providers: [
            HostFolderFiledStore,
            mockProvider(DotHttpErrorManagerService, {
                handle: jest.fn()
            }),
            mockProvider(DotBrowsingService, {
                getSitesTreePath: jest.fn(() => of(TREE_SELECT_SITES_MOCK)),
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
        service.getSitesTreePath.mockClear();

        spectator.component.writeValue('//demo.dotcms.com/system/');
        spectator.detectChanges();

        expect(service.getSitesTreePath).toHaveBeenCalled();
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
            service.getSitesTreePath.mockReturnValue(of(sites));
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
            service.getSitesTreePath.mockReturnValue(of(sites));
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
            service.getSitesTreePath.mockReturnValue(of(sites));
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
            service.getSitesTreePath.mockReturnValue(of(sites));
            store.loadSites({ path: null, isRequired: false });
            tick();
            store.setSiteSearchTerm('no-match-zzzz');
            spectator.detectChanges();
            showSitesPanel();

            expect(queryInOverlay('host-folder-sites-empty')).toBeTruthy();
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
            service.getSitesTreePath.mockReturnValue(of(TREE_SELECT_SITES_MOCK));
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
            service.getSitesTreePath.mockReturnValue(of(TREE_SELECT_SITES_MOCK));
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
