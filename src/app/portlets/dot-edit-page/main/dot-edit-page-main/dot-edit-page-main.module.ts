import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DotEditPageMainComponent } from './dot-edit-page-main.component';
import { RouterModule } from '@angular/router';
import { DotEditPageNavModule } from '../dot-edit-page-nav/dot-edit-page-nav.module';
import { DotContentletEditorModule } from '../../../../view/components/dot-contentlet-editor/dot-contentlet-editor.module';
import { DotRouterService } from '../../../../../../node_modules/dotcms-js/dotcms-js';

@NgModule({
    imports: [CommonModule, RouterModule, DotEditPageNavModule, DotContentletEditorModule],
    providers: [DotRouterService],
    declarations: [DotEditPageMainComponent]
})
export class DotEditPageMainModule {}
