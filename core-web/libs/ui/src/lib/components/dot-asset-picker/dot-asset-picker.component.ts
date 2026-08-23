import { DOCUMENT, NgTemplateOutlet } from '@angular/common';
import {
    ChangeDetectionStrategy,
    Component,
    computed,
    DestroyRef,
    effect,
    ElementRef,
    inject,
    OnInit,
    Renderer2,
    signal,
    viewChild
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { MessageService, type SortEvent } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { Dialog, DialogModule } from 'primeng/dialog';
import { DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';
import { Popover, PopoverModule } from 'primeng/popover';
import { SplitterModule } from 'primeng/splitter';

import {
    DotContentletService,
    DotContentTypeService,
    DotMessageService,
    DotUploadFileService
} from '@dotcms/data-access';
import {
    ComponentStatus,
    DotCMSBaseTypesContentTypes,
    DotCMSContentlet,
    DotContentDriveItem,
    DotContentDrivePaginateEvent,
    TreeNodeData
} from '@dotcms/dotcms-models';

import { DotAssetPickerFullscreenToggleComponent } from './components/dot-asset-picker-fullscreen-toggle/dot-asset-picker-fullscreen-toggle.component';
import { DotAssetPickerSidebarComponent } from './components/dot-asset-picker-sidebar/dot-asset-picker-sidebar.component';
import { DotAssetPickerToolbarComponent } from './components/dot-asset-picker-toolbar/dot-asset-picker-toolbar.component';
import {
    ASSET_PICKER_SPLITTER_MIN_SIZES,
    ASSET_PICKER_SPLITTER_SIZES,
    ERROR_MESSAGE_LIFE,
    SUCCESS_MESSAGE_LIFE,
    WARNING_MESSAGE_LIFE
} from './constants';
import { DotAssetPickerLocation, writeLastAssetLocation } from './last-asset-path';
import { DotAssetPickerStore } from './store/dot-asset-picker.store';
import { DotAssetPickerConfig } from './store/models';

import { DIALOG_SIZE_TRANSITION, MAXIMIZED_DIALOG_CLASS } from '../../dialog/fullscreen-dialog';
import { DotMessagePipe } from '../../dot-message/dot-message.pipe';
// Relative, not `@dotcms/ui`: the shell lives in this same lib, and the barrel would be a cycle.
import {
    DotDialogComponent,
    DotDialogContentComponent,
    DotDialogFooterComponent,
    DotDialogHeaderComponent
} from '../dot-dialog';
import { DotFolderListViewComponent } from '../dot-folder-list-view/dot-folder-list-view.component';
import { DotToastComponent } from '../dot-toast/dot-toast.component';
import { DotUploadDropzoneComponent } from '../dot-upload-dropzone/dot-upload-dropzone.component';
import { DotUploadTypeSelectorComponent } from '../dot-upload-type-selector/dot-upload-type-selector.component';
import {
    DotUploadBaseType,
    DotUploadFiles,
    DotUploadSelection,
    DotUploadSelectorPayload
} from '../dot-upload-type-selector/models';

/**
 * Asset picker dialog: a compact Content Drive for choosing a single asset.
 *
 * Opened through PrimeNG's `DialogService` with a {@link DotAssetPickerConfig} in
 * `DynamicDialogConfig.data`. Confirm closes the dialog with a hydrated contentlet; Cancel closes
 * with nothing.
 *
 * It never navigates: double-clicking a row selects it rather than opening an editor, and no
 * navigation service is injected, so routing away is impossible by construction.
 */
@Component({
    selector: 'dot-asset-picker',
    templateUrl: './dot-asset-picker.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    // `DotContentTypeService` (the toolbar's content-type filter) is `@Injectable()` with no
    // `providedIn: 'root'` and is only provided by the main app shell, so the picker provides it
    // itself — otherwise it throws `NullInjectorError` and renders blank in any other host, notably
    // the legacy Dojo binary-field builder the File/Image field still runs inside.
    //
    // `DotHttpErrorManagerService` deliberately does NOT appear here. Providing it would only move
    // the failure: it transitively needs `DotAlertConfirmService`, `DotRouterService` -> `Router` and
    // `DotEventsSocket`, and that host has no `Router` at all. The store reports failures as state
    // instead and this component toasts them — see the `requestError` effect.
    providers: [DotAssetPickerStore, MessageService, DotContentTypeService],
    imports: [
        ButtonModule,
        DialogModule,
        NgTemplateOutlet,
        PopoverModule,
        SplitterModule,
        DotDialogComponent,
        DotDialogHeaderComponent,
        DotDialogContentComponent,
        DotDialogFooterComponent,
        DotAssetPickerFullscreenToggleComponent,
        DotAssetPickerSidebarComponent,
        DotAssetPickerToolbarComponent,
        DotFolderListViewComponent,
        DotToastComponent,
        DotUploadDropzoneComponent,
        DotUploadTypeSelectorComponent,
        DotMessagePipe
    ]
})
export class DotAssetPickerComponent implements OnInit {
    readonly store = inject(DotAssetPickerStore);

    readonly #dialogRef = inject(DynamicDialogRef);
    readonly #dialogConfig = inject(DynamicDialogConfig<DotAssetPickerConfig>);
    readonly #contentletService = inject(DotContentletService);
    readonly #uploadFileService = inject(DotUploadFileService);
    readonly #messageService = inject(MessageService);
    readonly #dotMessageService = inject(DotMessageService);
    readonly #destroyRef = inject(DestroyRef);
    readonly #document = inject(DOCUMENT);
    readonly #renderer = inject(Renderer2);

    /**
     * The PrimeNG dialog hosting this picker. Injectable because `DynamicDialog` declares its
     * content inside a `<p-dialog>`, so we sit in the Dialog's element injector. `container()` is
     * the `.p-dialog` element. Optional so the component still renders outside a dialog (tests).
     */
    readonly #dialog = inject(Dialog, { optional: true });

    protected readonly $loading = computed(() => this.store.status() === ComponentStatus.LOADING);

    /** Dialog title, handed over by whoever opened the picker. */
    protected readonly $title = computed(() => this.store.config()?.title ?? '');

    // --- splitter --------------------------------------------------------------------------

    protected readonly ASSET_PICKER_SPLITTER_SIZES = ASSET_PICKER_SPLITTER_SIZES;
    protected readonly ASSET_PICKER_SPLITTER_MIN_SIZES = ASSET_PICKER_SPLITTER_MIN_SIZES;

    /**
     * The legacy theme gives `.p-splitter` a gray border and a radius, which read as a stray box
     * inside a dialog that already has its own chrome. The gutter keeps its own styling.
     */
    protected readonly splitterPt = {
        root: { class: 'border-0! rounded-none!' },
        gutterHandle: {
            'aria-label': this.#dotMessageService.get('dot.asset.picker.splitter.aria')
        },
        // PrimeNG types `pt` with every section required even though partial objects are the
        // documented usage, so the untouched sections are declared empty.
        panel: {}
    };

    constructor() {
        // The picker owns its dialog, so it owns full-screen too: resize the host `.p-dialog`
        // whenever `isFullscreen` flips. Same split as the image editor — the store holds the
        // flag, the shell does the DOM work.
        effect(() => this.#applyFullscreen(this.store.isFullscreen()));

        // The store says *what* failed to load; saying it is this component's job. It reports through
        // the picker's own toast rather than `DotHttpErrorManagerService`, which transitively needs
        // `Router` and `DotEventsSocket` — neither exists in the legacy Dojo binary-field host, and
        // injecting it there stopped the dialog from constructing at all.
        //
        // `requestError` is a fresh object per failure, so two identical failures in a row are two
        // distinct values and both get reported.
        effect(() => {
            const requestError = this.store.requestError();

            if (requestError) {
                this.#reportRequestError(requestError.messageKey);
            }
        });
    }

    /** `DotFolderListView` pages by offset; the store pages by cursor + page number. */
    protected readonly $offset = computed(() => {
        const { page, limit } = this.store.pagination();

        return (page - 1) * limit;
    });

    /** Folder an upload lands in — the tree selection, or the site root when nothing is selected. */
    protected readonly $targetFolder = computed(() => this.store.selectedNode()?.data);

    // --- upload flow -----------------------------------------------------------------------

    readonly $uploadSelectorPopover = viewChild<Popover>('uploadSelectorPopover');
    readonly $fileInput = viewChild<ElementRef<HTMLInputElement>>('fileInput');

    /** Drives the Asset/File prompt body. */
    readonly $uploadSelectorPayload = signal<DotUploadSelectorPayload | undefined>(undefined);

    /** Drag-and-drop has no trigger element to anchor a popover to, so it prompts with a modal. */
    readonly $uploadModalVisible = signal(false);

    /** Holds the chosen type while the OS file picker is open (Upload-button flow only). */
    readonly $activeSelection = signal<DotUploadSelection | undefined>(undefined);

    ngOnInit(): void {
        const config = this.#dialogConfig?.data;

        if (config) {
            this.store.initPicker(config);
        }
    }

    // --- selection -------------------------------------------------------------------------

    /**
     * The list always emits an array, even in single-selection mode. Double-click routes here too:
     * it marks the row and nothing else — confirming stays an explicit action.
     */
    protected onSelect(items: DotContentDriveItem[]): void {
        const asset = items?.[0] as DotCMSContentlet | undefined;

        if (asset) {
            this.store.setSelectedAsset(asset);
        } else {
            this.store.clearSelection();
        }
    }

    /**
     * Returns the asset to the caller. The row carries only what the list needs, so it is re-fetched
     * with its full content before closing.
     */
    protected confirm(): void {
        const asset = this.store.selectedAsset();

        // The button is disabled without a selection, but a programmatic call must not throw.
        if (!asset) {
            return;
        }

        this.#contentletService
            .getContentletByInodeWithContent(asset.inode)
            .pipe(takeUntilDestroyed(this.#destroyRef))
            .subscribe({
                next: (hydrated) => {
                    // Persist on confirm, not on selection: a row the user highlights and then
                    // cancels must not move a value shared by every picker in the system.
                    writeLastAssetLocation(this.#resolveAssetLocation(hydrated));
                    this.#dialogRef.close(hydrated);
                },
                // The row was fetched minutes ago; by now it can be gone, or permissions changed.
                // Without this the dialog just sits there and Confirm looks like it did nothing —
                // the picker stays open on purpose so the user can pick something else.
                error: () =>
                    this.#messageService.add({
                        severity: 'error',
                        summary: this.#dotMessageService.get('dot.asset.picker.confirm.error'),
                        detail: this.#dotMessageService.get(
                            'dot.asset.picker.confirm.error.detail'
                        ),
                        life: ERROR_MESSAGE_LIFE
                    })
            });
    }

    /** Toasts a failed request. `messageKey` comes from {@link ASSET_PICKER_ERROR_KEYS}. */
    #reportRequestError(messageKey: string): void {
        this.#messageService.add({
            severity: 'error',
            summary: this.#dotMessageService.get(messageKey),
            life: ERROR_MESSAGE_LIFE
        });
    }

    /**
     * Where the chosen asset lives, so the next open lands there.
     *
     * The folder is derived from `url` (`/images/logo.png` → `/images/`) rather than from the folder
     * being browsed, because a global search can return an asset from somewhere else entirely. The
     * site is the one being browsed — the row carries no hostname of its own.
     *
     * `DotCMSContentlet.folder` is the folder's *identifier*, not a path, so it is no help here.
     */
    #resolveAssetLocation(asset: DotCMSContentlet): DotAssetPickerLocation | undefined {
        const site = this.store.browsingSite();

        if (!site) {
            return undefined;
        }

        const lastSlash = asset.url?.lastIndexOf('/') ?? -1;
        const path = lastSlash >= 0 ? asset.url.slice(0, lastSlash + 1) : this.store.path();

        return { siteId: site.identifier, hostname: site.hostname, path };
    }

    /** Closing with no argument is how the caller reads "cancelled". */
    protected cancel(): void {
        this.#dialogRef.close();
    }

    // --- full screen -----------------------------------------------------------------------

    /**
     * Expands the host dialog to the viewport, or restores it.
     *
     * Full-screen is PrimeNG's own maximized state, so there is nothing to save and restore: the
     * theme sizes {@link MAXIMIZED_DIALOG_CLASS} with `!important`, which is what beats the
     * width/height `DialogService` writes inline. Dropping the class hands the windowed size back.
     *
     * `maximize()` keeps PrimeNG's `maximized` flag in step, so its own class computation agrees with
     * us; the class is applied here as well because the `Dialog` is `OnPush` and a toggle coming from
     * its projected content never marks it dirty.
     */
    #applyFullscreen(on: boolean): void {
        const container = this.#dialog?.container() as HTMLElement | undefined;

        if (!this.#dialog || !container) {
            return;
        }

        // Set the size transition (idempotent) before any toggle, honouring reduced-motion. It
        // lands on the first (windowed) effect run, so the first real toggle already animates.
        this.#renderer.setStyle(
            container,
            'transition',
            this.#prefersReducedMotion() ? '' : DIALOG_SIZE_TRANSITION
        );

        // `Boolean(...)`: PrimeNG leaves `maximized` UNSET until its own button is clicked, and
        // `undefined !== false` would fire `maximize()` on the effect's first (windowed) run —
        // flipping the flag to `true` and opening the picker full screen.
        if (Boolean(this.#dialog.maximized) !== on) {
            this.#dialog.maximize();
        }

        if (on) {
            this.#renderer.addClass(container, MAXIMIZED_DIALOG_CLASS);
        } else {
            this.#renderer.removeClass(container, MAXIMIZED_DIALOG_CLASS);
        }
    }

    /** Whether the user has requested reduced motion (skips the resize animation). */
    #prefersReducedMotion(): boolean {
        return (
            this.#document.defaultView?.matchMedia?.('(prefers-reduced-motion: reduce)').matches ??
            false
        );
    }

    // --- list ------------------------------------------------------------------------------

    protected onPaginate(event: DotContentDrivePaginateEvent): void {
        // Explicit checks because both can legitimately be 0.
        if (event.rows === undefined || event.first === undefined) {
            return;
        }

        this.store.setPagination({ limit: event.rows, page: event.page ?? 1 });
    }

    protected onSort(event: SortEvent): void {
        if (event.order === undefined || !event.field) {
            return;
        }

        this.store.setSort({ field: event.field, order: event.order === 1 ? 'asc' : 'desc' });
    }

    // --- upload ----------------------------------------------------------------------------

    /**
     * Upload button. When the folder pins a base type, skip the prompt and go straight to the OS
     * file picker; otherwise ask, anchored to the button.
     */
    protected onUpload(event: MouseEvent): void {
        const targetFolder = this.$targetFolder();
        const baseType = this.#resolvePreferredBaseType(targetFolder);

        if (baseType) {
            this.$activeSelection.set({ targetFolder, baseType });
            this.$fileInput()?.nativeElement.click();

            return;
        }

        this.#openUploadSelector({ targetFolder }, event);
    }

    /** Drag-and-drop: the files are already known, so a pinned base type uploads immediately. */
    protected onRequestUpload({ files, targetFolder }: DotUploadFiles): void {
        const baseType = this.#resolvePreferredBaseType(targetFolder);

        if (baseType) {
            this.#resolveFilesUpload({ files, targetFolder, baseType });

            return;
        }

        // No trigger element, so the prompt falls back to a modal.
        this.#openUploadSelector({ targetFolder, files });
    }

    protected onUploadTypeSelected(selection: DotUploadSelection): void {
        this.$uploadSelectorPopover()?.hide();
        this.$uploadModalVisible.set(false);
        this.$uploadSelectorPayload.set(undefined);

        if (selection.files?.length) {
            this.#resolveFilesUpload(selection);

            return;
        }

        this.$activeSelection.set(selection);
        this.$fileInput()?.nativeElement.click();
    }

    protected onFileChange(event: Event): void {
        const input = event.target as HTMLInputElement;
        const files = input.files;
        const selection = this.$activeSelection();

        // Consume the files BEFORE resetting the input: `input.files` is live, so `value = ''`
        // empties it and the upload would never fire.
        if (files?.length && selection) {
            this.#resolveFilesUpload({ ...selection, files });
        }

        this.$activeSelection.set(undefined);
        input.value = '';
    }

    protected onUploadModalVisibleChange(visible: boolean): void {
        this.$uploadModalVisible.set(visible);

        if (!visible) {
            this.$uploadSelectorPayload.set(undefined);
        }
    }

    protected onUploadSelectorPopoverHide(): void {
        // The modal may be taking over the shared payload — leave it alone if so.
        if (this.$uploadModalVisible()) {
            return;
        }

        this.$uploadSelectorPayload.set(undefined);
    }

    /**
     * Resolves a folder's stored preference to an upload base type. `undefined` means the folder has
     * no preference and the user should be asked.
     */
    #resolvePreferredBaseType(targetFolder?: TreeNodeData): DotUploadBaseType | undefined {
        const defaultBaseType =
            targetFolder && 'defaultBaseType' in targetFolder
                ? targetFolder.defaultBaseType
                : undefined;

        switch (defaultBaseType?.toUpperCase()) {
            case DotCMSBaseTypesContentTypes.DOTASSET:
                return DotCMSBaseTypesContentTypes.DOTASSET;
            case DotCMSBaseTypesContentTypes.FILEASSET:
                return DotCMSBaseTypesContentTypes.FILEASSET;
            default:
                return undefined;
        }
    }

    /**
     * One entry point for the Asset/File prompt: a popover anchored to the Upload button when there
     * is a trigger event, a centered modal otherwise.
     */
    #openUploadSelector(payload: DotUploadSelectorPayload, event?: MouseEvent): void {
        this.$uploadSelectorPayload.set(payload);

        // Mutually exclusive, so a lingering popover can't sit behind the modal.
        if (event) {
            this.$uploadModalVisible.set(false);
            this.$uploadSelectorPopover()?.show(event, event.currentTarget as HTMLElement);
        } else {
            this.$uploadModalVisible.set(true);
            this.$uploadSelectorPopover()?.hide();
        }
    }

    #resolveFilesUpload({ files, targetFolder, baseType }: DotUploadSelection): void {
        if (!files?.length) {
            return;
        }

        if (files.length > 1) {
            this.#messageService.add({
                severity: 'warn',
                summary: this.#dotMessageService.get('content-drive.work-in-progress'),
                detail: this.#dotMessageService.get('content-drive.multiple-files-warning'),
                life: WARNING_MESSAGE_LIFE
            });
        }

        this.#messageService.add({
            severity: 'info',
            summary: this.#dotMessageService.get('content-drive.file-upload-in-progress'),
            detail: this.#dotMessageService.get('content-drive.file-upload-in-progress-detail')
        });

        this.#uploadByBaseType(files[0], baseType, targetFolder);
    }

    #uploadByBaseType(file: File, baseType: string, targetFolder?: TreeNodeData): void {
        this.#uploadFileService
            .uploadFileByBaseType(file, baseType, {
                // A folder id carries its site; at the root fall back to the site being browsed.
                hostFolder: targetFolder?.id ?? this.store.browsingSite()?.identifier ?? '',
                indexPolicy: 'WAIT_FOR'
            })
            .pipe(takeUntilDestroyed(this.#destroyRef))
            .subscribe({
                next: ({ title }) => {
                    const typeLabel = this.#dotMessageService.get(
                        baseType === DotCMSBaseTypesContentTypes.FILEASSET
                            ? 'content-drive.dialog.upload-selector.file'
                            : 'content-drive.dialog.upload-selector.asset'
                    );

                    this.#messageService.add({
                        severity: 'success',
                        summary: this.#dotMessageService.get('content-drive.add-dotasset-success'),
                        detail: this.#dotMessageService.get(
                            'content-drive.add-dotasset-success-detail',
                            title,
                            typeLabel
                        ),
                        life: SUCCESS_MESSAGE_LIFE
                    });

                    this.store.loadItems(this.store.$request());
                },
                error: (error) => {
                    this.#messageService.add({
                        severity: 'error',
                        summary: this.#dotMessageService.get('content-drive.add-dotasset-error'),
                        detail:
                            error.error?.errors?.[0]?.message ??
                            this.#dotMessageService.get('content-drive.add-dotasset-error-detail'),
                        life: ERROR_MESSAGE_LIFE
                    });
                }
            });
    }
}
