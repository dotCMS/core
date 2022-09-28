import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';

import { DotPortletBaseModule } from '@components/dot-portlet-base/dot-portlet-base.module';
import { DotCategoriesListComponent } from './dot-categories-list.component';

@NgModule({
    declarations: [DotCategoriesListComponent],
    exports: [DotCategoriesListComponent],
    imports: [CommonModule, DotPortletBaseModule]
})
export class DotCategoriesListModule {}
