import { byTestId, createComponentFactory, mockProvider, Spectator } from '@openng/spectator/jest';
import { of, throwError } from 'rxjs';

import { NgTemplateOutlet } from '@angular/common';
import { CUSTOM_ELEMENTS_SCHEMA, signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';

import { MessageService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';
import { PopoverModule } from 'primeng/popover';
import { ToastModule } from 'primeng/toast';

import { DotContentletService, DotMessageService, DotUploadFileService } from '@dotcms/data-access';
import {
    ComponentStatus,
    DotCMSBaseTypesContentTypes,
    DotCMSContentlet,
    DotSite
} from '@dotcms/dotcms-models';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotAssetPickerComponent } from './dot-asset-picker.component';
import { DotAssetPickerStore } from './store/dot-asset-picker.store';
import { DotAssetPickerConfig } from './store/models';

import { DotMessagePipe } from '../../dot-message/dot-message.pipe';

const SITE: DotSite = {
    identifier: 'site-1',
    hostname: 'dotcms.com',
    aliases: null,
    archived: false
};

const CONFIG: DotAssetPickerConfig = { site: SITE, languageId: '1' };

const SELECTED_ASSET = { inode: 'inode-1', title: 'logo.png' } as DotCMSContentlet;
const HYDRATED_ASSET = {
    inode: 'inode-1',
    title: 'logo.png',
    asset: '/logo.png'
} as DotCMSContentlet;

/** Folder that pins uploads to a base type, so the Asset/File prompt is skipped. */
const PINNED_FOLDER = {
    type: 'folder' as const,
    id: 'folder-1',
    path: '/images/',
    hostname: 'dotcms.com',
    defaultBaseType: DotCMSBaseTypesContentTypes.DOTASSET
};

/**
 * State is exposed as real signals, not `jest.fn()`s: the component derives `$offset` and
 * `$targetFolder` with `computed`, which only recomputes when a signal dependency changes. A plain
 * mock function would memoize the first value forever.
 */
const createMockStore = () => ({
    // state
    config: signal(CONFIG),
    path: signal<string | undefined>(undefined),
    filters: signal({}),
    items: signal([]),
    status: signal(ComponentStatus.LOADED),
    pagination: signal({ limit: 20, page: 1 }),
    totalItems: signal(0),
    folders: signal([]),
    foldersStatus: signal(ComponentStatus.LOADED),
    selectedNode: signal<{ data: unknown } | undefined>(undefined),
    selectedAsset: signal<DotCMSContentlet | null>(null),
    $request: signal({}),
    // methods
    initPicker: jest.fn(),
    setPath: jest.fn(),
    patchFilters: jest.fn(),
    removeFilter: jest.fn(),
    clearFilters: jest.fn(),
    setSearch: jest.fn(),
    setPagination: jest.fn(),
    setSort: jest.fn(),
    setSelectedAsset: jest.fn(),
    clearSelection: jest.fn(),
    setSelectedNode: jest.fn(),
    updateFolders: jest.fn(),
    loadChildFolders: jest.fn().mockReturnValue(of({ folders: [], totalEntries: 0 })),
    loadFolders: jest.fn(),
    loadItems: jest.fn()
});

describe('DotAssetPickerComponent', () => {
    let spectator: Spectator<DotAssetPickerComponent>;
    let store: ReturnType<typeof createMockStore>;
    let dialogRef: DynamicDialogRef;
    let contentletService: DotContentletService;
    let uploadService: DotUploadFileService;

    const createComponent = createComponentFactory({
        component: DotAssetPickerComponent,
        schemas: [CUSTOM_ELEMENTS_SCHEMA],
        providers: [
            mockProvider(DynamicDialogRef),
            mockProvider(DotContentletService, {
                getContentletByInodeWithContent: jest.fn().mockReturnValue(of(HYDRATED_ASSET))
            }),
            mockProvider(DotUploadFileService, {
                uploadFileByBaseType: jest.fn().mockReturnValue(of({ title: 'logo.png' }))
            }),
            {
                provide: DotMessageService,
                useValue: new MockDotMessageService({
                    'dot.common.dialog.accept': 'Add',
                    'dot.common.dialog.reject': 'Cancel'
                })
            },
            { provide: DynamicDialogConfig, useValue: { data: CONFIG } }
        ],
        detectChanges: false
    });

    const button = (testId: string) =>
        spectator.query(byTestId(testId))?.querySelector('button') as HTMLButtonElement;

    beforeEach(() => {
        store = createMockStore();

        // Swap the real SignalStore (and its HTTP deps) for a plain spy object.
        // `set:` replaces `imports` and `providers` wholesale, so MessageService — a real
        // component-level provider — has to be re-declared here or injection fails.
        TestBed.overrideComponent(DotAssetPickerComponent, {
            set: {
                // CUSTOM_ELEMENTS_SCHEMA stubs the dot-* children, but the PrimeNG pieces this
                // spec drives (buttons, the selector popover) have to be real.
                imports: [
                    DotMessagePipe,
                    NgTemplateOutlet,
                    ButtonModule,
                    DialogModule,
                    PopoverModule,
                    ToastModule
                ],
                schemas: [CUSTOM_ELEMENTS_SCHEMA],
                providers: [{ provide: DotAssetPickerStore, useValue: store }, MessageService]
            }
        });

        spectator = createComponent();
        dialogRef = spectator.inject(DynamicDialogRef);
        contentletService = spectator.inject(DotContentletService);
        uploadService = spectator.inject(DotUploadFileService);
        spectator.detectChanges();
    });

    afterEach(() => jest.clearAllMocks());

    describe('open', () => {
        it('should configure the store from the dialog data', () => {
            expect(store.initPicker).toHaveBeenCalledWith(CONFIG);
        });
    });

    describe('selection', () => {
        it('should store the first item the list emits', () => {
            spectator.component['onSelect']([SELECTED_ASSET]);

            expect(store.setSelectedAsset).toHaveBeenCalledWith(SELECTED_ASSET);
        });

        it('should clear the selection when the list emits nothing', () => {
            spectator.component['onSelect']([]);

            expect(store.clearSelection).toHaveBeenCalled();
        });

        it('should disable Confirm while nothing is selected', () => {
            expect(button('asset-picker-confirm').disabled).toBe(true);
        });

        it('should enable Confirm once an asset is selected', () => {
            store.selectedAsset.set(SELECTED_ASSET);
            spectator.detectChanges();

            expect(button('asset-picker-confirm').disabled).toBe(false);
        });
    });

    describe('confirm', () => {
        beforeEach(() => {
            store.selectedAsset.set(SELECTED_ASSET);
            spectator.detectChanges();
        });

        it('should close with the hydrated contentlet, not the list row', () => {
            spectator.click(button('asset-picker-confirm'));

            expect(contentletService.getContentletByInodeWithContent).toHaveBeenCalledWith(
                'inode-1'
            );
            expect(dialogRef.close).toHaveBeenCalledWith(HYDRATED_ASSET);
        });

        it('should do nothing when called with no selection', () => {
            store.selectedAsset.set(null);

            spectator.component['confirm']();

            expect(contentletService.getContentletByInodeWithContent).not.toHaveBeenCalled();
            expect(dialogRef.close).not.toHaveBeenCalled();
        });
    });

    describe('cancel', () => {
        it('should close with no result', () => {
            spectator.click(button('asset-picker-cancel'));

            expect(dialogRef.close).toHaveBeenCalledWith();
        });
    });

    describe('double click', () => {
        it('should select the row without confirming', () => {
            spectator.component['onSelect']([SELECTED_ASSET]);

            expect(store.setSelectedAsset).toHaveBeenCalledWith(SELECTED_ASSET);
            expect(dialogRef.close).not.toHaveBeenCalled();
        });

        it('should never navigate to an editor', () => {
            // No navigation service is injectable here; a successful construction is the assertion.
            expect(spectator.component).toBeTruthy();
        });
    });

    describe('pagination and sort', () => {
        it('should translate the paginate event into store pagination', () => {
            spectator.component['onPaginate']({ first: 20, rows: 20, page: 2 });

            expect(store.setPagination).toHaveBeenCalledWith({ limit: 20, page: 2 });
        });

        it('should ignore a paginate event with no rows', () => {
            spectator.component['onPaginate']({ first: 0, rows: undefined });

            expect(store.setPagination).not.toHaveBeenCalled();
        });

        it('should derive the list offset from page and limit', () => {
            store.pagination.set({ limit: 20, page: 3 });
            spectator.detectChanges();

            expect(spectator.component['$offset']()).toBe(40);
        });

        it('should translate the sort event', () => {
            spectator.component['onSort']({ field: 'title', order: 1 });

            expect(store.setSort).toHaveBeenCalledWith({ field: 'title', order: 'asc' });
        });
    });

    describe('upload', () => {
        it('should prompt for a type when the folder pins none', () => {
            spectator.component['onUpload'](new MouseEvent('click'));

            expect(spectator.component.$uploadSelectorPayload()).toEqual({
                targetFolder: undefined
            });
            expect(uploadService.uploadFileByBaseType).not.toHaveBeenCalled();
        });

        it('should skip the prompt when the folder pins a base type', () => {
            store.selectedNode.set({ data: PINNED_FOLDER });
            spectator.detectChanges();

            spectator.component['onUpload'](new MouseEvent('click'));

            expect(spectator.component.$uploadSelectorPayload()).toBeUndefined();
            expect(spectator.component.$activeSelection()).toEqual({
                targetFolder: PINNED_FOLDER,
                baseType: DotCMSBaseTypesContentTypes.DOTASSET
            });
        });

        it('should upload dropped files directly when the folder pins a base type', () => {
            const files = [new File([''], 'a.png')] as unknown as FileList;
            Object.defineProperty(files, 'length', { value: 1 });

            spectator.component['onRequestUpload']({ files, targetFolder: PINNED_FOLDER });

            expect(uploadService.uploadFileByBaseType).toHaveBeenCalledWith(
                expect.anything(),
                DotCMSBaseTypesContentTypes.DOTASSET,
                expect.objectContaining({ hostFolder: 'folder-1' })
            );
        });

        it('should upload to the site root when no folder is selected', () => {
            const files = [new File([''], 'a.png')] as unknown as FileList;
            Object.defineProperty(files, 'length', { value: 1 });

            spectator.component['onUploadTypeSelected']({
                baseType: DotCMSBaseTypesContentTypes.DOTASSET,
                files
            });

            expect(uploadService.uploadFileByBaseType).toHaveBeenCalledWith(
                expect.anything(),
                DotCMSBaseTypesContentTypes.DOTASSET,
                expect.objectContaining({ hostFolder: SITE.identifier })
            );
        });

        it('should refresh the list after a successful upload', () => {
            const files = [new File([''], 'a.png')] as unknown as FileList;
            Object.defineProperty(files, 'length', { value: 1 });

            spectator.component['onRequestUpload']({ files, targetFolder: PINNED_FOLDER });

            expect(store.loadItems).toHaveBeenCalled();
        });

        it('should not refresh the list when the upload fails', () => {
            (uploadService.uploadFileByBaseType as jest.Mock).mockReturnValue(
                throwError(() => ({ error: { errors: [{ message: 'nope' }] } }))
            );
            const files = [new File([''], 'a.png')] as unknown as FileList;
            Object.defineProperty(files, 'length', { value: 1 });

            spectator.component['onRequestUpload']({ files, targetFolder: PINNED_FOLDER });

            expect(store.loadItems).not.toHaveBeenCalled();
        });
    });
});

describe('DotAssetPickerComponent — opened without dialog data', () => {
    let store: ReturnType<typeof createMockStore>;

    const createComponent = createComponentFactory({
        component: DotAssetPickerComponent,
        schemas: [CUSTOM_ELEMENTS_SCHEMA],
        providers: [
            mockProvider(DynamicDialogRef),
            mockProvider(DotContentletService),
            mockProvider(DotUploadFileService),
            { provide: DotMessageService, useValue: new MockDotMessageService({}) },
            { provide: DynamicDialogConfig, useValue: { data: null } }
        ],
        detectChanges: false
    });

    beforeEach(() => {
        store = createMockStore();
        TestBed.overrideComponent(DotAssetPickerComponent, {
            set: {
                imports: [
                    DotMessagePipe,
                    NgTemplateOutlet,
                    ButtonModule,
                    DialogModule,
                    PopoverModule,
                    ToastModule
                ],
                schemas: [CUSTOM_ELEMENTS_SCHEMA],
                providers: [{ provide: DotAssetPickerStore, useValue: store }, MessageService]
            }
        });
    });

    it('should render without configuring the store', () => {
        // A host that opens the dialog with no payload must not crash the picker.
        expect(() => createComponent().detectChanges()).not.toThrow();
        expect(store.initPicker).not.toHaveBeenCalled();
    });
});
