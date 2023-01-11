import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { DotPortletBoxModule } from './components/dot-portlet-box/dot-portlet-box.module';
import { DotPortletToolbarComponent } from './components/dot-portlet-toolbar/dot-portlet-toolbar.component';
import { DotPortletToolbarModule } from './components/dot-portlet-toolbar/dot-portlet-toolbar.module';
import { DotPortletBaseComponent } from './dot-portlet-base.component';

@NgModule({
    declarations: [DotPortletBaseComponent],
    exports: [DotPortletBaseComponent, DotPortletToolbarComponent],
    imports: [CommonModule, DotPortletToolbarModule, DotPortletBoxModule]
})
export class DotPortletBaseModule {}
