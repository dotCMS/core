import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DotEditPageToolbarComponent } from './dot-edit-page-toolbar.component';
import {
    SelectButtonModule,
    InputSwitchModule,
    CheckboxModule,
    ToolbarModule
} from 'primeng/primeng';
import { FormsModule } from '@angular/forms';
import { DotEditPageLockInfoComponent } from './components/dot-edit-page-lock-info/dot-edit-page-lock-info.component';
import { DotEditContentViewAsToolbarModule } from '../dot-edit-content-view-as-toolbar/dot-edit-content-view-as-toolbar.module';

@NgModule({
    imports: [
        CommonModule,
        CheckboxModule,
        DotEditContentViewAsToolbarModule,
        FormsModule,
        InputSwitchModule,
        SelectButtonModule,
        ToolbarModule
    ],
    exports: [DotEditPageToolbarComponent],
    declarations: [DotEditPageToolbarComponent, DotEditPageLockInfoComponent]
})
export class DotEditPageToolbarModule {}
