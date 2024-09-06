import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { DropdownModule } from 'primeng/dropdown';
import { RadioButtonModule } from 'primeng/radiobutton';

import { SearchableDropDownModule } from '@components/_common/searchable-dropdown';
import {
    DotFieldRequiredDirective,
    DotFieldValidationMessageComponent,
    DotMessagePipe,
    DotSafeHtmlPipe
} from '@dotcms/ui';

import { DotCardinalitySelectorComponent } from './dot-cardinality-selector/dot-cardinality-selector.component';
import { DotEditRelationshipsComponent } from './dot-edit-relationship/dot-edit-relationships.component';
import { DotNewRelationshipsComponent } from './dot-new-relationships/dot-new-relationships.component';
import { DotRelationshipsPropertyComponent } from './dot-relationships-property.component';
import { DotEditContentTypeCacheService } from './services/dot-edit-content-type-cache.service';
import { DotRelationshipService } from './services/dot-relationship.service';

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
        DotFieldValidationMessageComponent,
        FormsModule,
        RadioButtonModule,
        SearchableDropDownModule,
        DotSafeHtmlPipe,
        DotMessagePipe,
        DotFieldRequiredDirective
    ],
    providers: [DotRelationshipService, DotEditContentTypeCacheService]
})
export class DotRelationshipsModule {}
