import { CommonModule } from '@angular/common';
import { HttpClientModule } from '@angular/common/http';
import { NgModule } from '@angular/core';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';

import { AutoCompleteModule } from 'primeng/autocomplete';
import { ButtonModule } from 'primeng/button';
import { DatePickerModule } from 'primeng/datepicker';
import { DialogModule } from 'primeng/dialog';
import { SelectModule } from 'primeng/select';
import { ToggleSwitchModule } from 'primeng/toggleswitch';
import { InputTextModule } from 'primeng/inputtext';
import { MenuModule } from 'primeng/menu';
import { MessageModule } from 'primeng/message';
import { MultiSelectModule } from 'primeng/multiselect';
import { TooltipModule } from 'primeng/tooltip';

import {
    ApiRoot,
    CoreWebService,
    DotcmsConfigService,
    DotcmsEventsService,
    LoggerService,
    StringUtils,
    UserModel,
    BrowserUtil
} from '@dotcms/dotcms-js';
import { DotNotLicenseComponent } from '@dotcms/ui';

import { AppRulesComponent } from './app.component';
import { DotAutocompleteTagsModule } from './components/dot-autocomplete-tags/dot-autocomplete-tags.module';
import { DotUnlicenseModule } from './components/dot-unlicense/dot-unlicense.module';
import { Dropdown } from './components/dropdown/dropdown';
import { InputDate } from './components/input-date/input-date';
import { RestDropdown } from './components/restdropdown/RestDropdown';
import { ServersideCondition } from './condition-types/serverside-condition/serverside-condition';
import { VisitorsLocationComponent } from './custom-types/visitors-location/visitors-location.component';
import { VisitorsLocationContainer } from './custom-types/visitors-location/visitors-location.container';
import { DotAutofocusModule } from './directives/dot-autofocus/dot-autofocus.module';
import { AreaPickerDialogComponent } from './google-map/area-picker-dialog.component';
import { ModalDialogComponent } from './modal-dialog/dialog-component';
import { AddToBundleDialogComponent } from './push-publish/add-to-bundle-dialog-component';
import { AddToBundleDialogContainer } from './push-publish/add-to-bundle-dialog-container';
import { ConditionComponent } from './rule-condition-component';
import { ActionService } from './services/Action';
import { BundleService } from './services/bundle-service';
import { ConditionGroupComponent } from './rule-condition-group-component';
import { ConditionService } from './services/Condition';
import { ConditionGroupService } from './services/ConditionGroup';
import { RuleViewService } from './services/dot-view-rule-service';
import { GoogleMapService } from './services/GoogleMapService';
import { RuleService } from './services/Rule';
import { I18nService } from './services/system/locale/I18n';
import { RuleActionComponent } from './rule-action-component';
import { RuleComponent } from './rule-component';
import { RuleEngineComponent } from './rule-engine';
import { RuleEngineContainer } from './rule-engine.container';

@NgModule({
    imports: [
        CommonModule,
        FormsModule,
        ReactiveFormsModule,
        SelectModule,
        MultiSelectModule,
        InputTextModule,
        ToggleSwitchModule,
        AutoCompleteModule,
        DialogModule,
        ButtonModule,
        MessageModule,
        MessageModule,
        DatePickerModule,
        DotAutocompleteTagsModule,
        HttpClientModule,
        DotAutofocusModule,
        DotUnlicenseModule,
        MenuModule,
        TooltipModule,
        DotNotLicenseComponent
    ],
    declarations: [
        AddToBundleDialogComponent,
        AddToBundleDialogContainer,
        AreaPickerDialogComponent,
        ConditionComponent,
        ConditionGroupComponent,
        Dropdown,
        InputDate,
        ModalDialogComponent,
        RestDropdown,
        RuleActionComponent,
        RuleComponent,
        RuleEngineComponent,
        RuleEngineContainer,
        ServersideCondition,
        VisitorsLocationComponent,
        VisitorsLocationContainer,
        AppRulesComponent
    ],
    providers: [
        ApiRoot,
        BrowserUtil,
        CoreWebService,
        DotcmsConfigService,
        DotcmsEventsService,
        LoggerService,
        StringUtils,
        UserModel,
        ActionService,
        BundleService,
        ConditionGroupService,
        ConditionService,
        GoogleMapService,
        I18nService,
        RuleService,
        RuleViewService,
        RouterModule
    ],
    exports: [RuleEngineContainer, AppRulesComponent]
})
export class RuleEngineModule {}
