// CUSTOM MDOULES
import { ActionButtonModule } from './view/components/_common/action-button/action-button.module';
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
    TreeTableModule
} from 'primeng/primeng';

export const CUSTOM_MODULES = [
    ActionButtonModule,
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
    PasswordModule,
    RadioButtonModule,
    SharedModule,
    SplitButtonModule,
    TabViewModule,
    ToolbarModule,
    TreeTableModule
];
