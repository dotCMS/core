import {
    Spectator,
    byTestId,
    createComponentFactory,
    mockProvider,
    SpyObject
} from '@ngneat/spectator/jest';
import { of } from 'rxjs';

import { Clipboard } from '@angular/cdk/clipboard';

import { DotMessageService } from '@dotcms/data-access';
import { DotBrowsingService } from '@dotcms/ui';

import { DotHostFolderFieldComponent } from './host-folder-field.component';

import { TREE_SELECT_SITES_MOCK, TREE_SELECT_MOCK } from '../../../../utils/mocks';
import { HostFolderFiledStore } from '../../store/host-folder-field.store';
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
    });

    it('should commit the pending selection and hide the overlay on Select', () => {
        jest.spyOn(store, 'commit');

        spectator.component.onSelect();

        expect(store.commit).toHaveBeenCalled();
        expect(overlayMock.hide).toHaveBeenCalled();
    });

    describe('onOverlayShow', () => {
        type ComponentWithPrivateScroll = {
            scrollSelectedFolderIntoView: (attempt?: number) => void;
        };

        const componentWithScroll = () =>
            spectator.component as unknown as ComponentWithPrivateScroll;

        it('should open the overlay through the store', () => {
            jest.spyOn(store, 'openOverlay');

            spectator.component.onOverlayShow();

            expect(store.openOverlay).toHaveBeenCalled();
        });

        it('should schedule a scroll to the selected folder once the overlay renders', async () => {
            const scrollSpy = jest.spyOn(componentWithScroll(), 'scrollSelectedFolderIntoView');

            spectator.component.onOverlayShow();
            spectator.detectChanges();
            await spectator.fixture.whenStable();

            expect(scrollSpy).toHaveBeenCalled();
        });
    });

    describe('scrollSelectedFolderIntoView', () => {
        type ComponentWithPrivateScroll = {
            scrollSelectedFolderIntoView: (attempt?: number) => void;
        };

        const callScroll = () =>
            (
                spectator.component as unknown as ComponentWithPrivateScroll
            ).scrollSelectedFolderIntoView();

        const stubFolderTree = (root: HTMLElement | undefined) => {
            Object.defineProperty(spectator.component, '$folderTree', {
                value: () => (root ? { el: { nativeElement: root } } : undefined),
                writable: true
            });
        };

        it('should not scroll when the overlay is not open', () => {
            const node = TREE_SELECT_MOCK[0].children[0];
            store.setPendingNode(node);
            const scrollIntoViewSpy = jest.spyOn(HTMLElement.prototype, 'scrollIntoView');

            callScroll();

            expect(scrollIntoViewSpy).not.toHaveBeenCalled();
        });

        it('should not scroll when there is no selected folder', () => {
            store.openOverlay();
            const scrollIntoViewSpy = jest.spyOn(HTMLElement.prototype, 'scrollIntoView');

            callScroll();

            expect(scrollIntoViewSpy).not.toHaveBeenCalled();
        });

        it('should retry on the next animation frame while folders are still loading', () => {
            const node = TREE_SELECT_MOCK[0].children[0];
            store.setPendingNode(node);
            store.openOverlay();
            jest.spyOn(store, 'treeSelection').mockReturnValue(node);
            jest.spyOn(store, 'foldersLoading').mockReturnValue(true);
            const rafSpy = jest.spyOn(window, 'requestAnimationFrame').mockImplementation(() => 0);

            callScroll();

            expect(rafSpy).toHaveBeenCalled();
        });

        it('should scroll the selected node into view once folders finish loading', () => {
            const node = TREE_SELECT_MOCK[0].children[0];
            store.setPendingNode(node);
            store.openOverlay();
            jest.spyOn(store, 'treeSelection').mockReturnValue(node);

            const selectedElement = document.createElement('div');
            selectedElement.classList.add('p-tree-node-content', 'p-tree-node-selected');
            const scrollIntoViewSpy = jest.fn();
            selectedElement.scrollIntoView = scrollIntoViewSpy;

            const treeRoot = document.createElement('div');
            treeRoot.appendChild(selectedElement);
            stubFolderTree(treeRoot);

            callScroll();

            expect(scrollIntoViewSpy).toHaveBeenCalledWith({ block: 'nearest' });
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
