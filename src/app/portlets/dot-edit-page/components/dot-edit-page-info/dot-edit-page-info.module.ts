import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { DotEditPageInfoComponent } from './dot-edit-page-info.component';
import { ButtonModule } from 'primeng/primeng';
import { DotCopyButtonModule } from '@components/dot-copy-button/dot-copy-button.module';
import { DotApiLinkModule } from '@components/dot-api-link/dot-api-link.module';

@NgModule({
    imports: [CommonModule, ButtonModule, DotCopyButtonModule, DotApiLinkModule],
    exports: [DotEditPageInfoComponent],
    declarations: [DotEditPageInfoComponent],
})
export class DotEditPageInfoModule {}
