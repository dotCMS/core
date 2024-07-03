import {
    ChangeDetectionStrategy,
    Component,
    DestroyRef,
    EventEmitter,
    inject,
    OnInit,
    Output,
    ViewChild
} from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { SidebarModule } from 'primeng/sidebar';

import { DotCategory } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';

import { DotCategoryFieldItem } from '../../models/dot-category-field.models';
import { CategoryFieldStore } from '../../store/content-category-field.store';
import { DotCategoryFieldCategoryListComponent } from '../dot-category-field-category-list/dot-category-field-category-list.component';

/**
 * Component for the sidebar that appears when editing content category field.
 */
@Component({
    selector: 'dot-category-field-sidebar',
    standalone: true,
    imports: [
        DialogModule,
        ButtonModule,
        DotMessagePipe,
        SidebarModule,
        DotCategoryFieldCategoryListComponent
    ],
    templateUrl: './dot-category-field-sidebar.component.html',
    styleUrl: './dot-category-field-sidebar.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotCategoryFieldSidebarComponent implements OnInit {
    @ViewChild(DotCategoryFieldCategoryListComponent)
    listComponent: DotCategoryFieldCategoryListComponent;

    /**
     * Indicates whether the sidebar is visible or not.
     */
    visible = true;

    /**
     * Output that emit if the sidebar is closed
     */
    @Output() closedSidebar = new EventEmitter<void>();

    readonly store = inject(CategoryFieldStore);

    #destroyRef = inject(DestroyRef);

    ngOnInit(): void {
        this.store.getCategories();

        this.#destroyRef.onDestroy(() => {
            this.store.clean();
        });
    }

    /**
     * Handles the click event on an item.
     *
     * @param {number} index - The index of the item being clicked.
     * @param {DotCategory} item - The item being clicked.
     * @returns {void}
     */
    itemClicked({ index, item }: DotCategoryFieldItem): void {
        this.store.getCategories({ index, item });
    }

    /**
     * Handle the selection of an item
     * @param $event
     * @param item
     */
    itemChecked({ selected, item }: { selected: string[]; item: DotCategory }) {
        this.store.updateSelected(selected, item);
    }
}
