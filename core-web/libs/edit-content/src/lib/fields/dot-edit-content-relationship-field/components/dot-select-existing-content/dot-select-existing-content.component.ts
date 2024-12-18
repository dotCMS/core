import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject, model, output } from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { IconFieldModule } from 'primeng/iconfield';
import { InputGroupModule } from 'primeng/inputgroup';
import { InputIconModule } from 'primeng/inputicon';
import { InputTextModule } from 'primeng/inputtext';
import { MenuModule } from 'primeng/menu';
import { OverlayPanelModule } from 'primeng/overlaypanel';
import { TableModule } from 'primeng/table';

import { DotMessageService } from '@dotcms/data-access';
import { DotMessagePipe } from '@dotcms/ui';

import { SearchComponent } from './components/search/search.compoment';
import { ExistingContentStore } from './store/existing-content.store';

import { RelationshipFieldItem } from '../../models/relationship.models';
import { PaginationComponent } from '../pagination/pagination.component';

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
        DatePipe,
        PaginationComponent,
        InputGroupModule,
        OverlayPanelModule,
        SearchComponent
    ],
    templateUrl: './dot-select-existing-content.component.html',
    styleUrls: ['./dot-select-existing-content.component.scss'],
    providers: [ExistingContentStore],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotSelectExistingContentComponent {
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
     * A signal that controls the visibility of the existing content dialog.
     * When true, the dialog is shown allowing users to select existing content.
     * When false, the dialog is hidden.
     */
    $visible = model(false, { alias: 'visible' });

    /**
     * A signal that holds the selected items.
     * It is used to store the selected content items.
     */
    $selectedItems = model<RelationshipFieldItem[]>([]);
    $selectedItem = model<RelationshipFieldItem | null>(null);

    /**
     * A computed signal that determines if the apply button is disabled.
     * It is disabled when no items are selected.
     */
    $isApplyDisabled = computed(() => this.$selectedItems().length === 0);

    /**
     * A computed signal that determines the label for the apply button.
     * It is used to display the appropriate message based on the number of selected items.
     */
    $applyLabel = computed(() => {
        const selectedItems = this.$selectedItems();

        const messageKey =
            selectedItems.length === 1
                ? 'dot.file.relationship.dialog.apply.one.entry'
                : 'dot.file.relationship.dialog.apply.entries';

        return this.#dotMessage.get(messageKey, selectedItems.length.toString());
    });

    /**
     * A signal that sends the selected items when the dialog is closed.
     * It is used to notify the parent component that the user has selected content items.
     */
    onSelectItems = output<RelationshipFieldItem[]>();

    /**
     * A method that closes the existing content dialog.
     * It sets the visibility signal to false, hiding the dialog.
     */
    closeDialog() {
        this.$visible.set(false);
    }

    /**
     * Closes the existing content dialog and sends the selected items to the parent component.
     * It sets the visibility signal to false, hiding the dialog, and emits the selected items
     * through the "selectItems" output signal.
     */
    emitSelectedItems() {
        this.onSelectItems.emit(this.$selectedItems());
    }

    /**
     * Checks if an item is selected.
     * @param item - The item to check.
     * @returns True if the item is selected, false otherwise.
     */
    checkIfSelected(item: RelationshipFieldItem) {
        return this.$selectedItems().some((selectedItem) => selectedItem.id === item.id);
    }

    /**
     * Shows the existing content dialog and loads the content.
     */
    onShowDialog() {
        this.store.applyInitialState();
        this.store.loadContent();
    }
}
