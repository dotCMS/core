import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DotCategoriesPermissionsComponent } from './dot-categories-permissions.component';
import { IFrameModule } from '@components/_common/iframe';
import { DotPortletBaseModule } from '@components/dot-portlet-base/dot-portlet-base.module';

@NgModule({
    declarations: [DotCategoriesPermissionsComponent],
    exports: [DotCategoriesPermissionsComponent],
    imports: [CommonModule, DotPortletBaseModule, IFrameModule]
})
export class DotCategoriesPermissionsModule {}
