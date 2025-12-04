import { describe, it, expect } from '@jest/globals';
import { createComponentFactory, mockProvider, Spectator } from '@ngneat/spectator/jest';
import { of, throwError } from 'rxjs';

import { MessageService } from 'primeng/api';
import { AutoCompleteCompleteEvent } from 'primeng/autocomplete';

import { DotContentTypeService, DotFolderService, DotMessageService } from '@dotcms/data-access';
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
                loadFolders: jest.fn(),
                closeDialog: jest.fn()
            }),
            mockProvider(DotFolderService, {
                createFolder: jest.fn().mockReturnValue(of({}))
            }),
            mockProvider(MessageService, {
                add: jest.fn()
            }),
            {
                provide: DotMessageService,
                useValue: new MockDotMessageService({
                    'content-drive.dialog.folder.message.create-success':
                        'Folder created successfully',
                    'content-drive.dialog.folder.message.create-error': 'Error creating folder'
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
            expect(component.folderForm.get('url')?.value).toBe('');
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
                url: 'test-url'
            });

            expect(component.folderForm.invalid).toBe(true);
        });

        it('should be invalid when url is empty', () => {
            component.folderForm.patchValue({
                title: 'Test Title',
                url: ''
            });

            expect(component.folderForm.invalid).toBe(true);
        });

        it('should be valid when both title and url are provided', () => {
            component.folderForm.patchValue({
                title: 'Test Title',
                url: 'test-url'
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
                url: 'test-url'
            });
            spectator.detectChanges();

            const createButton = spectator.query('button.p-button-primary');

            expect(createButton?.getAttribute('disabled')).toBeNull();
        });
    });

    describe('finalPath computed', () => {
        it('should generate correct path with no existing path', () => {
            store.path.mockReturnValue(undefined);
            component.folderForm.patchValue({ url: 'new-folder' });
            spectator.detectChanges();

            expect(component.$finalPath()).toBe('//demo.dotcms.com/new-folder/');
        });

        it('should generate correct path with existing path', () => {
            store.path.mockReturnValue('/documents');
            component.folderForm.patchValue({ url: 'new-folder' });
            spectator.detectChanges();

            expect(component.$finalPath()).toBe('//demo.dotcms.com/documents/new-folder/');
        });
    });

    describe('url auto-generation from title', () => {
        it('should generate url slug from title when url is not touched', () => {
            component.folderForm.patchValue({ title: 'My New Folder' });
            spectator.detectChanges();

            expect(component.folderForm.get('url')?.value).toBe('my-new-folder');
        });

        it('should not override url when manually touched', () => {
            // First touch the url field
            component.folderForm.get('url')?.markAsTouched();
            component.folderForm.patchValue({
                url: 'custom-url',
                title: 'My New Folder'
            });
            spectator.detectChanges();

            expect(component.folderForm.get('url')?.value).toBe('custom-url');
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
                url: 'test-folder'
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

        it('should reload folders and close dialog on success', () => {
            const createButton = spectator.query(
                '[data-testid="content-drive-dialog-folder-create"]'
            );
            spectator.click(createButton);

            expect(store.loadFolders).toHaveBeenCalled();
            expect(store.closeDialog).toHaveBeenCalled();
        });

        it('should show success message on successful creation', () => {
            const createButton = spectator.query(
                '[data-testid="content-drive-dialog-folder-create"]'
            );
            spectator.click(createButton);

            expect(messageService.add).toHaveBeenCalledWith({
                severity: 'success',
                summary: 'Success',
                detail: 'Folder created successfully'
            });
        });

        it('should show error message on creation failure', () => {
            folderService.createFolder.mockReturnValue(
                throwError(() => ({ error: { message: 'Creation failed' } }))
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
                url: 'integration-test'
            });
            spectator.detectChanges();

            expect(component.$finalPath()).toContain('integration-test');
        });
    });
});
