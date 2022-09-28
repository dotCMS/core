import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { CategoriesPermissionsComponent } from './categories-permissions.component';
import { IFrameModule } from '@components/_common/iframe';
import { DotPortletBaseModule } from '@components/dot-portlet-base/dot-portlet-base.module';

@NgModule({
    declarations: [CategoriesPermissionsComponent],
    exports: [CategoriesPermissionsComponent],
    imports: [CommonModule, DotPortletBaseModule, IFrameModule]
})
export class CategoriesPermissionsModule {}
