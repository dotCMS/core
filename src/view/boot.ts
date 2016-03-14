import {bootstrap}    from 'angular2/platform/browser'
import {HTTP_PROVIDERS} from 'angular2/http'
import {ApiRoot} from '../api/persistence/ApiRoot'
import {UserModel} from "../api/auth/UserModel"
import {RuleService} from "../api/rule-engine/Rule"
import {ActionService} from "../api/rule-engine/Action"
import {ConditionGroupService} from "../api/rule-engine/ConditionGroup"
import {ConditionTypeService} from "../api/rule-engine/ConditionType"
import {ConditionService} from "../api/rule-engine/Condition"
import {I18nService} from "../api/system/locale/I18n"
import {ActionTypeService} from "../api/rule-engine/ActionType"
import {GalacticBus} from "../api/system/GalacticBus";
import {RuleEngineContainer} from "./components/rule-engine/rule-engine.container";
bootstrap(RuleEngineContainer, [
  GalacticBus,
  ApiRoot,
  I18nService,
  ActionTypeService,
  UserModel,
  RuleService,
  ActionService,
  ConditionGroupService,
  ConditionService,
  ConditionTypeService,
  HTTP_PROVIDERS
]);