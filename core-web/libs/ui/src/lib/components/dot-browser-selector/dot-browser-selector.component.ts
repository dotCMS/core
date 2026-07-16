import { signalMethod } from '@ngrx/signals';

import {
    ChangeDetectionStrategy,
    Component,
    computed,
    inject,
    OnInit,
    signal
} from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';

import { DotContentletService } from '@dotcms/data-access';
import { ContentByFolderParams, TreeNodeSelectItem } from '@dotcms/dotcms-models';

import { DotDataViewComponent } from './components/dot-dataview/dot-dataview.component';
import { DotSideBarComponent } from './components/dot-sidebar/dot-sidebar.component';
import { DotBrowserSelectorStore, SYSTEM_HOST_ID } from './store/browser.store';

import { DotMessagePipe } from '../../dot-message/dot-message.pipe';
@Component({
    selector: 'dot-select-existing-file',
    imports: [DotSideBarComponent, DotDataViewComponent, ButtonModule, DotMessagePipe],
    templateUrl: './dot-browser-selector.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [DotBrowserSelectorStore]
})
export class DotBrowserSelectorComponent implements OnInit {
    /**
     * A readonly property that injects the `DotBrowserSelectorStore` service.
     * This store is used to manage the state and actions related to selecting existing files.
     */
    readonly store = inject(DotBrowserSelectorStore);

    /**
     * A readonly property that injects the `dotContentletService` service.
     * This service is used to manage the state and actions related to selecting existing files.
     */
    readonly #dotContentletService = inject(DotContentletService);
    /**
     * A reference to the dynamic dialog instance.
     * This is a read-only property that is injected using Angular's dependency injection.
     * It provides access to the dialog's methods and properties.
     */
    readonly #dialogRef = inject(DynamicDialogRef);

    /**
     * A readonly property that injects the `DynamicDialogConfig` service.
     * This service is used to get the dialog data.
     */
    readonly #dialogConfig = inject(DynamicDialogConfig<ContentByFolderParams>);

    /**
     * Signal representing the folder parameters.
     * This is used to store the folder parameters.
     */
    $folderParams = signal<ContentByFolderParams>({
        hostFolderId: SYSTEM_HOST_ID,
        mimeTypes: []
    });

    /**
     * True when hostFolderId is empty (no site/folder selected yet).
     * Used to disable the upload button until the user picks a destination.
     * Note: System Host is treated as a valid selection and enables the button.
     */
    $uploadDisabled = computed(() => this.$folderParams().hostFolderId === '');

    /**
     * Derives the file input accept attribute from the mimeTypes in folderParams.
     * e.g. ['image'] → 'image/*', [] → '*'
     */
    $acceptAttr = computed(() => {
        const { mimeTypes = [] } = this.$folderParams();
        if (mimeTypes.length === 0) return '*';

        return mimeTypes.map((m) => (m.includes('/') ? m : `${m}/*`)).join(',');
    });

    constructor() {
        this.loadContent(this.$folderParams);
    }

    ngOnInit() {
        const params = this.#dialogConfig?.data as ContentByFolderParams;
        this.$folderParams.update((prev) => ({ ...prev, ...params }));
    }

    onNodeSelect(event: TreeNodeSelectItem): void {
        const { id } = event?.node?.data ?? {};
        if (!id) {
            return;
        }

        this.$folderParams.update((prev) => ({ ...prev, hostFolderId: id }));
        this.store.setSelectedContent(null);
    }

    /**
     * Cancels the current file upload and closes the dialog.
     */
    closeDialog(): void {
        this.#dialogRef.close();
    }

    /**
     * Handles the file selected via the OS file picker in the dataview,
     * uploading it as a dotAsset to the current folder and refreshing the list.
     */
    onFileUpload(file: File): void {
        this.store.uploadFile({ file, folderParams: this.$folderParams() });
    }

    /**
     * Retrieves the selected content from the store, fetches it by ID using the upload service,
     * and closes the dialog with the retrieved content.
     */
    addContent(): void {
        const content = this.store.selectedContent();
        this.#dotContentletService
            .getContentletByInodeWithContent(content.inode)
            .subscribe((content) => {
                this.#dialogRef.close(content);
            });
    }

    /**
     * Loads the content for the given folder parameters.
     */
    readonly loadContent = signalMethod<ContentByFolderParams>((params) => {
        this.store.loadContent(params);
    });

    onNodeExpand(event: TreeNodeSelectItem): void {
        this.store.loadChildren(event);
    }
}
