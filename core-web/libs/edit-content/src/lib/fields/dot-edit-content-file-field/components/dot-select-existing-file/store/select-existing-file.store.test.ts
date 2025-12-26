import { createFakeEvent } from '@ngneat/spectator';
import { SpyObject, mockProvider } from '@ngneat/spectator/jest';
import { of, throwError } from 'rxjs';

import { fakeAsync, TestBed, tick } from '@angular/core/testing';

import { ComponentStatus } from '@dotcms/dotcms-models';

import { SelectExisingFileStore } from './select-existing-file.store';

import { DotEditContentService } from '../../../../../services/dot-edit-content.service';
import { TREE_SELECT_MOCK, TREE_SELECT_SITES_MOCK } from '../../../../../utils/mocks';

describe('SelectExisingFileStore', () => {
    let store: InstanceType<typeof SelectExisingFileStore>;
    let editContentService: SpyObject<DotEditContentService>;

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [
                SelectExisingFileStore,
                mockProvider(DotEditContentService, {
                    getSitesTreePath: jest.fn().mockReturnValue(of(TREE_SELECT_SITES_MOCK)),
                    getContentByFolder: jest.fn().mockReturnValue(of([]))
                })
            ]
        });

        store = TestBed.inject(SelectExisingFileStore);
        editContentService = TestBed.inject(
            DotEditContentService
        ) as SpyObject<DotEditContentService>;
    });

    it('should be created', () => {
        expect(store).toBeTruthy();
    });

    describe('Method: loadFolders', () => {
        it('should set folders status to LOADING and then to LOADED with data', fakeAsync(() => {
            editContentService.getSitesTreePath.mockReturnValue(of(TREE_SELECT_SITES_MOCK));

            store.loadFolders();

            tick(50);

            expect(store.folders().status).toBe(ComponentStatus.LOADED);
            expect(store.folders().data).toEqual(TREE_SELECT_SITES_MOCK);
        }));

        it('should set folders status to ERROR on service error', fakeAsync(() => {
            editContentService.getSitesTreePath.mockReturnValue(throwError(() => 'error'));

            store.loadFolders();

            tick(50);

            expect(store.folders().status).toBe(ComponentStatus.ERROR);
            expect(store.folders().data).toEqual([]);
        }));
    });

    describe('Method: loadChildren', () => {
        it('should load children for a node', fakeAsync(() => {
            const mockChildren = {
                parent: {
                    id: 'demo.dotcms.com',
                    hostName: 'demo.dotcms.com',
                    path: '',
                    type: 'site',
                    addChildrenAllowed: true
                },
                folders: [...TREE_SELECT_SITES_MOCK]
            };

            editContentService.getFoldersTreeNode.mockReturnValue(of(mockChildren));

            const node = { ...TREE_SELECT_MOCK[0] };

            const mockItem = {
                originalEvent: createFakeEvent('click'),
                node
            };
            store.loadChildren(mockItem);

            tick(50);

            expect(node.children).toEqual(mockChildren.folders);
            expect(node.loading).toBe(false);
            expect(node.leaf).toBe(true);
            expect(node.icon).toBe('pi pi-folder-open');
            expect(store.folders().nodeExpaned).toBe(node);
        }));

        it('should handle error when loading children', fakeAsync(() => {
            editContentService.getFoldersTreeNode.mockReturnValue(throwError(() => 'error'));

            const node = { ...TREE_SELECT_MOCK[0], children: [] };

            const mockItem = {
                originalEvent: createFakeEvent('click'),
                node
            };
            store.loadChildren(mockItem);

            tick(50);

            expect(node.children).toEqual([]);
            expect(node.loading).toBe(false);
        }));
    });
});
