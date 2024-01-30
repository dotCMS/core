import { NgIf } from '@angular/common';
import { NgModule } from '@angular/core';

import { DotKeyValueComponent } from '@dotcms/ui';

import { DotContentTypeFieldsVariablesComponent } from './dot-content-type-fields-variables.component';
import { DotFieldVariablesService } from './services/dot-field-variables.service';

@NgModule({
    imports: [NgIf, DotKeyValueComponent],
    exports: [DotContentTypeFieldsVariablesComponent],
    providers: [DotFieldVariablesService],
    declarations: [DotContentTypeFieldsVariablesComponent]
})
export class DotContentTypeFieldsVariablesModule {}
