import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';

import { DotPortletBaseModule } from '@components/dot-portlet-base/dot-portlet-base.module';
import { CategoriesListComponent } from './categories-list.component';

@NgModule({
    declarations: [CategoriesListComponent],
    exports: [CategoriesListComponent],
    imports: [CommonModule, DotPortletBaseModule]
})
export class CategoriesListModule {}
