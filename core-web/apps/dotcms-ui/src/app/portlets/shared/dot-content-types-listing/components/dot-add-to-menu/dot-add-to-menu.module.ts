import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';

import { DropdownModule } from 'primeng/dropdown';
import { InputTextModule } from 'primeng/inputtext';
import { RadioButtonModule } from 'primeng/radiobutton';

import {
    DotAutofocusDirective,
    DotFieldRequiredDirective,
    DotFieldValidationMessageComponent,
    DotMessagePipe
} from '@dotcms/ui';

import { DotAddToMenuComponent } from './dot-add-to-menu.component';

import { DotAddToMenuService } from '../../../../../api/services/add-to-menu/add-to-menu.service';
import { DotMenuService } from '../../../../../api/services/dot-menu.service';
import { DotDialogModule } from '../../../../../view/components/dot-dialog/dot-dialog.module';
import { DotNavigationService } from '../../../../../view/components/dot-navigation/services/dot-navigation.service';
import { DotPipesModule } from '../../../../../view/pipes/dot-pipes.module';

@NgModule({
    declarations: [DotAddToMenuComponent],
    exports: [DotAddToMenuComponent],
    imports: [
        CommonModule,
        DotAutofocusDirective,
        DotDialogModule,
        DotFieldValidationMessageComponent,
        DotPipesModule,
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
