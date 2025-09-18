import { JsonPipe } from '@angular/common';
import { Component, computed, inject } from '@angular/core';
import { FormBuilder, FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';


import { ButtonModule } from 'primeng/button';
import { DropdownModule } from 'primeng/dropdown';
import { InputSwitchModule } from 'primeng/inputswitch';
import { InputTextModule } from 'primeng/inputtext';
import { TabViewModule } from 'primeng/tabview';

import { DotFolderCreateBody, DotFolderService } from '@dotcms/data-access';

import { DotContentDriveStore } from '../../../store/dot-content-drive.store';

interface FolderForm {
    title: FormControl<string>;
    url: FormControl<string>;
    path: FormControl<string>;
    sortOrder: FormControl<number | null>;
    allowedFileExtensions: FormControl<string>;
    defaultFileAssetType: FormControl<string>;
    showOnMenu: FormControl<boolean>;
}

@Component({
    selector: 'dot-content-drive-dialog-folder',
    imports: [TabViewModule, ReactiveFormsModule, InputTextModule, JsonPipe, DropdownModule, InputSwitchModule, ButtonModule],
    templateUrl: './dot-content-drive-dialog-folder.component.html',
    styleUrls: ['./dot-content-drive-dialog-folder.component.scss'],
})
export class DotContentDriveDialogFolderComponent {


    #fb = inject(FormBuilder);
    #dotFolderService = inject(DotFolderService);
    #store = inject(DotContentDriveStore);

    $currentPath = this.#store.path;
    $hostName = computed(() => this.#store.currentSite().hostname);
    $currentSite = this.#store.currentSite

    folderForm: FormGroup<FolderForm> = this.#fb.group({
        title: new FormControl('', { validators: [Validators.required], nonNullable: true }),
        url: new FormControl('', { validators: [Validators.required], nonNullable: true }),
        path: new FormControl('', { nonNullable: true }),
        sortOrder: new FormControl<number | null>(null),
        allowedFileExtensions: new FormControl('', { nonNullable: true }),
        defaultFileAssetType: new FormControl('', { nonNullable: true }),
        showOnMenu: new FormControl(false, { nonNullable: true })
    });

    createFolder() {
        const body: DotFolderCreateBody = this.#createFolderBody();

        this.#dotFolderService.createFolder(body).subscribe((folder) => {
            console.log(folder);
        });
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

        const fileMasks = this.#getAllowedFileExtensions(formValue.allowedFileExtensions);
        if (fileMasks.length > 0) {
            data.fileMasks = fileMasks;
        }

        if (formValue.defaultFileAssetType && formValue.defaultFileAssetType.trim() !== '') {
            data.defaultAssetType = formValue.defaultFileAssetType;
        }

        return {
            assetPath: `//${this.$hostName()}/${formValue.url}/`,
            data
        };
    }

    #getAllowedFileExtensions(extensions: string): string[] {
        if (!extensions || extensions.trim() === '') {
            return [];
        }
        return extensions.split(',').map((extension) => extension.trim()).filter(ext => ext.length > 0);
    }
}

