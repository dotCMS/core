import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { DotCategoriesPermissionsComponent } from './dot-categories-permissions.component';

import { IFrameModule } from '../../../view/components/_common/iframe/iframe.module';
import { DotPortletBaseModule } from '../../../view/components/dot-portlet-base/dot-portlet-base.module';

@NgModule({
    declarations: [DotCategoriesPermissionsComponent],
    exports: [DotCategoriesPermissionsComponent],
    imports: [CommonModule, DotPortletBaseModule, IFrameModule]
})
export class DotCategoriesPermissionsModule {}
