import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DotEditPageToolbarComponent } from './dot-edit-page-toolbar.component';
import { CheckboxModule, ToolbarModule } from 'primeng/primeng';
import { FormsModule } from '@angular/forms';
import { DotEditPageViewAsControllerModule } from '../dot-edit-page-view-as-controller/dot-edit-page-view-as-controller.module';
import { DotEditPageStateControllerModule } from '../dot-edit-page-state-controller/dot-edit-page-state-controller.module';

@NgModule({
    imports: [
        CommonModule,
        CheckboxModule,
        DotEditPageViewAsControllerModule,
        DotEditPageStateControllerModule,
        FormsModule,
        ToolbarModule
    ],
    exports: [DotEditPageToolbarComponent],
    declarations: [DotEditPageToolbarComponent]
})
export class DotEditPageToolbarModule {}
