import { DragulaModule, DragulaService } from 'ng2-dragula';

import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';

import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { CheckboxModule } from 'primeng/checkbox';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { DialogModule } from 'primeng/dialog';
import { InputTextModule } from 'primeng/inputtext';
import { MultiSelectModule } from 'primeng/multiselect';
import { PopoverModule } from 'primeng/popover';
import { RadioButtonModule } from 'primeng/radiobutton';
import { SelectModule } from 'primeng/select';
import { SplitButtonModule } from 'primeng/splitbutton';
import { TabsModule } from 'primeng/tabs';
import { TooltipModule } from 'primeng/tooltip';

import {
    DotContentTypesInfoService,
    DotWorkflowService,
    DotWorkflowsActionsService
} from '@dotcms/data-access';
import {
    DotAddToBundleComponent,
    DotApiLinkComponent,
    DotAutofocusDirective,
    DotCopyButtonComponent,
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
import { ContentTypeFieldsAddRowComponent } from './components/fields/content-type-fields-add-row/content-type-fields-add-row.component';
import { ContentTypeFieldsDropZoneComponent } from './components/fields/content-type-fields-drop-zone';
import { ContentTypeFieldsPropertiesFormComponent } from './components/fields/content-type-fields-properties-form';
import { CategoriesPropertyComponent } from './components/fields/content-type-fields-properties-form/field-properties/categories-property';
import { CheckboxPropertyComponent } from './components/fields/content-type-fields-properties-form/field-properties/checkbox-property';
import { DataTypePropertyComponent } from './components/fields/content-type-fields-properties-form/field-properties/data-type-property';
import { DefaultValuePropertyComponent } from './components/fields/content-type-fields-properties-form/field-properties/default-value-property';
import { DotRelationshipsPropertyComponent } from './components/fields/content-type-fields-properties-form/field-properties/dot-relationships-property/dot-relationships-property.component';
import { DotEditContentTypeCacheService } from './components/fields/content-type-fields-properties-form/field-properties/dot-relationships-property/services/dot-edit-content-type-cache.service';
import { DynamicFieldPropertyDirective } from './components/fields/content-type-fields-properties-form/field-properties/dynamic-field-property-directive/dynamic-field-property.directive';
import { HintPropertyComponent } from './components/fields/content-type-fields-properties-form/field-properties/hint-property';
import { NamePropertyComponent } from './components/fields/content-type-fields-properties-form/field-properties/name-property';
import { NewRenderModePropertyComponent } from './components/fields/content-type-fields-properties-form/field-properties/new-render-mode-proptery';
import { RegexCheckPropertyComponent } from './components/fields/content-type-fields-properties-form/field-properties/regex-check-property';
import { ValuesPropertyComponent } from './components/fields/content-type-fields-properties-form/field-properties/values-property';
import { ContentTypeFieldsRowComponent } from './components/fields/content-type-fields-row';
import { ContentTypeFieldsTabComponent } from './components/fields/content-type-fields-tab';
import { DotContentTypeFieldsVariablesComponent } from './components/fields/dot-content-type-fields-variables/dot-content-type-fields-variables.component';
import { DotFieldVariablesService } from './components/fields/dot-content-type-fields-variables/services/dot-field-variables.service';
import { FieldDragDropService } from './components/fields/service/field-drag-drop.service';
import { FieldPropertyService } from './components/fields/service/field-properties.service';
import { FieldService } from './components/fields/service/field.service';
import { ContentTypesFormComponent } from './components/form/content-types-form.component';
import { ContentTypesLayoutComponent } from './components/layout/content-types-layout.component';
import { DotContentTypesEditComponent } from './dot-content-types-edit.component';
import { dotContentTypesEditRoutes } from './dot-content-types-edit.routes';

import { DotAddToMenuService } from '../../../api/services/add-to-menu/add-to-menu.service';
import { DotMenuService } from '../../../api/services/dot-menu.service';
import { DotDirectivesModule } from '../../../shared/dot-directives.module';
import { DotInlineEditComponent } from '../../../view/components/_common/dot-inline-edit/dot-inline-edit.component';
import { DotMdIconSelectorComponent } from '../../../view/components/_common/dot-md-icon-selector/dot-md-icon-selector.component';
import { DotPageSelectorComponent } from '../../../view/components/_common/dot-page-selector/dot-page-selector.component';
import { DotSiteSelectorFieldComponent } from '../../../view/components/_common/dot-site-selector-field/dot-site-selector-field.component';
import { DotTextareaContentComponent } from '../../../view/components/_common/dot-textarea-content/dot-textarea-content.component';
import { DotWorkflowsActionsSelectorFieldComponent } from '../../../view/components/_common/dot-workflows-actions-selector-field/dot-workflows-actions-selector-field.component';
import { DotWorkflowsActionsSelectorFieldService } from '../../../view/components/_common/dot-workflows-actions-selector-field/services/dot-workflows-actions-selector-field.service';
import { DotWorkflowsSelectorFieldComponent } from '../../../view/components/_common/dot-workflows-selector-field/dot-workflows-selector-field.component';
import { DotLoadingIndicatorComponent } from '../../../view/components/_common/iframe/dot-loading-indicator/dot-loading-indicator.component';
import { IframeComponent } from '../../../view/components/_common/iframe/iframe-component/iframe.component';
import { SearchableDropdownComponent } from '../../../view/components/_common/searchable-dropdown/component/searchable-dropdown.component';
import { DotBaseTypeSelectorComponent } from '../../../view/components/dot-base-type-selector/dot-base-type-selector.component';
import { DotCopyLinkComponent } from '../../../view/components/dot-copy-link/dot-copy-link.component';
import { DotFieldHelperComponent } from '../../../view/components/dot-field-helper/dot-field-helper.component';
import { DotNavigationService } from '../../../view/components/dot-navigation/services/dot-navigation.service';
import { DotPortletBoxComponent } from '../../../view/components/dot-portlet-base/components/dot-portlet-box/dot-portlet-box.component';
import { DotRelationshipTreeComponent } from '../../../view/components/dot-relationship-tree/dot-relationship-tree.component';
import { DotSecondaryToolbarComponent } from '../../../view/components/dot-secondary-toolbar/dot-secondary-toolbar.component';
import { DotMaxlengthDirective } from '../../../view/directives/dot-maxlength/dot-maxlength.directive';
import { DotAddToMenuComponent } from '../dot-content-types-listing/components/dot-add-to-menu/dot-add-to-menu.component';
import { DotFeatureFlagResolver } from '../resolvers/dot-feature-flag-resolver.service';

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
        DataTypePropertyComponent,
        DefaultValuePropertyComponent,
        DotContentTypesEditComponent,
        DynamicFieldPropertyDirective,
        HintPropertyComponent,
        NamePropertyComponent,
        RegexCheckPropertyComponent,
        ValuesPropertyComponent,
        DotBlockEditorSettingsComponent,
        NewRenderModePropertyComponent
    ],
    exports: [DotContentTypesEditComponent],
    imports: [
        ContentTypesLayoutComponent,
        ContentTypesFormComponent,
        ButtonModule,
        CardModule,
        CheckboxModule,
        ConfirmDialogModule,
        CommonModule,
        ContentTypeFieldsAddRowComponent,
        DialogModule,
        DotAddToBundleComponent,
        DotApiLinkComponent,
        DotAutofocusDirective,
        DotBaseTypeSelectorComponent,
        DotContentTypeFieldsVariablesComponent,
        RouterModule.forChild(dotContentTypesEditRoutes),
        DotCopyLinkComponent,
        DotDirectivesModule,
        DotSafeHtmlPipe,
        DotSecondaryToolbarComponent,
        DotFieldHelperComponent,
        DotFieldValidationMessageComponent,
        DotBinarySettingsComponent,
        TooltipModule,
        DotIconComponent,
        DotMaxlengthDirective,
        DotMenuComponent,
        DotPageSelectorComponent,
        DotRelationshipsPropertyComponent,
        DotTextareaContentComponent,
        DotWorkflowsActionsSelectorFieldComponent,
        DotWorkflowsSelectorFieldComponent,
        DragulaModule,
        SelectModule,
        FormsModule,
        IframeComponent,
        DotInlineEditComponent,
        DotLoadingIndicatorComponent,
        InputTextModule,
        MultiSelectModule,
        PopoverModule,
        RadioButtonModule,
        ReactiveFormsModule,
        SearchableDropdownComponent,
        DotSiteSelectorFieldComponent,
        SplitButtonModule,
        TabsModule,
        DotRelationshipTreeComponent,
        DotPortletBoxComponent,
        DotMdIconSelectorComponent,
        DotAddToMenuComponent,
        DotFieldRequiredDirective,
        DotCopyButtonComponent,
        PopoverModule,
        DotMessagePipe
    ],
    providers: [
        DotContentTypesInfoService,
        DotWorkflowService,
        DragulaService,
        FieldDragDropService,
        FieldPropertyService,
        FieldService,
        DotAddToMenuService,
        DotMenuService,
        DotNavigationService,
        DotWorkflowsActionsService,
        DotWorkflowsActionsSelectorFieldService,
        DotFeatureFlagResolver,
        DotEditContentTypeCacheService,
        DotFieldVariablesService
    ],
    schemas: []
})
export class DotContentTypesEditModule {}
