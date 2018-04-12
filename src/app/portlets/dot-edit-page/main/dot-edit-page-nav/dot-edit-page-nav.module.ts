import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DotEditPageNavComponent } from './dot-edit-page-nav.component';
import { RouterModule } from '@angular/router';
import { TooltipModule } from 'primeng/primeng';

@NgModule({
    imports: [CommonModule, RouterModule, TooltipModule],
    declarations: [DotEditPageNavComponent],
    exports: [DotEditPageNavComponent],
    providers: []
})
export class DotEditPageNavModule {}
