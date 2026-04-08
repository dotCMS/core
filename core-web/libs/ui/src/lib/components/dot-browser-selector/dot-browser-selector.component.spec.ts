import { createComponentFactory, mockProvider, Spectator } from '@ngneat/spectator/jest';
import { of } from 'rxjs';

import { CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { TestBed } from '@angular/core/testing';

import { DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';

import { DotContentletService } from '@dotcms/data-access';
import { ComponentStatus, TreeNodeItem, TreeNodeSelectItem } from '@dotcms/dotcms-models';
import { createFakeContentlet, createFakeEvent } from '@dotcms/utils-testing';

import { DotBrowserSelectorComponent } from './dot-browser-selector.component';
import { DotBrowserSelectorStore, SYSTEM_HOST_ID } from './store/browser.store';

import { DotMessagePipe } from '../../dot-message/dot-message.pipe';

const createMockStore = () => ({
    folders: jest.fn().mockReturnValue({ data: [], status: ComponentStatus.INIT }),
    content: jest.fn().mockReturnValue({ data: [], status: ComponentStatus.INIT, error: null }),
    foldersIsLoading: jest.fn().mockReturnValue(false),
    contentIsLoading: jest.fn().mockReturnValue(false),
    selectedContent: jest.fn().mockReturnValue(null),
    loadContent: jest.fn(),
    loadFolders: jest.fn(),
    loadChildren: jest.fn(),
    setSelectedContent: jest.fn(),
    uploadFile: jest.fn()
});

const mockNode = (id: string): TreeNodeItem => ({
    key: id,
    label: id,
    data: { id, hostname: id, path: '', type: 'site' },
    expandedIcon: 'pi pi-folder-open',
    collapsedIcon: 'pi pi-folder'
});

const mockNodeSelectEvent = (id: string): TreeNodeSelectItem => ({
    originalEvent: createFakeEvent('click'),
    node: mockNode(id)
});

describe('DotBrowserSelectorComponent', () => {
    let spectator: Spectator<DotBrowserSelectorComponent>;
    let mockStore: ReturnType<typeof createMockStore>;

    const createComponent = createComponentFactory({
        component: DotBrowserSelectorComponent,
        schemas: [CUSTOM_ELEMENTS_SCHEMA],
        providers: [
            mockProvider(DynamicDialogRef),
            mockProvider(DotContentletService, {
                getContentletByInodeWithContent: jest
                    .fn()
                    .mockReturnValue(of(createFakeContentlet()))
            }),
            { provide: DynamicDialogConfig, useValue: { data: null } }
        ],
        detectChanges: false
    });

    beforeEach(() => {
        mockStore = createMockStore();

        // Override the component's own providers so the real SignalStore
        // (and its transitive deps) is never constructed in these tests.
        TestBed.overrideComponent(DotBrowserSelectorComponent, {
            set: {
                imports: [DotMessagePipe],
                schemas: [CUSTOM_ELEMENTS_SCHEMA],
                providers: [{ provide: DotBrowserSelectorStore, useValue: mockStore }]
            }
        });

        spectator = createComponent();
        spectator.detectChanges();
    });

    describe('$uploadDisabled', () => {
        it('should be false when hostFolderId is SYSTEM_HOST_ID (initial state)', () => {
            expect(spectator.component.$uploadDisabled()).toBe(false);
        });

        it('should be true when hostFolderId is an empty string', () => {
            spectator.component.$folderParams.set({ hostFolderId: '', mimeTypes: [] });
            expect(spectator.component.$uploadDisabled()).toBe(true);
        });

        it('should be false when a real site is selected', () => {
            spectator.component.$folderParams.set({
                hostFolderId: 'demo.dotcms.com',
                mimeTypes: []
            });
            expect(spectator.component.$uploadDisabled()).toBe(false);
        });

        it('should transition from true to false after onNodeSelect sets a valid id', () => {
            spectator.component.$folderParams.set({ hostFolderId: '', mimeTypes: [] });
            expect(spectator.component.$uploadDisabled()).toBe(true);

            spectator.component.onNodeSelect(mockNodeSelectEvent('demo.dotcms.com'));

            expect(spectator.component.$uploadDisabled()).toBe(false);
        });
    });

    describe('$acceptAttr', () => {
        it('should return "*" when mimeTypes is empty', () => {
            expect(spectator.component.$acceptAttr()).toBe('*');
        });

        it('should append "/*" when mimeType has no slash', () => {
            spectator.component.$folderParams.set({
                hostFolderId: SYSTEM_HOST_ID,
                mimeTypes: ['image']
            });
            expect(spectator.component.$acceptAttr()).toBe('image/*');
        });

        it('should keep full MIME type unchanged when it already contains a slash', () => {
            spectator.component.$folderParams.set({
                hostFolderId: SYSTEM_HOST_ID,
                mimeTypes: ['image/png']
            });
            expect(spectator.component.$acceptAttr()).toBe('image/png');
        });

        it('should join multiple mimeTypes with a comma', () => {
            spectator.component.$folderParams.set({
                hostFolderId: SYSTEM_HOST_ID,
                mimeTypes: ['image', 'application/pdf']
            });
            expect(spectator.component.$acceptAttr()).toBe('image/*,application/pdf');
        });
    });

    describe('onNodeSelect', () => {
        it('should update hostFolderId in $folderParams', () => {
            spectator.component.onNodeSelect(mockNodeSelectEvent('demo.dotcms.com'));
            expect(spectator.component.$folderParams().hostFolderId).toBe('demo.dotcms.com');
        });

        it('should preserve mimeTypes when updating hostFolderId', () => {
            spectator.component.$folderParams.set({
                hostFolderId: SYSTEM_HOST_ID,
                mimeTypes: ['image']
            });
            spectator.component.onNodeSelect(mockNodeSelectEvent('demo.dotcms.com'));
            expect(spectator.component.$folderParams().mimeTypes).toEqual(['image']);
        });

        it('should not update $folderParams when the selected node has no id', () => {
            const before = spectator.component.$folderParams().hostFolderId;
            const invalidEvent: TreeNodeSelectItem = {
                originalEvent: createFakeEvent('click'),
                node: { ...mockNode(''), data: {} as TreeNodeItem['data'] }
            };
            spectator.component.onNodeSelect(invalidEvent);
            expect(spectator.component.$folderParams().hostFolderId).toBe(before);
        });
    });

    describe('onFileUpload', () => {
        it('should delegate to store.uploadFile with the file and current folderParams', () => {
            const mockFile = new File(['content'], 'photo.png', { type: 'image/png' });

            spectator.component.onFileUpload(mockFile);

            expect(mockStore.uploadFile).toHaveBeenCalledWith({
                file: mockFile,
                folderParams: { hostFolderId: SYSTEM_HOST_ID, mimeTypes: [] }
            });
        });

        it('should pass the updated hostFolderId after a node selection', () => {
            spectator.component.onNodeSelect(mockNodeSelectEvent('demo.dotcms.com'));

            const mockFile = new File(['content'], 'report.pdf', { type: 'application/pdf' });
            spectator.component.onFileUpload(mockFile);

            expect(mockStore.uploadFile).toHaveBeenCalledWith({
                file: mockFile,
                folderParams: { hostFolderId: 'demo.dotcms.com', mimeTypes: [] }
            });
        });
    });
});
