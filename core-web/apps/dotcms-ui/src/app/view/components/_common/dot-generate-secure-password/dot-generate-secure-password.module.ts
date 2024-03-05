import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { ButtonModule } from 'primeng/button';

import { DotGenerateSecurePasswordService } from '@dotcms/data-access';
import { DotClipboardUtil, DotMessagePipe } from '@dotcms/ui';

import { DotGenerateSecurePasswordComponent } from './dot-generate-secure-password.component';

import { DotPipesModule } from '../../../pipes/dot-pipes.module';
import { DotDialogModule } from '../../dot-dialog/dot-dialog.module';

@NgModule({
    declarations: [DotGenerateSecurePasswordComponent],
    exports: [DotGenerateSecurePasswordComponent],
    providers: [DotGenerateSecurePasswordService, DotClipboardUtil],
    imports: [ButtonModule, CommonModule, DotDialogModule, DotPipesModule, DotMessagePipe]
})
export class DotGenerateSecurePasswordModule {}
