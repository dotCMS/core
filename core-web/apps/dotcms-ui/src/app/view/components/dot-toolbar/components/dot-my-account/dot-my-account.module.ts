import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { NgModule } from '@angular/core';

import { DotMyAccountComponent } from './dot-my-account.component';
import { DotDialogModule } from '@components/dot-dialog/dot-dialog.module';
import { DotPipesModule } from '@pipes/dot-pipes.module';
import { PasswordModule } from 'primeng/password';
import { InputTextModule } from 'primeng/inputtext';
import { CheckboxModule } from 'primeng/checkbox';
import { DotAlertConfirmService } from '@dotcms/app/api/services/dot-alert-confirm/dot-alert-confirm.service';

@NgModule({
    imports: [
        PasswordModule,
        InputTextModule,
        FormsModule,
        DotDialogModule,
        CommonModule,
        CheckboxModule,
        DotPipesModule
    ],
    exports: [DotMyAccountComponent],
    declarations: [DotMyAccountComponent],
    providers: [DotAlertConfirmService]
})
export class DotMyAccountModule {}
