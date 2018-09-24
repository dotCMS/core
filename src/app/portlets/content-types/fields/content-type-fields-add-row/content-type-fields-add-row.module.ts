import { ButtonModule, TooltipModule } from 'primeng/primeng';
import { HotkeysService } from 'angular2-hotkeys';
import { ContentTypeFieldsAddRowComponent } from './content-type-fields-add-row.component';
import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { DotIconButtonModule } from '@components/_common/dot-icon-button/dot-icon-button.module';

@NgModule({
    declarations: [ContentTypeFieldsAddRowComponent],
    exports: [ContentTypeFieldsAddRowComponent],
    imports: [CommonModule, ButtonModule, TooltipModule, DotIconButtonModule],
    providers: [HotkeysService]
})
export class ContentTypeFieldsAddRowModule {}
