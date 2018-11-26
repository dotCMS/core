import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TableModule } from 'primeng/table';
import { FieldVariablesService } from '../service';
import { DotActionButtonModule } from '@components/_common/dot-action-button/dot-action-button.module';
import { FormsModule } from '@angular/forms';
import { ButtonModule } from 'primeng/primeng';
import { DotIconButtonModule } from '@components/_common/dot-icon-button/dot-icon-button.module';
import { ContentTypeFieldsVariablesComponent } from './content-type-fields-variables.component';

@NgModule({
    imports: [
        CommonModule,
        ButtonModule,
        FormsModule,
        TableModule,
        DotActionButtonModule,
        DotIconButtonModule
    ],
    exports: [ContentTypeFieldsVariablesComponent],
    providers: [FieldVariablesService],
    declarations: [ContentTypeFieldsVariablesComponent]
})
export class DotContentTypeFieldsVariablesModule {}
