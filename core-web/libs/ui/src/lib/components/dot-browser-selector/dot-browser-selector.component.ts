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

import { DotContentletService } from '@dotcms/data-access';

import { DotDataViewComponent } from './components/dot-dataview/dot-dataview.component';
import { DotSideBarComponent } from './components/dot-sidebar/dot-sidebar.component';
import { DotBrowserSelectorStore } from './store/browser.store';

import { DotMessagePipe } from '../../dot-message/dot-message.pipe';

type DialogData = {
    mimeTypes: string[];
};

@Component({
    selector: 'dot-select-existing-file',
    imports: [DotSideBarComponent, DotDataViewComponent, ButtonModule, DotMessagePipe],
    templateUrl: './dot-browser-selector.component.html',
    styleUrls: ['./dot-browser-selector.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [DotBrowserSelectorStore]
})
export class DotBrowserSelectorComponent implements OnInit {
    /**
     * Injects the SelectExistingFileStore into the component.
     *
     * @readonly
     * @type {SelectExistingFileStore}
     */
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
     * Reference to the DotSideBarComponent instance.
     * This is used to interact with the sidebar component within the template.
     *
     * @type {DotSideBarComponent}
     */
    $sideBarRef = viewChild.required(DotSideBarComponent);

    /**
     * A readonly property that injects the `DynamicDialogConfig` service.
     * This service is used to get the dialog data.
     */
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
        const mimeTypes = data?.mimeTypes ?? [];
        this.store.setMimeTypes(mimeTypes);
        this.store.loadContent();
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
        this.#dotContentletService
            .getContentletByInodeWithContent(content.inode)
            .subscribe((content) => {
                this.#dialogRef.close(content);
            });
    }
}
