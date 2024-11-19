import {
    ChangeDetectionStrategy,
    Component,
    effect,
    inject,
    OnInit,
    viewChild
} from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { DynamicDialogRef, DynamicDialogConfig } from 'primeng/dynamicdialog';

import { DotFileFieldUploadService } from '@dotcms/edit-content/fields/dot-edit-content-file-field/services/upload-file/upload-file.service';
import { INPUT_TYPES } from '@dotcms/edit-content/models/dot-edit-content-file.model';
import { DotMessagePipe } from '@dotcms/ui';

import { DotDataViewComponent } from './components/dot-dataview/dot-dataview.component';
import { DotSideBarComponent } from './components/dot-sidebar/dot-sidebar.component';
import { SelectExisingFileStore } from './store/select-existing-file.store';

type DialogData = {
    inputType: INPUT_TYPES;
};

@Component({
    selector: 'dot-select-existing-file',
    standalone: true,
    imports: [DotSideBarComponent, DotDataViewComponent, ButtonModule, DotMessagePipe],
    templateUrl: './dot-select-existing-file.component.html',
    styleUrls: ['./dot-select-existing-file.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [SelectExisingFileStore]
})
export class DotSelectExistingFileComponent implements OnInit {
    /**
     * Injects the SelectExistingFileStore into the component.
     *
     * @readonly
     * @type {SelectExistingFileStore}
     */
    /**
     * A readonly property that injects the `SelectExisingFileStore` service.
     * This store is used to manage the state and actions related to selecting existing files.
     */
    readonly store = inject(SelectExisingFileStore);

    readonly #uploadService = inject(DotFileFieldUploadService);
    /**
     * A reference to the dynamic dialog instance.
     * This is a read-only property that is injected using Angular's dependency injection.
     * It provides access to the dialog's methods and properties.
     */
    readonly #dialogRef = inject(DynamicDialogRef);

    /**
     * Reference to the DotSideBarComponent instance.
     * This is used to interact with the sidebar component within the template.
     *
     * @type {DotSideBarComponent}
     */
    $sideBarRef = viewChild.required(DotSideBarComponent);

    readonly #dialogConfig = inject(DynamicDialogConfig<DialogData>);

    constructor() {
        effect(() => {
            const folders = this.store.folders();

            if (folders.nodeExpaned) {
                this.$sideBarRef().detectChanges();
            }
        });
    }

    ngOnInit() {
        const data = this.#dialogConfig?.data as DialogData;
        const inputType = data?.inputType === 'Image' ? ['image'] : [];
        this.store.setMimeTypes(inputType);
    }

    /**
     * Cancels the current file upload and closes the dialog.
     *
     * @remarks
     * This method is used to terminate the ongoing file upload process and
     * close the associated dialog reference.
     */
    closeDialog(): void {
        this.#dialogRef.close();
    }

    /**
     * Retrieves the selected content from the store, fetches it by ID using the upload service,
     * and closes the dialog with the retrieved content.
     */
    addContent(): void {
        const content = this.store.selectedContent();
        this.#uploadService.getContentById(content.identifier).subscribe((content) => {
            this.#dialogRef.close(content);
        });
    }
}
