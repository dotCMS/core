import { HttpErrorResponse } from '@angular/common/http';
import {
    Component,
    computed,
    effect,
    inject,
    input,
    signal,
    viewChild,
    ChangeDetectionStrategy
} from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import {
    FormBuilder,
    FormControl,
    FormGroup,
    ReactiveFormsModule,
    Validators
} from '@angular/forms';

import { MessageService } from 'primeng/api';
import { AutoComplete, AutoCompleteCompleteEvent, AutoCompleteModule } from 'primeng/autocomplete';
import { AutoFocusModule } from 'primeng/autofocus';
import { ButtonModule } from 'primeng/button';
import { InputNumberModule } from 'primeng/inputnumber';
import { InputTextModule } from 'primeng/inputtext';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { RadioButtonModule } from 'primeng/radiobutton';
import { SelectModule } from 'primeng/select';
import { TabsModule } from 'primeng/tabs';
import { ToggleSwitchModule } from 'primeng/toggleswitch';

import { DotContentTypeService, DotFolderService, DotMessageService } from '@dotcms/data-access';
import { DotContentDriveActionableFolder, DotFolderEntity } from '@dotcms/dotcms-models';
import { DotFieldRequiredDirective, DotMessagePipe } from '@dotcms/ui';

import {
    SUGGESTED_ALLOWED_FILE_EXTENSIONS,
    DEFAULT_FILE_ASSET_TYPES,
    FOLDER_UPLOAD_BEHAVIOR_OPTIONS
} from '../../../shared/constants';
import { DotContentDriveStore } from '../../../store/dot-content-drive.store';
interface FolderForm {
    title: FormControl<string>;
    sortOrder: FormControl<number | null>;
    allowedFileExtensions: FormControl<string[]>;
    defaultFileAssetType: FormControl<string>;
    defaultBaseType: FormControl<string | null>;
    showOnMenu: FormControl<boolean>;
    name: FormControl<string>;
}

@Component({
    selector: 'dot-content-drive-dialog-folder',
    imports: [
        TabsModule,
        ReactiveFormsModule,
        InputTextModule,
        DotMessagePipe,
        SelectModule,
        ToggleSwitchModule,
        ButtonModule,
        InputNumberModule,
        ProgressSpinnerModule,
        AutoCompleteModule,
        AutoFocusModule,
        RadioButtonModule,
        DotFieldRequiredDirective
    ],
    templateUrl: './dot-content-drive-dialog-folder.component.html',
    changeDetection: ChangeDetectionStrategy.Eager,
    host: { class: 'block' }
})
export class DotContentDriveDialogFolderComponent {
    #fb = inject(FormBuilder);
    #dotFolderService = inject(DotFolderService);
    #store = inject(DotContentDriveStore);
    #messageService = inject(MessageService);
    #dotMessageService = inject(DotMessageService);
    #dotContentTypeService = inject(DotContentTypeService);

    /**
     * Empty string until the site resolves. `#getAssetPath` concatenates this into a `//host/path/`
     * string, so `undefined` would have produced the literal text "undefined" in a saved path.
     */
    #hostName = this.#store.currentSite()?.hostname ?? '';

    /**
     * The folder being edited, or `undefined` in create mode. Typed as the narrow
     * {@link DotContentDriveActionableFolder} so both sources work: a full row from the table and a
     * search view from the sidebar tree.
     */
    $folder = input<DotContentDriveActionableFolder | undefined>(undefined, { alias: 'folder' });

    readonly $fileAssetTypes = toSignal(
        this.#dotContentTypeService.getContentTypes({ type: 'FILEASSET' })
    );

    /**
     * Whether the form can be shown yet.
     *
     * The values come from the folder plus the fetched file-asset types, so rendering earlier means
     * rendering a form that {@link setFolderFormEffect} then patches under the user: anything typed
     * in the meantime is silently overwritten. It also used to prune the extension chips, because
     * PrimeNG rebuilds those from whatever the user had filtered the suggestions down to.
     */
    readonly $formReady = computed(() => !!this.$fileAssetTypes());

    /** Options for the "Upload Behavior" radio group (bound to the `defaultBaseType` control). */
    protected readonly uploadBehaviorOptions = FOLDER_UPLOAD_BEHAVIOR_OPTIONS;

    /** Allowed-file-extensions field; chips are added through its own model. See {@link #addExtension}. */
    readonly $extensionsAutoComplete = viewChild<AutoComplete>('extensionsAutoComplete');

    folderForm: FormGroup<FolderForm> = this.#fb.group({
        title: this.#fb.control('', { validators: [Validators.required], nonNullable: true }),
        sortOrder: this.#fb.control<number | null>(1),
        // Type argument given: from `[]` alone the control infers `FormControl<never[]>`.
        allowedFileExtensions: this.#fb.control<string[]>([], { nonNullable: true }),
        defaultFileAssetType: this.#fb.control(DEFAULT_FILE_ASSET_TYPES[0].id, {
            nonNullable: true
        }),
        defaultBaseType: this.#fb.control<string | null>(null),
        showOnMenu: this.#fb.control(false, { nonNullable: true }),
        name: this.#fb.control('', { validators: [Validators.required], nonNullable: true })
    });

    /** Signal containing the current site information from the store */
    $currentSite = this.#store.currentSite;

    /** Signal tracking changes to the folder title form control */
    $title = toSignal(this.folderForm.controls.title.valueChanges);

    /** Signal tracking changes to the folder URL form control */
    $name = toSignal(this.folderForm.controls.name.valueChanges);

    /**
     * Suggestions offered while the user types; {@link onCompleteMethod} fills it per keystroke.
     *
     * It starts empty on purpose. PrimeNG rebuilds the chips from this list on every write to the
     * control, keeping only the values it can find here — so a saved extension the list does not
     * carry (say `*.svg`) would be dropped from the chips while staying in the form value:
     * invisible to the user, impossible to remove, and sent right back on save. With nothing to
     * match against, PrimeNG keeps the written value verbatim and the chips mirror the control.
     * Nothing is lost visually: the panel only opens once a keystroke has produced suggestions.
     */
    $filteredAllowedFileExtensions = signal<string[]>([]);

    /** Signal tracking the loading state during folder creation */
    $isLoading = signal(false);

    $originalName = signal<string | undefined>(undefined);

    setFolderFormEffect = effect(() => {
        const folder = this.$folder();
        const assetType = this.$fileAssetTypes()?.find(
            (asset) => asset.id === folder?.defaultFileType
        );

        if (folder && assetType) {
            const cleanName = folder.name;

            this.$originalName.set(cleanName);

            this.folderForm.patchValue({
                title: folder.title,
                sortOrder: folder.sortOrder,
                allowedFileExtensions: folder.filesMasks?.trim().length
                    ? folder.filesMasks.split(',')
                    : [],
                defaultFileAssetType: assetType.variable,
                // Normalize to the uppercase enum the radio options use (DOTASSET/FILEASSET),
                // matching the defensive `.toUpperCase()` in the shell (#resolvePreferredBaseType)
                // and toolbar ($uploadLabelKey) — otherwise a non-uppercase backend value would
                // leave the radio on "Ask each time" while the toolbar/upload flow treat it as pinned.
                defaultBaseType: folder.defaultBaseType?.toUpperCase() ?? null,
                showOnMenu: folder.showOnMenu,
                name: cleanName
            });
        }
    });

    /**
     * Computed signal that generates the full folder path
     * Combines hostname, current path, and URL to create the complete folder path
     * Ensures proper path formatting by removing trailing slashes
     */
    $finalPath = computed(() => {
        // `toSignal` has no initial value, so this is undefined until the control first emits.
        const name = this.$name() ?? '';

        return this.#getAssetPath(name);
    });

    /**
     * Effect that automatically generates a navigation label based on the name
     * Only runs when the navigation label field has not been manually edited by the user
     * Converts the name to a navigation label-friendly format and sets it as the navigation label value
     */
    readonly navigationLabelEffect = effect(() => {
        if (this.folderForm.get('title')?.dirty || !!this.$folder()) {
            return;
        }

        const name = this.$name();
        const navigationLabel = this.#getNavigationLabel(name || '');

        this.folderForm.get('title')?.setValue(navigationLabel || '');
    });

    /**
     * Handles the enter key press event for adding file extensions
     * Adds the input value to the allowedFileExtensions form control if it's not a duplicate
     *
     * @param {Event} event - The keyboard event from the input element
     */
    onEnterKey(event: Event) {
        // Stop the AutoComplete/form from also handling Enter (double-add) and keep the typed
        // text from persisting into the next entry, which was resetting the current selection.
        event.preventDefault();

        this.#addExtension(event);
    }

    /**
     * Adds the currently typed extension as a chip.
     *
     * The value is pushed through the AutoComplete's own model instead of
     * `control.setValue([...current, value])`: PrimeNG's `writeControlValue` re-derives the chips
     * from the current `suggestions` list, so writing the whole array back drops every entry that
     * is not part of the active filter. That left the form value and the visible chips out of
     * sync — the previously saved extension stayed in the form value and came back on reload.
     *
     * @param {Event} event - The originating keyboard event
     */
    #addExtension(event: Event) {
        const input = event.target as HTMLInputElement;
        const extension = (input?.value ?? '').trim();

        if (!extension) {
            return;
        }

        const currentExtensions = this.folderForm.controls.allowedFileExtensions.value ?? [];

        if (currentExtensions.includes(extension)) {
            // Clear the input so the next extension starts fresh and existing chips are preserved.
            input.value = '';

            return;
        }

        this.$extensionsAutoComplete()?.onOptionSelect(event, extension);
    }

    /**
     * Handles the autocomplete filtering for allowed file extensions
     * Filters the ALLOWED_FILE_EXTENSIONS array based on the query string
     * and updates the filteredAllowedFileExtensions signal with matching extensions
     *
     * @param {AutoCompleteCompleteEvent} param0 - The autocomplete event containing the query string
     */
    onCompleteMethod({ query }: AutoCompleteCompleteEvent) {
        const extensions = SUGGESTED_ALLOWED_FILE_EXTENSIONS.filter((extension) =>
            extension.includes(query)
        );

        this.$filteredAllowedFileExtensions.set(extensions);
    }

    /**
     * Handles the success case for folder creation and saving
     * Reloads the content drive, loads items, and closes the dialog
     */
    #onSuccess() {
        this.#store.reloadContentDrive();
        this.#store.loadFolders();
        this.#store.closeDialog();
    }

    /**
     * Creates a new folder using the form data
     * Sets loading state, makes API call to create folder, and handles success/error cases
     * On success: Reloads folders, closes dialog, shows success message
     * On error: Shows error message, logs error, resets loading state
     */
    createFolder() {
        this.$isLoading.set(true);
        const body: DotFolderEntity = this.#createFolderBody();

        this.#dotFolderService.createFolder(body).subscribe({
            next: () => {
                this.#onSuccess();

                this.#messageService.add({
                    severity: 'success',
                    summary: 'Success',
                    detail: this.#dotMessageService.get(
                        'content-drive.dialog.folder.message.create-success'
                    )
                });
            },
            error: (err) => {
                const { error } = err as HttpErrorResponse;

                console.error('Error creating folder:', err);

                this.$isLoading.set(false);

                this.#messageService.add({
                    severity: 'error',
                    summary: this.#dotMessageService.get(
                        'content-drive.dialog.folder.message.create-error'
                    ),
                    detail: error.message
                });
            }
        });
    }

    saveFolder() {
        this.$isLoading.set(true);
        const body: DotFolderEntity = this.#createFolderBody();

        this.#dotFolderService.saveFolder(body).subscribe({
            next: () => {
                this.#onSuccess();

                this.#messageService.add({
                    severity: 'success',
                    summary: this.#dotMessageService.get(
                        'content-drive.dialog.folder.message.save-success'
                    )
                });
            },
            error: (err) => {
                const { error } = err as HttpErrorResponse;

                console.error('Error saving folder:', err);

                this.$isLoading.set(false);

                this.#messageService.add({
                    severity: 'error',
                    summary: this.#dotMessageService.get(
                        'content-drive.dialog.folder.message.save-error'
                    ),
                    detail: error.message
                });
            }
        });
    }

    #createFolderBody() {
        const formValue = this.folderForm.getRawValue();

        const data: DotFolderEntity['data'] = {
            title: formValue.title // Always include title
        };

        // Only add properties if they have values
        if (formValue.showOnMenu !== undefined && formValue.showOnMenu !== null) {
            data.showOnMenu = formValue.showOnMenu;
        }

        if (this.$originalName() && formValue.name !== this.$originalName()) {
            data.name = this.#getSlugTitle(formValue.name);
        }

        if (formValue.sortOrder !== null && formValue.sortOrder !== undefined) {
            data.sortOrder = formValue.sortOrder;
        }

        if (formValue.allowedFileExtensions.length > 0) {
            data.fileMasks = formValue.allowedFileExtensions;
        } else if (this.$folder()) {
            // Clearing every extension on an existing folder. The backend skips the write when
            // `fileMasks` is absent *or* an empty list (`UtilMethods.isSet` is false for both), so
            // neither can express "clear" and the saved list would come back on the next load. A
            // single blank mask joins to an empty string server-side, which is how a folder with no
            // restrictions is stored and reads back as no extensions. On create there is nothing to
            // clear, so the field stays omitted.
            data.fileMasks = [''];
        }

        if (formValue.defaultFileAssetType && formValue.defaultFileAssetType.trim() !== '') {
            data.defaultAssetType = formValue.defaultFileAssetType;
        }

        // Always send defaultBaseType, including null for "Ask each time". Sending an explicit
        // null (rather than omitting the field) lets the backend clear a previously pinned
        // preference when the user reverts to "Ask each time".
        data.defaultBaseType = formValue.defaultBaseType;

        const assetPath = this.#getAssetPath(this.$originalName() ?? formValue.name);

        return {
            assetPath,
            data
        };
    }
    /**
     * Sanitizes a string to be a valid slug
     * - Converts to lowercase
     * - Replaces spaces with hyphens
     * - Returns an empty string if the title is null or undefined
     */
    #getSlugTitle(title: string): string {
        return title?.trim()?.toLowerCase()?.replace(/ /g, '-') ?? '';
    }

    /**
     * Generates a navigation label from a given name
     * Converts a slug to a human-readable format
     * - Converts hyphens to spaces
     * - Capitalizes the first letter of each word
     * - Returns an empty string if the name is null or undefined
     *
     * @param {string} name - The name of the folder
     * @returns {string} The navigation label
     */
    #getNavigationLabel(name: string): string {
        return (
            name
                ?.trim()
                ?.replace(/-/g, ' ')
                ?.replace(/(^[a-zA-Z])|\s([a-zA-Z])/g, (char) => char.toUpperCase()) ?? ''
        );
    }

    /**
     * Closes the folder dialog by calling the store's closeDialog method
     */
    closeDialog() {
        this.#store.closeDialog();
    }

    /**
     * Path of the folder that will contain the folder being created or edited, without a trailing
     * slash (empty at the site root).
     *
     * **Create** anchors on the folder currently open in the drive (`store.path()`), so the preview
     * reflects where the new folder will land — even before a name is typed.
     *
     * **Edit** must anchor on the edited folder's *own* parent instead. The table only ever opens
     * this dialog for a row of the open folder, so the two coincide there; the sidebar tree can open
     * it for any folder at any depth, and anchoring on the open path would then build a path to a
     * different folder entirely — saving would 404, or silently overwrite a same-named folder under
     * the open one.
     *
     * @returns {string} The parent path, e.g. `/application/blog` or `''` at the site root
     */
    #getParentPath(): string {
        const folder = this.$folder();

        if (!folder) {
            return this.#store.path()?.replace(/\/$/, '') ?? '';
        }

        const withoutTrailingSlash = folder.path.replace(/\/$/, '');
        const lastSeparator = withoutTrailingSlash.lastIndexOf('/');

        return lastSeparator <= 0 ? '' : withoutTrailingSlash.slice(0, lastSeparator);
    }

    /**
     * Generates the asset path for a given name
     * Combines hostname, parent path, and name to create the complete folder path
     * Ensures proper path formatting by removing trailing slashes
     *
     * Reads signals in a pure computed only; it never writes a form control, so it can't
     * re-introduce the form-control-write feedback loop that the old title→name `urlEffect` caused.
     *
     * @param {string} name - The name of the folder
     * @returns {string} The asset path
     */
    #getAssetPath(name: string) {
        const slugName = this.#getSlugTitle(name);
        const parentPath = this.#getParentPath();
        let finalPath = this.#hostName;

        if (parentPath) {
            finalPath += parentPath;
        }

        if (!slugName) {
            return `//${finalPath}/`;
        }

        return `//${finalPath}/${slugName}/`;
    }
}
