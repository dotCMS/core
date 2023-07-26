import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';

import { DotEditLayoutGridModule } from '@components/dot-edit-layout-designer/components/dot-edit-layout-grid/dot-edit-layout-grid.module';
import { DotEditLayoutSidebarModule } from '@components/dot-edit-layout-designer/components/dot-edit-layout-sidebar/dot-edit-layout-sidebar.module';
import { DotMessagePipe } from '@dotcms/ui';
import { DotPipesModule } from '@pipes/dot-pipes.module';

import { DotLayoutDesignerComponent } from './dot-layout-designer.component';

@NgModule({
    imports: [
        CommonModule,
        DotEditLayoutSidebarModule,
        DotEditLayoutGridModule,
        DotPipesModule,
        FormsModule,
        ReactiveFormsModule,
        DotMessagePipe
    ],
    declarations: [DotLayoutDesignerComponent],
    exports: [DotLayoutDesignerComponent]
})
export class DotLayoutDesignerModule {}
