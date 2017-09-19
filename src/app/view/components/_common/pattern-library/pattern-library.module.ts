import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Routes } from '@angular/router';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { PatternLibraryComponent } from './pattern-library.component';
import { NGFACES_MODULES } from './../../../../modules';
// CUSTOM MDOULES
import { ActionButtonModule } from '../action-button/action-button.module';
import { ListingDataTableModule} from '../../listing-data-table/listing-data-table.module';
import { FieldValidationMessageModule} from '../field-validation-message/file-validation-message.module';
import { SiteSelectorModule} from '../site-selector/site-selector.module';
import { SearchableDropDownModule } from '../searchable-dropdown/searchable-dropdown.module';
import { DotTextareaContentModule } from '../dot-textarea-content/dot-textarea-content.module';

import { DotDropdownModule} from '../dropdown-component/dot-dropdown.module';

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
        ActionButtonModule,
        ListingDataTableModule,
        FieldValidationMessageModule,
        SiteSelectorModule,
        SearchableDropDownModule,
        RouterModule.forChild(routes),
    ],
    declarations: [
        PatternLibraryComponent
    ]
})

export class PatternLibraryModule { }
