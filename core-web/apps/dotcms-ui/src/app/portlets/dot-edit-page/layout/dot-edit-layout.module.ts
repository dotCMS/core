import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

import { DotEditLayoutDesignerModule } from '@components/dot-edit-layout-designer/dot-edit-layout-designer.module';

import { DotEditLayoutComponent } from './dot-edit-layout/dot-edit-layout.component';

const routes: Routes = [
    {
        component: DotEditLayoutComponent,
        path: ''
    }
];

@NgModule({
    declarations: [DotEditLayoutComponent],
    imports: [CommonModule, RouterModule.forChild(routes), DotEditLayoutDesignerModule],
    exports: [DotEditLayoutComponent]
})
export class DotEditLayoutModule {}
