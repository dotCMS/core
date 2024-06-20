import { createFakeEvent } from '@ngneat/spectator';
import { mockProvider } from '@ngneat/spectator/jest';

import { TestBed } from '@angular/core/testing';

import { DotEditContentService } from '../../../../services/dot-edit-content.service';
import { TREE_SELECT_MOCK } from '../../../../utils/mocks';
import { HostFolderFiledStore } from '../host-folder-field.store';

describe('StoreMethod: chooseNode', () => {
    let store: InstanceType<typeof HostFolderFiledStore>;

    beforeEach(() => {
        store = TestBed.overrideProvider(
            DotEditContentService,
            mockProvider(DotEditContentService)
        ).runInInjectionContext(() => new HostFolderFiledStore());
    });

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
