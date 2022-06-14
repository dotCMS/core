import { DotRelationshipsPropertyComponent } from './dot-relationships-property.component';
import { DotCardinalitySelectorComponent } from './dot-cardinality-selector/dot-cardinality-selector.component';
import { DotRelationshipService } from './services/dot-relationship.service';
import { DotEditContentTypeCacheService } from './services/dot-edit-content-type-cache.service';
import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { DotFieldValidationMessageModule } from '@components/_common/dot-field-validation-message/dot-file-validation-message.module';
import { SearchableDropDownModule } from '@components/_common/searchable-dropdown';
import { DotNewRelationshipsComponent } from './dot-new-relationships/dot-new-relationships.component';
import { DotEditRelationshipsComponent } from './dot-edit-relationship/dot-edit-relationships.component';
import { DotPipesModule } from '@pipes/dot-pipes.module';
import { DropdownModule } from 'primeng/dropdown';
import { RadioButtonModule } from 'primeng/radiobutton';

@NgModule({
    declarations: [
        DotRelationshipsPropertyComponent,
        DotNewRelationshipsComponent,
        DotCardinalitySelectorComponent,
        DotEditRelationshipsComponent
    ],
    exports: [DotRelationshipsPropertyComponent],
    imports: [
        CommonModule,
        DropdownModule,
        DotFieldValidationMessageModule,
        FormsModule,
        RadioButtonModule,
        SearchableDropDownModule,
        DotPipesModule
    ],
    providers: [DotRelationshipService, DotEditContentTypeCacheService]
})
export class DotRelationshipsModule {}
