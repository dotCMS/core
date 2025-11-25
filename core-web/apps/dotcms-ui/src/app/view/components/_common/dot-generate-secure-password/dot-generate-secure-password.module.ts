import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { ButtonModule } from 'primeng/button';

import { DotGenerateSecurePasswordService } from '@dotcms/data-access';
import { DotClipboardUtil, DotDialogModule, DotMessagePipe, DotSafeHtmlPipe } from '@dotcms/ui';

import { DotGenerateSecurePasswordComponent } from './dot-generate-secure-password.component';

@NgModule({
    declarations: [DotGenerateSecurePasswordComponent],
    exports: [DotGenerateSecurePasswordComponent],
    providers: [DotGenerateSecurePasswordService, DotClipboardUtil],
    imports: [ButtonModule, CommonModule, DotDialogModule, DotSafeHtmlPipe, DotMessagePipe]
})
export class DotGenerateSecurePasswordModule {}
