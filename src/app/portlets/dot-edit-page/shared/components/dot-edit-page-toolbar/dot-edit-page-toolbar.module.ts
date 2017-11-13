import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DotEditPageToolbarComponent } from './dot-edit-page-toolbar.component';
import { ToolbarModule } from 'primeng/primeng';
import { ButtonModule } from 'primeng/primeng';

@NgModule({
    imports: [ButtonModule, CommonModule, ToolbarModule],
    exports: [DotEditPageToolbarComponent],
    declarations: [DotEditPageToolbarComponent]
})
export class DotEditPageToolbarModule {}
