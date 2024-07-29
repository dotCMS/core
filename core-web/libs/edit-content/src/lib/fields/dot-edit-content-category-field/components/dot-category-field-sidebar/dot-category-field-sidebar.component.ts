import { animate, state, style, transition, trigger } from '@angular/animations';
import {
    ChangeDetectionStrategy,
    Component,
    computed,
    EventEmitter,
    inject,
    Input,
    OnDestroy,
    OnInit,
    Output
} from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { InputTextModule } from 'primeng/inputtext';
import { SidebarModule } from 'primeng/sidebar';

import { DotMessagePipe, DotCollapseBreadcrumbComponent } from '@dotcms/ui';

import { CategoryFieldStore } from '../../store/content-category-field.store';
import { DotCategoryFieldCategoryListComponent } from '../dot-category-field-category-list/dot-category-field-category-list.component';
import { DotCategoryFieldSearchComponent } from '../dot-category-field-search/dot-category-field-search.component';
import { DotCategoryFieldSearchListComponent } from '../dot-category-field-search-list/dot-category-field-search-list.component';
import { DotCategoryFieldSelectedComponent } from '../dot-category-field-selected/dot-category-field-selected.component';

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
        DotCategoryFieldSelectedComponent,
        DotCollapseBreadcrumbComponent
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
export class DotCategoryFieldSidebarComponent implements OnInit, OnDestroy {
    /**
     * Indicates the visibility of the sidebar.
     *
     * @memberof DotCategoryFieldSidebarComponent
     */
    @Input() visible = false;

    /**
     * Output that emit if the sidebar is closed
     */
    @Output() closedSidebar = new EventEmitter<void>();

    /**
     * Store based on the `CategoryFieldStore`.
     *
     * @memberof DotCategoryFieldSidebarComponent
     */
    readonly store = inject(CategoryFieldStore);

    /**
     * Computed property for retrieving all category keys.
     */
    $allCategoryKeys = computed(() => this.store.selected().map((category) => category.key));

    ngOnInit(): void {
        this.store.getCategories();
    }

    ngOnDestroy(): void {
        this.store.clean();
    }
}
