// CUSTOM MDOULES
import { DotActionButtonModule } from './view/components/_common/dot-action-button/dot-action-button.module';
import { FieldValidationMessageModule } from './view/components/_common/field-validation-message/file-validation-message.module';
import { ListingDataTableModule } from './view/components/listing-data-table/listing-data-table.module';
import { SiteSelectorModule } from './view/components/_common/site-selector/site-selector.module';
import { SearchableDropDownModule } from './view/components/_common/searchable-dropdown';
import { IFrameModule } from './view/components/_common/iframe';
import { ToolbarAddContenletModule } from './view/components/toolbar-add-contentlet';

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
    MultiSelectModule
} from 'primeng/primeng';
import { DotTextareaContentModule } from './view/components/_common/dot-textarea-content/dot-textarea-content.module';
import { DotGlobalMessageModule } from './view/components/_common/dot-global-message/dot-global-message.module';

export const CUSTOM_MODULES = [
    DotActionButtonModule,
    DotTextareaContentModule,
    FieldValidationMessageModule,
    IFrameModule,
    ListingDataTableModule,
    SearchableDropDownModule,
    SiteSelectorModule,
    ToolbarAddContenletModule,
    DotGlobalMessageModule
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
    TreeTableModule,
    MultiSelectModule
];
