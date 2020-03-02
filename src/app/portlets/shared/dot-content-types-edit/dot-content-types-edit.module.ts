import {
    ButtonModule,
    TabViewModule,
    SplitButtonModule,
    DropdownModule,
    InputTextModule,
    CheckboxModule,
    RadioButtonModule,
    ConfirmDialogModule,
    DialogModule,
    OverlayPanelModule,
    MultiSelectModule
} from 'primeng/primeng';
import {
    CheckboxPropertyComponent,
    NamePropertyComponent,
    CategoriesPropertyComponent,
    DataTypePropertyComponent,
    DefaultValuePropertyComponent,
    HintPropertyComponent,
    RegexCheckPropertyComponent,
    ValuesPropertyComponent
} from '@portlets/shared/dot-content-types-edit/components/fields/content-type-fields-properties-form/field-properties';
import {
    ContentTypeFieldsDropZoneComponent,
    ContentTypesFieldsListComponent,
    ContentTypeFieldsRowComponent,
    ContentTypeFieldsTabComponent,
    ContentTypeFieldsAddRowModule,
    ContentTypeFieldsPropertiesFormComponent,
    ContentTypesFieldDragabbleItemComponent
} from '@portlets/shared/dot-content-types-edit/components/fields';
import {
    FieldDragDropService,
    FieldPropertyService,
    FieldService
} from '@portlets/shared/dot-content-types-edit/components/fields/service';
import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DotContentTypesEditComponent } from './dot-content-types-edit.component';
import { ContentTypesLayoutComponent } from './components/layout/content-types-layout.component';
import { ContentTypesFormComponent } from './components/form/content-types-form.component';
import { DotApiLinkModule } from '@components/dot-api-link/dot-api-link.module';
import { DotCopyButtonModule } from '@components/dot-copy-button/dot-copy-button.module';
import { DotMenuModule } from '@components/_common/dot-menu/dot-menu.module';
import { DotDialogModule } from '@components/dot-dialog/dot-dialog.module';
import { IFrameModule } from '@components/_common/iframe';
import { DotFieldValidationMessageModule } from '@components/_common/dot-field-validation-message/dot-file-validation-message.module';
import { SiteSelectorFieldModule } from '@components/_common/dot-site-selector-field/dot-site-selector-field.module';
import { DotWorkflowsSelectorFieldModule } from '@components/_common/dot-workflows-selector-field/dot-workflows-selector-field.module';
import { DotWorkflowsActionsSelectorFieldModule } from '@components/_common/dot-workflows-actions-selector-field/dot-workflows-actions-selector-field.module';
import { DotPageSelectorModule } from '@components/_common/dot-page-selector/dot-page-selector.module';
import { DotFieldHelperModule } from '@components/dot-field-helper/dot-field-helper.module';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { MdInputTextModule } from '@directives/md-inputtext/md-input-text.module';
import { DotContentTypeFieldsVariablesModule } from '@portlets/shared/dot-content-types-edit/components/fields/dot-content-type-fields-variables/dot-content-type-fields-variables.module';
import { DragulaModule, DragulaService } from 'ng2-dragula';
import { DotIconButtonTooltipModule } from '@components/_common/dot-icon-button-tooltip/dot-icon-button-tooltip.module';
import { DotIconModule } from '@components/_common/dot-icon/dot-icon.module';
import { DynamicFieldPropertyDirective } from '@portlets/shared/dot-content-types-edit/components/fields/content-type-fields-properties-form/field-properties/dynamic-field-property-directive/dynamic-field-property.directive';
import { DotContentTypesEditRoutingModule } from './dot-content-types-edit-routing.module';
import { SearchableDropDownModule } from '@components/_common/searchable-dropdown';
import { DotTextareaContentModule } from '@components/_common/dot-textarea-content/dot-textarea-content.module';
import { DotAddToBundleModule } from '@components/_common/dot-add-to-bundle';
import { DotBaseTypeSelectorModule } from '@components/dot-base-type-selector';
import { DotDirectivesModule } from '@shared/dot-directives.module';
import { DotIconButtonModule } from '@components/_common/dot-icon-button/dot-icon-button.module';
import { DotRelationshipsModule } from '@portlets/shared/dot-content-types-edit/components/fields/content-type-fields-properties-form/field-properties/dot-relationships-property/dot-relationships.module';
import { DotMaxlengthModule } from '@directives/dot-maxlength/dot-maxlength.module';
import { DotAutofocusModule } from '@directives/dot-autofocus/dot-autofocus.module';
import { DotContentTypesInfoService } from '@services/dot-content-types-info';
import { DotWorkflowService } from '@services/dot-workflow/dot-workflow.service';
import { FormatDateService } from '@services/format-date-service';
import { DotSecondaryToolbarModule } from '@components/dot-secondary-toolbar';

@NgModule({
    declarations: [
        CategoriesPropertyComponent,
        CheckboxPropertyComponent,
        ContentTypesFieldDragabbleItemComponent,
        ContentTypeFieldsDropZoneComponent,
        ContentTypeFieldsPropertiesFormComponent,
        ContentTypeFieldsRowComponent,
        ContentTypeFieldsTabComponent,
        ContentTypesFieldsListComponent,
        ContentTypesFormComponent,
        ContentTypesLayoutComponent,
        DataTypePropertyComponent,
        DefaultValuePropertyComponent,
        DotContentTypesEditComponent,
        DynamicFieldPropertyDirective,
        HintPropertyComponent,
        NamePropertyComponent,
        RegexCheckPropertyComponent,
        ValuesPropertyComponent
    ],
    exports: [DotContentTypesEditComponent],
    entryComponents: [
        CategoriesPropertyComponent,
        CheckboxPropertyComponent,
        DataTypePropertyComponent,
        DefaultValuePropertyComponent,
        HintPropertyComponent,
        NamePropertyComponent,
        RegexCheckPropertyComponent,
        ValuesPropertyComponent
    ],
    imports: [
        ButtonModule,
        CheckboxModule,
        ConfirmDialogModule,
        CommonModule,
        ContentTypeFieldsAddRowModule,
        DialogModule,
        DotAddToBundleModule,
        DotApiLinkModule,
        DotAutofocusModule,
        DotBaseTypeSelectorModule,
        DotContentTypeFieldsVariablesModule,
        DotContentTypesEditRoutingModule,
        DotCopyButtonModule,
        DotDialogModule,
        DotDirectivesModule,
        DotSecondaryToolbarModule,
        DotFieldHelperModule,
        DotFieldValidationMessageModule,
        DotIconButtonModule,
        DotIconButtonTooltipModule,
        DotIconModule,
        DotMaxlengthModule,
        DotMenuModule,
        DotPageSelectorModule,
        DotRelationshipsModule,
        DotTextareaContentModule,
        DotWorkflowsActionsSelectorFieldModule,
        DotWorkflowsSelectorFieldModule,
        DragulaModule,
        DropdownModule,
        FormsModule,
        IFrameModule,
        InputTextModule,
        MdInputTextModule,
        MultiSelectModule,
        OverlayPanelModule,
        RadioButtonModule,
        ReactiveFormsModule,
        SearchableDropDownModule,
        SiteSelectorFieldModule,
        SplitButtonModule,
        TabViewModule
    ],
    providers: [
        DotContentTypesInfoService,
        DotWorkflowService,
        DragulaService,
        FieldDragDropService,
        FieldPropertyService,
        FieldService,
        FormatDateService
    ]
})
export class DotContentTypesEditModule {}
