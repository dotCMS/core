import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DotDialogModule } from '@components/dot-dialog/dot-dialog.module';
import { DotPipesModule } from '@pipes/dot-pipes.module';
import { DotGenerateSecurePasswordService } from '@services/dot-generate-secure-password/dot-generate-secure-password.service';
import { DotGenerateSecurePasswordComponent } from './dot-generate-secure-password.component';
import { ButtonModule } from 'primeng/button';
import { DotClipboardUtil } from '@dotcms/app/api/util/clipboard/ClipboardUtil';

@NgModule({
    declarations: [DotGenerateSecurePasswordComponent],
    exports: [DotGenerateSecurePasswordComponent],
    providers: [DotGenerateSecurePasswordService, DotClipboardUtil],
    imports: [
        ButtonModule,
        CommonModule,
        DotDialogModule,
        DotPipesModule
    ]
})
export class DotGenerateSecurePasswordModule {}
