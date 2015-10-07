/// <reference path="../../../../typings/angular2/angular2.d.ts" />
/// <reference path="../../../../typings/coreweb/coreweb-api.d.ts" />


import {Attribute, Component, Directive, View, NgFor, NgIf, EventEmitter, Inject} from 'angular2/angular2';

import {conditionTemplate} from './templates/index'

import {ApiRoot} from 'api/persistence/ApiRoot'
import {ConditionTypesProvider, ConditionTypeModel} from 'api/rule-engine/ConditionTypes';


import {BrowserConditionlet} from './conditionlets/browser-conditionlet/browser-conditionlet'
import {RequestHeaderConditionlet} from './conditionlets/request-header-conditionlet/request-header-conditionlet'
import {UsersVisitedUrlConditionlet} from './conditionlets/users-visited-url-conditionlet'
import {UsersIpAddressConditionlet} from './conditionlets/users-ip-address-conditionlet'
import {UsersCityConditionlet} from './conditionlets/users-city-conditionlet'
import {UsersTimeConditionlet} from './conditionlets/users-time-conditionlet'
import {UsersLandingPageUrlConditionlet} from './conditionlets/users-landing-page-url-conditionlet'
import {UsersPlatformConditionlet} from './conditionlets/users-platform-conditionlet'
import {UsersLanguageConditionlet} from './conditionlets/users-language-conditionlet'
import {UsersPageVisitsConditionlet} from './conditionlets/users-page-visits-conditionlet'
import {UsersCountryConditionlet} from './conditionlets/users-country-conditionlet'
import {UsersUrlParameterConditionlet} from './conditionlets/users-url-parameter-conditionlet'
import {UsersReferringUrlConditionlet} from './conditionlets/users-referring-url-conditionlet'
import {UsersCurrentUrlConditionlet} from './conditionlets/users-current-url-conditionlet'
import {UsersHostConditionlet} from './conditionlets/users-host-conditionlet'
import {UsersStateConditionlet} from './conditionlets/users-state-conditionlet'
import {UsersSiteVisitsConditionlet} from './conditionlets/users-site-visits-conditionlet'
import {UsersDateTimeConditionlet} from './conditionlets/users-date-time-conditionlet'
import {UsersOperatingSystemConditionlet} from './conditionlets/users-operating-system-conditionlet'
import {UsersLogInConditionlet} from './conditionlets/users-log-in-conditionlet'

@Component({
  selector: 'rule-condition',
  properties: ["conditionMeta", "index"]
})
@View({
  template: conditionTemplate,
  directives: [NgIf, NgFor,
    UsersVisitedUrlConditionlet,
    UsersIpAddressConditionlet,
    UsersCityConditionlet,
    UsersTimeConditionlet,
    UsersLandingPageUrlConditionlet,
    RequestHeaderConditionlet,
    UsersPlatformConditionlet,
    UsersLanguageConditionlet,
    UsersPageVisitsConditionlet,
    UsersCountryConditionlet,
    UsersUrlParameterConditionlet,
    UsersReferringUrlConditionlet,
    UsersCurrentUrlConditionlet,
    UsersHostConditionlet,
    UsersStateConditionlet,
    UsersSiteVisitsConditionlet,
    UsersDateTimeConditionlet,
    UsersOperatingSystemConditionlet,
    UsersLogInConditionlet,
    BrowserConditionlet
  ]
})
class ConditionComponent {
  index:number
  _conditionMeta:any
  condition:any
  conditionValue:string
  conditionType:ConditionTypeModel
  conditionTypes:Array<any>
  typesProvider:ConditionTypesProvider

  constructor(@Inject(ApiRoot) apiRoot:ApiRoot, @Inject(ConditionTypesProvider) typesProvider:ConditionTypesProvider) {
    this.conditionTypes = []
    this.typesProvider = typesProvider
    typesProvider.promise.then(()=> {
      this.conditionTypes = typesProvider.ary
    })
    this.condition = {}
    this.conditionValue = ''
    this.conditionType = new ConditionTypeModel('', {})
    this.index = 0
  }

  onSetConditionMeta(snapshot) {
    console.log("Condition's type is ", this.condition);
    this.condition = snapshot.val()
    this.conditionType = this.typesProvider.getType(this.condition.conditionlet)
    var rhsValues = this.conditionType.rhsValues(this.condition.values);
  }


  set conditionMeta(conditionRef) {
    console.log("Setting conditionMeta: ", conditionRef.key())
    this._conditionMeta = conditionRef
    conditionRef.once('value', this.onSetConditionMeta.bind(this))
  }

  get conditionMeta() {
    return this._conditionMeta;
  }


  setConditionlet(conditionTypeId) {
    console.log('Setting condition type id to: ', conditionTypeId)
    let newVal = ''
    this.conditionType = this.typesProvider.getType(this.condition.conditionlet)
    let foo = this.conditionType.rhsValues(this.condition.values)

    //this.condition.values[key] = {id: key, key: key, priority: 10, value: newVal}
    this.conditionValue = newVal
    this.condition.conditionlet = conditionTypeId

    this.updateCondition()
  }

  toggleOperator() {
    this.condition.operator = this.condition.operator === 'AND' ? 'OR' : 'AND'
    this.updateCondition()
  }

  onConditionletChange(event) {
    console.log('onConditionletChange', event)
  }

  updateCondition() {
    console.log('Updating Condition: ', this.condition)
    this.conditionMeta.set(this.condition)
  }

  removeCondition() {
    console.log('Removing Condition: ', this.condition)
    this.conditionMeta.remove()
  }


  updateAndStuff(event) {
    let target = event.ngTarget
    let val = target.value
    let key = 'aFakeId'

    this.condition.values[key] = {id: key, priority: 10, value: ''}
    this.condition.comparison = val.comparatorValue

  }
}

export {ConditionComponent}