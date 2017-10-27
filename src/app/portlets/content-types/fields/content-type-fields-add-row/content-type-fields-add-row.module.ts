import { ButtonModule, TooltipModule } from 'primeng/primeng';
import { HotkeysService } from 'angular2-hotkeys';
import { ContentTypeFieldsAddRowComponent } from './content-type-fields-add-row.component';
import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

@NgModule({
    declarations: [
        ContentTypeFieldsAddRowComponent,
    ],
    exports: [
        ContentTypeFieldsAddRowComponent,
    ],
    imports: [
        CommonModule,
        ButtonModule,
        TooltipModule
    ],
    providers: [
        HotkeysService
    ],
})

export class ContentTypeFieldsAddRowModule {}
