import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { DotEditPageInfoComponent } from './dot-edit-page-info.component';
import { ButtonModule } from 'primeng/primeng';
import { DotClipboardUtil } from '../../../../api/util/clipboard/ClipboardUtil';

@NgModule({
    imports: [CommonModule, ButtonModule],
    exports: [DotEditPageInfoComponent],
    declarations: [DotEditPageInfoComponent],
    providers: [DotClipboardUtil]
})
export class DotEditPageInfoModule {}
