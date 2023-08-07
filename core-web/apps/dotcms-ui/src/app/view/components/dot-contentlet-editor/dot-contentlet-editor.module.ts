import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { DotContentCompareModule } from '@components/dot-content-compare/dot-content-compare.module';
import { DotMessagePipe } from '@dotcms/ui';
import { DotPipesModule } from '@pipes/dot-pipes.module';

import { DotAddContentletComponent } from './components/dot-add-contentlet/dot-add-contentlet.component';
import { DotContentletWrapperComponent } from './components/dot-contentlet-wrapper/dot-contentlet-wrapper.component';
import { DotCreateContentletComponent } from './components/dot-create-contentlet/dot-create-contentlet.component';
import { DotCreateContentletResolver } from './components/dot-create-contentlet/dot-create-contentlet.resolver.service';
import { DotEditContentletComponent } from './components/dot-edit-contentlet/dot-edit-contentlet.component';
import { DotReorderMenuComponent } from './components/dot-reorder-menu/dot-reorder-menu.component';
import { DotContentletEditorService } from './services/dot-contentlet-editor.service';

import { DotIframeDialogModule } from '../dot-iframe-dialog/dot-iframe-dialog.module';

@NgModule({
    imports: [
        CommonModule,
        DotIframeDialogModule,
        DotPipesModule,
        DotContentCompareModule,
        DotMessagePipe
    ],
    declarations: [
        DotAddContentletComponent,
        DotContentletWrapperComponent,
        DotCreateContentletComponent,
        DotEditContentletComponent,
        DotReorderMenuComponent
    ],
    exports: [
        DotEditContentletComponent,
        DotAddContentletComponent,
        DotCreateContentletComponent,
        DotReorderMenuComponent
    ],
    providers: [DotContentletEditorService, DotCreateContentletResolver]
})
export class DotContentletEditorModule {}
