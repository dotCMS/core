import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DotEditPageNavComponent } from './dot-edit-page-nav.component';
import { RouterModule } from '@angular/router';

@NgModule({
    imports: [CommonModule, RouterModule],
    declarations: [DotEditPageNavComponent],
    exports: [DotEditPageNavComponent]
})
export class DotEditPageNavModule {}
