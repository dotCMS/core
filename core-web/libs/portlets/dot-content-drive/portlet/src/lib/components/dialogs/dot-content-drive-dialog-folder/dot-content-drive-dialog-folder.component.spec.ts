import { describe, it, expect } from '@jest/globals';
import { byTestId, createComponentFactory, mockProvider, Spectator } from '@openng/spectator/jest';
import { delay, of, throwError } from 'rxjs';

import { MessageService } from 'primeng/api';
import { AutoComplete, AutoCompleteCompleteEvent } from 'primeng/autocomplete';

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

/** An existing folder as it comes back from the backend, for the edit-mode flows. */
const editableFolder = (overrides: Partial<DotContentDriveFolder> = {}): DotContentDriveFolder =>
    ({
        name: 'app',
        title: 'App',
        sortOrder: 1,
        filesMasks: '',
        defaultFileType: 'FileAsset',
        showOnMenu: false,
        __icon__: 'folderIcon',
        description: '',
        extension: 'folder',
        hasTitleImage: false,
        hostId: '1',
        iDate: 1,
        identifier: '1',
        inode: '1',
        mimeType: '',
        modDate: 1,
        owner: null,
        parent: '',
        path: '',
        permissions: [],
        type: 'folder',
        ...overrides
    }) as DotContentDriveFolder;

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

        it('should preview the current folder (not root) when name is empty', () => {
            component.folderForm.patchValue({ name: '' });
            spectator.detectChanges();

            expect(component.$finalPath()).toBe('//demo.dotcms.com/documents/');
        });

        it('should preview the current folder (not root) when name is null', () => {
            component.folderForm.patchValue({ name: null });
            spectator.detectChanges();

            expect(component.$finalPath()).toBe('//demo.dotcms.com/documents/');
        });

        it('should preview the current folder (not root) when name is whitespace', () => {
            component.folderForm.patchValue({ name: '   ' });
            spectator.detectChanges();

            expect(component.$finalPath()).toBe('//demo.dotcms.com/documents/');
        });

        it('should preview the site root when at root (no path) and name is empty', () => {
            store.path.mockReturnValue(undefined);
            component.folderForm.patchValue({ name: '' });
            spectator.detectChanges();

            expect(component.$finalPath()).toBe('//demo.dotcms.com/');
        });

        it('should handle path with trailing slash', () => {
            store.path.mockReturnValue('/documents/');
            component.folderForm.patchValue({ name: 'new-folder' });
            spectator.detectChanges();

            expect(component.$finalPath()).toBe('//demo.dotcms.com/documents/new-folder/');
        });

        it('should convert name to slug in path', () => {
            component.folderForm.patchValue({ name: 'My New Folder' });
            spectator.detectChanges();

            expect(component.$finalPath()).toBe('//demo.dotcms.com/documents/my-new-folder/');
        });

        it('should handle name with spaces', () => {
            component.folderForm.patchValue({ name: 'test folder name' });
            spectator.detectChanges();

            expect(component.$finalPath()).toBe('//demo.dotcms.com/documents/test-folder-name/');
        });
    });

    describe('create form respects the currently opened folder', () => {
        it('previews the opened folder as the parent before a name is typed', () => {
            // Opening "New folder" while inside /documents must show that folder as the parent in
            // the create form — not the site root — otherwise the form misrepresents where the
            // folder will be created (issue: "New button not respecting the current opened folder").
            store.path.mockReturnValue('/documents');
            component.folderForm.patchValue({ name: '' });
            spectator.detectChanges();

            const preview = spectator.query(byTestId('folder-path-preview'))?.textContent?.trim();

            expect(preview).toBe('//demo.dotcms.com/documents/');
        });
    });

    describe('title auto-generation from name', () => {
        it('should generate navigation label from name when title is not dirty', () => {
            component.folderForm.patchValue({ name: 'my-new-folder' });
            spectator.detectChanges();

            expect(component.folderForm.get('title')?.value).toBe('My New Folder');
        });

        it('should handle multiple hyphens correctly', () => {
            component.folderForm.patchValue({ name: 'my-very-long-folder-name' });
            spectator.detectChanges();

            expect(component.folderForm.get('title')?.value).toBe('My Very Long Folder Name');
        });

        it('should not override title when manually edited (dirty)', () => {
            // First mark title as dirty by setting a value
            component.folderForm.get('title')?.setValue('Custom Title');
            component.folderForm.get('title')?.markAsDirty();
            spectator.detectChanges();

            // Now change the name
            component.folderForm.patchValue({ name: 'different-name' });
            spectator.detectChanges();

            expect(component.folderForm.get('title')?.value).toBe('Custom Title');
        });

        it('should not auto-generate title when folder is being edited', () => {
            // Simulate editing an existing folder
            const mockFolder: DotContentDriveFolder = {
                name: 'existing-folder',
                title: 'Existing Folder',
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

            // Change the name
            component.folderForm.patchValue({ name: 'new-name' });
            spectator.detectChanges();

            // Title should remain as it was set from the folder
            expect(component.folderForm.get('title')?.value).toBe('Existing Folder');
        });

        it('should capitalize first letter of each word', () => {
            component.folderForm.patchValue({ name: 'test-folder' });
            spectator.detectChanges();

            expect(component.folderForm.get('title')?.value).toBe('Test Folder');
        });

        it('should handle single word names', () => {
            component.folderForm.patchValue({ name: 'documents' });
            spectator.detectChanges();

            expect(component.folderForm.get('title')?.value).toBe('Documents');
        });

        it('should handle empty name', () => {
            component.folderForm.patchValue({ name: '' });
            spectator.detectChanges();

            expect(component.folderForm.get('title')?.value).toBe('');
        });

        it('should handle name with leading/trailing spaces (trimmed)', () => {
            component.folderForm.patchValue({ name: '  test-folder  ' });
            spectator.detectChanges();

            expect(component.folderForm.get('title')?.value).toBe('Test Folder');
        });
    });

    describe('file extensions functionality', () => {
        /** The AutoComplete's own text input (the one the user types extensions into). */
        const extensionsInput = () =>
            (spectator.query(AutoComplete) as AutoComplete).inputEL?.nativeElement as
                | HTMLInputElement
                | undefined;

        /** The chips the field is actually showing, which must mirror the form control. */
        const renderedChips = () => (spectator.query(AutoComplete) as AutoComplete).modelValue();

        /**
         * A real Enter press. `code` matters: PrimeNG's own key handler switches on it, so without
         * it only our `(keydown.enter)` binding (which matches on `key`) would run and the test
         * would miss that PrimeNG handles the same press first, on the inner input.
         */
        const pressEnter = () =>
            new KeyboardEvent('keydown', { key: 'Enter', code: 'Enter', bubbles: true });

        /** Types `text` and lets the AutoComplete debounce run so the suggestions get filtered. */
        const type = (text: string) => {
            const input = extensionsInput();
            input.value = text;
            input.dispatchEvent(new Event('input', { bubbles: true }));
            jest.advanceTimersByTime(500);
            spectator.detectChanges();

            return input;
        };

        beforeEach(() => jest.useFakeTimers());
        afterEach(() => jest.useRealTimers());

        it('should filter file extensions on autocomplete', () => {
            const event: AutoCompleteCompleteEvent = {
                query: 'jpg',
                originalEvent: new Event('input')
            };

            component.onCompleteMethod(event);

            expect(component.$filteredAllowedFileExtensions()).toContain('*.jpg');
        });

        it('should add extension on enter key if not duplicate', () => {
            const input = type('*.pdf');
            input.dispatchEvent(pressEnter());
            spectator.detectChanges();

            expect(component.folderForm.get('allowedFileExtensions')?.value).toContain('*.pdf');
            // Input is cleared so the next entry starts fresh and existing chips are preserved.
            expect(input.value).toBe('');
        });

        it('should preserve existing selection when adding another extension', () => {
            component.folderForm.patchValue({
                allowedFileExtensions: ['*.jpg']
            });
            spectator.detectChanges();

            const input = type('*.png');
            input.dispatchEvent(pressEnter());
            spectator.detectChanges();

            expect(component.folderForm.get('allowedFileExtensions')?.value).toEqual([
                '*.jpg',
                '*.png'
            ]);
        });

        it('should keep the rendered chips in sync with the form value when adding an extension', () => {
            // Regression: adding an extension used to write the whole array back through the form
            // control, and PrimeNG re-derives the chips from the *filtered* suggestions on write.
            // Every already-selected extension outside the active filter was dropped from the
            // chips while staying in the form value, so the stale extension came back on reload.
            component.folderForm.patchValue({
                allowedFileExtensions: ['*.jpg']
            });
            spectator.detectChanges();

            const input = type('*.png');
            input.dispatchEvent(pressEnter());
            spectator.detectChanges();

            expect(renderedChips()).toEqual(['*.jpg', '*.png']);
            expect(renderedChips()).toEqual(
                component.folderForm.get('allowedFileExtensions')?.value
            );
        });

        it('should not add duplicate extension on enter key', () => {
            component.folderForm.patchValue({
                allowedFileExtensions: ['*.pdf']
            });
            spectator.detectChanges();

            const input = type('*.pdf');
            input.dispatchEvent(pressEnter());
            spectator.detectChanges();

            expect(component.folderForm.get('allowedFileExtensions')?.value).toEqual(['*.pdf']);
            expect(input.value).toBe('');
        });

        it('should render a chip for every saved extension, including ones off the suggested list', () => {
            // Regression: PrimeNG re-derives the chips from `suggestions` on every control write,
            // including the one that loads the folder. An extension the suggested list does not
            // carry was dropped from the chips while staying in the form value, so the user could
            // neither see nor remove it and it was sent straight back on save. The folder is bound
            // at creation time here because that is how the shell opens this dialog.
            const editSpectator = createComponent({
                props: { folder: editableFolder({ filesMasks: '*.jpg,*.svg' }) }
            });
            editSpectator.detectChanges();

            const chips = (editSpectator.query(AutoComplete) as AutoComplete).modelValue();

            expect(chips).toEqual(['*.jpg', '*.svg']);
            expect(chips).toEqual(
                editSpectator.component.folderForm.get('allowedFileExtensions')?.value
            );
        });

        describe('while the file asset types are still loading', () => {
            // `getContentTypes` is mocked once for the whole file, so restore it or the delayed
            // observable leaks into every test that runs after this one.
            afterEach(() => {
                (
                    spectator.inject(DotContentTypeService).getContentTypes as jest.Mock
                ).mockReturnValue(of(mockFileAssetTypes));
            });

            const openWhileLoading = () => {
                (
                    spectator.inject(DotContentTypeService).getContentTypes as jest.Mock
                ).mockReturnValue(of(mockFileAssetTypes).pipe(delay(1000)));

                const loading = createComponent({
                    props: { folder: editableFolder({ filesMasks: '*.jpg,*.svg' }) }
                });
                loading.detectChanges();

                return loading;
            };

            it('should show a spinner and render no field at all', () => {
                // Not only the extensions field: no input may exist before the values that belong in
                // it, or setFolderFormEffect overwrites whatever was typed in the meantime. The save
                // button has to stay away too, since submitting now would persist an empty form over
                // the folder's real data.
                const loading = openWhileLoading();

                expect(loading.query(byTestId('folder-form-loading'))).toBeTruthy();
                expect(loading.query('#name')).toBeNull();
                expect(loading.query('#title')).toBeNull();
                expect(
                    loading.query('[data-testid="allowed-file-extensions-autocomplete"]')
                ).toBeNull();
                expect(
                    loading.query('[data-testid="content-drive-dialog-folder-create"]')
                ).toBeNull();
            });

            it('should render the form already populated once they arrive', () => {
                // The form must never be visible before its values: patching a form the user can
                // already type into overwrites their input, and on this field it also pruned chips
                // down to whatever the active suggestion filter matched.
                const loading = openWhileLoading();

                jest.advanceTimersByTime(1000);
                loading.detectChanges();

                const autoComplete = loading.query(AutoComplete) as AutoComplete;

                expect(loading.query(byTestId('folder-form-loading'))).toBeNull();
                expect(autoComplete.modelValue()).toEqual(['*.jpg', '*.svg']);
                expect(autoComplete.modelValue()).toEqual(
                    loading.component.folderForm.get('allowedFileExtensions')?.value
                );
            });
        });

        it('should replace the saved extension when it is removed and a new one is typed', () => {
            // The reported flow: open a folder that already has an extension, remove it, type a
            // different one. The new extension must be what ends up in the payload.
            const autoComplete = spectator.query(AutoComplete) as AutoComplete;

            component.folderForm.patchValue({
                allowedFileExtensions: ['*.jpg']
            });
            spectator.detectChanges();

            autoComplete.removeOption({ stopPropagation: () => undefined } as unknown as Event, 0);
            spectator.detectChanges();

            const input = type('*.png');
            input.dispatchEvent(pressEnter());
            spectator.detectChanges();

            expect(component.folderForm.get('allowedFileExtensions')?.value).toEqual(['*.png']);
            expect(renderedChips()).toEqual(['*.png']);
        });
    });

    describe('overlay panels', () => {
        // Both panels are appended to the body: inside the dialog's scrolling content they get
        // clipped, and the wheel events land on the dialog instead of the panel's own list.
        it('should append the file extensions suggestions panel to the body', () => {
            expect(
                spectator
                    .query('[data-testid="allowed-file-extensions-autocomplete"]')
                    ?.getAttribute('appendTo')
            ).toBe('body');
        });

        it('should append the default file asset type options panel to the body', () => {
            expect(spectator.query('#defaultFileAssetType')?.getAttribute('appendTo')).toBe('body');
        });
    });

    describe('upload behavior (defaultBaseType)', () => {
        it('should render the three upload-behavior options', () => {
            expect(spectator.query('[data-testid="upload-behavior-option-null"]')).toBeTruthy();
            expect(spectator.query('[data-testid="upload-behavior-option-DOTASSET"]')).toBeTruthy();
            expect(
                spectator.query('[data-testid="upload-behavior-option-FILEASSET"]')
            ).toBeTruthy();
        });

        it('should default to "Ask each time" (null) on create', () => {
            expect(component.folderForm.get('defaultBaseType')?.value).toBeNull();
        });

        it('should pre-select the radio from the folder defaultBaseType on edit', () => {
            spectator.setInput('folder', editableFolder({ defaultBaseType: 'DOTASSET' }));
            spectator.detectChanges();

            expect(component.folderForm.get('defaultBaseType')?.value).toBe('DOTASSET');
        });

        it('should normalize a non-uppercase defaultBaseType to the uppercase radio value', () => {
            // The radio options and the shell/toolbar consumers use the uppercase enum; a
            // lowercase backend value must still select the matching option (not "Ask each time").
            spectator.setInput('folder', editableFolder({ defaultBaseType: 'dotasset' }));
            spectator.detectChanges();

            expect(component.folderForm.get('defaultBaseType')?.value).toBe('DOTASSET');
        });

        it('should send defaultBaseType in the body when a preference is chosen', () => {
            component.folderForm.patchValue({
                title: 'App',
                name: 'app',
                defaultBaseType: 'FILEASSET'
            });
            spectator.detectChanges();

            spectator.click(spectator.query('[data-testid="content-drive-dialog-folder-create"]'));

            expect(folderService.createFolder).toHaveBeenCalledWith(
                expect.objectContaining({
                    data: expect.objectContaining({ defaultBaseType: 'FILEASSET' })
                })
            );
        });

        it('should send defaultBaseType as null when "Ask each time" so the backend can clear it', () => {
            component.folderForm.patchValue({ title: 'App', name: 'app' });
            spectator.detectChanges();

            spectator.click(spectator.query('[data-testid="content-drive-dialog-folder-create"]'));

            expect(folderService.createFolder).toHaveBeenCalledWith(
                expect.objectContaining({
                    data: expect.objectContaining({ defaultBaseType: null })
                })
            );
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
                    defaultAssetType: DEFAULT_FILE_ASSET_TYPES[0].id,
                    defaultBaseType: null
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
                    fileMasks: ['*.jpg', '*.png'],
                    defaultBaseType: null
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
                    defaultAssetType: DEFAULT_FILE_ASSET_TYPES[0].id,
                    defaultBaseType: null
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
                    defaultAssetType: DEFAULT_FILE_ASSET_TYPES[0].id,
                    defaultBaseType: null
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
                    defaultAssetType: DEFAULT_FILE_ASSET_TYPES[0].id,
                    defaultBaseType: null
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
                    defaultAssetType: DEFAULT_FILE_ASSET_TYPES[0].id,
                    defaultBaseType: null
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
                    fileMasks: ['*.jpg', '*.png'],
                    defaultBaseType: null
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
                    defaultAssetType: 'Video',
                    defaultBaseType: null
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
                    fileMasks: ['*.jpg', '*.png'],
                    defaultBaseType: null
                }
            });
        });

        it('should send a blank mask when every extension is removed, so the saved list is cleared', () => {
            // The backend skips the write when `fileMasks` is absent or an empty list, so neither
            // can express "clear" and the saved extensions came back on the next load. A single
            // blank mask joins to an empty string server-side, which is how a folder with no
            // restrictions is stored.
            component.folderForm.patchValue({ allowedFileExtensions: [] });
            spectator.detectChanges();

            const saveButton = spectator.query(
                '[data-testid="content-drive-dialog-folder-create"]'
            );
            spectator.click(saveButton);

            expect(folderService.saveFolder).toHaveBeenCalledWith(
                expect.objectContaining({
                    data: expect.objectContaining({ fileMasks: [''] })
                })
            );
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
                throwError(() => ({ error: { message: 'Save failed' } }))
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
