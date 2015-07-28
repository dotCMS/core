/// <reference path="../../../typings/es6/lib.es6.d.ts" />

/// <reference path="../../../typings/angular2/angular2.d.ts" />
/// <reference path="../../../typings/dotcms/dotcms-core-web.d.ts" />
/// <reference path="../../../typings/entity-forge/entity-forge.d.ts" />


import {Attribute, Component, Directive, View, NgFor, NgIf, EventEmitter} from 'angular2/angular2';

import {SingleValueInput, ComparisonInput} from './conditionlets/single-value-input'
import {UsersCountryConditionlet} from './conditionlets/users-country'

import conditionTemplate from './templates/rule-condition-component.tpl.html!text'


var conditionletsAry = []
var conditionletsMap = new Map()
var conditionletsPromise;


let initConditionlets = function () {
  let conditionletsRef:EntityMeta = new EntityMeta('/api/v1/system/conditionlets')
  conditionletsPromise = new Promise((resolve, reject) => {
    conditionletsRef.once('value', (snap) => {
      let conditionlets = snap['val']()
      let results = (Object.keys(conditionlets).map((key) => {
        conditionletsMap.set(key, conditionlets[key])
        return conditionlets[key]
      }))

      Array.prototype.push.apply(conditionletsAry, results);
      resolve(snap);
    })
  });
}


@Component({
  selector: 'rule-condition',
  properties: ["conditionMeta", "index"]
})
@View({
  template: conditionTemplate,
  directives: [NgIf, NgFor, SingleValueInput, ComparisonInput, UsersCountryConditionlet]
})
class ConditionComponent {
  index:number;
  _conditionMeta:any;
  condition:any;
  conditionValue:string;
  conditionType:string;
  conditionlet:any;
  conditionlets:Array<any>;

  constructor() {
    console.log('Creating ConditionComponent')
    this.conditionType = 'text'
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
    this.conditionType = this.getConditionletDataType(this.conditionlet.id)
    this.conditionValue = this.getComparisonValue()
  }


  set conditionMeta(conditionMeta) {
    console.log("Setting conditionMeta: ", conditionMeta.key())
    this._conditionMeta = conditionMeta
    conditionMeta.once('value', this.onSetConditionMeta.bind(this))
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

  setConditionlet(condtitionletId) {
    console.log('Setting conditionlet id to: ', condtitionletId)
    let dataType = this.getConditionletDataType(condtitionletId)
    if (dataType != this.conditionType) {
      console.log('Condition Type changed, resetting value.')
      let newVal = ''
      let key = this.getComparisonValueKey() || 'aFakeId'
      this.condition.values[key] = {id: key, priority: 10, value: newVal}
      this.conditionValue = newVal

      this.conditionType = dataType
    }
    this.condition.conditionlet = condtitionletId
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