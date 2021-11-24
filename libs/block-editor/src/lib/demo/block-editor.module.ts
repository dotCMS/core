import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { BlockEditorComponent } from './block-editor.component';

import { FormsModule } from '@angular/forms';
import { OrderListModule } from 'primeng/orderlist';
import { ListboxModule } from 'primeng/listbox';
import { NgxTiptapModule } from '../ngx-tiptap.module';
import { DotImageService } from '../extensions/services/dot-image/dot-image.service';

@NgModule({
    declarations: [BlockEditorComponent],
    exports: [BlockEditorComponent],
    imports: [CommonModule, FormsModule, NgxTiptapModule, OrderListModule, ListboxModule],
    providers: [DotImageService]
})
export class BlockEditorModule {}
