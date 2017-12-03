import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Routes } from '@angular/router';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { PatternLibraryComponent } from './pattern-library.component';
import { NGFACES_MODULES } from './../../../../modules';
// CUSTOM MDOULES
import { DotActionButtonModule } from '../dot-action-button/dot-action-button.module';
import { ListingDataTableModule} from '../../listing-data-table/listing-data-table.module';
import { FieldValidationMessageModule} from '../field-validation-message/file-validation-message.module';
import { SiteSelectorModule} from '../site-selector/site-selector.module';
import { SearchableDropDownModule } from '../searchable-dropdown/searchable-dropdown.module';
import { DotTextareaContentModule } from '../dot-textarea-content/dot-textarea-content.module';

import { DotDropdownModule} from '../dropdown-component/dot-dropdown.module';
import { SiteSelectorFieldModule } from '../site-selector-field/site-selector-field.module';

const routes: Routes = [
    {
        component: PatternLibraryComponent,
        path: ''
    }
];

@NgModule({
    imports: [
        CommonModule,
        FormsModule,
        ReactiveFormsModule,
        DotDropdownModule,
        DotTextareaContentModule,
        ...NGFACES_MODULES,
        DotActionButtonModule,
        ListingDataTableModule,
        FieldValidationMessageModule,
        SiteSelectorModule,
        SiteSelectorFieldModule,
        SearchableDropDownModule,
        RouterModule.forChild(routes),
    ],
    declarations: [
        PatternLibraryComponent
    ]
})

export class PatternLibraryModule { }
