import { NgModule } from '@angular/core';

import { DotKeyValueModule } from '@components/dot-key-value-ng/dot-key-value-ng.module';

import { DotContentTypeFieldsVariablesComponent } from './dot-content-type-fields-variables.component';
import { DotFieldVariablesService } from './services/dot-field-variables.service';

@NgModule({
    imports: [DotKeyValueModule],
    exports: [DotContentTypeFieldsVariablesComponent],
    providers: [DotFieldVariablesService],
    declarations: [DotContentTypeFieldsVariablesComponent]
})
export class DotContentTypeFieldsVariablesModule {}
