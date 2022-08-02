import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DotEditSidebarComponent } from '@portlets/dot-edit-page/components/dot-edit-sidebar/dot-edit-sidebar.component';

@NgModule({
    imports: [CommonModule],
    declarations: [DotEditSidebarComponent],
    exports: [DotEditSidebarComponent]
})
export class DotEditSidebarModule {}
