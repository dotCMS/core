import { NgModule } from '@angular/core';
import { DotFieldVariablesService } from './services/dot-field-variables.service';
import { DotContentTypeFieldsVariablesComponent } from './dot-content-type-fields-variables.component';
import { DotKeyValueModule } from '@components/dot-key-value/dot-key-value.module';

@NgModule({
    imports: [
        DotKeyValueModule,
    ],
    exports: [DotContentTypeFieldsVariablesComponent],
    providers: [DotFieldVariablesService],
    declarations: [DotContentTypeFieldsVariablesComponent]
})
export class DotContentTypeFieldsVariablesModule {}
