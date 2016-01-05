import {bootstrap}    from 'angular2/platform/browser'
import {Provider} from 'angular2/core'
import {HTTP_PROVIDERS} from 'angular2/http'

import {RuleEngineComponent} from './components/rule-engine/rule-engine'

import {ApiRoot} from '../api/persistence/ApiRoot'
import {EntityMeta, EntitySnapshot} from '../api/persistence/EntityBase'
import {UserModel} from "../api/auth/UserModel"
import {RuleService, RuleModel} from "../api/rule-engine/Rule"
import {ActionService} from "../api/rule-engine/Action"
import {CwChangeEvent} from "../api/util/CwEvent"
import {RestDataStore} from "../api/persistence/RestDataStore"
import {DataStore} from "../api/persistence/DataStore"
import {ConditionGroupService} from "../api/rule-engine/ConditionGroup"
import {ConditionTypeService} from "../api/rule-engine/ConditionType"
import {ConditionService} from "../api/rule-engine/Condition"
import {I18nService, TreeNode} from "../api/system/locale/I18n"
import {ComparisonService} from "../api/system/ruleengine/conditionlets/Comparisons"
import {InputService} from "../api/system/ruleengine/conditionlets/Inputs"
import {ActionTypeService} from "../api/rule-engine/ActionType"
import {CwFilter} from "../api/util/CwFilter"
bootstrap(RuleEngineComponent, [ApiRoot,
  I18nService,
  ComparisonService,
  InputService,
  ActionTypeService,
  UserModel,
  RuleService,
  ActionService,
  ConditionGroupService,
  ConditionService,
  ConditionTypeService,
  HTTP_PROVIDERS,
  new Provider(DataStore, {useClass: RestDataStore})
]);