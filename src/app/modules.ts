// CUSTOM MDOULES
import { DotActionButtonModule } from './view/components/_common/dot-action-button/dot-action-button.module';
import { FieldValidationMessageModule } from './view/components/_common/field-validation-message/file-validation-message.module';
import { ListingDataTableModule } from './view/components/listing-data-table/listing-data-table.module';
import { SiteSelectorModule } from './view/components/_common/site-selector/site-selector.module';
import { SearchableDropDownModule } from './view/components/_common/searchable-dropdown';
import { IFrameModule } from './view/components/_common/iframe';

import {
    AutoCompleteModule,
    BreadcrumbModule,
    ButtonModule,
    CalendarModule,
    CheckboxModule,
    ConfirmDialogModule,
    DataTableModule,
    DialogModule,
    DropdownModule,
    InputTextModule,
    InputTextareaModule,
    PasswordModule,
    RadioButtonModule,
    SharedModule,
    SplitButtonModule,
    TabViewModule,
    ToolbarModule,
    TreeTableModule,
    MultiSelectModule,
    SelectButtonModule
} from 'primeng/primeng';
import { DotIconModule } from './view/components/_common/dot-icon/dot-icon.module';
import { DotIconButtonModule } from './view/components/_common/dot-icon-button/dot-icon-button.module';
import { DotTextareaContentModule } from './view/components/_common/dot-textarea-content/dot-textarea-content.module';
import { DotGlobalMessageModule } from './view/components/_common/dot-global-message/dot-global-message.module';
import { DotContentletEditorModule } from './view/components/dot-contentlet-editor/dot-contentlet-editor.module';
import { DotWorkflowTaskDetailModule } from './view/components/dot-workflow-task-detail/dot-workflow-task-detail.module';
import { DotCrumbtrailModule } from './view/components/dot-crumbtrail/dot-crumbtrail.module';

export const CUSTOM_MODULES = [
    DotActionButtonModule,
    DotContentletEditorModule,
    DotCrumbtrailModule,
    DotWorkflowTaskDetailModule,
    DotGlobalMessageModule,
    DotIconModule,
    DotIconButtonModule,
    DotTextareaContentModule,
    FieldValidationMessageModule,
    IFrameModule,
    ListingDataTableModule,
    SearchableDropDownModule,
    SiteSelectorModule
];

export const NGFACES_MODULES = [
    AutoCompleteModule,
    BreadcrumbModule,
    ButtonModule,
    CalendarModule,
    CheckboxModule,
    ConfirmDialogModule,
    DataTableModule,
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
