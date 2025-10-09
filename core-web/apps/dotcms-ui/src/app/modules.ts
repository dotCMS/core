// CUSTOM MDOULES

import { SharedModule } from 'primeng/api';
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
import { SplitButtonModule } from 'primeng/splitbutton';
import { TableModule } from 'primeng/table';
import { TabViewModule } from 'primeng/tabview';
import { ToolbarModule } from 'primeng/toolbar';
import { TreeTableModule } from 'primeng/treetable';

import { DotContentCompareModule } from '@dotcms/portlets/dot-ema/ui';
import { DotDialogComponent, DotIconComponent } from '@dotcms/ui';

import { DotActionButtonComponent } from './view/components/_common/dot-action-button/dot-action-button.component';
import { DotDownloadBundleDialogComponent } from './view/components/_common/dot-download-bundle-dialog/dot-download-bundle-dialog.component';
import { DotGenerateSecurePasswordComponent } from './view/components/_common/dot-generate-secure-password/dot-generate-secure-password.component';
import { DotPushPublishDialogComponent } from './view/components/_common/dot-push-publish-dialog/dot-push-publish-dialog.component';
import { DotSiteSelectorComponent } from './view/components/_common/dot-site-selector/dot-site-selector.component';
import { DotTextareaContentComponent } from './view/components/_common/dot-textarea-content/dot-textarea-content.component';
import { DotWizardComponent } from './view/components/_common/dot-wizard/dot-wizard.component';
import { IFrameModule } from './view/components/_common/iframe';
import { SearchableDropdownComponent } from './view/components/_common/searchable-dropdown/component/searchable-dropdown.component';
import { DotContentletEditorModule } from './view/components/dot-contentlet-editor/dot-contentlet-editor.module';
import { DotLargeMessageDisplayComponent } from './view/components/dot-large-message-display/dot-large-message-display.component';
import { DotListingDataTableComponent } from './view/components/dot-listing-data-table/dot-listing-data-table.component';
import { DotMessageDisplayComponent } from './view/components/dot-message-display/dot-message-display.component';
import { DotToolbarComponent } from './view/components/dot-toolbar/dot-toolbar.component';
import { DotWorkflowTaskDetailComponent } from './view/components/dot-workflow-task-detail/dot-workflow-task-detail.component';

export const CUSTOM_MODULES = [
    DotToolbarComponent,
    DotActionButtonComponent,
    DotContentletEditorModule,
    DotDialogComponent,
    DotIconComponent,
    DotTextareaContentComponent,
    DotWorkflowTaskDetailComponent,
    DotMessageDisplayComponent,
    IFrameModule,
    DotListingDataTableComponent,
    SearchableDropdownComponent,
    DotSiteSelectorComponent,
    DotLargeMessageDisplayComponent,
    DotPushPublishDialogComponent,
    DotContentCompareModule,
    DotDownloadBundleDialogComponent,
    DotWizardComponent,
    DotGenerateSecurePasswordComponent
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
