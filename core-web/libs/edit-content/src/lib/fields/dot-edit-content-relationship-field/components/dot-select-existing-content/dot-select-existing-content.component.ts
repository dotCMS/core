import { DatePipe } from '@angular/common';
import {
    ChangeDetectionStrategy,
    Component,
    computed,
    inject,
    model,
    OnInit,
    effect
} from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { ChipModule } from 'primeng/chip';
import { DialogModule } from 'primeng/dialog';
import { DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';
import { IconFieldModule } from 'primeng/iconfield';
import { InputGroupModule } from 'primeng/inputgroup';
import { InputIconModule } from 'primeng/inputicon';
import { InputTextModule } from 'primeng/inputtext';
import { MenuModule } from 'primeng/menu';
import { OverlayPanelModule } from 'primeng/overlaypanel';
import { TableModule } from 'primeng/table';

import { DotMessageService } from '@dotcms/data-access';
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
    providers: [ExistingContentStore],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotSelectExistingContentComponent implements OnInit {
    /**
     * A readonly instance of the ExistingContentStore injected into the component.
     * This store is used to manage the state and actions related to the existing content.
     */
    readonly store = inject(ExistingContentStore);

    /**
     * A readonly instance of the DotMessageService injected into the component.
     * This service is used to get localized messages.
     */
    readonly #dotMessage = inject(DotMessageService);

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
    readonly #dialogConfig = inject(DynamicDialogConfig<DialogData>);

    /**
     * A signal that holds the selected items.
     * It is used to store the selected content items.
     */
    $selectedItems = model<DotCMSContentlet[] | DotCMSContentlet | null>(null);

    /**
     * A computed signal that holds the items.
     * It is used to store the items.
     */
    $items = computed(() => {
        const selectedItems = this.$selectedItems();

        if (selectedItems) {
            const isArray = Array.isArray(selectedItems);
            const items = isArray ? selectedItems : [selectedItems];

            return items;
        }

        return [];
    });

    /**
     * A computed signal that determines the label for the apply button.
     * It is used to display the appropriate message based on the number of selected items.
     */
    $applyLabel = computed(() => {
        const count = this.$items().length;

        const messageKey =
            count === 1
                ? 'dot.file.relationship.dialog.apply.one.entry'
                : 'dot.file.relationship.dialog.apply.entries';

        return this.#dotMessage.get(messageKey, count.toString());
    });

    constructor() {
        effect(
            () => {
                this.$selectedItems.set(this.store.selectedItems());
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
     * A method that closes the existing content dialog.
     * It sets the visibility signal to false, hiding the dialog.
     */
    applyChanges() {
        this.#dialogRef.close(this.$items());
    }

    /**
     * A method that closes the existing content dialog.
     * It sets the visibility signal to false, hiding the dialog.
     */
    closeDialog() {
        this.#dialogRef.close();
    }

    /**
     * Checks if an item is selected.
     * @param item - The item to check.
     * @returns True if the item is selected, false otherwise.
     */
    checkIfSelected(item: DotCMSContentlet) {
        const items = this.$items();

        return items.some((selectedItem) => selectedItem.inode === item.inode);
    }
}
