import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';

import { AutoCompleteModule } from 'primeng/autocomplete';
import { ButtonModule } from 'primeng/button';
import { FileUploadModule } from 'primeng/fileupload';
import { InputTextModule } from 'primeng/inputtext';

import {
    DotAutofocusDirective,
    DotFieldRequiredDirective,
    DotFieldValidationMessageComponent,
    DotMessagePipe,
    DotSafeHtmlPipe
} from '@dotcms/ui';

import { DotCreatePersonaFormComponent } from './dot-create-persona-form.component';

import { DotAutocompleteTagsModule } from '../../_common/dot-autocomplete-tags/dot-autocomplete-tags.module';
import { SiteSelectorFieldModule } from '../../_common/dot-site-selector-field/dot-site-selector-field.module';

@NgModule({
    imports: [
        CommonModule,
        FileUploadModule,
        InputTextModule,
        ReactiveFormsModule,
        SiteSelectorFieldModule,
        DotFieldValidationMessageComponent,
        DotAutofocusDirective,
        ButtonModule,
        AutoCompleteModule,
        DotAutocompleteTagsModule,
        DotSafeHtmlPipe,
        DotFieldRequiredDirective,
        DotMessagePipe
    ],
    declarations: [DotCreatePersonaFormComponent],
    exports: [DotCreatePersonaFormComponent]
})
export class DotCreatePersonaFormModule {}
