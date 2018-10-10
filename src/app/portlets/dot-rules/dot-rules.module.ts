import { CommonModule } from '@angular/common';
import { RouterModule, Routes } from '@angular/router';
import { NgModule } from '@angular/core';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { HttpModule } from '@angular/http';
import {
    ActionService,
    AddToBundleDialogComponent,
    AddToBundleDialogContainer,
    AppRulesComponent,
    AreaPickerDialogComponent,
    BundleService,
    ConditionComponent,
    ConditionGroupComponent,
    ConditionGroupService,
    ConditionService,
    Dropdown,
    GoogleMapService,
    I18nService,
    InputDate,
    InputOption,
    InputText,
    ModalDialogComponent,
    PushPublishDialogComponent,
    PushPublishDialogContainer,
    RestDropdown,
    RuleActionComponent,
    RuleComponent,
    RuleEngineComponent,
    RuleEngineContainer,
    RuleService,
    ServersideCondition,
    VisitorsLocationComponent,
    VisitorsLocationContainer,
    DotAutocompleteTagsModule
} from 'dot-rules';
import { DotDirectivesModule } from '../../shared/dot-directives.module';
import {
    DropdownModule, MultiSelectModule, InputTextModule, InputSwitchModule,
    AutoCompleteModule, ButtonModule, DialogModule, MessagesModule, MessageModule, CalendarModule
} from 'primeng/primeng';

const routes: Routes = [
    {
        component: AppRulesComponent,
        path: ''
    }
];

@NgModule({
    imports: [
        CommonModule,
        FormsModule,
        HttpModule,
        ReactiveFormsModule,
        RouterModule.forChild(routes),
        DotDirectivesModule,
        DropdownModule, MultiSelectModule, InputTextModule, InputSwitchModule,
        AutoCompleteModule, ButtonModule, DialogModule, MessagesModule, MessageModule, CalendarModule, DotAutocompleteTagsModule
    ],
    declarations: [
        AddToBundleDialogComponent,
        AddToBundleDialogContainer,
        AppRulesComponent, // Entr`y rule component
        AreaPickerDialogComponent,
        ConditionComponent,
        ConditionGroupComponent,
        Dropdown,
        InputDate,
        InputOption,
        InputText,
        ModalDialogComponent,
        PushPublishDialogComponent,
        PushPublishDialogContainer,
        RestDropdown,
        RuleActionComponent,
        RuleComponent,
        RuleEngineComponent,
        RuleEngineContainer,
        ServersideCondition,
        VisitorsLocationComponent,
        VisitorsLocationContainer
    ],
    providers: [
        ActionService,
        BundleService,
        ConditionGroupService,
        ConditionService,
        GoogleMapService,
        I18nService,
        RuleService
    ],
    exports: [AppRulesComponent]
})
export class DotRulesModule {}
