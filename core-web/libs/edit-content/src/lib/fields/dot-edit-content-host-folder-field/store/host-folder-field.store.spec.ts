import { createFakeEvent } from '@ngneat/spectator';
import { SpyObject, mockProvider } from '@ngneat/spectator/jest';
import { of } from 'rxjs';

import { TestBed } from '@angular/core/testing';

import { SYSTEM_HOST_NAME, HostFolderFiledStore } from './host-folder-field.store';

import { DotEditContentService } from '../../../services/dot-edit-content.service';
import { TREE_SELECT_SITES_MOCK, TREE_SELECT_MOCK } from '../../../utils/mocks';

describe('HostFolderFiledStore', () => {
    let store: InstanceType<typeof HostFolderFiledStore>;
    let service: SpyObject<DotEditContentService>;

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [
                HostFolderFiledStore,
                mockProvider(DotEditContentService, {
                    getSitesTreePath: jest.fn(() => of(TREE_SELECT_SITES_MOCK))
                })
            ]
        });

        store = TestBed.inject(HostFolderFiledStore);

        service = TestBed.inject(DotEditContentService) as SpyObject<DotEditContentService>;
    });

    describe('Method: loadSites', () => {
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

    describe('Method: chooseNode', () => {
        it('should update the form value with the correct format with root path', () => {
            const mockItem = {
                originalEvent: createFakeEvent('input'),
                node: { ...TREE_SELECT_MOCK[0] }
            };
            store.chooseNode(mockItem);
            const value = store.pathToSave();
            expect(value).toBe('demo.dotcms.com:/');
        });

        it('should update the form value with the correct format with one level', () => {
            const mockItem = {
                originalEvent: createFakeEvent('input'),
                node: { ...TREE_SELECT_MOCK[0].children[0] }
            };
            store.chooseNode(mockItem);
            const value = store.pathToSave();
            expect(value).toBe('demo.dotcms.com:/level1/');
        });

        it('should update the form value with the correct format with two level', () => {
            const mockItem = {
                originalEvent: createFakeEvent('input'),
                node: { ...TREE_SELECT_MOCK[0].children[0].children[0] }
            };
            store.chooseNode(mockItem);
            const value = store.pathToSave();
            expect(value).toBe('demo.dotcms.com:/level1/child1/');
        });

        it('should be null when data is null', () => {
            const mockItem = {
                originalEvent: createFakeEvent('input'),
                node: { ...TREE_SELECT_MOCK[0].children[0].children[0] }
            };
            delete mockItem.node.data;
            store.chooseNode(mockItem);
            const value = store.pathToSave();
            expect(value).toBe(null);
        });
    });
});
