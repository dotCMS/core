import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DotPortletBaseComponent } from './dot-portlet-base.component';
import { DotPortletToolbarModule } from './components/dot-portlet-toolbar/dot-portlet-toolbar.module';
import { DotPortletToolbarComponent } from './components/dot-portlet-toolbar/dot-portlet-toolbar.component';

@NgModule({
    declarations: [DotPortletBaseComponent],
    exports: [DotPortletBaseComponent, DotPortletToolbarComponent],
    imports: [CommonModule, DotPortletToolbarModule]
})
export class DotPortletBaseModule {}
