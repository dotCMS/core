import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { DotMessagePipeModule } from '@dotcms/ui';

import { DotLayoutSidebarComponent } from './dot-layout-property-sidebar.component';

import { DotLayoutPropertiesItemModule } from '../dot-layout-properties-item/dot-layout-properties-item.module';

@NgModule({
    declarations: [DotLayoutSidebarComponent],
    imports: [CommonModule, DotLayoutPropertiesItemModule, FormsModule, DotMessagePipeModule],
    exports: [DotLayoutSidebarComponent],
    providers: []
})
export class DotLayoutSidebarModule {}
