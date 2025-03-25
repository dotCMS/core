import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject, model, OnInit, effect } from '@angular/core';

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
import { ContentletStatusPipe } from '@dotcms/edit-content/pipes/contentlet-status.pipe';
import { LanguagePipe } from '@dotcms/edit-content/pipes/language.pipe';
import { DotMessagePipe } from '@dotcms/ui';

import { SearchComponent } from './components/search/search.compoment';
import { ExistingContentStore } from './store/existing-content.store';

import { SelectionMode } from '../../models/relationship.models';
import { PaginationComponent } from '../pagination/pagination.component';

type DialogData = {
    contentTypeId: string;
    selectionMode: SelectionMode;
    currentItemsIds: string[];
};

@Component({
    selector: 'dot-select-existing-content',
    standalone: true,
    imports: [
        TableModule,
        ButtonModule,
        MenuModule,
        DotMessagePipe,
        DialogModule,
        IconFieldModule,
        InputIconModule,
        InputTextModule,
        PaginationComponent,
        InputGroupModule,
        OverlayPanelModule,
        SearchComponent,
        ContentletStatusPipe,
        LanguagePipe,
        DatePipe,
        ChipModule
    ],
    templateUrl: './dot-select-existing-content.component.html',
    styleUrls: ['./dot-select-existing-content.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
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
    $selectedItems = model<DotCMSContentlet[] | DotCMSContentlet | null>(null);

    constructor() {
        effect(
            () => {
                this.$selectedItems.set(this.store.initSelectedItems());
            },
            {
                allowSignalWrites: true
            }
        );
        effect(
            () => {
                this.store.setSelectedItems(this.$selectedItems());
            },
            {
                allowSignalWrites: true
            }
        );
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
            currentItemsIds: data.currentItemsIds
        });
    }

    /**
     * Checks if an item is selected.
     * @param item - The item to check.
     * @returns True if the item is selected, false otherwise.
     */
    checkIfSelected(item: DotCMSContentlet) {
        const items = this.store.items();

        return items.some((selectedItem) => selectedItem.inode === item.inode);
    }
}
