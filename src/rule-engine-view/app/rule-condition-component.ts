/// <reference path="../../../typings/es6/lib.es6.d.ts" />

/// <reference path="../../../typings/angular2/angular2.d.ts" />
/// <reference path="../../../typings/dotcms/dotcms-core-web.d.ts" />
/// <reference path="../../../typings/entity-forge/entity-forge.d.ts" />


import {Attribute, Component, Directive, View, NgFor, NgIf, EventEmitter} from 'angular2/angular2';

import {ConditionletDirective} from './conditionlets/conditionlet-base';

import conditionTemplate from './templates/rule-condition-component.tpl.html!text'
import {UsersVisitedUrlConditionlet} from './conditionlets/users-visited-url-conditionlet'
import {UsersIpAddressConditionlet} from './conditionlets/users-ip-address-conditionlet'
import {UsersCityConditionlet} from './conditionlets/users-city-conditionlet'
import {UsersTimeConditionlet} from './conditionlets/users-time-conditionlet'
import {UsersLandingPageUrlConditionlet} from './conditionlets/users-landing-page-url-conditionlet'
import {UsersBrowserHeaderConditionlet} from './conditionlets/users-browser-header-conditionlet'
import {UsersPlatformConditionlet} from './conditionlets/users-platform-conditionlet'
import {UsersLanguageConditionlet} from './conditionlets/users-language-conditionlet'
import {UsersPageVisitsConditionlet} from './conditionlets/users-page-visits-conditionlet'
import {UsersCountryConditionlet} from './conditionlets/users-country-conditionlet'
import {MockTrueConditionlet} from './conditionlets/mock-true-conditionlet'
import {UsersUrlParameterConditionlet} from './conditionlets/users-url-parameter-conditionlet'
import {UsersReferringUrlConditionlet} from './conditionlets/users-referring-url-conditionlet'
import {UsersCurrentUrlConditionlet} from './conditionlets/users-current-url-conditionlet'
import {UsersHostConditionlet} from './conditionlets/users-host-conditionlet'
import {UsersStateConditionlet} from './conditionlets/users-state-conditionlet'
import {UsersSiteVisitsConditionlet} from './conditionlets/users-site-visits-conditionlet'
import {UsersDateTimeConditionlet} from './conditionlets/users-date-time-conditionlet'
import {UsersOperatingSystemConditionlet} from './conditionlets/users-operating-system-conditionlet'
import {UsersLogInConditionlet} from './conditionlets/users-log-in-conditionlet'
import {UsersBrowserConditionlet} from './conditionlets/users-browser-conditionlet'

var conditionletsAry = []
var conditionletsMap = new Map()
var conditionletsPromise;


let initConditionlets = function () {
  let conditionletsRef:EntityMeta = new EntityMeta('/api/v1/system/conditionlets')
  conditionletsPromise = new Promise((resolve, reject) => {
    conditionletsRef.once('value', (snap) => {
      let conditionlets = snap['val']()
      let str = [];
      let results = (Object.keys(conditionlets).map((key) => {
        conditionletsMap.set(key, conditionlets[key])
        let dashKey = key.replace(/([A-Z])/g, function ($1) {
          return '-' + $1.toLowerCase();
        });
        dashKey = dashKey.substring(1)
        str.push("import {" + key +  "} from './conditionlets/" + dashKey + "'")
        return conditionlets[key]
      }))
      console.log(str.join('\n'))
      Array.prototype.push.apply(conditionletsAry, results);
      resolve(snap);
    })
  });
}


/*
 ,
 */

@Component({
  selector: 'rule-condition',
  properties: ["conditionMeta", "index"]
})
@View({
  template: conditionTemplate,
  directives: [NgIf, NgFor, ConditionletDirective,
    UsersCountryConditionlet,
    UsersPageVisitsConditionlet,
    UsersVisitedUrlConditionlet,
    UsersIpAddressConditionlet,
    UsersCityConditionlet,
    UsersTimeConditionlet,
    UsersLandingPageUrlConditionlet,
    UsersBrowserHeaderConditionlet,
    UsersPlatformConditionlet,
    UsersLanguageConditionlet,
    UsersPageVisitsConditionlet,
    UsersCountryConditionlet,
    MockTrueConditionlet,
    UsersUrlParameterConditionlet,
    UsersReferringUrlConditionlet,
    UsersCurrentUrlConditionlet,
    UsersHostConditionlet,
    UsersStateConditionlet,
    UsersSiteVisitsConditionlet,
    UsersDateTimeConditionlet,
    UsersOperatingSystemConditionlet,
    UsersLogInConditionlet,
    UsersBrowserConditionlet
  ]
})
class ConditionComponent {
  index:number;
  _conditionMeta:any;
  condition:any;
  conditionValue:string;
  conditionlet:any;
  conditionlets:Array<any>;

  constructor() {
    console.log('Creating ConditionComponent')
    this.conditionlets = []
    conditionletsPromise.then(()=> {
      this.conditionlets = conditionletsAry
    })
    this.condition = {}
    this.conditionValue = ''
    this.conditionlet = {}
    this.index = 0
  }

  onSetConditionMeta(snapshot) {
    console.log("Condition's type is ", this.condition);
    this.condition = snapshot.val()
    this.conditionlet = conditionletsMap.get(this.condition.conditionlet)
    this.conditionValue = this.getComparisonValue()
  }


  set conditionMeta(conditionRef) {
    console.log("Setting conditionMeta: ", conditionRef.key())
    this._conditionMeta = conditionRef
    conditionRef.once('value', this.onSetConditionMeta.bind(this))
  }

  get conditionMeta() {
    return this._conditionMeta;
  }

  getConditionletDataType(conditionletId) {
    let dataType;
    switch (conditionletId) {
      case 'UsersTimeConditionlet':
        dataType = 'time'
        break;
      case 'UsersDateTimeConditionlet':
        dataType = 'date'
        break;
      default :
      {
        dataType = 'text'
      }
    }
    return dataType

  }

  setConditionlet(conditionletId) {
    console.log('Setting conditionlet id to: ', conditionletId)
    let dataType = this.getConditionletDataType(conditionletId)
    if (dataType != this.getConditionletDataType(this.conditionlet.id)) {
      console.log('Condition data type changed, resetting condition value.')
      let newVal = ''
      let key = this.getComparisonValueKey() || 'aFakeId'
      this.condition.values[key] = {id: key, priority: 10, value: newVal}
      this.conditionValue = newVal
    }
    this.condition.conditionlet = conditionletId
    this.conditionlet = conditionletsMap.get(this.condition.conditionlet)

    this.updateCondition()
  }

  setComparison(comparisonId) {
    console.log('Setting conditionlet comparison id to: ', comparisonId)
    this.condition.comparison = comparisonId
    this.updateCondition()

  }

  getComparisonValueKey() {
    let key = null
    let keys = Object.keys(this.condition.values)
    if (keys.length) {
      key = keys[0]
    }
    return key
  }

  getComparisonValue() {
    let value = ''
    let key = this.getComparisonValueKey()
    if (key) {
      value = this.condition.values[key].value
    }
    return value
  }

  setComparisonValue(newValue) {
    if (newValue === undefined) {
      return
    }
    let key = this.getComparisonValueKey() || 'aFakeId'
    this.condition.values[key] = {id: key, priority: 10, value: newValue}
    this.updateCondition()
  }

  toggleOperator() {
    this.condition.operator = this.condition.operator === 'AND' ? 'OR' : 'AND'
    this.updateCondition()
  }

  onFoo(event){
    alert('wow')
  }

  onConditionletChange(event){
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
}

export {ConditionComponent, initConditionlets}