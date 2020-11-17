import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DotPortletBaseComponent } from './dot-portlet-base.component';
import { DotPortletToolbarModule } from './components/dot-portlet-toolbar/dot-portlet-toolbar.module';
import { DotPortletToolbarComponent } from './components/dot-portlet-toolbar/dot-portlet-toolbar.component';
import { DotPortletBoxModule } from './components/dot-portlet-box/dot-portlet-box.module';

@NgModule({
    declarations: [DotPortletBaseComponent],
    exports: [DotPortletBaseComponent, DotPortletToolbarComponent],
    imports: [CommonModule, DotPortletToolbarModule, DotPortletBoxModule]
})
export class DotPortletBaseModule {}
