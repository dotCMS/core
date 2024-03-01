import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';

import { DropdownModule } from 'primeng/dropdown';
import { InputTextModule } from 'primeng/inputtext';
import { RadioButtonModule } from 'primeng/radiobutton';

import { DotDialogModule } from '@components/dot-dialog/dot-dialog.module';
import { DotNavigationService } from '@components/dot-navigation/services/dot-navigation.service';
import { DotAddToMenuService } from '@dotcms/app/api/services/add-to-menu/add-to-menu.service';
import { DotMenuService } from '@dotcms/app/api/services/dot-menu.service';
import {
    DotAutofocusDirective,
    DotFieldRequiredDirective,
    DotFieldValidationMessageComponent,
    DotMessagePipe,
    DotSafeHtmlPipe
} from '@dotcms/ui';

import { DotAddToMenuComponent } from './dot-add-to-menu.component';

@NgModule({
    declarations: [DotAddToMenuComponent],
    exports: [DotAddToMenuComponent],
    imports: [
        CommonModule,
        DotAutofocusDirective,
        DotDialogModule,
        DotFieldValidationMessageComponent,
        DotSafeHtmlPipe,
        DropdownModule,
        InputTextModule,
        RadioButtonModule,
        ReactiveFormsModule,
        DotFieldRequiredDirective,
        DotMessagePipe
    ],
    providers: [DotAddToMenuService, DotMenuService, DotNavigationService]
})
export class DotAddToMenuModule {}
