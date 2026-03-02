import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';

import { MenuItem } from 'primeng/api';
import { TabsModule } from 'primeng/tabs';

import { DotMessagePipe } from '@dotcms/ui';

import { DotCategoriesCreateEditStore } from './store/dot-categories-create-edit.store';

import { DotPortletBaseComponent } from '../../../view/components/dot-portlet-base/dot-portlet-base.component';
import { DotCategoriesListComponent } from '../dot-categories-list/dot-categories-list.component';
import { DotCategoriesPermissionsComponent } from '../dot-categories-permissions/dot-categories-permissions.component';
@Component({
    selector: 'dot-categories-create-edit-list',
    templateUrl: './dot-categories-create-edit.component.html',
    styleUrls: ['./dot-categories-create-edit.component.scss'],
    providers: [DotCategoriesCreateEditStore],
    imports: [
        CommonModule,
        DotMessagePipe,
        TabsModule,
        DotCategoriesListComponent,
        DotPortletBaseComponent,
        DotCategoriesPermissionsComponent
    ]
})
export class DotCategoriesCreateEditComponent {
    readonly #store = inject(DotCategoriesCreateEditStore);
    vm$ = this.#store.vm$;

    /**
     * The function takes a category object as a parameter, and then calls the updateCategory function
     * in the store service, passing the category object as a parameter
     * @param {MenuItem} category
     * @memberof DotCategoriesCreateEditComponent
     */
    updateCategory(category: MenuItem) {
        this.#store.updateCategory(category);
    }
}
