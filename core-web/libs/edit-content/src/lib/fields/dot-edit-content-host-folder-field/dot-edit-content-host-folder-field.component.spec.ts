import { createFakeEvent } from '@ngneat/spectator';
import { Spectator, createComponentFactory, mockProvider, SpyObject } from '@ngneat/spectator/jest';
import { of } from 'rxjs';

import { ControlContainer, FormGroupDirective } from '@angular/forms';

import { mockMatchMedia } from '@dotcms/utils-testing';

import { DotEditContentHostFolderFieldComponent } from './dot-edit-content-host-folder-field.component';

import { DotEditContentService } from '../../services/dot-edit-content.service';
import {
    HOST_FOLDER_TEXT_MOCK,
    TREE_SELECT_MOCK_NODE,
    TREE_SELECT_SITES_MOCK,
    TREE_SELECT_MOCK,
    createFormGroupDirectiveMock
} from '../../utils/mocks';

describe('DotEditContentHostFolderFieldComponent', () => {
    let spectator: Spectator<DotEditContentHostFolderFieldComponent>;
    let component: DotEditContentHostFolderFieldComponent;
    let service: SpyObject<DotEditContentService>;

    const createComponent = createComponentFactory({
        component: DotEditContentHostFolderFieldComponent,
        componentViewProviders: [
            { provide: ControlContainer, useValue: createFormGroupDirectiveMock() }
        ],
        providers: [
            FormGroupDirective,
            mockProvider(DotEditContentService, {
                getSitesTreePath: jest.fn().mockReturnValue(of(TREE_SELECT_SITES_MOCK))
            })
        ],
        detectChanges: false
    });

    beforeEach(() => {
        spectator = createComponent({
            props: {
                field: {
                    ...HOST_FOLDER_TEXT_MOCK
                }
            }
        });
        service = spectator.inject(DotEditContentService);
        component = spectator.component;
        component.formControl.setValue(null);
        mockMatchMedia();
    });

    it('should create the component', () => {
        spectator.detectChanges();
        expect(spectator.component).toBeTruthy();
    });

    it('should show options', () => {
        spectator.detectChanges();
        const options = component.options();

        expect(service.getSitesTreePath).toHaveBeenCalled();
        expect(options).toBe(TREE_SELECT_SITES_MOCK);
        expect(component.treeSelect.options).toBe(TREE_SELECT_SITES_MOCK);
    });

    describe('The init value with the root path', () => {
        it('should show a root path', () => {
            const rootPath = TREE_SELECT_SITES_MOCK[0].key;
            component.formControl.setValue(rootPath);
            spectator.detectChanges();
            const label = spectator.query('.p-treeselect-label');
            expect(label).toHaveText(rootPath);
        });

        it('should show the selected path in the tree', () => {
            const rootPath = TREE_SELECT_SITES_MOCK[0].key;
            component.formControl.setValue(rootPath);
            spectator.detectChanges();

            const triggerElement = spectator.query('.p-treeselect-trigger');
            spectator.click(triggerElement);
            const labelElement = spectator.query('.p-treenode-content.p-highlight');
            expect(labelElement).toHaveText(rootPath);
        });
    });

    describe('The init value with the one levels', () => {
        it('should show a path selected', () => {
            service.buildTreeByPaths.mockReturnValue(of(TREE_SELECT_MOCK_NODE));
            const rootPath = 'demo.dotcms.com/level1/';
            component.formControl.setValue(rootPath);
            spectator.detectChanges();
            const label = spectator.query('.p-treeselect-label');
            expect(label).toHaveText(rootPath);
        });

        it('should show the selected path in the tree', () => {
            service.buildTreeByPaths.mockReturnValue(of(TREE_SELECT_MOCK_NODE));
            const rootPath = 'demo.dotcms.com/level1/';
            component.formControl.setValue(rootPath);
            spectator.detectChanges();

            const triggerElement = spectator.query('.p-treeselect-trigger');
            spectator.click(triggerElement);
            const labelElement = spectator.query('.p-treenode-content.p-highlight');
            expect(labelElement).toHaveText('level1');
        });
    });

    describe('The init value with the two levels', () => {
        it('should show a path selected', () => {
            const mockResponse = {
                ...TREE_SELECT_MOCK_NODE,
                node: { ...TREE_SELECT_MOCK[0].children[0].children[0] }
            };
            service.buildTreeByPaths.mockReturnValue(of(mockResponse));
            const rootPath = 'demo.dotcms.com/level1/child1/';
            component.formControl.setValue(rootPath);
            spectator.detectChanges();
            const label = spectator.query('.p-treeselect-label');
            expect(label).toHaveText(rootPath);
        });

        it('should show the selected path in the tree', () => {
            const mockResponse = {
                ...TREE_SELECT_MOCK_NODE,
                node: { ...TREE_SELECT_MOCK[0].children[0].children[0] }
            };
            service.buildTreeByPaths.mockReturnValue(of(mockResponse));
            const rootPath = 'demo.dotcms.com/level1/child1/';
            component.formControl.setValue(rootPath);
            spectator.detectChanges();

            const triggerElement = spectator.query('.p-treeselect-trigger');
            spectator.click(triggerElement);
            const labelElement = spectator.query('.p-treenode-content.p-highlight');
            expect(labelElement).toHaveText('child1');
        });
    });

    describe('Select levels: onNodeSelect', () => {
        it('should update the form value with the correct format with root path', () => {
            spectator.detectChanges();
            const mockItem = {
                originalEvent: createFakeEvent('input'),
                node: { ...TREE_SELECT_MOCK[0] }
            };
            component.onNodeSelect(mockItem);
            const value = component.formControl.value;
            expect(value).toBe('demo.dotcms.com:/');
        });

        it('should update the form value with the correct format with one level', () => {
            spectator.detectChanges();
            const mockItem = {
                originalEvent: createFakeEvent('input'),
                node: { ...TREE_SELECT_MOCK[0].children[0] }
            };
            component.onNodeSelect(mockItem);
            const value = component.formControl.value;
            expect(value).toBe('demo.dotcms.com:/level1/');
        });

        it('should update the form value with the correct format with two level', () => {
            spectator.detectChanges();
            const mockItem = {
                originalEvent: createFakeEvent('input'),
                node: { ...TREE_SELECT_MOCK[0].children[0].children[0] }
            };
            component.onNodeSelect(mockItem);
            const value = component.formControl.value;
            expect(value).toBe('demo.dotcms.com:/level1/child1/');
        });
    });

    describe('Expand level: onNodeExpand', () => {
        it('should not call getFoldersTreeNode when it is a node with children', () => {
            spectator.detectChanges();
            const mockItem = {
                originalEvent: createFakeEvent('select'),
                node: {
                    ...TREE_SELECT_MOCK[0]
                }
            };
            component.onNodeExpand(mockItem);
            expect(service.getFoldersTreeNode).not.toHaveBeenCalled();
        });

        it('should not call getFoldersTreeNode when it is a node is loading', () => {
            spectator.detectChanges();
            const mockItem = {
                originalEvent: createFakeEvent('select'),
                node: {
                    ...TREE_SELECT_MOCK[0],
                    icon: 'spinner'
                }
            };
            component.onNodeExpand(mockItem);
            expect(service.getFoldersTreeNode).not.toHaveBeenCalled();
        });

        it('should call getFoldersTreeNode when it is a node with no children', async () => {
            const response = TREE_SELECT_MOCK[0].children;
            service.getFoldersTreeNode.mockReturnValue(of(response));
            spectator.detectChanges();

            await spectator.fixture.whenStable();

            const treeDetectChangesSpy = jest.spyOn(component.treeSelect.cd, 'detectChanges');
            const mockNode = { ...TREE_SELECT_SITES_MOCK[0] };
            const mockItem = {
                originalEvent: createFakeEvent('select'),
                node: mockNode
            };
            component.onNodeExpand(mockItem);
            const hostName = mockNode.data.hostname;
            const path = mockNode.data.path;
            expect(service.getFoldersTreeNode).toHaveBeenCalledWith(hostName, path);
            expect(mockNode.children).toBe(response);
            expect(mockNode.leaf).toBe(true);
            expect(mockNode.icon).toBe('pi pi-folder-open');
            expect(treeDetectChangesSpy).toHaveBeenCalled();
        });

        it('should call getFoldersTreeNode when it is a node with no children and empty response', async () => {
            const response = [];
            service.getFoldersTreeNode.mockReturnValue(of(response));
            spectator.detectChanges();

            await spectator.fixture.whenStable();

            const treeDetectChangesSpy = jest.spyOn(component.treeSelect.cd, 'detectChanges');
            const mockNode = { ...TREE_SELECT_SITES_MOCK[0] };
            const mockItem = {
                originalEvent: createFakeEvent('select'),
                node: mockNode
            };
            component.onNodeExpand(mockItem);
            const hostName = mockNode.data.hostname;
            const path = mockNode.data.path;
            expect(service.getFoldersTreeNode).toHaveBeenCalledWith(hostName, path);
            expect(mockNode.children).toBe(response);
            expect(mockNode.leaf).toBe(true);
            expect(mockNode.icon).toBe('pi pi-folder-open');
            expect(treeDetectChangesSpy).toHaveBeenCalled();
        });
    });
});
