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

import { DotContentTypesInfoService, DotWorkflowService } from '@dotcms/data-access';
import {
    DotAddToBundleComponent,
    DotApiLinkComponent,
    DotAutofocusDirective,
    DotCopyButtonComponent,
    DotDialogComponent,
    DotFieldRequiredDirective,
    DotFieldValidationMessageComponent,
    DotIconComponent,
    DotMenuComponent,
    DotMessagePipe,
    DotSafeHtmlPipe
} from '@dotcms/ui';

import { DotBinarySettingsComponent } from './components/dot-binary-settings/dot-binary-settings.component';
import { DotBlockEditorSettingsComponent } from './components/dot-block-editor-settings/dot-block-editor-settings.component';
import { DotConvertToBlockInfoComponent } from './components/dot-convert-to-block-info/dot-convert-to-block-info.component';
import { DotConvertWysiwygToBlockComponent } from './components/dot-convert-wysiwyg-to-block/dot-convert-wysiwyg-to-block.component';
import { ContentTypesFieldDragabbleItemComponent } from './components/fields/content-type-field-dragabble-item';
import { ContentTypeFieldsAddRowModule } from './components/fields/content-type-fields-add-row/content-type-fields-add-row.module';
import { ContentTypeFieldsDropZoneComponent } from './components/fields/content-type-fields-drop-zone';
import { ContentTypeFieldsPropertiesFormComponent } from './components/fields/content-type-fields-properties-form';
import { CategoriesPropertyComponent } from './components/fields/content-type-fields-properties-form/field-properties/categories-property';
import { CheckboxPropertyComponent } from './components/fields/content-type-fields-properties-form/field-properties/checkbox-property';
import { DataTypePropertyComponent } from './components/fields/content-type-fields-properties-form/field-properties/data-type-property';
import { DefaultValuePropertyComponent } from './components/fields/content-type-fields-properties-form/field-properties/default-value-property';
import { DotRelationshipsModule } from './components/fields/content-type-fields-properties-form/field-properties/dot-relationships-property/dot-relationships.module';
import { DynamicFieldPropertyDirective } from './components/fields/content-type-fields-properties-form/field-properties/dynamic-field-property-directive/dynamic-field-property.directive';
import { HintPropertyComponent } from './components/fields/content-type-fields-properties-form/field-properties/hint-property';
import { NamePropertyComponent } from './components/fields/content-type-fields-properties-form/field-properties/name-property';
import { RegexCheckPropertyComponent } from './components/fields/content-type-fields-properties-form/field-properties/regex-check-property';
import { ValuesPropertyComponent } from './components/fields/content-type-fields-properties-form/field-properties/values-property';
import { ContentTypeFieldsRowComponent } from './components/fields/content-type-fields-row';
import { ContentTypeFieldsTabComponent } from './components/fields/content-type-fields-tab';
import { DotContentTypeFieldsVariablesModule } from './components/fields/dot-content-type-fields-variables/dot-content-type-fields-variables.module';
import { FieldDragDropService } from './components/fields/service/field-drag-drop.service';
import { FieldPropertyService } from './components/fields/service/field-properties.service';
import { FieldService } from './components/fields/service/field.service';
import { ContentTypesFormComponent } from './components/form/content-types-form.component';
import { ContentTypesLayoutComponent } from './components/layout/content-types-layout.component';
import { DotContentTypesEditRoutingModule } from './dot-content-types-edit-routing.module';
import { DotContentTypesEditComponent } from './dot-content-types-edit.component';

import { DotDirectivesModule } from '../../../shared/dot-directives.module';
import { DotInlineEditModule } from '../../../view/components/_common/dot-inline-edit/dot-inline-edit.module';
import { DotMdIconSelectorModule } from '../../../view/components/_common/dot-md-icon-selector/dot-md-icon-selector.module';
import { DotPageSelectorModule } from '../../../view/components/_common/dot-page-selector/dot-page-selector.module';
import { SiteSelectorFieldModule } from '../../../view/components/_common/dot-site-selector-field/dot-site-selector-field.module';
import { DotTextareaContentComponent } from '../../../view/components/_common/dot-textarea-content/dot-textarea-content.component';
import { DotWorkflowsActionsSelectorFieldModule } from '../../../view/components/_common/dot-workflows-actions-selector-field/dot-workflows-actions-selector-field.module';
import { DotWorkflowsSelectorFieldModule } from '../../../view/components/_common/dot-workflows-selector-field/dot-workflows-selector-field.module';
import { DotLoadingIndicatorModule } from '../../../view/components/_common/iframe/dot-loading-indicator/dot-loading-indicator.module';
import { IframeComponent } from '../../../view/components/_common/iframe/iframe-component/iframe.component';
import { SearchableDropdownComponent } from '../../../view/components/_common/searchable-dropdown/component/searchable-dropdown.component';
import { DotBaseTypeSelectorModule } from '../../../view/components/dot-base-type-selector/dot-base-type-selector.module';
import { DotCopyLinkModule } from '../../../view/components/dot-copy-link/dot-copy-link.module';
import { DotFieldHelperModule } from '../../../view/components/dot-field-helper/dot-field-helper.module';
import { DotPortletBoxComponent } from '../../../view/components/dot-portlet-base/components/dot-portlet-box/dot-portlet-box.component';
import { DotRelationshipTreeModule } from '../../../view/components/dot-relationship-tree/dot-relationship-tree.module';
import { DotSecondaryToolbarComponent } from '../../../view/components/dot-secondary-toolbar/dot-secondary-toolbar.component';
import { DotMaxlengthModule } from '../../../view/directives/dot-maxlength/dot-maxlength.module';
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
        ContentTypesFormComponent,
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
        ContentTypesLayoutComponent,
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
        DotDialogComponent,
        DotDirectivesModule,
        DotSafeHtmlPipe,
        DotSecondaryToolbarComponent,
        DotFieldHelperModule,
        DotFieldValidationMessageComponent,
        DotBinarySettingsComponent,
        TooltipModule,
        DotIconComponent,
        DotMaxlengthModule,
        DotMenuComponent,
        DotPageSelectorModule,
        DotRelationshipsModule,
        DotTextareaContentComponent,
        DotWorkflowsActionsSelectorFieldModule,
        DotWorkflowsSelectorFieldModule,
        DragulaModule,
        DropdownModule,
        FormsModule,
        IframeComponent,
        DotInlineEditModule,
        DotLoadingIndicatorModule,
        InputTextModule,
        MultiSelectModule,
        OverlayPanelModule,
        RadioButtonModule,
        ReactiveFormsModule,
        SearchableDropdownComponent,
        SiteSelectorFieldModule,
        SplitButtonModule,
        TabViewModule,
        DotRelationshipTreeModule,
        DotPortletBoxComponent,
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
