import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { CheckboxModule } from 'primeng/checkbox';
import { InputTextModule } from 'primeng/inputtext';
import { PasswordModule } from 'primeng/password';

import { DotDialogModule } from '@components/dot-dialog/dot-dialog.module';
import { DotAlertConfirmService } from '@dotcms/data-access';
import { DotPipesModule } from '@pipes/dot-pipes.module';

import { DotMyAccountComponent } from './dot-my-account.component';

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
