import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DotEditPageNavComponent } from './dot-edit-page-nav.component';
import { RouterModule } from '@angular/router';
import { TooltipModule } from 'primeng/primeng';
import { DotIconModule } from '../../../../view/components/_common/dot-icon/dot-icon.module';

@NgModule({
    imports: [CommonModule, RouterModule, TooltipModule, DotIconModule],
    declarations: [DotEditPageNavComponent],
    exports: [DotEditPageNavComponent],
    providers: []
})
export class DotEditPageNavModule {}
