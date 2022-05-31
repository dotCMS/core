import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DotAddContentletComponent } from './components/dot-add-contentlet/dot-add-contentlet.component';
import { DotCreateContentletComponent } from './components/dot-create-contentlet/dot-create-contentlet.component';
import { DotEditContentletComponent } from './components/dot-edit-contentlet/dot-edit-contentlet.component';
import { DotIframeDialogModule } from '../dot-iframe-dialog/dot-iframe-dialog.module';
import { DotContentletEditorService } from './services/dot-contentlet-editor.service';
import { DotContentletWrapperComponent } from './components/dot-contentlet-wrapper/dot-contentlet-wrapper.component';
import { DotReorderMenuComponent } from './components/dot-reorder-menu/dot-reorder-menu.component';
import { DotPipesModule } from '@pipes/dot-pipes.module';
import { DotCreateContentletResolver } from './components/dot-create-contentlet/dot-create-contentlet.resolver.service';
import { DotContentCompareModule } from '@components/dot-content-compare/dot-content-compare.module';

@NgModule({
    imports: [CommonModule, DotIframeDialogModule, DotPipesModule, DotContentCompareModule],
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
