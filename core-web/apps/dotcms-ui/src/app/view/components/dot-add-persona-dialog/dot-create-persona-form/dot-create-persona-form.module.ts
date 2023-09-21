import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';

import { AutoCompleteModule } from 'primeng/autocomplete';
import { ButtonModule } from 'primeng/button';
import { FileUploadModule } from 'primeng/fileupload';
import { InputTextModule } from 'primeng/inputtext';

import { DotAutocompleteTagsModule } from '@components/_common/dot-autocomplete-tags/dot-autocomplete-tags.module';
import { DotFieldValidationMessageModule } from '@components/_common/dot-field-validation-message/dot-file-validation-message.module';
import { SiteSelectorFieldModule } from '@components/_common/dot-site-selector-field/dot-site-selector-field.module';
import { DotCreatePersonaFormComponent } from '@components/dot-add-persona-dialog/dot-create-persona-form/dot-create-persona-form.component';
import { DotAutofocusModule } from '@directives/dot-autofocus/dot-autofocus.module';
import { DotFieldRequiredDirective, DotMessagePipe } from '@dotcms/ui';
import { DotPipesModule } from '@pipes/dot-pipes.module';

@NgModule({
    imports: [
        CommonModule,
        FileUploadModule,
        InputTextModule,
        ReactiveFormsModule,
        SiteSelectorFieldModule,
        DotFieldValidationMessageModule,
        DotAutofocusModule,
        ButtonModule,
        AutoCompleteModule,
        DotAutocompleteTagsModule,
        DotPipesModule,
        DotFieldRequiredDirective,
        DotMessagePipe
    ],
    declarations: [DotCreatePersonaFormComponent],
    exports: [DotCreatePersonaFormComponent]
})
export class DotCreatePersonaFormModule {}
