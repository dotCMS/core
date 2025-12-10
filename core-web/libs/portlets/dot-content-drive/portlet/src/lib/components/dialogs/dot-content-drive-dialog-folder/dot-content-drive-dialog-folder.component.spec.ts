import { describe, it, expect } from '@jest/globals';
import { createComponentFactory, mockProvider, Spectator } from '@ngneat/spectator/jest';
import { of, throwError } from 'rxjs';

import { MessageService } from 'primeng/api';
import { AutoCompleteCompleteEvent } from 'primeng/autocomplete';

import { DotContentTypeService, DotFolderService, DotMessageService } from '@dotcms/data-access';
import { DotContentDriveFolder } from '@dotcms/dotcms-models';
import { createFakeSite, MockDotMessageService } from '@dotcms/utils-testing';

import { DotContentDriveDialogFolderComponent } from './dot-content-drive-dialog-folder.component';

import { DEFAULT_FILE_ASSET_TYPES } from '../../../shared/constants';
import { DotContentDriveStore } from '../../../store/dot-content-drive.store';

const mockSite = createFakeSite({
    hostname: 'demo.dotcms.com'
});

const mockFileAssetTypes = [
    {
        id: 'FileAsset',
        variable: 'File'
    },
    {
        id: 'Video',
        variable: 'Video'
    }
];

describe('DotContentDriveDialogFolderComponent', () => {
    let spectator: Spectator<DotContentDriveDialogFolderComponent>;
    let component: DotContentDriveDialogFolderComponent;
    let folderService: jest.Mocked<DotFolderService>;
    let store: jest.Mocked<InstanceType<typeof DotContentDriveStore>>;
    let messageService: jest.Mocked<MessageService>;

    const createComponent = createComponentFactory({
        component: DotContentDriveDialogFolderComponent,
        providers: [
            mockProvider(DotContentDriveStore, {
                currentSite: jest.fn().mockReturnValue(mockSite),
                path: jest.fn().mockReturnValue('/documents'),
                reloadContentDrive: jest.fn(),
                loadFolders: jest.fn(),
                closeDialog: jest.fn()
            }),
            mockProvider(DotFolderService, {
                createFolder: jest.fn().mockReturnValue(of({})),
                saveFolder: jest.fn().mockReturnValue(of({}))
            }),
            mockProvider(MessageService, {
                add: jest.fn()
            }),
            {
                provide: DotMessageService,
                useValue: new MockDotMessageService({
                    'content-drive.dialog.folder.message.create-success':
                        'Folder created successfully',
                    'content-drive.dialog.folder.message.create-error': 'Error creating folder',
                    'content-drive.dialog.folder.message.save-success': 'Folder saved successfully',
                    'content-drive.dialog.folder.message.save-error': 'Error saving folder'
                })
            },
            mockProvider(DotContentTypeService, {
                getAllContentTypes: jest.fn().mockReturnValue(of([])),
                getContentTypes: jest.fn().mockReturnValue(of(mockFileAssetTypes))
            })
        ]
    });

    beforeEach(() => {
        spectator = createComponent();
        component = spectator.component;
        folderService = spectator.inject(DotFolderService);
        store = spectator.inject(DotContentDriveStore);
        messageService = spectator.inject(MessageService);
    });

    describe('initial state', () => {
        it('should initialize form with default values', () => {
            expect(component.folderForm.get('title')?.value).toBe('');
            expect(component.folderForm.get('name')?.value).toBe('');
            expect(component.folderForm.get('sortOrder')?.value).toBe(1);
            expect(component.folderForm.get('allowedFileExtensions')?.value).toEqual([]);
            expect(component.folderForm.get('defaultFileAssetType')?.value).toBe(
                DEFAULT_FILE_ASSET_TYPES[0].id
            );
            expect(component.folderForm.get('showOnMenu')?.value).toBe(false);
        });

        it('should have form invalid initially', () => {
            expect(component.folderForm.invalid).toBe(true);
        });
    });

    describe('form validation', () => {
        it('should be invalid when title is empty', () => {
            component.folderForm.patchValue({
                title: '',
                name: 'test-name'
            });

            expect(component.folderForm.invalid).toBe(true);
        });

        it('should be invalid when name is empty', () => {
            component.folderForm.patchValue({
                title: 'Test Title',
                name: ''
            });

            expect(component.folderForm.invalid).toBe(true);
        });

        it('should be valid when both title and name are provided', () => {
            component.folderForm.patchValue({
                title: 'Test Title',
                name: 'test-name'
            });

            expect(component.folderForm.valid).toBe(true);
        });
    });

    describe('button states', () => {
        it('should disable create button when form is invalid', () => {
            const createButton = spectator.query('button.p-button-primary');

            expect(createButton?.getAttribute('disabled')).not.toBeNull();
        });

        it('should enable create button when form is valid', () => {
            component.folderForm.patchValue({
                title: 'Test Title',
                name: 'test-name'
            });
            spectator.detectChanges();

            const createButton = spectator.query('button.p-button-primary');

            expect(createButton?.getAttribute('disabled')).toBeNull();
        });
    });

    describe('finalPath computed', () => {
        it('should generate correct path with no existing path', () => {
            store.path.mockReturnValue(undefined);
            component.folderForm.patchValue({ name: 'new-folder' });
            spectator.detectChanges();

            expect(component.$finalPath()).toBe('//demo.dotcms.com/new-folder/');
        });

        it('should generate correct path with existing path', () => {
            store.path.mockReturnValue('/documents');
            component.folderForm.patchValue({ name: 'new-folder' });
            spectator.detectChanges();

            expect(component.$finalPath()).toBe('//demo.dotcms.com/documents/new-folder/');
        });
    });

    describe('name auto-generation from title', () => {
        it('should generate name slug from title when name is not touched', () => {
            component.folderForm.patchValue({ title: 'My New Folder' });
            spectator.detectChanges();

            expect(component.folderForm.get('name')?.value).toBe('my-new-folder');
        });

        it('should not override name when manually touched', () => {
            // First touch the name field
            component.folderForm.get('name')?.markAsTouched();
            component.folderForm.patchValue({
                name: 'custom-name',
                title: 'My New Folder'
            });
            spectator.detectChanges();

            expect(component.folderForm.get('name')?.value).toBe('custom-name');
        });
    });

    describe('file extensions functionality', () => {
        it('should filter file extensions on autocomplete', () => {
            const event: AutoCompleteCompleteEvent = {
                query: 'jpg',
                originalEvent: new Event('input')
            };

            component.onCompleteMethod(event);

            expect(component.$filteredAllowedFileExtensions()).toContain('*.jpg');
        });

        it('should add extension on enter key if not duplicate', () => {
            spectator.triggerEventHandler(
                '[data-testid="allowed-file-extensions-autocomplete"]',
                'keyup.enter',
                {
                    target: {
                        value: '*.pdf'
                    }
                }
            );

            spectator.detectChanges();

            expect(component.folderForm.get('allowedFileExtensions')?.value).toContain('*.pdf');
        });

        it('should not add duplicate extension on enter key', () => {
            component.folderForm.patchValue({
                allowedFileExtensions: ['*.pdf']
            });
            spectator.detectChanges();

            spectator.triggerEventHandler(
                '[data-testid="allowed-file-extensions-autocomplete"]',
                'keyup.enter',
                {
                    target: {
                        value: '*.pdf'
                    }
                }
            );

            spectator.detectChanges();

            expect(component.folderForm.get('allowedFileExtensions')?.value).toEqual(['*.pdf']);
        });
    });

    describe('create folder button interactions', () => {
        beforeEach(() => {
            component.folderForm.patchValue({
                title: 'Test Folder',
                name: 'test-folder'
            });
            spectator.detectChanges();
        });

        it('should call folder service with basic data when create button is clicked', () => {
            const createButton = spectator.query(
                '[data-testid="content-drive-dialog-folder-create"]'
            );

            spectator.click(createButton);

            expect(folderService.createFolder).toHaveBeenCalledWith({
                assetPath: '//demo.dotcms.com/documents/test-folder/',
                data: {
                    title: 'Test Folder',
                    showOnMenu: false,
                    sortOrder: 1,
                    defaultAssetType: DEFAULT_FILE_ASSET_TYPES[0].id
                }
            });
        });

        it('should call folder service with file extensions when provided', () => {
            component.folderForm.patchValue({
                allowedFileExtensions: ['*.jpg', '*.png']
            });
            spectator.detectChanges();

            const createButton = spectator.query(
                '[data-testid="content-drive-dialog-folder-create"]'
            );
            spectator.click(createButton);

            expect(folderService.createFolder).toHaveBeenCalledWith({
                assetPath: '//demo.dotcms.com/documents/test-folder/',
                data: {
                    title: 'Test Folder',
                    showOnMenu: false,
                    sortOrder: 1,
                    defaultAssetType: DEFAULT_FILE_ASSET_TYPES[0].id,
                    fileMasks: ['*.jpg', '*.png']
                }
            });
        });

        it('should call folder service with custom sort order', () => {
            component.folderForm.patchValue({
                sortOrder: 5
            });
            spectator.detectChanges();

            const createButton = spectator.query(
                '[data-testid="content-drive-dialog-folder-create"]'
            );
            spectator.click(createButton);

            expect(folderService.createFolder).toHaveBeenCalledWith({
                assetPath: '//demo.dotcms.com/documents/test-folder/',
                data: {
                    title: 'Test Folder',
                    showOnMenu: false,
                    sortOrder: 5,
                    defaultAssetType: DEFAULT_FILE_ASSET_TYPES[0].id
                }
            });
        });

        it('should call folder service with showOnMenu false', () => {
            component.folderForm.patchValue({
                showOnMenu: false
            });
            spectator.detectChanges();

            const createButton = spectator.query(
                '[data-testid="content-drive-dialog-folder-create"]'
            );
            spectator.click(createButton);

            expect(folderService.createFolder).toHaveBeenCalledWith({
                assetPath: '//demo.dotcms.com/documents/test-folder/',
                data: {
                    title: 'Test Folder',
                    showOnMenu: false,
                    sortOrder: 1,
                    defaultAssetType: DEFAULT_FILE_ASSET_TYPES[0].id
                }
            });
        });

        it('should reload content drive, load folders and close dialog on success', () => {
            const createButton = spectator.query(
                '[data-testid="content-drive-dialog-folder-create"]'
            );
            spectator.click(createButton);

            expect(store.reloadContentDrive).toHaveBeenCalled();
            expect(store.loadFolders).toHaveBeenCalled();
            expect(store.closeDialog).toHaveBeenCalled();
        });

        it('should show success message on successful creation', () => {
            const createButton = spectator.query(
                '[data-testid="content-drive-dialog-folder-create"]'
            );

            expect(createButton).toBeTruthy();
            expect(component.folderForm.valid).toBe(true);

            spectator.click(createButton);
            spectator.detectChanges();

            expect(messageService.add).toHaveBeenCalledWith({
                severity: 'success',
                summary: 'Success',
                detail: 'Folder created successfully'
            });
        });

        it('should show error message on creation failure', () => {
            folderService.createFolder.mockReturnValue(
                throwError({ error: { message: 'Creation failed' } })
            );

            const createButton = spectator.query(
                '[data-testid="content-drive-dialog-folder-create"]'
            );
            spectator.click(createButton);

            expect(messageService.add).toHaveBeenCalledWith({
                severity: 'error',
                summary: 'Error creating folder',
                detail: 'Creation failed'
            });
        });
    });

    describe('dialog button interactions', () => {
        it('should close dialog when cancel button is clicked', () => {
            const cancelButton = spectator.query(
                '[data-testid="content-drive-dialog-folder-cancel"]'
            );

            spectator.click(cancelButton);

            expect(store.closeDialog).toHaveBeenCalled();
        });
    });

    describe('component integration', () => {
        it('should update final path when form values change', () => {
            component.folderForm.patchValue({
                name: 'integration-test'
            });
            spectator.detectChanges();

            expect(component.$finalPath()).toContain('integration-test');
        });
    });

    describe('createFolderBody constraints', () => {
        beforeEach(() => {
            component.folderForm.patchValue({
                title: 'Test Folder',
                name: 'test-folder'
            });
            spectator.detectChanges();
        });

        it('should only include showOnMenu when it is not undefined and not null', () => {
            component.folderForm.patchValue({
                showOnMenu: true
            });
            spectator.detectChanges();

            const createButton = spectator.query(
                '[data-testid="content-drive-dialog-folder-create"]'
            );
            spectator.click(createButton);

            expect(folderService.createFolder).toHaveBeenCalledWith({
                assetPath: '//demo.dotcms.com/documents/test-folder/',
                data: {
                    title: 'Test Folder',
                    showOnMenu: true,
                    sortOrder: 1,
                    defaultAssetType: DEFAULT_FILE_ASSET_TYPES[0].id
                }
            });
        });

        it('should not include showOnMenu when it is null', () => {
            // Since showOnMenu is nonNullable, we need to set it directly on the control
            // to test the null case, which the component logic handles
            component.folderForm.get('showOnMenu')?.setValue(null as unknown as boolean, {
                emitEvent: false
            });
            spectator.detectChanges();

            const createButton = spectator.query(
                '[data-testid="content-drive-dialog-folder-create"]'
            );
            spectator.click(createButton);

            expect(folderService.createFolder).toHaveBeenCalled();
            const lastCall = folderService.createFolder.mock.calls.at(-1)?.[0];
            expect(lastCall?.data.showOnMenu).toBeUndefined();
        });

        it('should only include sortOrder when it is not null and not undefined', () => {
            component.folderForm.patchValue({
                sortOrder: 5
            });
            spectator.detectChanges();

            const createButton = spectator.query(
                '[data-testid="content-drive-dialog-folder-create"]'
            );
            spectator.click(createButton);

            expect(folderService.createFolder).toHaveBeenCalledWith({
                assetPath: '//demo.dotcms.com/documents/test-folder/',
                data: {
                    title: 'Test Folder',
                    showOnMenu: false,
                    sortOrder: 5,
                    defaultAssetType: DEFAULT_FILE_ASSET_TYPES[0].id
                }
            });
        });

        it('should not include sortOrder when it is null', () => {
            component.folderForm.patchValue({
                sortOrder: null
            });
            spectator.detectChanges();

            const createButton = spectator.query(
                '[data-testid="content-drive-dialog-folder-create"]'
            );
            spectator.click(createButton);

            expect(folderService.createFolder).toHaveBeenCalled();
            const lastCall = folderService.createFolder.mock.calls.at(-1)?.[0];
            expect(lastCall?.data.sortOrder).toBeUndefined();
        });

        it('should only include fileMasks when allowedFileExtensions has items', () => {
            component.folderForm.patchValue({
                allowedFileExtensions: ['*.jpg', '*.png']
            });
            spectator.detectChanges();

            const createButton = spectator.query(
                '[data-testid="content-drive-dialog-folder-create"]'
            );
            spectator.click(createButton);

            expect(folderService.createFolder).toHaveBeenCalledWith({
                assetPath: '//demo.dotcms.com/documents/test-folder/',
                data: {
                    title: 'Test Folder',
                    showOnMenu: false,
                    sortOrder: 1,
                    defaultAssetType: DEFAULT_FILE_ASSET_TYPES[0].id,
                    fileMasks: ['*.jpg', '*.png']
                }
            });
        });

        it('should not include fileMasks when allowedFileExtensions is empty', () => {
            component.folderForm.patchValue({
                allowedFileExtensions: []
            });
            spectator.detectChanges();

            const createButton = spectator.query(
                '[data-testid="content-drive-dialog-folder-create"]'
            );
            spectator.click(createButton);

            expect(folderService.createFolder).toHaveBeenCalled();
            const lastCall = folderService.createFolder.mock.calls.at(-1)?.[0];
            expect(lastCall?.data.fileMasks).toBeUndefined();
        });

        it('should only include defaultAssetType when it is not empty', () => {
            component.folderForm.patchValue({
                defaultFileAssetType: 'Video'
            });
            spectator.detectChanges();

            const createButton = spectator.query(
                '[data-testid="content-drive-dialog-folder-create"]'
            );
            spectator.click(createButton);

            expect(folderService.createFolder).toHaveBeenCalledWith({
                assetPath: '//demo.dotcms.com/documents/test-folder/',
                data: {
                    title: 'Test Folder',
                    showOnMenu: false,
                    sortOrder: 1,
                    defaultAssetType: 'Video'
                }
            });
        });

        it('should not include defaultAssetType when it is empty', () => {
            component.folderForm.patchValue({
                defaultFileAssetType: ''
            });
            spectator.detectChanges();

            const createButton = spectator.query(
                '[data-testid="content-drive-dialog-folder-create"]'
            );
            spectator.click(createButton);

            expect(folderService.createFolder).toHaveBeenCalled();
            const lastCall = folderService.createFolder.mock.calls.at(-1)?.[0];
            expect(lastCall?.data.defaultAssetType).toBeUndefined();
        });

        it('should not include defaultAssetType when it is only whitespace', () => {
            component.folderForm.patchValue({
                defaultFileAssetType: '   '
            });
            spectator.detectChanges();

            const createButton = spectator.query(
                '[data-testid="content-drive-dialog-folder-create"]'
            );
            spectator.click(createButton);

            expect(folderService.createFolder).toHaveBeenCalled();
            const lastCall = folderService.createFolder.mock.calls.at(-1)?.[0];
            expect(lastCall?.data.defaultAssetType).toBeUndefined();
        });

        it('should use originalName in assetPath when it exists and name has not changed', () => {
            // Simulate editing an existing folder
            const mockFolder: DotContentDriveFolder = {
                name: 'original-folder',
                title: 'Original Folder',
                sortOrder: 1,
                filesMasks: '',
                defaultFileType: 'FileAsset',
                showOnMenu: false,
                __icon__: 'folderIcon',
                description: '',
                extension: 'folder',
                hasTitleImage: false,
                hostId: '1',
                iDate: 1234567890,
                identifier: '1',
                inode: '1',
                mimeType: '',
                modDate: 1234567890,
                owner: null,
                parent: '',
                path: '',
                permissions: [],
                type: 'folder'
            };

            spectator.setInput('folder', mockFolder);
            spectator.detectChanges();

            // Don't change the name
            component.folderForm.patchValue({
                title: 'Updated Title'
            });
            spectator.detectChanges();

            const saveButton = spectator.query(
                '[data-testid="content-drive-dialog-folder-create"]'
            );
            spectator.click(saveButton);

            expect(folderService.saveFolder).toHaveBeenCalled();
            const lastCall = folderService.saveFolder.mock.calls.at(-1)?.[0];
            expect(lastCall?.assetPath).toBe('//demo.dotcms.com/documents/original-folder/');
        });

        it('should use form name in assetPath when originalName does not exist', () => {
            const createButton = spectator.query(
                '[data-testid="content-drive-dialog-folder-create"]'
            );
            spectator.click(createButton);

            expect(folderService.createFolder).toHaveBeenCalled();
            const lastCall = folderService.createFolder.mock.calls.at(-1)?.[0];
            expect(lastCall?.assetPath).toBe('//demo.dotcms.com/documents/test-folder/');
        });

        it('should include name in data when originalName exists and name has changed', () => {
            // Simulate editing an existing folder
            const mockFolder: DotContentDriveFolder = {
                name: 'original-folder',
                title: 'Original Folder',
                sortOrder: 1,
                filesMasks: '',
                defaultFileType: 'FileAsset',
                showOnMenu: false,
                __icon__: 'folderIcon',
                description: '',
                extension: 'folder',
                hasTitleImage: false,
                hostId: '1',
                iDate: 1234567890,
                identifier: '1',
                inode: '1',
                mimeType: '',
                modDate: 1234567890,
                owner: null,
                parent: '',
                path: '',
                permissions: [],
                type: 'folder'
            };

            spectator.setInput('folder', mockFolder);
            spectator.detectChanges();

            // Verify originalName is set
            expect(component.$originalName()).toBe('original-folder');
            expect(component.folderForm.get('name')?.value).toBe('original-folder');

            // Change the name and mark as touched to prevent urlEffect from interfering
            component.folderForm.get('name')?.setValue('new-folder-name');
            component.folderForm.get('name')?.markAsTouched();
            spectator.detectChanges();

            // Verify the form value is updated
            expect(component.folderForm.get('name')?.value).toBe('new-folder-name');

            const saveButton = spectator.query(
                '[data-testid="content-drive-dialog-folder-create"]'
            );
            spectator.click(saveButton);

            expect(folderService.saveFolder).toHaveBeenCalled();
            const lastCall = folderService.saveFolder.mock.calls.at(-1)?.[0];
            expect(lastCall?.data.name).toBe('new-folder-name');
            // assetPath uses $originalName() when it exists, even if name changed
            // The name field in data is what tells the backend to rename it
            expect(lastCall?.assetPath).toBe('//demo.dotcms.com/documents/original-folder/');
        });

        it('should not include name in data when originalName does not exist', () => {
            const createButton = spectator.query(
                '[data-testid="content-drive-dialog-folder-create"]'
            );
            spectator.click(createButton);

            expect(folderService.createFolder).toHaveBeenCalled();
            const lastCall = folderService.createFolder.mock.calls.at(-1)?.[0];
            expect(lastCall?.data.name).toBeUndefined();
        });

        it('should not include name in data when originalName exists but name has not changed', () => {
            // Simulate editing an existing folder
            const mockFolder: DotContentDriveFolder = {
                name: 'original-folder',
                title: 'Original Folder',
                sortOrder: 1,
                filesMasks: '',
                defaultFileType: 'FileAsset',
                showOnMenu: false,
                __icon__: 'folderIcon',
                description: '',
                extension: 'folder',
                hasTitleImage: false,
                hostId: '1',
                iDate: 1234567890,
                identifier: '1',
                inode: '1',
                mimeType: '',
                modDate: 1234567890,
                owner: null,
                parent: '',
                path: '',
                permissions: [],
                type: 'folder'
            };

            spectator.setInput('folder', mockFolder);
            spectator.detectChanges();

            // Don't change the name
            component.folderForm.patchValue({
                title: 'Updated Title'
            });
            spectator.detectChanges();

            const saveButton = spectator.query(
                '[data-testid="content-drive-dialog-folder-create"]'
            );
            spectator.click(saveButton);

            expect(folderService.saveFolder).toHaveBeenCalled();
            const lastCall = folderService.saveFolder.mock.calls.at(-1)?.[0];
            expect(lastCall?.data.name).toBeUndefined();
        });
    });

    describe('saveFolder method', () => {
        beforeEach(() => {
            // Clear any previous mock calls
            folderService.saveFolder.mockClear();
            folderService.createFolder.mockClear();
            store.reloadContentDrive.mockClear();
            store.loadFolders.mockClear();
            store.closeDialog.mockClear();
            messageService.add.mockClear();

            // Simulate editing an existing folder
            const mockFolder: DotContentDriveFolder = {
                name: 'existing-folder',
                title: 'Existing Folder',
                sortOrder: 1,
                filesMasks: '*.jpg,*.png',
                defaultFileType: 'FileAsset',
                showOnMenu: true,
                __icon__: 'folderIcon',
                description: '',
                extension: 'folder',
                hasTitleImage: false,
                hostId: '1',
                iDate: 1234567890,
                identifier: '1',
                inode: '1',
                mimeType: '',
                modDate: 1234567890,
                owner: null,
                parent: '',
                path: '',
                permissions: [],
                type: 'folder'
            };

            spectator.setInput('folder', mockFolder);
            spectator.detectChanges();

            component.folderForm.patchValue({
                title: 'Updated Folder',
                name: 'updated-folder'
            });
            spectator.detectChanges();
        });

        it('should call saveFolder service method when folder exists', () => {
            const saveButton = spectator.query(
                '[data-testid="content-drive-dialog-folder-create"]'
            );

            spectator.click(saveButton);

            expect(folderService.saveFolder).toHaveBeenCalled();
            expect(folderService.createFolder).not.toHaveBeenCalled();
        });

        it('should call saveFolder with correct body structure', () => {
            const saveButton = spectator.query(
                '[data-testid="content-drive-dialog-folder-create"]'
            );

            spectator.click(saveButton);

            // assetPath uses $originalName() when it exists (for editing existing folders)
            // The name field in data is what tells the backend to rename it
            expect(folderService.saveFolder).toHaveBeenCalledWith({
                assetPath: '//demo.dotcms.com/documents/existing-folder/',
                data: {
                    title: 'Updated Folder',
                    name: 'updated-folder',
                    showOnMenu: true,
                    sortOrder: 1,
                    defaultAssetType: 'File',
                    fileMasks: ['*.jpg', '*.png']
                }
            });
        });

        it('should reload content drive, load folders and close dialog on success', () => {
            const saveButton = spectator.query(
                '[data-testid="content-drive-dialog-folder-create"]'
            );
            spectator.click(saveButton);

            expect(store.reloadContentDrive).toHaveBeenCalled();
            expect(store.loadFolders).toHaveBeenCalled();
            expect(store.closeDialog).toHaveBeenCalled();
        });

        it('should show success message on successful save', () => {
            const saveButton = spectator.query(
                '[data-testid="content-drive-dialog-folder-create"]'
            );

            spectator.click(saveButton);
            spectator.detectChanges();

            expect(messageService.add).toHaveBeenCalledWith({
                severity: 'success',
                summary: 'Folder saved successfully',
                detail: undefined
            });
        });

        it('should show error message on save failure', () => {
            folderService.saveFolder.mockReturnValue(
                throwError({ error: { message: 'Save failed' } })
            );

            const saveButton = spectator.query(
                '[data-testid="content-drive-dialog-folder-create"]'
            );
            spectator.click(saveButton);

            expect(messageService.add).toHaveBeenCalledWith({
                severity: 'error',
                summary: 'Error saving folder',
                detail: 'Save failed'
            });
        });
    });
});
