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
    InputToggle,
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
    VisitorsLocationContainer
} from 'dotcms-rules-engine/dotcms-rules-engine';
import { DotDirectivesModule } from '@shared/dot-directives.module';

const routes: Routes = [
    {
        component: AppRulesComponent,
        path: ''
    }
];

@NgModule({
    imports: [CommonModule, FormsModule, HttpModule, ReactiveFormsModule, RouterModule.forChild(routes), DotDirectivesModule],
    declarations: [
        AddToBundleDialogComponent,
        AddToBundleDialogContainer,
        AppRulesComponent, // Entry rule component
        AreaPickerDialogComponent,
        ConditionComponent,
        ConditionGroupComponent,
        Dropdown,
        InputDate,
        InputOption,
        InputText,
        InputToggle,
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
    providers: [ActionService, BundleService, ConditionGroupService, ConditionService, GoogleMapService, I18nService, RuleService],
    exports: [AppRulesComponent]
})
export class RuleEngineModule {}
