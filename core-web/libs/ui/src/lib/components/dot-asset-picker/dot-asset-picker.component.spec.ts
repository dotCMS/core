import { byTestId, createComponentFactory, mockProvider, Spectator } from '@openng/spectator/jest';
import { of, throwError } from 'rxjs';

import { NgTemplateOutlet } from '@angular/common';
import { CUSTOM_ELEMENTS_SCHEMA, signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';

import { MessageService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { Dialog, DialogModule } from 'primeng/dialog';
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
import { readLastAssetLocation, writeLastAssetLocation } from './last-asset-path';
import { DotAssetPickerStore } from './store/dot-asset-picker.store';
import { DotAssetPickerConfig } from './store/models';

import { DIALOG_SIZE_TRANSITION, MAXIMIZED_DIALOG_CLASS } from '../../dialog/fullscreen-dialog';
import { DotMessagePipe } from '../../dot-message/dot-message.pipe';

const SITE: DotSite = {
    identifier: 'site-1',
    hostname: 'dotcms.com',
    aliases: null,
    archived: false
};

const CONFIG: DotAssetPickerConfig = { site: SITE, languageId: '1' };

/** The site the mock store reports as being browsed, as a storable location. */
const BROWSING_SITE_LOCATION = { siteId: SITE.identifier, hostname: SITE.hostname };

const SELECTED_ASSET = { inode: 'inode-1', title: 'logo.png' } as DotCMSContentlet;
const HYDRATED_ASSET = {
    inode: 'inode-1',
    title: 'logo.png',
    url: '/images/logo.png'
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
const createMockStore = () => {
    const isFullscreen = signal(false);

    return {
        // state
        config: signal(CONFIG),
        browsingSite: signal<{ identifier: string; hostname: string } | undefined>({
            identifier: SITE.identifier,
            hostname: SITE.hostname
        }),
        path: signal<string | undefined>(undefined),
        filters: signal({}),
        isFullscreen,
        treeSearch: signal(''),
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
        selectNode: jest.fn(),
        setTreeSearch: jest.fn(),
        expandNode: jest.fn(),
        loadMore: jest.fn(),
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
        loadItems: jest.fn(),
        toggleFullscreen: jest.fn(() => isFullscreen.set(!isFullscreen()))
    };
};

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
        window.localStorage.clear();
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

        // `mockProvider` builds one mock instance for the whole describe and `clearAllMocks` only
        // clears calls, not implementations — so re-seed here or a test that overrides a return
        // value leaks into every test after it.
        (contentletService.getContentletByInodeWithContent as jest.Mock).mockReturnValue(
            of(HYDRATED_ASSET)
        );
        (uploadService.uploadFileByBaseType as jest.Mock).mockReturnValue(
            of({ title: 'logo.png' })
        );

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

        it("should remember the asset's own folder, derived from its url", () => {
            spectator.click(button('asset-picker-confirm'));

            expect(readLastAssetLocation()?.path).toBe('/images/');
        });

        it('should remember the site the asset came from, not just the folder', () => {
            // The picker browses every site, so a bare `/images/` would send the next open to a
            // folder of the same name on whichever site the editor happens to be on.
            store.browsingSite.set({ identifier: 'site-2', hostname: 'blog.dotcms.com' });
            spectator.detectChanges();

            spectator.click(button('asset-picker-confirm'));

            expect(readLastAssetLocation()).toEqual({
                siteId: 'site-2',
                hostname: 'blog.dotcms.com',
                path: '/images/'
            });
        });

        it('should fall back to the browsed folder when the asset has no url', () => {
            (contentletService.getContentletByInodeWithContent as jest.Mock).mockReturnValue(
                of({ inode: 'inode-1' } as DotCMSContentlet)
            );
            store.path.set('/docs/');
            spectator.detectChanges();

            spectator.click(button('asset-picker-confirm'));

            expect(readLastAssetLocation()?.path).toBe('/docs/');
        });

        it('should overwrite the remembered folder when a different asset is confirmed', () => {
            writeLastAssetLocation({ ...BROWSING_SITE_LOCATION, path: '/old/' });

            spectator.click(button('asset-picker-confirm'));

            expect(readLastAssetLocation()?.path).toBe('/images/');
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

        it('should leave the remembered folder untouched', () => {
            // Highlighting a row and backing out must not move a system-wide value.
            writeLastAssetLocation({ ...BROWSING_SITE_LOCATION, path: '/old/' });
            store.selectedAsset.set(SELECTED_ASSET);
            spectator.detectChanges();

            spectator.click(button('asset-picker-cancel'));

            expect(readLastAssetLocation()?.path).toBe('/old/');
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
        window.localStorage.clear();
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

describe('DotAssetPickerComponent — full screen', () => {
    let spectator: Spectator<DotAssetPickerComponent>;
    let store: ReturnType<typeof createMockStore>;
    let container: HTMLElement;
    /** Stand-in for the PrimeNG `Dialog`, whose `maximized` flag the picker keeps in step. */
    let dialog: {
        maximized: boolean | undefined;
        maximize: jest.Mock;
        container: () => HTMLElement;
    };

    const createComponent = createComponentFactory({
        component: DotAssetPickerComponent,
        schemas: [CUSTOM_ELEMENTS_SCHEMA],
        providers: [
            mockProvider(DynamicDialogRef),
            mockProvider(DotContentletService),
            mockProvider(DotUploadFileService),
            { provide: DotMessageService, useValue: new MockDotMessageService({}) },
            { provide: DynamicDialogConfig, useValue: { data: CONFIG } }
        ],
        detectChanges: false
    });

    beforeEach(() => {
        window.localStorage.clear();
        store = createMockStore();

        // The `.p-dialog` element. `DialogService` sizes it inline, which is why full screen has to
        // come from a class the theme declares with `!important`.
        container = document.createElement('div');
        // A plain length, not the real `min(90vw, 1040px)`: jsdom drops CSS values it can't parse.
        container.style.width = '1040px';
        dialog = {
            // UNSET, like PrimeNG's own field before its maximize button is ever clicked. Starting
            // this at `false` is what hid the bug where the picker opened full screen.
            maximized: undefined,
            maximize: jest.fn(() => (dialog.maximized = !dialog.maximized)),
            container: () => container
        };

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
                providers: [
                    { provide: DotAssetPickerStore, useValue: store },
                    { provide: Dialog, useValue: dialog },
                    MessageService
                ]
            }
        });

        spectator = createComponent();
        spectator.detectChanges();
    });

    afterEach(() => jest.clearAllMocks());

    it('should open windowed, not full screen', () => {
        // Regression: PrimeNG leaves `maximized` unset, so a strict `!== false` fired `maximize()`
        // on the effect's first run and the picker opened filling the viewport.
        expect(container.classList.contains(MAXIMIZED_DIALOG_CLASS)).toBe(false);
        expect(dialog.maximize).not.toHaveBeenCalled();
        expect(dialog.maximized).toBeFalsy();
    });

    it('should maximize the dialog when the flag flips', () => {
        store.isFullscreen.set(true);
        spectator.detectChanges();

        expect(container.classList.contains(MAXIMIZED_DIALOG_CLASS)).toBe(true);
    });

    it('should keep PrimeNG’s own maximized flag in step', () => {
        // Otherwise the next time PrimeNG recomputes its host classes it would drop ours.
        store.isFullscreen.set(true);
        spectator.detectChanges();

        expect(dialog.maximized).toBe(true);
        expect(dialog.maximize).toHaveBeenCalledTimes(1);
    });

    it('should hand the windowed size back on exit', () => {
        store.isFullscreen.set(true);
        spectator.detectChanges();
        store.isFullscreen.set(false);
        spectator.detectChanges();

        expect(container.classList.contains(MAXIMIZED_DIALOG_CLASS)).toBe(false);
        expect(dialog.maximized).toBe(false);
        // Nothing to restore: the windowed size was never overwritten.
        expect(container.style.width).toBe('1040px');
    });

    it('should animate the resize by default', () => {
        expect(container.style.transition).toBe(DIALOG_SIZE_TRANSITION);
    });
});
