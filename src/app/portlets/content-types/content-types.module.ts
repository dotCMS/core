import { CommonModule } from '@angular/common';
import { ContentTypesEditComponent } from './edit';
import { ContentTypesFormComponent } from './form';
import { ContentTypesInfoService } from '@services/content-types-info';
import { ContentTypesLayoutComponent } from './layout';
import { ContentTypesPortletComponent } from './main';
import { ContentTypesRoutingModule } from './content-types-routing.module';
import { DotFieldValidationMessageModule } from '@components/_common/dot-field-validation-message/dot-file-validation-message.module';
import { FormatDateService } from '@services/format-date-service';
import { DotIconButtonTooltipModule } from '@components/_common/dot-icon-button-tooltip/dot-icon-button-tooltip.module';
import { ListingDataTableModule } from '@components/listing-data-table/listing-data-table.module';
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
import { PushPublishContentTypesDialogModule } from '@components/_common/push-publish-dialog/push-publish-dialog.module';
import { SearchableDropDownModule } from '@components/_common/searchable-dropdown';
import { DotAutofocusModule } from '@directives/dot-autofocus/dot-autofocus.module';
import { DotContentTypeFieldsVariablesModule } from './fields/dot-content-type-fields-variables/dot-content-type-fields-variables.module';
import { DotRelationshipsModule } from './fields/content-type-fields-properties-form/field-properties/dot-relationships-property/dot-relationships.module';
import { DotApiLinkModule } from '@components/dot-api-link/dot-api-link.module';
import { DotCopyButtonModule } from '@components/dot-copy-button/dot-copy-button.module';
import { DotFieldHelperModule } from '@components/dot-field-helper/dot-field-helper.module';
import { DotMaxlengthModule } from '@directives/dot-maxlength/dot-maxlength.module';

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
        ListingDataTableModule,
        MdInputTextModule,
        MultiSelectModule,
        OverlayPanelModule,
        PushPublishContentTypesDialogModule,
        RadioButtonModule,
        ReactiveFormsModule,
        SearchableDropDownModule,
        SiteSelectorFieldModule,
        SplitButtonModule,
        TabViewModule,
        DotFieldHelperModule,
        DotMaxlengthModule,
    ],
    providers: [
        ContentTypeEditResolver,
        ContentTypesInfoService,
        DotWorkflowService,
        DragulaService,
        FieldDragDropService,
        FieldPropertyService,
        FieldService,
        FormatDateService
    ]
})
export class ContentTypesModule {}
