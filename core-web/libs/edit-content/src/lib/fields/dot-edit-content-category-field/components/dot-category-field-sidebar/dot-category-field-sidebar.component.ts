import { animate, state, style, transition, trigger } from '@angular/animations';
import { JsonPipe } from '@angular/common';
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
import { InputTextModule } from 'primeng/inputtext';
import { SidebarModule } from 'primeng/sidebar';

import { DotMessagePipe } from '@dotcms/ui';

import { CategoryFieldStore } from '../../store/content-category-field.store';
import { DotCategoryFieldCategoryListComponent } from '../dot-category-field-category-list/dot-category-field-category-list.component';
import { DotCategoryFieldSearchComponent } from '../dot-category-field-search/dot-category-field-search.component';
import {
    DotCategoryFieldSearchListComponent
} from "../dot-category-field-search-list/dot-category-field-search-list.component";

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
        DotCategoryFieldCategoryListComponent,
        InputTextModule,
        DotCategoryFieldSearchComponent,
        DotCategoryFieldSearchListComponent,
        JsonPipe
    ],
    templateUrl: './dot-category-field-sidebar.component.html',
    styleUrl: './dot-category-field-sidebar.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush,
    animations: [
        trigger('fadeAnimation', [
            state(
                'void',
                style({
                    opacity: 0
                })
            ),
            transition(':enter, :leave', [animate('50ms ease-in-out')])
        ])
    ]
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

    readonly store: InstanceType<typeof CategoryFieldStore> = inject(CategoryFieldStore);

    readonly #destroyRef = inject(DestroyRef);

    ngOnInit(): void {
        this.store.getCategories();

        this.#destroyRef.onDestroy(() => {
            this.store.clean();
        });
    }
}
