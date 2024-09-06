import { DragulaModule, DragulaService } from 'ng2-dragula';

import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { CheckboxModule } from 'primeng/checkbox';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { DialogModule } from 'primeng/dialog';
import { DropdownModule } from 'primeng/dropdown';
import { InputTextModule } from 'primeng/inputtext';
import { MultiSelectModule } from 'primeng/multiselect';
import { OverlayPanelModule } from 'primeng/overlaypanel';
import { RadioButtonModule } from 'primeng/radiobutton';
import { SplitButtonModule } from 'primeng/splitbutton';
import { TabViewModule } from 'primeng/tabview';
import { TooltipModule } from 'primeng/tooltip';

import { DotInlineEditModule } from '@components/_common/dot-inline-edit/dot-inline-edit.module';
import { DotPageSelectorModule } from '@components/_common/dot-page-selector/dot-page-selector.module';
import { SiteSelectorFieldModule } from '@components/_common/dot-site-selector-field/dot-site-selector-field.module';
import { DotTextareaContentModule } from '@components/_common/dot-textarea-content/dot-textarea-content.module';
import { DotWorkflowsActionsSelectorFieldModule } from '@components/_common/dot-workflows-actions-selector-field/dot-workflows-actions-selector-field.module';
import { DotWorkflowsSelectorFieldModule } from '@components/_common/dot-workflows-selector-field/dot-workflows-selector-field.module';
import { IFrameModule } from '@components/_common/iframe';
import { SearchableDropDownModule } from '@components/_common/searchable-dropdown';
import { DotBaseTypeSelectorModule } from '@components/dot-base-type-selector';
import { DotCopyLinkModule } from '@components/dot-copy-link/dot-copy-link.module';
import { DotFieldHelperModule } from '@components/dot-field-helper/dot-field-helper.module';
import { DotPortletBoxModule } from '@components/dot-portlet-base/components/dot-portlet-box/dot-portlet-box.module';
import { DotRelationshipTreeModule } from '@components/dot-relationship-tree/dot-relationship-tree.module';
import { DotSecondaryToolbarModule } from '@components/dot-secondary-toolbar';
import { DotMaxlengthModule } from '@directives/dot-maxlength/dot-maxlength.module';
import { DotMdIconSelectorModule } from '@dotcms/app/view/components/_common/dot-md-icon-selector/dot-md-icon-selector.module';
import { DotContentTypesInfoService, DotWorkflowService } from '@dotcms/data-access';
import {
    DotAddToBundleComponent,
    DotApiLinkComponent,
    DotAutofocusDirective,
    DotCopyButtonComponent,
    DotDialogModule,
    DotFieldRequiredDirective,
    DotFieldValidationMessageComponent,
    DotIconModule,
    DotMenuComponent,
    DotMessagePipe,
    DotSafeHtmlPipe
} from '@dotcms/ui';
import {
    ContentTypeFieldsAddRowModule,
    ContentTypeFieldsDropZoneComponent,
    ContentTypeFieldsPropertiesFormComponent,
    ContentTypeFieldsRowComponent,
    ContentTypeFieldsTabComponent,
    ContentTypesFieldDragabbleItemComponent,
    ContentTypesFieldsListComponent
} from '@portlets/shared/dot-content-types-edit/components/fields';
import {
    CategoriesPropertyComponent,
    CheckboxPropertyComponent,
    DataTypePropertyComponent,
    DefaultValuePropertyComponent,
    HintPropertyComponent,
    NamePropertyComponent,
    RegexCheckPropertyComponent,
    ValuesPropertyComponent
} from '@portlets/shared/dot-content-types-edit/components/fields/content-type-fields-properties-form/field-properties';
import { DotRelationshipsModule } from '@portlets/shared/dot-content-types-edit/components/fields/content-type-fields-properties-form/field-properties/dot-relationships-property/dot-relationships.module';
import { DynamicFieldPropertyDirective } from '@portlets/shared/dot-content-types-edit/components/fields/content-type-fields-properties-form/field-properties/dynamic-field-property-directive/dynamic-field-property.directive';
import { DotContentTypeFieldsVariablesModule } from '@portlets/shared/dot-content-types-edit/components/fields/dot-content-type-fields-variables/dot-content-type-fields-variables.module';
import {
    FieldDragDropService,
    FieldPropertyService,
    FieldService
} from '@portlets/shared/dot-content-types-edit/components/fields/service';
import { DotDirectivesModule } from '@shared/dot-directives.module';

import { DotBinarySettingsComponent } from './components/dot-binary-settings/dot-binary-settings.component';
import { DotBlockEditorSettingsComponent } from './components/dot-block-editor-settings/dot-block-editor-settings.component';
import { DotConvertToBlockInfoComponent } from './components/dot-convert-to-block-info/dot-convert-to-block-info.component';
import { DotConvertWysiwygToBlockComponent } from './components/dot-convert-wysiwyg-to-block/dot-convert-wysiwyg-to-block.component';
import { ContentTypesFormComponent } from './components/form/content-types-form.component';
import { ContentTypesLayoutComponent } from './components/layout/content-types-layout.component';
import { DotContentTypesEditRoutingModule } from './dot-content-types-edit-routing.module';
import { DotContentTypesEditComponent } from './dot-content-types-edit.component';

import { DotAddToMenuModule } from '../dot-content-types-listing/components/dot-add-to-menu/dot-add-to-menu.module';

@NgModule({
    declarations: [
        DotConvertToBlockInfoComponent,
        DotConvertWysiwygToBlockComponent,
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
        ValuesPropertyComponent,
        DotBlockEditorSettingsComponent
    ],
    exports: [DotContentTypesEditComponent],
    imports: [
        ButtonModule,
        CheckboxModule,
        ConfirmDialogModule,
        CommonModule,
        ContentTypeFieldsAddRowModule,
        DialogModule,
        DotAddToBundleComponent,
        DotApiLinkComponent,
        DotAutofocusDirective,
        DotBaseTypeSelectorModule,
        DotContentTypeFieldsVariablesModule,
        DotContentTypesEditRoutingModule,
        DotCopyLinkModule,
        DotDialogModule,
        DotDirectivesModule,
        DotSafeHtmlPipe,
        DotSecondaryToolbarModule,
        DotFieldHelperModule,
        DotFieldValidationMessageComponent,
        DotBinarySettingsComponent,
        TooltipModule,
        DotIconModule,
        DotMaxlengthModule,
        DotMenuComponent,
        DotPageSelectorModule,
        DotRelationshipsModule,
        DotTextareaContentModule,
        DotWorkflowsActionsSelectorFieldModule,
        DotWorkflowsSelectorFieldModule,
        DragulaModule,
        DropdownModule,
        FormsModule,
        IFrameModule,
        DotInlineEditModule,
        InputTextModule,
        MultiSelectModule,
        OverlayPanelModule,
        RadioButtonModule,
        ReactiveFormsModule,
        SearchableDropDownModule,
        SiteSelectorFieldModule,
        SplitButtonModule,
        TabViewModule,
        DotRelationshipTreeModule,
        DotPortletBoxModule,
        DotMdIconSelectorModule,
        DotAddToMenuModule,
        DotFieldRequiredDirective,
        DotCopyButtonComponent,
        OverlayPanelModule,
        DotMessagePipe
    ],
    providers: [
        DotContentTypesInfoService,
        DotWorkflowService,
        DragulaService,
        FieldDragDropService,
        FieldPropertyService,
        FieldService
    ],
    schemas: []
})
export class DotContentTypesEditModule {}
