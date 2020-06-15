import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { NgModule } from '@angular/core';

import { InputTextModule, CheckboxModule, PasswordModule } from 'primeng/primeng';

import { DotMyAccountComponent } from './dot-my-account.component';
import { DotDialogModule } from '@components/dot-dialog/dot-dialog.module';
import { MdInputTextModule } from '@directives/md-inputtext/md-input-text.module';
import { DotPipesModule } from '@pipes/dot-pipes.module';

@NgModule({
    imports: [
        PasswordModule,
        MdInputTextModule,
        InputTextModule,
        FormsModule,
        DotDialogModule,
        CommonModule,
        CheckboxModule,
        DotPipesModule
    ],
    exports: [DotMyAccountComponent],
    declarations: [DotMyAccountComponent],
    providers: []
})
export class DotMyAccountModule {}
