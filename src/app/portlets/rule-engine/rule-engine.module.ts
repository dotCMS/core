import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AddToBundleDialogComponent } from './push-publish/add-to-bundle-dialog-component';
import { AddToBundleDialogContainer } from './push-publish/add-to-bundle-dialog-container';
import { AreaPickerDialogComponent } from './google-map/area-picker-dialog.component';
import { ConditionComponent } from './rule-condition-component';
import { ConditionGroupComponent } from './rule-condition-group-component';
import { Dropdown, InputOption } from './semantic/modules/dropdown/dropdown';
import { InputDate } from './semantic/elements/input-date/input-date';
import { InputText } from './semantic/elements/input-text/input-text';
import { InputToggle } from './input/toggle/InputToggle';
import { ModalDialogComponent } from './modal-dialog/dialog-component';
import { PushPublishDialogComponent } from './push-publish/push-publish-dialog-component';
import { PushPublishDialogContainer } from './push-publish/push-publish-dialog-container';
import { RestDropdown } from './semantic/modules/restdropdown/RestDropdown';
import { RuleActionComponent } from './rule-action-component';
import { RuleComponent } from './rule-component';
import { RuleEngineComponent } from './rule-engine';
import { RuleEngineContainer } from './rule-engine.container';
import { ServersideCondition } from './condition-types/serverside-condition/serverside-condition';
import { VisitorsLocationComponent } from './custom-types/visitors-location/visitors-location.component';
import { VisitorsLocationContainer } from './custom-types/visitors-location/visitors-location.container';

import { RuleService } from './services/Rule';
import { I18nService } from '../../api/system/locale/I18n';
import { GoogleMapService } from '../../api/maps/GoogleMapService';
import { ConditionService } from './services/Condition';
import { ConditionGroupService } from './services/ConditionGroup';
import { ActionService } from './services/Action';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';

declare var jQuery: any;
declare var $: any;

@NgModule({
    imports: [CommonModule, FormsModule, ReactiveFormsModule],
    declarations: [
        AddToBundleDialogComponent,
        AddToBundleDialogContainer,
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
    providers: [
        RuleService,
        I18nService,
        GoogleMapService,
        ConditionService,
        ConditionGroupService,
        ActionService,
    ]
})

export class RuleEngineModule {}
