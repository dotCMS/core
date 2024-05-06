import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';

import { DotEditLayoutGridModule } from '@components/dot-edit-layout-designer/components/dot-edit-layout-grid/dot-edit-layout-grid.module';
import { DotEditLayoutSidebarModule } from '@components/dot-edit-layout-designer/components/dot-edit-layout-sidebar/dot-edit-layout-sidebar.module';
import { DotMessagePipe, DotSafeHtmlPipe } from '@dotcms/ui';

import { DotLayoutDesignerComponent } from './dot-layout-designer.component';

@NgModule({
    imports: [
        CommonModule,
        DotEditLayoutSidebarModule,
        DotEditLayoutGridModule,
        DotSafeHtmlPipe,
        FormsModule,
        ReactiveFormsModule,
        DotMessagePipe
    ],
    declarations: [DotLayoutDesignerComponent],
    exports: [DotLayoutDesignerComponent]
})
export class DotLayoutDesignerModule {}
