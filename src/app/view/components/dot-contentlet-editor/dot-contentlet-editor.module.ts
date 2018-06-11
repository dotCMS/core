import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DotAddContentletComponent } from './components/dot-add-contentlet/dot-add-contentlet.component';
import { DotCreateContentletComponent } from './components/dot-create-contentlet/dot-create-contentlet.component';
import { DotEditContentletComponent } from './components/dot-edit-contentlet/dot-edit-contentlet.component';
import { DotIframeDialogModule } from '../dot-iframe-dialog/dot-iframe-dialog.module';
import { DotContentletEditorService } from './services/dot-contentlet-editor.service';
import { DotContentletEditorComponent } from './dot-contentlet-editor.component';
import { DotContentletWrapperComponent } from './components/dot-contentlet-wrapper/dot-contentlet-wrapper.component';

@NgModule({
    imports: [CommonModule, DotIframeDialogModule],
    declarations: [
        DotAddContentletComponent,
        DotContentletEditorComponent,
        DotContentletWrapperComponent,
        DotCreateContentletComponent,
        DotEditContentletComponent
    ],
    exports: [DotContentletEditorComponent, DotEditContentletComponent],
    providers: [DotContentletEditorService]
})
export class DotContentletEditorModule {}
