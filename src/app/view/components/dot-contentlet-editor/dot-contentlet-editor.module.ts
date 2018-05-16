import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DotAddContentletComponent } from './components/dot-add-contentlet/dot-add-contentlet.component';
import { DotCreateContentletComponent } from './components/dot-create-contentlet/dot-create-contentlet.component';
import { DotEditContentletComponent } from './components/dot-edit-contentlet/dot-edit-contentlet.component';
import { DotIframeDialogModule } from '../dot-iframe-dialog/dot-iframe-dialog.module';
import { DotContentletEditorService } from './services/dot-contentlet-editor.service';
import { DotContentletEditorComponent } from './dot-contentlet-editor.component';


@NgModule({
    imports: [CommonModule, DotIframeDialogModule],
    declarations: [DotContentletEditorComponent, DotAddContentletComponent, DotCreateContentletComponent, DotEditContentletComponent],
    exports: [DotContentletEditorComponent],
    providers: [DotContentletEditorService]
})
export class DotContentletEditorModule {}
