import { CommonModule } from '@angular/common';
import { ContentTypesEditComponent } from './edit';
import { ContentTypesFormComponent } from './form';
import { ContentTypesInfoService } from '../../api/services/content-types-info';
import { ContentTypesLayoutComponent } from './layout';
import { ContentTypesPortletComponent } from './main';
import { ContentTypesRoutingModule } from './content-types-routing.module';
import { FieldValidationMessageModule } from '../../view/components/_common/field-validation-message/file-validation-message.module';
import { FormatDateService } from '../../api/services/format-date-service';
import { IconButtonTooltipModule } from '../../view/components/_common/icon-button-tooltip/icon-button-tooltip.module';
import { ListingDataTableModule } from '../../view/components/listing-data-table/listing-data-table.module';
import { NgModule } from '@angular/core';
import { ReactiveFormsModule, FormsModule } from '@angular/forms';
import { SiteSelectorFieldModule } from '../../view/components/_common/site-selector-field/site-selector-field.module';
import { DragulaModule } from 'ng2-dragula';
import { DragulaService } from 'ng2-dragula';
import { FieldService, FieldDragDropService, FieldPropertyService } from './fields/service';
import { ContentTypeFieldsAddRowModule } from './fields/content-type-fields-add-row';
import { ContentTypeEditResolver } from './edit/content-types-edit-resolver.service';

import {
    ContentTypeFieldsDropZoneComponent,
    ContentTypeFieldsPropertiesFormComponent,
    ContentTypeFieldsRowComponent,
    ContentTypeFieldsRowListComponent,
    ContentTypesFieldDragabbleItemComponent,
    ContentTypesFieldsListComponent
} from './fields';
import {
    ButtonModule,
    ConfirmDialogModule,
    DialogModule,
    DropdownModule,
    InputTextModule,
    OverlayPanelModule,
    SplitButtonModule,
    RadioButtonModule,
    CheckboxModule,
    TabViewModule,
    MultiSelectModule
} from 'primeng/primeng';
import {
    NamePropertyComponent,
    CheckboxPropertyComponent,
    CategoriesPropertyComponent,
    DataTypePropertyComponent,
    HintPropertyComponent,
    DefaultValuePropertyComponent,
    RegexCheckPropertyComponent,
    ValuesPropertyComponent
} from './fields/content-type-fields-properties-form/field-properties';
import {
    DynamicFieldPropertyDirective
} from './fields/content-type-fields-properties-form/field-properties/dynamic-field-property-directive/dynamic-field-property.directive';
import { IFrameModule } from '../../view/components/_common/iframe';
import { DotTextareaContentModule } from '../../view/components/_common/dot-textarea-content/dot-textarea-content.module';
import { SearchableDropDownModule } from '../../view/components/_common/searchable-dropdown';
import { DotWorkflowService } from '../../api/services/dot-workflow/dot-workflow.service';
import { PushPublishContentTypesDialogModule } from '../../view/components/_common/push-publish-dialog/push-publish-dialog.module';
import { DotAddToBundleModule } from '../../view/components/_common/dot-add-to-bundle/dot-add-to-bundle.module';
import { DotDirectivesModule } from '../../shared/dot-directives.module';
import { DotWorkflowsSelectorFieldModule } from '../../view/components/_common/dot-workflows-selector-field/dot-workflows-selector-field.module';
import { DotPageSelectorModule } from '../../view/components/_common/dot-page-selector/dot-page-selector.module';
import { DotBaseTypeSelectorModule } from '../../view/components/dot-base-type-selector/dot-base-type-selector.module';

@NgModule({
    declarations: [
        CategoriesPropertyComponent,
        CheckboxPropertyComponent,
        ContentTypeFieldsDropZoneComponent,
        ContentTypeFieldsPropertiesFormComponent,
        ContentTypeFieldsRowComponent,
        ContentTypeFieldsRowListComponent,
        ContentTypesEditComponent,
        ContentTypesFieldDragabbleItemComponent,
        ContentTypesFieldsListComponent,
        ContentTypesFormComponent,
        ContentTypesLayoutComponent,
        ContentTypesPortletComponent,
        DataTypePropertyComponent,
        DefaultValuePropertyComponent,
        DynamicFieldPropertyDirective,
        HintPropertyComponent,
        NamePropertyComponent,
        RegexCheckPropertyComponent,
        ValuesPropertyComponent
    ],
    entryComponents: [
        NamePropertyComponent,
        CheckboxPropertyComponent,
        CategoriesPropertyComponent,
        DataTypePropertyComponent,
        DefaultValuePropertyComponent,
        HintPropertyComponent,
        RegexCheckPropertyComponent,
        ValuesPropertyComponent
    ],
    exports: [ContentTypesPortletComponent],
    imports: [
        ButtonModule,
        CheckboxModule,
        CommonModule,
        ConfirmDialogModule,
        ContentTypesRoutingModule,
        DialogModule,
        DragulaModule,
        DropdownModule,
        FieldValidationMessageModule,
        FormsModule,
        IFrameModule,
        IconButtonTooltipModule,
        InputTextModule,
        ListingDataTableModule,
        OverlayPanelModule,
        PushPublishContentTypesDialogModule,
        RadioButtonModule,
        ReactiveFormsModule,
        SearchableDropDownModule,
        SiteSelectorFieldModule,
        ContentTypeFieldsAddRowModule,
        SplitButtonModule,
        TabViewModule,
        DotTextareaContentModule,
        MultiSelectModule,
        DotAddToBundleModule,
        DotDirectivesModule,
        DotWorkflowsSelectorFieldModule,
        DotPageSelectorModule,
        DotBaseTypeSelectorModule
    ],
    providers: [
        ContentTypesInfoService,
        DragulaService,
        FieldDragDropService,
        FieldPropertyService,
        FieldService,
        FormatDateService,
        ContentTypeEditResolver,
        DotWorkflowService
    ]
})
export class ContentTypesModule {}
