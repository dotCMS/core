import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DotEditPageToolbarComponent } from './dot-edit-page-toolbar.component';
import { ToolbarModule, SelectButtonModule, InputSwitchModule, SplitButtonModule, ButtonModule } from 'primeng/primeng';
import { FormsModule } from '@angular/forms';

@NgModule({
    imports: [
        SplitButtonModule,
        CommonModule,
        ToolbarModule,
        SelectButtonModule,
        InputSwitchModule,
        FormsModule,
        ButtonModule
    ],
    exports: [DotEditPageToolbarComponent],
    declarations: [DotEditPageToolbarComponent]
})
export class DotEditPageToolbarModule {}
