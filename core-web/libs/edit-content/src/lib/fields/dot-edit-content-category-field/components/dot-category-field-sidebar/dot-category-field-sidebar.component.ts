import {
    ChangeDetectionStrategy,
    Component,
    DestroyRef,
    EventEmitter,
    inject,
    OnInit,
    Output
} from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { SidebarModule } from 'primeng/sidebar';

import { DotMessagePipe } from '@dotcms/ui';

import { CategoryFieldStore } from '../../store/content-category-field.store';
import { DotCategoryFieldCategoryListComponent } from '../dot-category-field-category-list/dot-category-field-category-list.component';

/**
 * The DotCategoryFieldSidebarComponent is a sidebar panel that allows editing of content category field.
 * It provides interfaces for item selection and click handling, and communicates with a store
 * to fetch and update the categories' data.
 *
 * @property {boolean} visible - Indicates the visibility of the sidebar. Default is `true`.
 * @property {EventEmitter<void>} closedSidebar - Event emitted when the sidebar is closed.
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
    /**
     * Indicates whether the sidebar is visible or not.
     */
    visible = true;

    /**
     * Output that emit if the sidebar is closed
     */
    @Output() closedSidebar = new EventEmitter<void>();

    readonly store = inject(CategoryFieldStore);

    readonly #destroyRef = inject(DestroyRef);

    ngOnInit(): void {
        this.store.getCategories();

        this.#destroyRef.onDestroy(() => {
            this.store.clean();
        });
    }
}
