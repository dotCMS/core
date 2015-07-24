/// <reference path="../../../typings/es6/lib.es6.d.ts" />

/// <reference path="../../../typings/angular2/angular2.d.ts" />
/// <reference path="../../../typings/dotcms/dotcms-core-web.d.ts" />
/// <reference path="../../../typings/entity-forge/entity-forge.d.ts" />


import {NgFor, NgIf, Component, Directive, View} from 'angular2/angular2';
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
  properties: ["conditionMeta"]
})
@View({
  template: conditionTemplate,
  directives: [NgIf, NgFor]
})
class ConditionComponent {
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

  }

  onSetConditionMeta(snapshot) {
    console.log("Condition's type is ", this.condition);
    this.condition = snapshot.val()
    this.conditionlet = conditionletsMap.get(this.condition.conditionlet)
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

  setConditionlet(condtitionletId) {
    console.log('Setting conditionlet id to: ', condtitionletId)
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
    let key = this.getComparisonValueKey() || 'aFakeId'
    this.condition.values[key] = {id: key, priority: 10, value: newValue}
    this.updateCondition()
  }


  toggleOperator() {
    //ClauseActionCreators.toggleOperator(this._clause)
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