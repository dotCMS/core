import { NgClass } from '@angular/common';
import {
    ChangeDetectionStrategy,
    Component,
    inject,
    model,
    OnDestroy,
    OnInit,
    output
} from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { InputTextModule } from 'primeng/inputtext';

import { DotMessagePipe } from '@dotcms/ui';

import { CategoryFieldStore } from '../../store/content-category-field.store';
import { DotCategoryFieldCategoryListComponent } from '../dot-category-field-category-list/dot-category-field-category-list.component';
import { DotCategoryFieldSearchComponent } from '../dot-category-field-search/dot-category-field-search.component';
import { DotCategoryFieldSearchListComponent } from '../dot-category-field-search-list/dot-category-field-search-list.component';
import { DotCategoryFieldSelectedComponent } from '../dot-category-field-selected/dot-category-field-selected.component';

/**
 * The DotCategoryFieldDialogComponent is a dialog panel that allows editing of content category field.
 * It provides interfaces for item selection and click handling, and communicates with a store
 * to fetch and update the categories' data.
 *
 * @property {boolean} visible - Indicates the visibility of the dialog. Default is `true`.
 * @property {output<void>} closedDialog - Output emitted when the dialog is closed.
 */
@Component({
    selector: 'dot-category-field-dialog',
    imports: [
        DialogModule,
        ButtonModule,
        DotMessagePipe,
        DotCategoryFieldCategoryListComponent,
        InputTextModule,
        DotCategoryFieldSearchComponent,
        DotCategoryFieldSearchListComponent,
        DotCategoryFieldSelectedComponent,
        NgClass
    ],
    templateUrl: './dot-category-field-dialog.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotCategoryFieldDialogComponent implements OnInit, OnDestroy {
    /**
     * Indicates the visibility of the dialog.
     *
     * @memberof DotCategoryFieldDialogComponent
     */
    $isVisible = model<boolean>(false, { alias: 'isVisible' });

    /**
     * Output that emit if the Dialog is closed
     */
    closedDialog = output<void>();

    /**
     * Store based on the `CategoryFieldStore`.
     *
     * @memberof DotCategoryFieldDialogComponent
     */
    readonly store = inject(CategoryFieldStore);

    ngOnInit(): void {
        this.store.getCategories();
    }

    ngOnDestroy(): void {
        this.store.clean();
    }

    confirmCategories(): void {
        this.store.applyDialogSelection();
        this.closedDialog.emit();
    }
}
