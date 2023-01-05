import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { DotMessagePipeModule } from '@pipes/dot-message/dot-message-pipe.module';
import { ButtonModule } from 'primeng/button';
import { MenuModule } from 'primeng/menu';
import { ToolbarModule } from 'primeng/toolbar';
import { DotPortletToolbarComponent } from './dot-portlet-toolbar.component';

@NgModule({
    declarations: [DotPortletToolbarComponent],
    imports: [CommonModule, ToolbarModule, ButtonModule, DotMessagePipeModule, MenuModule],
    exports: [DotPortletToolbarComponent]
})
export class DotPortletToolbarModule {}
