import { ChangeDetectionStrategy, Component, inject, OnInit } from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { DynamicDialogRef } from 'primeng/dynamicdialog';

import { DotMessagePipe } from '@dotcms/ui';

import { DotDataViewComponent } from './components/dot-dataview/dot-dataview.component';
import { DotSideBarComponent } from './components/dot-sidebar/dot-sidebar.component';
import { SelectExisingFileStore } from './store/select-existing-file.store';

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
    /**
     * A reference to the dynamic dialog instance.
     * This is a read-only property that is injected using Angular's dependency injection.
     * It provides access to the dialog's methods and properties.
     */
    readonly #dialogRef = inject(DynamicDialogRef);

    ngOnInit() {
        this.store.loadContent();
        this.store.loadFolders();
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
}
