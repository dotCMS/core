import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DotCreatePersonaFormComponent } from '@components/dot-add-persona-dialog/dot-create-persona-form/dot-create-persona-form.component';
import { ReactiveFormsModule } from '@angular/forms';
import { DotFieldValidationMessageModule } from '@components/_common/dot-field-validation-message/dot-file-validation-message.module';
import { DotAutofocusModule } from '@directives/dot-autofocus/dot-autofocus.module';
import { SiteSelectorFieldModule } from '@components/_common/dot-site-selector-field/dot-site-selector-field.module';
import { DotAutocompleteTagsModule } from '@components/_common/dot-autocomplete-tags/dot-autocomplete-tags.module';
import { DotPipesModule } from '@pipes/dot-pipes.module';
import { FileUploadModule } from 'primeng/fileupload';
import { InputTextModule } from 'primeng/inputtext';
import { ButtonModule } from 'primeng/button';
import { AutoCompleteModule } from 'primeng/autocomplete';

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
        DotPipesModule
    ],
    declarations: [DotCreatePersonaFormComponent],
    exports: [DotCreatePersonaFormComponent]
})
export class DotCreatePersonaFormModule {}
