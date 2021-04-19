import { DotLargeMessageDisplayModule } from './view/components/dot-large-message-display/dot-large-message-display.module';
// CUSTOM MDOULES
import { DotActionButtonModule } from './view/components/_common/dot-action-button/dot-action-button.module';
import { DotFieldValidationMessageModule } from './view/components/_common/dot-field-validation-message/dot-file-validation-message.module';
import { DotListingDataTableModule } from './view/components/dot-listing-data-table/dot-listing-data-table.module';
import { DotSiteSelectorModule } from './view/components/_common/dot-site-selector/dot-site-selector.module';
import { SearchableDropDownModule } from './view/components/_common/searchable-dropdown';
import { IFrameModule } from './view/components/_common/iframe';

import { TableModule } from 'primeng/table';

import { DotIconModule } from '@dotcms/ui';
import { UiDotIconButtonModule } from './view/components/_common/dot-icon-button/dot-icon-button.module';
import { DotTextareaContentModule } from './view/components/_common/dot-textarea-content/dot-textarea-content.module';
import { DotContentletEditorModule } from './view/components/dot-contentlet-editor/dot-contentlet-editor.module';
import { DotWorkflowTaskDetailModule } from './view/components/dot-workflow-task-detail/dot-workflow-task-detail.module';
import { DotDialogModule } from '@components/dot-dialog/dot-dialog.module';
import { DotMessageDisplayModule } from '@components/dot-message-display/dot-message-display.module';
import { DotToolbarModule } from '@components/dot-toolbar/dot-toolbar.module';
import { DotPushPublishDialogModule } from '@components/_common/dot-push-publish-dialog';
import { DotDownloadBundleDialogModule } from '@components/_common/dot-download-bundle-dialog/dot-download-bundle-dialog.module';
import { DotWizardModule } from '@components/_common/dot-wizard/dot-wizard.module';

import { AutoCompleteModule } from 'primeng/autocomplete';
import { BreadcrumbModule } from 'primeng/breadcrumb';
import { ButtonModule } from 'primeng/button';
import { CalendarModule } from 'primeng/calendar';
import { CheckboxModule } from 'primeng/checkbox';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { DialogModule } from 'primeng/dialog';
import { DropdownModule } from 'primeng/dropdown';
import { InputTextModule } from 'primeng/inputtext';
import { InputTextareaModule } from 'primeng/inputtextarea';
import { MultiSelectModule } from 'primeng/multiselect';
import { PasswordModule } from 'primeng/password';
import { RadioButtonModule } from 'primeng/radiobutton';
import { SelectButtonModule } from 'primeng/selectbutton';
import { SharedModule } from 'primeng/api';
import { SplitButtonModule } from 'primeng/splitbutton';
import { TabViewModule } from 'primeng/tabview';
import { ToolbarModule } from 'primeng/toolbar';
import { TreeTableModule } from 'primeng/treetable';

export const CUSTOM_MODULES = [
    DotToolbarModule,
    DotActionButtonModule,
    DotContentletEditorModule,
    DotDialogModule,
    UiDotIconButtonModule,
    DotIconModule,
    DotTextareaContentModule,
    DotWorkflowTaskDetailModule,
    DotMessageDisplayModule,
    DotFieldValidationMessageModule,
    IFrameModule,
    DotListingDataTableModule,
    SearchableDropDownModule,
    DotSiteSelectorModule,
    DotLargeMessageDisplayModule,
    DotPushPublishDialogModule,
    DotDownloadBundleDialogModule,
    DotWizardModule
];

export const NGFACES_MODULES = [
    AutoCompleteModule,
    BreadcrumbModule,
    ButtonModule,
    CalendarModule,
    CheckboxModule,
    ConfirmDialogModule,
    TableModule,
    DialogModule,
    DropdownModule,
    InputTextModule,
    InputTextareaModule,
    MultiSelectModule,
    PasswordModule,
    RadioButtonModule,
    SelectButtonModule,
    SharedModule,
    SplitButtonModule,
    TabViewModule,
    ToolbarModule,
    TreeTableModule
];
