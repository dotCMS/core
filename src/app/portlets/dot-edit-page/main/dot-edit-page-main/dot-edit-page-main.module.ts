import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DotEditPageMainComponent } from './dot-edit-page-main.component';
import { RouterModule } from '@angular/router';
import { DotEditPageNavModule } from '../dot-edit-page-nav/dot-edit-page-nav.module';

@NgModule({
    imports: [CommonModule, RouterModule, DotEditPageNavModule],
    declarations: [DotEditPageMainComponent],
})
export class DotEditPageMainModule {}
