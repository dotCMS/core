import { SpyObject, mockProvider } from '@ngneat/spectator/jest';
import { of } from 'rxjs';

import { TestBed } from '@angular/core/testing';

import { SYSTEM_HOST_NAME } from './loadSites';

import { DotEditContentService } from '../../../../services/dot-edit-content.service';
import { TREE_SELECT_SITES_MOCK } from '../../../../utils/mocks';
import { HostFolderFiledStore } from '../host-folder-field.store';

describe('StoreMethod: loadSites', () => {
    let store: InstanceType<typeof HostFolderFiledStore>;
    let service: SpyObject<DotEditContentService>;

    beforeEach(() => {
        store = TestBed.overrideProvider(
            DotEditContentService,
            mockProvider(DotEditContentService)
        ).runInInjectionContext(() => new HostFolderFiledStore());
        service = TestBed.inject(DotEditContentService) as SpyObject<DotEditContentService>;
    });

    describe('System Host isRequired', () => {
        it('should include System Host when isRequired is false.', () => {
            service.getSitesTreePath.mockReturnValue(of(TREE_SELECT_SITES_MOCK));
            const props = {
                path: null,
                isRequired: false
            };
            store.loadSites(props);
            const hasSystemHost = store.tree().some((item) => item.label === SYSTEM_HOST_NAME);
            expect(hasSystemHost).toBe(true);
        });

        it('should not include System Host when isRequired is true.', () => {
            service.getSitesTreePath.mockReturnValue(of(TREE_SELECT_SITES_MOCK));
            const props = {
                path: null,
                isRequired: true
            };
            store.loadSites(props);
            const hasSystemHost = store.tree().some((item) => item.label === SYSTEM_HOST_NAME);
            expect(hasSystemHost).toBe(false);
        });
    });

    describe('when path is not empty', () => {
        it('should select the node if the path is not empty and not required', () => {
            const node = TREE_SELECT_SITES_MOCK[0];
            service.getSitesTreePath.mockReturnValue(of(TREE_SELECT_SITES_MOCK));
            const props = {
                path: node.label,
                isRequired: false
            };
            store.loadSites(props);
            expect(service.getCurrentSiteAsTreeNodeItem).not.toHaveBeenCalled();
            expect(store.nodeSelected().key).toBe(node.key);
        });

        it('should select the node if the path is not empty and is required', () => {
            const node = TREE_SELECT_SITES_MOCK[0];
            service.getSitesTreePath.mockReturnValue(of(TREE_SELECT_SITES_MOCK));
            const props = {
                path: node.label,
                isRequired: true
            };
            store.loadSites(props);
            expect(service.getCurrentSiteAsTreeNodeItem).not.toHaveBeenCalled();
            expect(store.nodeSelected().key).toBe(node.key);
        });
    });

    describe('when path is empty', () => {
        it('should select System Host if the path is not empty and not required', () => {
            service.getSitesTreePath.mockReturnValue(of(TREE_SELECT_SITES_MOCK));
            const props = {
                path: null,
                isRequired: false
            };
            store.loadSites(props);
            expect(service.getCurrentSiteAsTreeNodeItem).not.toHaveBeenCalled();
            expect(store.nodeSelected().label).toBe(SYSTEM_HOST_NAME);
        });

        it('should select current site if the path is not empty and is required', () => {
            const hostNode = TREE_SELECT_SITES_MOCK[1];
            service.getSitesTreePath.mockReturnValue(of(TREE_SELECT_SITES_MOCK));
            service.getCurrentSiteAsTreeNodeItem.mockReturnValue(of(hostNode));
            const props = {
                path: null,
                isRequired: true
            };
            store.loadSites(props);
            expect(service.getCurrentSiteAsTreeNodeItem).toHaveBeenCalled();
            expect(store.nodeSelected().label).toBe(hostNode.label);
        });
    });
});
