import {bootstrap}    from 'angular2/platform/browser'
import {HTTP_PROVIDERS} from 'angular2/http'
import {ApiRoot} from '../api/persistence/ApiRoot'
import {UserModel} from "../api/auth/UserModel"
import {RuleService} from "../api/rule-engine/Rule"
import {ActionService} from "../api/rule-engine/Action"
import {ConditionGroupService} from "../api/rule-engine/ConditionGroup"
import {ConditionService} from "../api/rule-engine/Condition"
import {I18nService} from "../api/system/locale/I18n"
import {RuleEngineContainer} from "./components/rule-engine/rule-engine.container";
bootstrap(RuleEngineContainer, [
  ApiRoot,
  I18nService,
  UserModel,
  RuleService,
  ActionService,
  ConditionGroupService,
  ConditionService,
  HTTP_PROVIDERS
]);