import { NgTemplateOutlet } from '@angular/common';
import {
    ChangeDetectionStrategy,
    Component,
    computed,
    DestroyRef,
    ElementRef,
    inject,
    OnInit,
    signal,
    viewChild
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { MessageService, type SortEvent } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';
import { Popover, PopoverModule } from 'primeng/popover';
import { ToastModule } from 'primeng/toast';

import { DotContentletService, DotMessageService, DotUploadFileService } from '@dotcms/data-access';
import {
    ComponentStatus,
    DotCMSBaseTypesContentTypes,
    DotCMSContentlet,
    DotContentDriveItem,
    DotContentDrivePaginateEvent,
    TreeNodeData
} from '@dotcms/dotcms-models';

import { DotAssetPickerSidebarComponent } from './components/dot-asset-picker-sidebar/dot-asset-picker-sidebar.component';
import { DotAssetPickerToolbarComponent } from './components/dot-asset-picker-toolbar/dot-asset-picker-toolbar.component';
import { ERROR_MESSAGE_LIFE, SUCCESS_MESSAGE_LIFE, WARNING_MESSAGE_LIFE } from './constants';
import { writeLastAssetPath } from './last-asset-path';
import { DotAssetPickerStore } from './store/dot-asset-picker.store';
import { DotAssetPickerConfig } from './store/models';

import { DotMessagePipe } from '../../dot-message/dot-message.pipe';
import { DotFolderListViewComponent } from '../dot-folder-list-view/dot-folder-list-view.component';
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
    providers: [DotAssetPickerStore, MessageService],
    imports: [
        ButtonModule,
        DialogModule,
        NgTemplateOutlet,
        PopoverModule,
        ToastModule,
        DotAssetPickerSidebarComponent,
        DotAssetPickerToolbarComponent,
        DotFolderListViewComponent,
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

    protected readonly $loading = computed(() => this.store.status() === ComponentStatus.LOADING);

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
            .subscribe((hydrated) => {
                // Persist on confirm, not on selection: a row the user highlights and then cancels
                // must not move a value shared by every picker in the system.
                writeLastAssetPath(this.#resolveAssetFolder(hydrated));
                this.#dialogRef.close(hydrated);
            });
    }

    /**
     * Folder the chosen asset lives in, so the next open lands there.
     *
     * Derived from `url` (`/images/logo.png` → `/images/`) rather than the folder being browsed,
     * because a global search can return an asset from somewhere else entirely. Falls back to the
     * browsed folder when the row carries no usable url.
     *
     * `DotCMSContentlet.folder` is the folder's *identifier*, not a path, so it is no help here.
     */
    #resolveAssetFolder(asset: DotCMSContentlet): string | undefined {
        const lastSlash = asset.url?.lastIndexOf('/') ?? -1;

        return lastSlash >= 0 ? asset.url.slice(0, lastSlash + 1) : this.store.path();
    }

    /** Closing with no argument is how the caller reads "cancelled". */
    protected cancel(): void {
        this.#dialogRef.close();
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
                hostFolder: targetFolder?.id ?? this.store.config()?.site?.identifier ?? '',
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
