import { DotRelationshipsPropertyComponent } from './dot-relationships-property.component';
import { DotNewRelationshipsComponent } from './new-relationships.component.ts/dot-new-relationships.component';
import { DotCardinalitySelectorComponent } from './dot-cardinality-selector/dot-cardinality-selector.component';
import { DotEditRelationshipsComponent } from './edit-relationship.component.ts/dot-edit-relationships.component';
import { DotRelationshipService } from './services/dot-relationship.service';
import { DotEditContentTypeCacheService } from './services/dot-edit-content-type-cache.service';
import { CommonModule } from '@angular/common';
import { DropdownModule, RadioButtonModule } from 'primeng/primeng';
import { NgModule } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { FieldValidationMessageModule } from '@components/_common/field-validation-message/file-validation-message.module';
import { SearchableDropDownModule } from '@components/_common/searchable-dropdown';

@NgModule({
    declarations: [
        DotRelationshipsPropertyComponent,
        DotNewRelationshipsComponent,
        DotCardinalitySelectorComponent,
        DotEditRelationshipsComponent
    ],
    entryComponents: [
        DotRelationshipsPropertyComponent
    ],
    exports: [DotRelationshipsPropertyComponent],
    imports: [
        CommonModule,
        DropdownModule,
        FieldValidationMessageModule,
        FormsModule,
        RadioButtonModule,
        SearchableDropDownModule
    ],
    providers: [
        DotRelationshipService,
        DotEditContentTypeCacheService
    ]
})
export class DotRelationshipsModule {}
