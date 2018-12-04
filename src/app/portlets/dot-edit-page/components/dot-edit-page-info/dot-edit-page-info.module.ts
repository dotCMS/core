import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { DotEditPageInfoComponent } from './dot-edit-page-info.component';
import { ButtonModule } from 'primeng/primeng';
import { DotClipboardUtil } from '../../../../api/util/clipboard/ClipboardUtil';
import { DotCopyButtonModule } from '@components/dot-copy-button/dot-copy-button.module';

@NgModule({
    imports: [CommonModule, ButtonModule, DotCopyButtonModule],
    exports: [DotEditPageInfoComponent],
    declarations: [DotEditPageInfoComponent],
    providers: [DotClipboardUtil]
})
export class DotEditPageInfoModule {}
