import { Routes, RouterModule } from '@angular/router';
import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';

import { DialogModule } from 'primeng/primeng';

import { DotContainerContentletService } from './services/dot-container-contentlet.service';
import { DotEditContentComponent } from './dot-edit-content.component';
import { DotEditContentHtmlService } from './services/dot-edit-content-html.service';
import { DotEditPageToolbarModule } from './components/dot-edit-page-toolbar/dot-edit-page-toolbar.module';
import { EditContentResolver } from './services/dot-edit-content-resolver.service';
import { IFrameModule } from '../../view/components/_common/iframe/index';
import { DotDragDropAPIHtmlService } from './services/html/dot-drag-drop-api-html.service';
import { DotDOMHtmlUtilService } from './services/html/dot-dom-html-util.service';

const routes: Routes = [
    {
        component: DotEditContentComponent,
        path: '',
        resolve: {
            editPageHTML: EditContentResolver
        }
    }
];


@NgModule({
    declarations: [DotEditContentComponent],
    imports: [CommonModule, DialogModule, RouterModule.forChild(routes), DotEditPageToolbarModule],
    exports: [DotEditContentComponent],
    providers: [
        DotContainerContentletService,
        EditContentResolver,
        DotEditContentHtmlService,
        DotDragDropAPIHtmlService,
        DotDOMHtmlUtilService
    ]
})
export class DotEditContentModule {}
