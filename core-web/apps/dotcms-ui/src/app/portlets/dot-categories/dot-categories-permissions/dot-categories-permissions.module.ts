import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { DotPortletBaseModule } from '@components/dot-portlet-base/dot-portlet-base.module';
import { IFrameModule } from '@components/_common/iframe';
import { DotCategoriesPermissionsComponent } from './dot-categories-permissions.component';

@NgModule({
    declarations: [DotCategoriesPermissionsComponent],
    exports: [DotCategoriesPermissionsComponent],
    imports: [CommonModule, DotPortletBaseModule, IFrameModule]
})
export class DotCategoriesPermissionsModule {}
