import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';

import { DotMessagePipe } from '@dotcms/ui';

import { DotLayoutDesignerComponent } from './dot-layout-designer.component';

import { DotPipesModule } from '../../../../pipes/dot-pipes.module';
import { DotEditLayoutGridModule } from '../dot-edit-layout-grid/dot-edit-layout-grid.module';
import { DotEditLayoutSidebarModule } from '../dot-edit-layout-sidebar/dot-edit-layout-sidebar.module';

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
