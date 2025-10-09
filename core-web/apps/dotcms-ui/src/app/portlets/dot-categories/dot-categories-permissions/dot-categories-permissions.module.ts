import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { DotCategoriesPermissionsComponent } from './dot-categories-permissions.component';

import { IframeComponent } from '../../../view/components/_common/iframe/iframe-component/iframe.component';
import { DotPortletBaseModule } from '../../../view/components/dot-portlet-base/dot-portlet-base.module';

@NgModule({
    declarations: [DotCategoriesPermissionsComponent],
    exports: [DotCategoriesPermissionsComponent],
    imports: [CommonModule, DotPortletBaseModule, IframeComponent]
})
export class DotCategoriesPermissionsModule {}
