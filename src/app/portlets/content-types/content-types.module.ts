import { CommonModule } from '@angular/common';
import { ContentTypesEditComponent } from './edit';
import { ContentTypesFormComponent } from './form';
import { DotContentTypesInfoService } from '@services/dot-content-types-info';
import { ContentTypesLayoutComponent } from './layout';
import { ContentTypesRoutingModule } from './content-types-routing.module';
import { DotFieldValidationMessageModule } from '@components/_common/dot-field-validation-message/dot-file-validation-message.module';
import { FormatDateService } from '@services/format-date-service';
import { DotIconButtonTooltipModule } from '@components/_common/dot-icon-button-tooltip/dot-icon-button-tooltip.module';
import { DotListingDataTableModule } from '@components/dot-listing-data-table/dot-listing-data-table.module';
import { NgModule } from '@angular/core';
import { ReactiveFormsModule, FormsModule } from '@angular/forms';
import { SiteSelectorFieldModule } from '@components/_common/dot-site-selector-field/dot-site-selector-field.module';
import { DragulaModule } from 'ng2-dragula';
import { DragulaService } from 'ng2-dragula';
import { FieldService, FieldDragDropService, FieldPropertyService } from './fields/service';
import { ContentTypeFieldsAddRowModule } from './fields/content-type-fields-add-row';
import { ContentTypeEditResolver } from './edit/content-types-edit-resolver.service';

import {
    ContentTypeFieldsDropZoneComponent,
    ContentTypeFieldsPropertiesFormComponent,
    ContentTypeFieldsRowComponent,
    ContentTypeFieldsTabComponent,
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

import { DotAddToBundleModule } from '@components/_common/dot-add-to-bundle/dot-add-to-bundle.module';
import { DotBaseTypeSelectorModule } from '@components/dot-base-type-selector/dot-base-type-selector.module';
import { DotDialogModule } from '../../view/components/dot-dialog/dot-dialog.module';
import { DotDirectivesModule } from '@shared/dot-directives.module';
import { DotIconButtonModule } from '@components/_common/dot-icon-button/dot-icon-button.module';
import { DotMenuModule } from '@components/_common/dot-menu/dot-menu.module';
import { DotIconModule } from '@components/_common/dot-icon/dot-icon.module';
import { DotPageSelectorModule } from '@components/_common/dot-page-selector/dot-page-selector.module';
import { DotTextareaContentModule } from '@components/_common/dot-textarea-content/dot-textarea-content.module';
import { DotWorkflowService } from '@services/dot-workflow/dot-workflow.service';
import { DotWorkflowsSelectorFieldModule } from '@components/_common/dot-workflows-selector-field/dot-workflows-selector-field.module';
// tslint:disable-next-line:max-line-length
import { DynamicFieldPropertyDirective } from './fields/content-type-fields-properties-form/field-properties/dynamic-field-property-directive/dynamic-field-property.directive';
import { IFrameModule } from '@components/_common/iframe';
import { MdInputTextModule } from '@directives/md-inputtext/md-input-text.module';
import { DotPushPublishContentTypesDialogModule } from '@components/_common/dot-push-publish-dialog/dot-push-publish-dialog.module';
import { SearchableDropDownModule } from '@components/_common/searchable-dropdown';
import { DotAutofocusModule } from '@directives/dot-autofocus/dot-autofocus.module';
import { DotContentTypeFieldsVariablesModule } from './fields/dot-content-type-fields-variables/dot-content-type-fields-variables.module';
import { DotRelationshipsModule } from './fields/content-type-fields-properties-form/field-properties/dot-relationships-property/dot-relationships.module';
import { DotApiLinkModule } from '@components/dot-api-link/dot-api-link.module';
import { DotCopyButtonModule } from '@components/dot-copy-button/dot-copy-button.module';
import { DotFieldHelperModule } from '@components/dot-field-helper/dot-field-helper.module';
import { DotMaxlengthModule } from '@directives/dot-maxlength/dot-maxlength.module';
import { DotWorkflowsActionsSelectorFieldModule } from '@components/_common/dot-workflows-actions-selector-field/dot-workflows-actions-selector-field.module';
import { DotEditToolbarModule } from '@portlets/dot-edit-page/main/dot-edit-toolbar/dot-edit-toolbar.module';

@NgModule({
    declarations: [
        CategoriesPropertyComponent,
        CheckboxPropertyComponent,
        ContentTypeFieldsDropZoneComponent,
        ContentTypeFieldsPropertiesFormComponent,
        ContentTypeFieldsRowComponent,
        ContentTypeFieldsTabComponent,
        ContentTypesEditComponent,
        ContentTypesFieldDragabbleItemComponent,
        ContentTypesFieldsListComponent,
        ContentTypesFormComponent,
        ContentTypesLayoutComponent,
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
    imports: [
        ButtonModule,
        CheckboxModule,
        CommonModule,
        ConfirmDialogModule,
        ContentTypeFieldsAddRowModule,
        ContentTypesRoutingModule,
        DialogModule,
        DotAddToBundleModule,
        DotApiLinkModule,
        DotAutofocusModule,
        DotBaseTypeSelectorModule,
        DotContentTypeFieldsVariablesModule,
        DotCopyButtonModule,
        DotDialogModule,
        DotDirectivesModule,
        DotIconButtonModule,
        DotIconButtonTooltipModule,
        DotIconModule,
        DotMenuModule,
        DotPageSelectorModule,
        DotRelationshipsModule,
        DotTextareaContentModule,
        DotWorkflowsSelectorFieldModule,
        DragulaModule,
        DropdownModule,
        DotFieldValidationMessageModule,
        FormsModule,
        IFrameModule,
        InputTextModule,
        DotListingDataTableModule,
        MdInputTextModule,
        MultiSelectModule,
        OverlayPanelModule,
        DotPushPublishContentTypesDialogModule,
        RadioButtonModule,
        ReactiveFormsModule,
        SearchableDropDownModule,
        SiteSelectorFieldModule,
        SplitButtonModule,
        TabViewModule,
        DotFieldHelperModule,
        DotMaxlengthModule,
        DotWorkflowsActionsSelectorFieldModule,
        DotEditToolbarModule
    ],
    providers: [
        ContentTypeEditResolver,
        DotContentTypesInfoService,
        DotWorkflowService,
        DragulaService,
        FieldDragDropService,
        FieldPropertyService,
        FieldService,
        FormatDateService
    ]
})
export class ContentTypesModule {}
