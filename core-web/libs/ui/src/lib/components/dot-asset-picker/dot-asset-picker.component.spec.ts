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
import { Splitter, SplitterModule } from 'primeng/splitter';

import { DotContentletService, DotMessageService, DotUploadFileService } from '@dotcms/data-access';
import {
    ComponentStatus,
    DotCMSBaseTypesContentTypes,
    DotCMSContentlet,
    DotContentDriveBrowseItem,
    DotSite
} from '@dotcms/dotcms-models';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { ASSET_PICKER_SPLITTER_MIN_SIZES, ASSET_PICKER_SPLITTER_SIZES } from './constants';
import { DotAssetPickerComponent } from './dot-asset-picker.component';
import { readLastAssetLocation, writeLastAssetLocation } from './last-asset-path';
import { DotAssetPickerStore } from './store/dot-asset-picker.store';
import { DotAssetPickerConfig } from './store/models';

import { DIALOG_SIZE_TRANSITION, MAXIMIZED_DIALOG_CLASS } from '../../dialog/fullscreen-dialog';
import { DotMessagePipe } from '../../dot-message/dot-message.pipe';
import {
    DotDialogComponent,
    DotDialogContentComponent,
    DotDialogFooterComponent,
    DotDialogHeaderComponent
} from '../dot-dialog';
import { DotToastComponent } from '../dot-toast/dot-toast.component';
import { DotUploadTypeSelectorComponent } from '../dot-upload-type-selector/dot-upload-type-selector.component';

/**
 * What every `overrideComponent({ set: { imports } })` below has to keep real.
 *
 * `set:` replaces the component's `imports` wholesale, and CUSTOM_ELEMENTS_SCHEMA silently accepts
 * whatever is missing — so a shell component left out of this list would render as an inert element
 * whose children still show up, and the assertions would keep passing while testing nothing. One
 * shared list, so that can't drift per describe.
 *
 * The PrimeNG pieces these specs drive (buttons, the selector popover) have to be real too. The
 * splitter especially: it projects the `#panel` templates, so stubbing it renders nothing between
 * the header and the footer.
 */
const PICKER_REAL_IMPORTS = [
    DotMessagePipe,
    NgTemplateOutlet,
    ButtonModule,
    DialogModule,
    PopoverModule,
    SplitterModule,
    DotToastComponent,
    DotDialogComponent,
    DotDialogHeaderComponent,
    DotDialogContentComponent,
    DotDialogFooterComponent,
    DotUploadTypeSelectorComponent
];

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

/** A folder row, as the list hands one back. Not a contentlet — it has no content to fetch. */
const SELECTED_FOLDER = {
    type: 'folder',
    identifier: 'folder-1',
    inode: 'folder-inode',
    title: 'images',
    name: 'images',
    path: '/images/'
} as DotContentDriveBrowseItem;

/** A menu link row. Also not a contentlet. */
const SELECTED_LINK = {
    type: 'link',
    extension: 'link',
    identifier: 'link-1',
    inode: 'link-inode',
    title: 'Docs',
    url: '/docs'
} as DotContentDriveBrowseItem;
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
        /** Failures the store records for this component to toast — see the `requestError` effect. */
        requestError: signal<{ messageKey: string } | null>(null),
        pagination: signal({ limit: 20, page: 1 }),
        totalItems: signal(0),
        /** What the paginator actually reads — see `withAssetBrowse`'s cursor-based row count. */
        $totalRecords: signal(0),
        folders: signal([]),
        foldersStatus: signal(ComponentStatus.LOADED),
        selectedNode: signal<{ data: unknown } | undefined>(undefined),
        selectedAsset: signal<DotContentDriveBrowseItem | null>(null),
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
                    'dot.common.dialog.reject': 'Cancel',
                    'dot.asset.picker.upload.rejected': "Can't upload this file",
                    'dot.asset.picker.upload.rejected.detail': 'Only {0} can be uploaded here.',
                    'dot.asset.picker.upload.types.image': 'images',
                    'dot.asset.picker.upload.types.video': 'video files',
                    'dot.asset.picker.upload.types.audio': 'audio files'
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
                imports: [...PICKER_REAL_IMPORTS],
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

    describe('reporting failed requests', () => {
        // The store cannot toast for itself and deliberately does not inject
        // `DotHttpErrorManagerService` — that pulls in `Router`, which the legacy Dojo host has none
        // of, and it stopped the picker from constructing there at all.
        const messageService = () => spectator.inject(MessageService, true);

        it('should toast what the store says failed', () => {
            const spyAdd = jest.spyOn(messageService(), 'add');

            store.requestError.set({ messageKey: 'dot.asset.picker.error.assets' });
            spectator.detectChanges();

            expect(spyAdd).toHaveBeenCalledWith(expect.objectContaining({ severity: 'error' }));
        });

        it('should say nothing while nothing has failed', () => {
            const spyAdd = jest.spyOn(messageService(), 'add');

            spectator.detectChanges();

            expect(spyAdd).not.toHaveBeenCalled();
        });

        it('should report a second identical failure', () => {
            // Each failure is a fresh object precisely so the effect re-runs — a repeated outage
            // must not go silent just because the message is the same.
            const spyAdd = jest.spyOn(messageService(), 'add');

            store.requestError.set({ messageKey: 'dot.asset.picker.error.folders' });
            spectator.detectChanges();
            store.requestError.set({ messageKey: 'dot.asset.picker.error.folders' });
            spectator.detectChanges();

            expect(spyAdd).toHaveBeenCalledTimes(2);
        });
    });

    describe('open', () => {
        it('should configure the store from the dialog data', () => {
            expect(store.initPicker).toHaveBeenCalledWith(CONFIG);
        });

        // Not decoration: if a shell component ever drops out of PICKER_REAL_IMPORTS,
        // CUSTOM_ELEMENTS_SCHEMA renders it as an inert element and every other assertion here
        // keeps passing. This is what fails instead.
        it('should lay itself out with the shared dialog shell', () => {
            const shell = spectator.query(DotDialogComponent);

            expect(shell).toBeTruthy();
            expect(spectator.query(DotDialogHeaderComponent)).toBeTruthy();
            expect(spectator.query(DotDialogContentComponent)).toBeTruthy();
            expect(spectator.query(DotDialogFooterComponent)).toBeTruthy();
        });
    });

    describe('splitter', () => {
        it('should render both panels inside the splitter', () => {
            const splitter = spectator.query(byTestId('asset-picker-splitter'));

            expect(splitter?.querySelector('[data-testid="asset-picker-sidebar"]')).toBeTruthy();
            expect(splitter?.querySelector('[data-testid="asset-picker-dropzone"]')).toBeTruthy();
        });

        it('should start at the sidebar ratio the fixed layout used to have', () => {
            expect(spectator.query(Splitter)?.panelSizes).toEqual(ASSET_PICKER_SPLITTER_SIZES);
        });

        it('should bound how far either panel can be dragged', () => {
            // No `maxSize` in this PrimeNG version — the content floor is what caps the sidebar.
            expect(spectator.query(Splitter)?.minSizes).toEqual(ASSET_PICKER_SPLITTER_MIN_SIZES);
        });

        it('should reset to the default ratio on every open', () => {
            // No `stateKey`: the width is deliberately not remembered, so each dialog opens the
            // same way regardless of what the last session dragged it to.
            expect(spectator.query(Splitter)?.stateKey).toBeFalsy();
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

        it('should tell the user when the asset can no longer be loaded', () => {
            // The row was fetched minutes ago — by now it can be gone or permissions can have
            // changed. Silently swallowing that left Confirm looking like it did nothing.
            (contentletService.getContentletByInodeWithContent as jest.Mock).mockReturnValue(
                throwError(() => new Error('gone'))
            );
            const messageService = spectator.inject(MessageService, true);
            const addSpy = jest.spyOn(messageService, 'add');

            spectator.click(button('asset-picker-confirm'));

            expect(addSpy).toHaveBeenCalledWith(expect.objectContaining({ severity: 'error' }));
            // The picker stays open so the user can pick something else.
            expect(dialogRef.close).not.toHaveBeenCalled();
        });

        it('should close with a folder as-is, without trying to hydrate it', () => {
            // A folder has an inode but is not a contentlet: `getContentletByInodeWithContent`
            // would 404, the picker would toast "confirm error" and stay open, and selecting a
            // folder would be impossible.
            store.selectedAsset.set(SELECTED_FOLDER);
            spectator.detectChanges();

            spectator.click(button('asset-picker-confirm'));

            expect(contentletService.getContentletByInodeWithContent).not.toHaveBeenCalled();
            expect(dialogRef.close).toHaveBeenCalledWith(SELECTED_FOLDER);
        });

        it('should close with a menu link as-is, without trying to hydrate it', () => {
            store.selectedAsset.set(SELECTED_LINK);
            spectator.detectChanges();

            spectator.click(button('asset-picker-confirm'));

            expect(contentletService.getContentletByInodeWithContent).not.toHaveBeenCalled();
            expect(dialogRef.close).toHaveBeenCalledWith(SELECTED_LINK);
        });

        it('should still hydrate a contentlet', () => {
            // The conditional must not become "never hydrate" — the four asset entry points
            // depend on receiving the full contentlet.
            spectator.click(button('asset-picker-confirm'));

            expect(contentletService.getContentletByInodeWithContent).toHaveBeenCalled();
        });

        it("should remember a folder's own path", () => {
            // A folder carries `path`, not `url`, so the location logic cannot read `url` alone.
            store.selectedAsset.set(SELECTED_FOLDER);
            spectator.detectChanges();

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

    describe('upload restriction', () => {
        const messageService = () => spectator.inject(MessageService, true);

        /** `new File()` defaults `type` to `''`, which the restriction deliberately allows. */
        const fileList = (type: string, name = 'asset.bin'): FileList => {
            const files = [new File([''], name, { type })] as unknown as FileList;
            Object.defineProperty(files, 'length', { value: 1 });

            return files;
        };

        /** Puts the picker in a media mode, the way an Image field opens it. */
        const restrictToImages = () => {
            store.config.set({ ...CONFIG, mimeTypes: ['image/*'] });
            spectator.detectChanges();
        };

        describe('in a media mode', () => {
            beforeEach(() => restrictToImages());

            it('should refuse a dropped file outside the allowed types', () => {
                const spyAdd = jest.spyOn(messageService(), 'add');

                spectator.component['onRequestUpload']({
                    files: fileList('application/pdf', 'report.pdf'),
                    targetFolder: PINNED_FOLDER
                });

                expect(uploadService.uploadFileByBaseType).not.toHaveBeenCalled();
                expect(spyAdd).toHaveBeenCalledWith(
                    expect.objectContaining({
                        severity: 'error',
                        detail: 'Only images can be uploaded here.'
                    })
                );
            });

            it('should not open the Asset/File prompt for a refused drop', () => {
                // Without the early gate the user is asked to choose a storage type and only then
                // told the file was never eligible.
                spectator.component['onRequestUpload']({
                    files: fileList('application/pdf', 'report.pdf')
                });

                expect(spectator.component.$uploadSelectorPayload()).toBeUndefined();
                expect(spectator.component.$uploadModalVisible()).toBe(false);
            });

            it('should refuse a file chosen through the OS dialog', () => {
                // `accept` is a hint the user can override from the dialog's own filter, so the
                // pre-upload check has to stand on its own.
                spectator.component.$activeSelection.set({
                    baseType: DotCMSBaseTypesContentTypes.DOTASSET
                });

                spectator.component['onFileChange']({
                    target: { files: fileList('application/pdf', 'report.pdf'), value: 'x' }
                } as unknown as Event);

                expect(uploadService.uploadFileByBaseType).not.toHaveBeenCalled();
            });

            it('should refuse a file after the Asset/File prompt is answered', () => {
                spectator.component['onUploadTypeSelected']({
                    baseType: DotCMSBaseTypesContentTypes.DOTASSET,
                    files: fileList('application/pdf', 'report.pdf')
                });

                expect(uploadService.uploadFileByBaseType).not.toHaveBeenCalled();
            });

            it('should refuse a drop into a folder that pins a base type', () => {
                // This route skips the prompt entirely — the one most easily left unguarded.
                spectator.component['onRequestUpload']({
                    files: fileList('audio/mpeg', 'song.mp3'),
                    targetFolder: PINNED_FOLDER
                });

                expect(uploadService.uploadFileByBaseType).not.toHaveBeenCalled();
            });

            it('should refuse a button upload into a folder that pins a base type', () => {
                store.selectedNode.set({ data: PINNED_FOLDER });
                spectator.detectChanges();

                spectator.component['onUpload'](new MouseEvent('click'));
                spectator.component['onFileChange']({
                    target: { files: fileList('application/pdf', 'report.pdf'), value: 'x' }
                } as unknown as Event);

                expect(uploadService.uploadFileByBaseType).not.toHaveBeenCalled();
            });

            it('should allow a file whose type the browser does not report', () => {
                // AC-010: the server stays the authority rather than blocking a file we cannot
                // classify.
                spectator.component['onRequestUpload']({
                    files: fileList('', 'mystery.dat'),
                    targetFolder: PINNED_FOLDER
                });

                expect(uploadService.uploadFileByBaseType).toHaveBeenCalled();
            });

            it('should upload an allowed file and refresh the list with the restriction intact', () => {
                store.$request.set({ mimeTypes: ['image/*'] });

                spectator.component['onRequestUpload']({
                    files: fileList('image/png', 'logo.png'),
                    targetFolder: PINNED_FOLDER
                });

                expect(uploadService.uploadFileByBaseType).toHaveBeenCalled();
                expect(store.loadItems).toHaveBeenCalledWith({ mimeTypes: ['image/*'] });
            });
        });

        describe('the hidden file input', () => {
            const fileInput = () =>
                spectator.query('input[type="file"]') as HTMLInputElement | null;

            it('should filter the OS dialog to the restricted family', () => {
                restrictToImages();

                expect(fileInput()?.getAttribute('accept')).toBe('image/*');
            });

            it('should carry every pattern a browse caller asked for', () => {
                store.config.set({ ...CONFIG, mimeTypes: ['image/*', 'video/*'] });
                spectator.detectChanges();

                expect(fileInput()?.getAttribute('accept')).toBe('image/*,video/*');
            });

            it('should carry no accept attribute at all when nothing is restricted', () => {
                // Absence, not `accept=""` — an empty value is a different thing to the browser,
                // and a test asserting `''` would pass against a broken implementation.
                expect(fileInput()?.hasAttribute('accept')).toBe(false);
            });
        });

        describe('the Asset/File prompt', () => {
            const selector = () => spectator.query(DotUploadTypeSelectorComponent);

            it('should hand the restriction label to the selector in a media mode', () => {
                restrictToImages();

                spectator.component['onUpload'](new MouseEvent('click'));
                spectator.detectChanges();

                expect(selector()?.$restrictionLabel()).toBe('images');
            });

            it('should hand the selector no label when nothing is restricted', () => {
                spectator.component['onUpload'](new MouseEvent('click'));
                spectator.detectChanges();

                expect(selector()?.$restrictionLabel()).toBe('');
            });
        });

        describe('in the File field, which restricts nothing', () => {
            // The over-reach guard. CONFIG carries no `mimeTypes`, exactly as a File field opens.
            it('should upload a dropped PDF', () => {
                const spyAdd = jest.spyOn(messageService(), 'add');

                spectator.component['onRequestUpload']({
                    files: fileList('application/pdf', 'report.pdf'),
                    targetFolder: PINNED_FOLDER
                });

                expect(uploadService.uploadFileByBaseType).toHaveBeenCalled();
                expect(spyAdd).not.toHaveBeenCalledWith(
                    expect.objectContaining({ severity: 'error' })
                );
            });

            it('should upload a PDF chosen through the OS dialog', () => {
                spectator.component.$activeSelection.set({
                    baseType: DotCMSBaseTypesContentTypes.DOTASSET
                });

                spectator.component['onFileChange']({
                    target: { files: fileList('application/zip', 'bundle.zip'), value: 'x' }
                } as unknown as Event);

                expect(uploadService.uploadFileByBaseType).toHaveBeenCalled();
            });

            it('should upload a PDF after the Asset/File prompt is answered', () => {
                spectator.component['onUploadTypeSelected']({
                    baseType: DotCMSBaseTypesContentTypes.FILEASSET,
                    files: fileList('application/pdf', 'report.pdf')
                });

                expect(uploadService.uploadFileByBaseType).toHaveBeenCalled();
            });
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
                imports: [...PICKER_REAL_IMPORTS],
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
                imports: [...PICKER_REAL_IMPORTS],
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
