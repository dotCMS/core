import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';

import {
    ApiRoot,
    BrowserUtil,
    CoreWebService,
    DotcmsConfigService,
    DotcmsEventsService,
    LoggerService,
    StringUtils,
    UserModel
} from '@dotcms/dotcms-js';

import { DotRulesComponent } from './app/dot-rules.component';
import { DotAreaPickerDialogComponent } from './components/dot-area-picker-dialog/dot-area-picker-dialog.component';
import { DotServersideConditionComponent } from './components/dot-serverside-condition/dot-serverside-condition.component';
import { DotVisitorsLocationContainerComponent } from './custom-types/visitors-location/dot-visitors-location-container.component';
import { DotVisitorsLocationComponent } from './custom-types/visitors-location/dot-visitors-location.component';
import { DotConditionGroupComponent } from './dot-condition-group/dot-condition-group.component';
import { DotRuleComponent } from './dot-rule/dot-rule.component';
import { DotRuleActionComponent } from './dot-rule-action/dot-rule-action.component';
import { DotRuleConditionComponent } from './dot-rule-condition/dot-rule-condition.component';
import { DotRuleEngineComponent } from './dot-rule-engine/dot-rule-engine.component';
import { DotRuleEngineContainerComponent } from './dot-rule-engine-container/dot-rule-engine-container.component';
import { ActionService } from './services/Action';
import { BundleService } from './services/bundle-service';
import { ConditionService } from './services/Condition';
import { ConditionGroupService } from './services/ConditionGroup';
import { RuleViewService } from './services/dot-view-rule-service';
import { GoogleMapService } from './services/GoogleMapService';
import { RuleService } from './services/Rule';
import { I18nService } from './services/system/locale/I18n';

@NgModule({
    imports: [
        DotAreaPickerDialogComponent,
        DotConditionGroupComponent,
        DotRuleActionComponent,
        DotRuleComponent,
        DotRuleConditionComponent,
        DotRuleEngineComponent,
        DotRuleEngineContainerComponent,
        DotRulesComponent,
        DotServersideConditionComponent,
        DotVisitorsLocationComponent,
        DotVisitorsLocationContainerComponent
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
    exports: [DotRuleEngineContainerComponent, DotRulesComponent]
})
export class RuleEngineModule {}
