import { signalMethod } from '@ngrx/signals';

import { NgClass } from '@angular/common';
import {
    ChangeDetectionStrategy,
    Component,
    inject,
    input,
    output,
    OnInit,
    OnDestroy,
    AfterViewInit,
    DestroyRef,
    computed,
    forwardRef
} from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { NG_VALUE_ACCESSOR } from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { DialogService, DynamicDialogRef } from 'primeng/dynamicdialog';
import { TooltipModule } from 'primeng/tooltip';

import { filter, map } from 'rxjs/operators';

import { DotAiService, DotMessageService } from '@dotcms/data-access';
import {
    DotCMSContentlet,
    DotCMSContentTypeField,
    DotFileMetadata,
    DotGeneratedAIImage
} from '@dotcms/dotcms-models';
import {
    DotDropZoneComponent,
    DotMessagePipe,
    DotAIImagePromptComponent,
    DotSpinnerComponent,
    DropZoneFileEvent,
    DropZoneFileValidity,
    DotBrowserSelectorComponent
} from '@dotcms/ui';
import { getFileMetadata } from '@dotcms/utils';

import {
    LegacyDialogImageEditorLauncher,
    LegacyDojoImageEditorLauncher
} from './../../services/image-editor';
import { DotFileFieldUploadService } from './../../services/upload-file/upload-file.service';
import { FileFieldStore } from './../../store/file-field.store';
import { getUiMessage } from './../../utils/messages';
import { DotFileFieldPreviewComponent } from './../dot-file-field-preview/dot-file-field-preview.component';
import { DotFileFieldUiMessageComponent } from './../dot-file-field-ui-message/dot-file-field-ui-message.component';
import { DotFormFileEditorComponent } from './../dot-form-file-editor/dot-form-file-editor.component';
import { DotFormImportUrlComponent } from './../dot-form-import-url/dot-form-import-url.component';

import {
    INPUT_TYPE,
    INPUT_TYPES,
    UploadedFile
} from '../../../../models/dot-edit-content-file.model';
import { BaseControlValueAccessor } from '../../../shared/base-control-value-accesor';

@Component({
    selector: 'dot-file-field',
    imports: [
        ButtonModule,
        DotMessagePipe,
        DotDropZoneComponent,
        DotSpinnerComponent,
        DotFileFieldUiMessageComponent,
        DotFileFieldPreviewComponent,
        TooltipModule,
        NgClass
    ],
    providers: [
        DotFileFieldUploadService,
        FileFieldStore,
        DialogService,
        LegacyDialogImageEditorLauncher,
        LegacyDojoImageEditorLauncher,
        {
            multi: true,
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => DotFileFieldComponent)
        }
    ],
    templateUrl: './dot-file-field.component.html',
    styleUrls: ['./dot-file-field.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotFileFieldComponent
    extends BaseControlValueAccessor<string>
    implements OnInit, AfterViewInit, OnDestroy
{
    /**
     * A readonly instance of the FileFieldStore injected into the component.
     * This store is used to manage the state and actions related to the file field.
     */
    readonly store = inject(FileFieldStore);
    /** Opens the legacy image editor JSP inside a PrimeNG dialog (new editor default). */
    readonly #legacyDialogImageEditorLauncher = inject(LegacyDialogImageEditorLauncher);
    /** Dispatches Dojo image-editor events (legacy web-component bridge only). */
    readonly #legacyDojoImageEditorLauncher = inject(LegacyDojoImageEditorLauncher);
    /**
     * A readonly private field that holds an instance of the DialogService.
     * This service is injected using Angular's dependency injection mechanism.
     * It is used to manage dialog interactions within the component.
     */
    readonly #dialogService = inject(DialogService);
    /**
     * A readonly private field that injects the DotMessageService.
     * This service is used for handling message-related functionalities within the component.
     */
    readonly #dotMessageService = inject(DotMessageService);
    /**
     * A readonly private field that holds a reference to the `DestroyRef` service.
     * This service is injected into the component to manage the destruction lifecycle.
     */
    readonly #destroyRef = inject(DestroyRef);
    /**
     * A readonly private field that injects the `DotAiService` service.
     * This service is used to provide AI-related functionalities within the component.
     */
    readonly #dotAiService = inject(DotAiService);
    /**
     * Reference to the dynamic dialog. It can be null if no dialog is currently open.
     *
     * @type {DynamicDialogRef | null}
     */
    #dialogRef: DynamicDialogRef | null = null;
    /**
     * DotCMS Content Type Field
     *
     * @memberof DotEditContentFileFieldComponent
     */
    $field = input.required<DotCMSContentTypeField>({ alias: 'field' });
    /**
     * DotCMS Contentlet
     *
     * @memberof DotEditContentFileFieldComponent
     */
    $contentlet = input.required<DotCMSContentlet>({ alias: 'contentlet' });
    /**
     * Signal indicating whether the field has an error.
     *
     * @type {boolean}
     * @default false
     */
    $hasError = input.required<boolean>({ alias: 'hasError' });
    /**
     * When true, forces the drop zone and action buttons to stack vertically.
     * Use in narrow containers where side-by-side layout would clip the buttons.
     */
    $vertical = input<boolean>(false, { alias: 'vertical' });
    /**
     * When false, hides the "Edit image" action (e.g. EMA quick edit).
     */
    $enableImageEditor = input<boolean>(true, { alias: 'enableImageEditor' });
    /**
     * When true, routes image editing through Dojo DOM events instead of the
     * dialog iframe. Only set by {@link DotBinaryFieldCeBridgeComponent}.
     */
    $useLegacyDojoImageEditor = input<boolean>(false, { alias: 'useLegacyDojoImageEditor' });

    /**
     * Emits when the field value changes due to a user action (upload, image edit,
     * dialog import or remove). Mirrors the legacy binary field contract consumed
     * by the legacy JSP editor and the FileAsset title/fileName auto-fill.
     */
    valueUpdated = output<{ value: string; fileName: string }>();

    /**
     * Current file name derived from the preview file, used in `valueUpdated`.
     */
    $currentFileName = computed<string>(() => {
        const uploaded = this.store.uploadedFile();

        if (!uploaded) {
            return '';
        }

        if (uploaded.source === 'temp') {
            return uploaded.file.fileName ?? uploaded.file.metadata?.name ?? '';
        }

        const file = uploaded.file;

        return file.fileName ?? getFileMetadata(file)?.name ?? file.title ?? '';
    });

    /**
     * Whether the "Edit image" action is available for the current file.
     *
     * Only Binary fields expose the image editor when enabled, and only when
     * the previewed file is actually an image. File/Image fields never show it.
     */
    $canEditImage = computed<boolean>(() => {
        if (!this.$enableImageEditor()) {
            return false;
        }

        if (this.store.inputType() !== INPUT_TYPES.Binary) {
            return false;
        }

        return !!this.#currentMetadata()?.isImage;
    });

    /**
     * Metadata of the current preview file, resolved for both temp and contentlet sources.
     */
    #currentMetadata = computed<DotFileMetadata | null>(() => {
        const uploaded = this.store.uploadedFile();

        if (!uploaded) {
            return null;
        }

        return uploaded.source === 'temp'
            ? (uploaded.file.metadata ?? null)
            : getFileMetadata(uploaded.file);
    });

    /**
     * Signal indicating whether the AI plugin is installed.
     *
     * This signal is initialized with a boolean value indicating the installation status
     * of the AI plugin. It uses the `checkPluginInstallation` method from the `#dotAiService`
     * to determine the initial state.
     *
     * @type {boolean}
     * @default false
     */
    $isAIPluginInstalled = toSignal(this.#dotAiService.checkPluginInstallation(), {
        initialValue: false
    });

    /**
     * Computed property that returns the tooltip text for the AI button.
     * If the AI plugin is not installed, it retrieves the tooltip message from the dotMessageService.
     * Otherwise, it returns null.
     *
     * @returns {string | null} The tooltip text for the AI button or null if the AI plugin is installed.
     */
    $tooltipTextAIBtn = computed(() => {
        const isAIPluginInstalled = this.$isAIPluginInstalled();
        if (!isAIPluginInstalled) {
            return this.#dotMessageService.get('dot.file.field.action.generate.with.tooltip');
        }

        return '';
    });

    constructor() {
        super();
        this.handleStoreValueChange(this.store.value);
        this.handleValueChange(this.$value);
        this.emitValueUpdated(this.store.value);
    }

    /**
     * OnInit lifecycle hook.
     *
     * Initialize the store with the content type field data.
     *
     * @memberof DotEditContentFileFieldComponent
     */
    ngOnInit() {
        const field = this.$field();

        this.store.initLoad({
            fieldVariable: field.variable,
            inputType: field.fieldType as INPUT_TYPE
        });
    }

    /**
     * AfterViewInit lifecycle hook.
     *
     * Hydrates the preview from the contentlet for:
     * - Binary fields, whose value is stored inline on the contentlet (so there
     *   is no separate asset to fetch via {@link getAssetData}).
     * - File/Image fields driven imperatively (binary web component) instead of
     *   via a reactive form value.
     */
    ngAfterViewInit() {
        const field = this.$field();
        const contentlet = this.$contentlet();

        if (!field?.variable || !contentlet) {
            return;
        }

        const isBinary = this.store.inputType() === INPUT_TYPES.Binary;

        // File/Image fields hydrate from the reactive form value via getAssetData.
        if (!isBinary && this.$value()) {
            return;
        }

        const value = this.$value() || contentlet[field.variable] || field.defaultValue;

        if (!value) {
            return;
        }

        // Binary previews need the metadata stored on the contentlet; bail out
        // when there is none (e.g. a freshly created, empty binary field).
        if (isBinary && !this.#hasContentletMetadata(contentlet, field.variable)) {
            return;
        }

        this.#originalValue = value;

        this.store.setFileFromContentlet({
            contentlet,
            fieldVariable: field.variable,
            value
        });
    }

    /**
     * Whether the contentlet carries file metadata for the given field, covering
     * FileAsset (`metaData`), dotAsset (`assetMetaData`) and per-field shapes.
     */
    #hasContentletMetadata(contentlet: DotCMSContentlet, fieldVariable: string): boolean {
        return !!(
            contentlet['metaData'] ||
            contentlet['assetMetaData'] ||
            contentlet[`${fieldVariable}MetaData`]
        );
    }

    /**
     * Opens the image editor for the current asset and applies the edited result.
     *
     * @memberof DotFileFieldComponent
     */
    onEditImage() {
        if (this.$isDisabled() || !this.$canEditImage()) {
            return;
        }

        const variable = this.$field().variable;
        const uploaded = this.store.uploadedFile();
        const inode = this.$contentlet()?.inode;
        const tempId = uploaded?.source === 'temp' ? uploaded.file.id : undefined;

        const launcher = this.$useLegacyDojoImageEditor()
            ? this.#legacyDojoImageEditorLauncher
            : this.#legacyDialogImageEditorLauncher;
        // Future PR: replace the dialog launcher path with the native Angular image editor.

        launcher
            .open({ inode, tempId, variable, fieldName: variable })
            .pipe(
                filter((tempFile) => !!tempFile),
                takeUntilDestroyed(this.#destroyRef)
            )
            .subscribe((tempFile) => {
                this.store.applyTempFile(tempFile);
            });
    }

    /**
     * Handle file drop event.
     *
     * If the field is disabled, nothing happens.
     * If the file is invalid, show an error message.
     * If the file is valid, call the store to handle the upload file.
     *
     * @param {DropZoneFileEvent} { validity, file }
     *
     * @return {void}
     */
    handleFileDrop({ validity, file }: DropZoneFileEvent): void {
        if (this.$isDisabled() || !file) {
            return;
        }

        if (!validity.valid) {
            this.#handleFileDropError(validity);

            return;
        }

        this.store.handleUploadFile(file);
    }

    /**
     * Handles the file input change event.
     *
     * If the field is disabled or file is empty, nothing happens.
     * If the file is not empty, the store is called to handle the upload file.
     *
     * @param files The file list from the input change event.
     *
     * @return {void}
     */
    fileSelected(files: FileList | null) {
        if (this.$isDisabled() || !files || files.length === 0) {
            return;
        }

        const file = files[0];

        if (!file) {
            return;
        }

        this.store.handleUploadFile(file);
    }

    /**
     * Handles the file drop error event.
     *
     * Gets the first error type from the validity and gets the corresponding UI message.
     * Sets the UI message in the store.
     *
     * @param {DropZoneFileValidity} validity The validity object with the error type.
     *
     * @return {void}
     */
    #handleFileDropError({ errorsType }: DropZoneFileValidity): void {
        const errorType = errorsType[0];
        const uiMessage = getUiMessage(errorType);
        this.store.setUIMessage(uiMessage);
    }

    /**
     * Shows the import from URL dialog.
     *
     * If the field is disabled, nothing happens.
     * Opens the dialog with the `DotFormImportUrlComponent` component
     * and passes the field type as data to the component.
     *
     * When the dialog is closed, gets the uploaded file from the component
     * and sets it as the preview file in the store.
     *
     * @return {void}
     */
    showImportUrlDialog() {
        if (this.$isDisabled()) {
            return;
        }

        const header = this.#dotMessageService.get('dot.file.field.dialog.import.from.url.header');

        this.#dialogRef = this.#dialogService.open(DotFormImportUrlComponent, {
            header,
            appendTo: 'body',
            closable: true,
            closeAriaLabel: 'Close',
            closeOnEscape: false,
            draggable: false,
            keepInViewport: false,
            modal: true,
            resizable: false,
            position: 'center',
            data: {
                inputType: this.$field().fieldType,
                acceptedFiles: this.store.acceptedFiles()
            }
        });

        this.#dialogRef.onClose
            .pipe(
                filter((file) => !!file),
                takeUntilDestroyed(this.#destroyRef)
            )
            .subscribe((file: UploadedFile) => {
                this.store.setPreviewFile(file);
            });
    }

    /**
     * Opens a dialog for generating AI images using the `DotAIImagePromptComponent`.
     *
     * If the field is disabled, nothing happens.
     * The dialog is configured with specific properties such as header, appendTo,
     * closeOnEscape, draggable, keepInViewport, maskStyleClass, resizable, modal,
     * width, and style.
     *
     * When the dialog is closed, it filters the selected image, maps it to an
     * `UploadedFile` object, and sets it as the preview file in the store.
     *
     * @private
     */
    showAIImagePromptDialog() {
        if (this.$isDisabled()) {
            return;
        }

        const header = this.#dotMessageService.get('dot.file.field.action.generate.dialog-title');

        this.#dialogRef = this.#dialogService.open(DotAIImagePromptComponent, {
            header,
            appendTo: 'body',
            closable: true,
            closeOnEscape: false,
            draggable: false,
            keepInViewport: false,
            maskStyleClass: 'p-dialog-mask-dynamic',
            resizable: false,
            modal: true,
            width: '90%',
            style: { 'max-width': '1040px' }
        });

        this.#dialogRef.onClose
            .pipe(
                filter((selectedImage: DotGeneratedAIImage) => !!selectedImage),
                map((selectedImage) => {
                    const previewFile: UploadedFile = {
                        source: 'contentlet',
                        file: selectedImage.response.contentlet
                    };

                    return previewFile;
                }),
                takeUntilDestroyed(this.#destroyRef)
            )
            .subscribe((file) => {
                this.store.setPreviewFile(file);
            });
    }

    /**
     * Opens the file editor dialog with specific configurations and handles the file upload process.
     *
     * If the field is disabled, nothing happens.
     * This method performs the following actions:
     * - Retrieves the header message for the dialog.
     * - Opens the `DotFormFileEditorComponent` dialog with various options such as header, appendTo, closeOnEscape, draggable, keepInViewport, maskStyleClass, resizable, modal, width, and style.
     * - Passes data to the dialog, including the uploaded file and a flag to allow file name editing.
     * - Subscribes to the dialog's onClose event to handle the uploaded file and update the store with the preview file.
     *
     */
    showFileEditorDialog() {
        if (this.$isDisabled()) {
            return;
        }

        const header = this.#dotMessageService.get('dot.file.field.dialog.create.new.file.header');

        this.#dialogRef = this.#dialogService.open(DotFormFileEditorComponent, {
            header,
            appendTo: 'body',
            closable: true,
            closeOnEscape: false,
            draggable: false,
            keepInViewport: false,
            maskStyleClass: 'p-dialog-mask-dynamic',
            resizable: false,
            modal: true,
            width: '90%',
            style: { 'max-width': '1040px' },
            data: {
                uploadedFile: this.store.uploadedFile(),
                allowFileNameEdit: true
            }
        });

        this.#dialogRef.onClose
            .pipe(
                filter((file) => !!file),
                takeUntilDestroyed(this.#destroyRef)
            )
            .subscribe((file) => {
                this.store.setPreviewFile(file);
            });
    }
    /**
     * Shows the select existing file dialog.
     *
     * If the field is disabled, nothing happens.
     * Opens the dialog with the `DotSelectExistingFileComponent` component
     * and passes the field type and accepted files as data to the component.
     *
     * When the dialog is closed, gets the uploaded file from the component
     * and sets it as the preview file in the store.
     *
     * @memberof DotEditContentFileFieldComponent
     */
    showSelectExistingFileDialog() {
        if (this.$isDisabled()) {
            return;
        }

        const fieldType = this.$field().fieldType;
        const title =
            fieldType === INPUT_TYPES.Image
                ? 'dot.file.field.dialog.select.existing.image.header'
                : 'dot.file.field.dialog.select.existing.file.header';
        const mimeTypes = fieldType === INPUT_TYPES.Image ? ['image'] : [];

        const header = this.#dotMessageService.get(title);

        this.#dialogRef = this.#dialogService.open(DotBrowserSelectorComponent, {
            header,
            appendTo: 'body',
            closeOnEscape: true,
            closable: true,
            dismissableMask: true,
            draggable: false,
            keepInViewport: false,
            maskStyleClass: 'p-dialog-mask-dynamic',
            resizable: false,
            modal: true,
            width: '90%',
            style: { 'max-width': '1040px' },
            contentStyle: { overflow: 'auto', 'min-height': '45rem' },
            data: {
                mimeTypes,
                showLinks: false,
                showDotAssets: true,
                showPages: false,
                showFiles: true,
                showFolders: false,
                showWorking: true,
                showArchived: false,
                sortByDesc: true
            }
        });

        this.#dialogRef.onClose
            .pipe(
                filter((file) => !!file),
                takeUntilDestroyed(this.#destroyRef)
            )
            .subscribe((file) => {
                this.store.setPreviewFile({ source: 'contentlet', file });
            });
    }

    /**
     * Cleanup method.
     *
     * Closes the dialog if it is still open when the component is destroyed.
     *
     * @return {void}
     */
    ngOnDestroy() {
        this.#dialogRef?.close();
    }

    /**
     * Mount-time chatter suppression for `handleStoreValueChange`.
     *
     * The signalMethod's initial firing observes the store's default
     * empty value before any user interaction, and after `writeValue`
     * the `handleValueChange` -> `store.getAssetData` async hydration
     * causes additional ticks (one or more empty-value ticks during
     * the in-flight HTTP, then one with the resolved identifier).
     * Propagating any of these dirties the parent form on mount.
     *
     * The rule: ignore every `handleStoreValueChange` tick until
     * `writeValue` has run *and* (if it had a value) the store has
     * caught up to it once. After that, every emission is user-driven.
     */
    #hasHydrated = false;

    /**
     * The value the field was loaded with (reactive form value or contentlet
     * value). Used to distinguish hydration ticks from user-driven changes when
     * emitting `valueUpdated`.
     */
    #originalValue: string | null = null;

    /** Last value emitted through `valueUpdated`, to avoid duplicate emissions. */
    #lastEmittedValue: string | null = null;

    override writeValue(value: string): void {
        super.writeValue(value);
        this.#originalValue = value ?? null;
        // If Angular wrote a falsy value, there is no async hydration
        // to wait for — the next tick is already user-territory.
        if (!value) {
            this.#hasHydrated = true;
        }
    }

    /**
     * Emits `valueUpdated` whenever the store value changes to a user-driven
     * value (upload, image edit, dialog import or remove), skipping the initial
     * load/hydration value.
     */
    readonly emitValueUpdated = signalMethod<string>((value) => {
        if (value === null || value === undefined) {
            return;
        }

        if (value === this.#originalValue || value === this.#lastEmittedValue) {
            return;
        }

        this.#lastEmittedValue = value;
        this.valueUpdated.emit({ value, fileName: this.$currentFileName() });
    });

    readonly handleStoreValueChange = signalMethod<string>((value) => {
        if (value === null || value === undefined || !this.onChange) {
            return;
        }

        if (!this.#hasHydrated) {
            // Mark hydration complete the first time the store catches
            // up to the value Angular pushed via `writeValue`. Skip
            // propagating that round-trip emission itself.
            if (value === this.$value()) {
                this.#hasHydrated = true;
            }
            return;
        }

        this.onChange(value);
    });

    readonly handleValueChange = signalMethod<string>((value) => {
        if (!value) {
            return;
        }

        // Binary fields keep the asset inline on the contentlet, so they hydrate
        // from contentlet metadata (ngAfterViewInit) rather than fetching a
        // separate asset by identifier.
        if (this.store.inputType() === INPUT_TYPES.Binary) {
            return;
        }

        this.store.getAssetData(value);
    });
}
