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

import { DotRulesComponent } from './entry/dot-rules.component';
import { DotRuleActionComponent } from './features/actions/dot-rule-action.component';
import { DotConditionGroupComponent } from './features/conditions/condition-group/dot-condition-group.component';
import { DotAreaPickerDialogComponent } from './features/conditions/geolocation/dot-area-picker-dialog.component';
import { DotVisitorsLocationContainerComponent } from './features/conditions/geolocation/dot-visitors-location-container.component';
import { DotVisitorsLocationComponent } from './features/conditions/geolocation/dot-visitors-location.component';
import { DotRuleConditionComponent } from './features/conditions/rule-condition/dot-rule-condition.component';
import { DotServersideConditionComponent } from './features/conditions/serverside-condition/dot-serverside-condition.component';
import { DotRuleComponent } from './features/rule/dot-rule.component';
import { DotRuleEngineContainerComponent } from './features/rule-engine/dot-rule-engine-container.component';
import { DotRuleEngineComponent } from './features/rule-engine/dot-rule-engine.component';
import { ActionService } from './services/api/action/Action';
import { BundleService } from './services/api/bundle/bundle-service';
import { ConditionService } from './services/api/condition/Condition';
import { ConditionGroupService } from './services/api/condition-group/ConditionGroup';
import { RuleService } from './services/api/rule/Rule';
import { I18nService } from './services/i18n/i18n.service';
import { GoogleMapService } from './services/maps/GoogleMapService';
import { RuleViewService } from './services/ui/dot-view-rule-service';

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
