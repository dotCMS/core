import { DatePipe } from '@angular/common';
import {
    ChangeDetectionStrategy,
    Component,
    computed,
    CUSTOM_ELEMENTS_SCHEMA,
    effect,
    inject,
    model,
    OnInit,
    signal
} from '@angular/core';
import { FormsModule } from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { CheckboxModule } from 'primeng/checkbox';
import { ChipModule } from 'primeng/chip';
import { DialogModule } from 'primeng/dialog';
import { DynamicDialogConfig } from 'primeng/dynamicdialog';
import { IconFieldModule } from 'primeng/iconfield';
import { InputGroupModule } from 'primeng/inputgroup';
import { InputIconModule } from 'primeng/inputicon';
import { InputTextModule } from 'primeng/inputtext';
import { MenuModule } from 'primeng/menu';
import { PopoverModule } from 'primeng/popover';
import { TableModule } from 'primeng/table';
import { ToggleSwitchModule } from 'primeng/toggleswitch';
import { TooltipModule } from 'primeng/tooltip';

import { DotCMSContentlet } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';

import { SearchComponent } from './components/search/search.component';
import { ExistingContentStore } from './store/existing-content.store';

import { ContentletStatusPipe } from '../../../../pipes/contentlet-status.pipe';
import { LanguagePipe } from '../../../../pipes/language.pipe';
import { InitLoadParams } from '../../models/relationship.models';

type DialogData = Omit<InitLoadParams, 'selectedItemsIds'> & {
    currentItemsIds: string[];
};

const STATIC_COLUMNS = 6;

@Component({
    selector: 'dot-select-existing-content',
    imports: [
        TableModule,
        ButtonModule,
        CheckboxModule,
        MenuModule,
        DotMessagePipe,
        DialogModule,
        IconFieldModule,
        InputIconModule,
        InputTextModule,
        InputGroupModule,
        PopoverModule,
        ContentletStatusPipe,
        LanguagePipe,
        DatePipe,
        ChipModule,
        FormsModule,
        TooltipModule,
        SearchComponent,
        ToggleSwitchModule
    ],
    templateUrl: './dot-select-existing-content.component.html',
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

    /**
     * Items that are selectable — excludes rows marked as constrained
     * (already related under a cardinality-restricted relationship).
     */
    $selectableItems = computed(() => {
        const isConstrained = this.store.isItemConstrained();

        return this.store.filteredData().filter((item) => !isConstrained(item.identifier));
    });

    /**
     * State of the header "Select All" checkbox. True when there is at least one
     * selectable item and every selectable item is currently selected.
     *
     * Forced to false in "selected view" mode to avoid a trivially-checked state
     * (filteredData is already filtered to the current selection), which would
     * otherwise let a single header click wipe the whole selection.
     */
    $selectAll = computed(() => {
        if (this.store.isSelectedView()) {
            return false;
        }

        const selectable = this.$selectableItems();

        if (selectable.length === 0) {
            return false;
        }

        const selection = this.store.currentItems();
        const selectedInodes = new Set(selection.map((item) => item.inode));

        return selectable.every((item) => selectedInodes.has(item.inode));
    });

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
            showFields: data.showFields,
            cardinality: data.cardinality,
            parentContentTypeId: data.parentContentTypeId,
            fieldVariable: data.fieldVariable,
            isParentField: data.isParentField,
            currentContentIdentifier: data.currentContentIdentifier,
            contentletContext: data.contentletContext
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

    /**
     * Handles the header "Select All" toggle, excluding constrained (already related) rows
     * so they are never added to the selection. A custom p-checkbox is used instead of
     * p-tableHeaderCheckbox because the latter ignores per-row [disabled] state.
     */
    onSelectAllChange(event: { checked: boolean }) {
        this.$selectionItems.set(event.checked ? [...this.$selectableItems()] : []);
    }
}
