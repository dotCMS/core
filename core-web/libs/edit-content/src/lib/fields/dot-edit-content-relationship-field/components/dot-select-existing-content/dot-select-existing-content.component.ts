import { DatePipe } from '@angular/common';
import {
    ChangeDetectionStrategy,
    Component,
    CUSTOM_ELEMENTS_SCHEMA,
    effect,
    inject,
    model,
    OnInit,
    signal
} from '@angular/core';
import { FormsModule } from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { ChipModule } from 'primeng/chip';
import { DialogModule } from 'primeng/dialog';
import { DynamicDialogConfig } from 'primeng/dynamicdialog';
import { IconFieldModule } from 'primeng/iconfield';
import { InputGroupModule } from 'primeng/inputgroup';
import { InputIconModule } from 'primeng/inputicon';
import { InputTextModule } from 'primeng/inputtext';
import { MenuModule } from 'primeng/menu';
import { OverlayPanelModule } from 'primeng/overlaypanel';
import { TableModule } from 'primeng/table';

import { DotCMSContentlet } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';

import { ExistingContentStore } from './store/existing-content.store';

import { ContentletStatusPipe } from '../../../../pipes/contentlet-status.pipe';
import { LanguagePipe } from '../../../../pipes/language.pipe';
import { SelectionMode } from '../../models/relationship.models';

type DialogData = {
    contentTypeId: string;
    selectionMode: SelectionMode;
    currentItemsIds: string[];
    showFields?: string[] | null;
};

const STATIC_COLUMNS = 6;

@Component({
    selector: 'dot-select-existing-content',
    imports: [
        TableModule,
        ButtonModule,
        MenuModule,
        DotMessagePipe,
        DialogModule,
        IconFieldModule,
        InputIconModule,
        InputTextModule,
        InputGroupModule,
        OverlayPanelModule,
        ContentletStatusPipe,
        LanguagePipe,
        DatePipe,
        ChipModule,
        FormsModule
    ],
    templateUrl: './dot-select-existing-content.component.html',
    styleUrls: ['./dot-select-existing-content.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    schemas: [CUSTOM_ELEMENTS_SCHEMA]
})
export class DotSelectExistingContentComponent implements OnInit {
    /**
     * A readonly instance of the ExistingContentStore injected into the component.
     * This store is used to manage the state and actions related to the existing content.
     */
    readonly store = inject(ExistingContentStore);

    /**
     * A readonly property that injects the `DynamicDialogConfig` service.
     * This service is used to get the dialog data.
     */
    readonly #dialogConfig = inject(DynamicDialogConfig<DialogData>);

    /**
     * A signal that holds the selected items.
     * It is used to store the selected content items.
     */
    $selectionItems = model<DotCMSContentlet[] | DotCMSContentlet | null>(null);

    /**
     * A signal that holds the static columns.
     * It is used to store the static columns.
     */
    $staticColumns = signal(STATIC_COLUMNS);

    constructor() {
        effect(() => {
            // Sync the selection items with the store
            const selectionItems = this.$selectionItems();
            if (selectionItems) {
                this.store.setSelectionItems(selectionItems);
            }
        });

        effect(() => {
            // Sync the selection items with the store
            const selectionItems = this.store.selectionItems();
            this.$selectionItems.set(selectionItems);
        });
    }

    ngOnInit() {
        const data: DialogData = this.#dialogConfig.data;

        if (!data.contentTypeId) {
            throw new Error('Content type id is required');
        }

        if (!data.selectionMode) {
            throw new Error('Selection mode is required');
        }

        this.store.initLoad({
            contentTypeId: data.contentTypeId,
            selectionMode: data.selectionMode,
            selectedItemsIds: data.currentItemsIds,
            showFields: data.showFields
        });
    }

    /**
     * Checks if an item is selected.
     * @param item - The item to check.
     * @returns True if the item is selected, false otherwise.
     */
    checkIfSelected(item: DotCMSContentlet) {
        const items = this.store.currentItems();

        return items.some((selectedItem) => selectedItem.inode === item.inode);
    }
}
