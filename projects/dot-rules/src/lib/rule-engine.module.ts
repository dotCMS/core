import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { HttpModule } from '@angular/http';


import { ActionService } from './services/Action';
import { AddToBundleDialogComponent } from './push-publish/add-to-bundle-dialog-component';
import { AddToBundleDialogContainer } from './push-publish/add-to-bundle-dialog-container';
import { AreaPickerDialogComponent } from './google-map/area-picker-dialog.component';
import { BundleService } from './services/bundle-service';
import { ConditionComponent } from './rule-condition-component';
import { ConditionGroupComponent } from './rule-condition-group-component';
import { ConditionGroupService } from './services/ConditionGroup';
import { ConditionService } from './services/Condition';
import { Dropdown } from './semantic/modules/dropdown/dropdown';
import { GoogleMapService } from './services/GoogleMapService';
import { I18nService } from './services/system/locale/I18n';
import { InputDate } from './semantic/elements/input-date/input-date';
import { ModalDialogComponent } from './modal-dialog/dialog-component';
import { RestDropdown } from './semantic/modules/restdropdown/RestDropdown';
import { RuleActionComponent } from './rule-action-component';
import { RuleComponent } from './rule-component';
import { RuleEngineComponent } from './rule-engine';
import { RuleEngineContainer } from './rule-engine.container';
import { RuleService } from './services/Rule';
import { ServersideCondition } from './condition-types/serverside-condition/serverside-condition';
import { VisitorsLocationComponent } from './custom-types/visitors-location/visitors-location.component';
import { VisitorsLocationContainer } from './custom-types/visitors-location/visitors-location.container';

import {
    ApiRoot,
    CoreWebService,
    DotcmsConfigService,
    DotcmsEventsService,
    LoggerService,
    SiteService,
    StringUtils,
    UserModel,
    BrowserUtil
} from 'dotcms-js';
import {
  DropdownModule, MultiSelectModule, InputTextModule, InputSwitchModule,
  AutoCompleteModule, ButtonModule, DialogModule, MessagesModule, MessageModule, CalendarModule
} from 'primeng/primeng';

import { DotAutocompleteTagsModule } from './components/dot-autocomplete-tags/dot-autocomplete-tags.module';
import { AppRulesComponent } from './app.component';
import { DotAutofocusModule } from './directives/dot-autofocus/dot-autofocus.module';
import { RuleViewService } from './services/dot-view-rule-service';


@NgModule({
    imports: [
        CommonModule,
        FormsModule,
        ReactiveFormsModule,
        DropdownModule,
        MultiSelectModule,
        InputTextModule,
        InputSwitchModule,
        AutoCompleteModule,
        DialogModule,
        ButtonModule,
        MessagesModule,
        MessageModule,
        CalendarModule,
        DotAutocompleteTagsModule,
        HttpModule,
        DotAutofocusModule
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
        SiteService,
        StringUtils,
        UserModel,


        ActionService,
        BundleService,
        ConditionGroupService,
        ConditionService,
        GoogleMapService,
        I18nService,
        RuleService,
        RuleViewService
    ],
    exports: [RuleEngineContainer, AppRulesComponent]
})
export class RuleEngineModule {}
