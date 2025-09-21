import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, effect, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import {
    FormBuilder,
    FormControl,
    FormGroup,
    ReactiveFormsModule,
    Validators
} from '@angular/forms';

import { MessageService } from 'primeng/api';
import { AutoCompleteCompleteEvent, AutoCompleteModule } from 'primeng/autocomplete';
import { ButtonModule } from 'primeng/button';
import { DropdownModule } from 'primeng/dropdown';
import { InputNumberModule } from 'primeng/inputnumber';
import { InputSwitchModule } from 'primeng/inputswitch';
import { InputTextModule } from 'primeng/inputtext';
import { TabViewModule } from 'primeng/tabview';

import { DotFolderCreateBody, DotFolderService, DotMessageService } from '@dotcms/data-access';
import { DotMessagePipe } from '@dotcms/ui';

import { ALLOWED_FILE_EXTENSIONS, DEFAULT_FILE_ASSET_TYPES } from '../../../shared/constants';
import { DotContentDriveStore } from '../../../store/dot-content-drive.store';

interface FolderForm {
    title: FormControl<string>;
    path: FormControl<string>;
    sortOrder: FormControl<number | null>;
    allowedFileExtensions: FormControl<string[]>;
    defaultFileAssetType: FormControl<string>;
    showOnMenu: FormControl<boolean>;
    url: FormControl<string>;
}

@Component({
    selector: 'dot-content-drive-dialog-folder',
    imports: [
        TabViewModule,
        ReactiveFormsModule,
        InputTextModule,
        DotMessagePipe,
        DropdownModule,
        InputSwitchModule,
        ButtonModule,
        InputNumberModule,
        AutoCompleteModule
    ],
    templateUrl: './dot-content-drive-dialog-folder.component.html',
    styleUrls: ['./dot-content-drive-dialog-folder.component.scss']
})
export class DotContentDriveDialogFolderComponent {
    #fb = inject(FormBuilder);
    #dotFolderService = inject(DotFolderService);
    #store = inject(DotContentDriveStore);
    #messageService = inject(MessageService);
    #dotMessageService = inject(DotMessageService);

    hostName = this.#store.currentSite().hostname;

    readonly DEFAULT_FILE_ASSET_TYPES = DEFAULT_FILE_ASSET_TYPES;

    folderForm: FormGroup<FolderForm> = this.#fb.group({
        title: this.#fb.control('', { validators: [Validators.required], nonNullable: true }),
        path: this.#fb.control('', { nonNullable: true }),
        sortOrder: this.#fb.control<number | null>(1),
        allowedFileExtensions: this.#fb.control([], { nonNullable: true }),
        defaultFileAssetType: this.#fb.control(DEFAULT_FILE_ASSET_TYPES[1].id, {
            nonNullable: true
        }),
        showOnMenu: this.#fb.control(true, { nonNullable: true }),
        url: this.#fb.control('', { validators: [Validators.required], nonNullable: true })
    });

    $currentSite = this.#store.currentSite;
    $title = toSignal(this.folderForm.get('title')?.valueChanges);
    $url = toSignal(this.folderForm.get('url')?.valueChanges);
    $filteredAllowedFileExtensions = signal<string[]>(ALLOWED_FILE_EXTENSIONS);

    $finalPath = computed(() => {
        const path = this.#store.path();
        const url = this.$url();

        let finalPath = this.hostName;

        if (path) {
            finalPath += `${path.replace(/\/$/, '')}`;
        }

        return `${finalPath}/${url}`;
    });

    /**
     * Handles the enter key press event for adding file extensions
     * Adds the input value to the allowedFileExtensions form control if it's not a duplicate
     *
     * @param {Event} event - The keyboard event from the input element
     */
    onEnterKey(event: Event) {
        const input = event.target as HTMLInputElement;
        const value = input.value.trim();

        if (value) {
            const currentExtensions = this.folderForm.get('allowedFileExtensions')?.value;
            const isDuplicate = currentExtensions?.includes(value);

            if (!isDuplicate) {
                const newValue = [...currentExtensions, value];
                this.folderForm.get('allowedFileExtensions')?.setValue(newValue);
            }
        }
    }

    /**
     * Handles the autocomplete filtering for allowed file extensions
     * Filters the ALLOWED_FILE_EXTENSIONS array based on the query string
     * and updates the filteredAllowedFileExtensions signal with matching extensions
     *
     * @param {AutoCompleteCompleteEvent} param0 - The autocomplete event containing the query string
     */
    onCompleteMethod({ query }: AutoCompleteCompleteEvent) {
        const extensions = ALLOWED_FILE_EXTENSIONS.filter((extension) => extension.includes(query));

        this.$filteredAllowedFileExtensions.set(extensions);
    }

    /**
     * Effect that automatically sets the URL field value based on the title
     * Only runs when the URL field has not been manually touched by the user
     * Converts the title to a URL-friendly slug format and sets it as the URL value
     */
    readonly urlEffect = effect(() => {
        if (this.folderForm.get('url')?.touched) {
            return;
        }

        const title = this.$title();
        const titleSlug = this.#getSlugTitle(title || '');

        this.folderForm.get('url')?.setValue(titleSlug);
    });

    async createFolder() {
        const body: DotFolderCreateBody = this.#createFolderBody();

        try {
            await this.#dotFolderService.createFolder(body).toPromise();
            this.#store.loadFolders();
            this.#store.closeDialog();

            this.#messageService.add({
                severity: 'success',
                summary: 'Success',
                detail: this.#dotMessageService.get(
                    'content-drive.dialog.folder.message.create-success'
                )
            });
        } catch (err: unknown) {
            const { error } = err as HttpErrorResponse;

            console.error('Error creating folder:', err);

            this.#messageService.add({
                severity: 'error',
                summary: this.#dotMessageService.get(
                    'content-drive.dialog.folder.message.create-error'
                ),
                detail: error.message
            });
        }
    }

    #createFolderBody() {
        const formValue = this.folderForm.getRawValue();

        const data: DotFolderCreateBody['data'] = {
            title: formValue.title // Always include title
        };

        // Only add properties if they have values
        if (formValue.showOnMenu !== undefined && formValue.showOnMenu !== null) {
            data.showOnMenu = formValue.showOnMenu;
        }

        if (formValue.sortOrder !== null && formValue.sortOrder !== undefined) {
            data.sortOrder = formValue.sortOrder;
        }

        if (formValue.allowedFileExtensions.length > 0) {
            data.fileMasks = formValue.allowedFileExtensions;
        }

        if (formValue.defaultFileAssetType && formValue.defaultFileAssetType.trim() !== '') {
            data.defaultAssetType = formValue.defaultFileAssetType;
        }

        const assetPath = this.$finalPath();

        return {
            assetPath,
            data
        };
    }

    #getSlugTitle(title: string): string {
        return title.toLowerCase().replace(/ /g, '-');
    }

    /**
     * Closes the folder dialog by calling the store's closeDialog method
     */
    closeDialog() {
        this.#store.closeDialog();
    }
}
