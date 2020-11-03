import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';

import { DotLayoutDesignerComponent } from './dot-layout-designer.component';
import { DotEditLayoutSidebarModule } from '@portlets/dot-edit-page/layout/components/dot-edit-layout-sidebar/dot-edit-layout-sidebar.module';
import { DotEditLayoutGridModule } from '@portlets/dot-edit-page/layout/components/dot-edit-layout-grid/dot-edit-layout-grid.module';
import { DotPipesModule } from '@pipes/dot-pipes.module';

@NgModule({
    imports: [
        CommonModule,
        DotEditLayoutSidebarModule,
        DotEditLayoutGridModule,
        DotPipesModule,
        FormsModule,
        ReactiveFormsModule
    ],
    declarations: [DotLayoutDesignerComponent],
    exports: [DotLayoutDesignerComponent]
})
export class DotLayoutDesignerModule {}
