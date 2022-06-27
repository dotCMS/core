import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';
import { DotDialogModule } from '@components/dot-dialog/dot-dialog.module';
import { DotNavigationService } from '@components/dot-navigation/services/dot-navigation.service';
import { DotFieldValidationMessageModule } from '@components/_common/dot-field-validation-message/dot-file-validation-message.module';
import { DotAutofocusModule } from '@directives/dot-autofocus/dot-autofocus.module';
import { DotAddToMenuService } from '@dotcms/app/api/services/add-to-menu/add-to-menu.service';
import { DotMenuService } from '@dotcms/app/api/services/dot-menu.service';
import { DotPipesModule } from '@pipes/dot-pipes.module';
import { DropdownModule } from 'primeng/dropdown';
import { InputTextModule } from 'primeng/inputtext';
import { RadioButtonModule } from 'primeng/radiobutton';
import { DotAddToMenuComponent } from './dot-add-to-menu.component';

@NgModule({
    declarations: [DotAddToMenuComponent],
    exports: [DotAddToMenuComponent],
    imports: [
        CommonModule,
        DotAutofocusModule,
        DotDialogModule,
        DotFieldValidationMessageModule,
        DotPipesModule,
        DropdownModule,
        InputTextModule,
        RadioButtonModule,
        ReactiveFormsModule
    ],
    providers: [DotAddToMenuService, DotMenuService, DotNavigationService]
})
export class DotAddToMenuModule {}
