/// <reference path="../../../../typings/angular2/angular2.d.ts" />
/// <reference path="../../../../typings/coreweb/coreweb-api.d.ts" />

import {NgFor, NgIf, Component, Directive, View} from 'angular2/angular2';

import {ruleActionTemplate} from './templates/index'


var actionletsAry = []
var actionletsMap = new Map()
var actionletsPromise;


export let initActionlets = function () {
  let actionletsRef:EntityMeta = new EntityMeta('/api/v1/system/ruleengine/actionlets')
  actionletsPromise = new Promise((resolve, reject) => {
    actionletsRef.once('value', (snap) => {
      let actionlets = snap['val']()
      let results = (Object.keys(actionlets).map((key) => {
        actionletsMap.set(key, actionlets[key])
        return actionlets[key]
      }))

      Array.prototype.push.apply(actionletsAry,results);
      resolve(snap);
    })
  });
}
@Component({
  selector: 'rule-action',
  properties: ["actionMeta"]
})
@View({
  template: ruleActionTemplate,
  directives: [NgIf, NgFor],
})
export class RuleActionComponent {
  _actionMeta:any;
  action:any;
  actionValue:string;
  actionlet:any;
  actionlets:Array<any>;

  constructor() {
    console.log('Creating actionComponent')
    this.actionlets = []
    actionletsPromise.then(()=> {
      this.actionlets = actionletsAry
    })
    this.action = {}
    this.actionValue = ''
    this.actionlet = {}
  }

  setActionName(name:string) {
    this.action.name = name
    this.updateAction()
  }

  onSetActionMeta(snapshot){
    console.log("Action's type is ", this.action, snapshot);
    this.action = snapshot.val()
    this.actionlet = actionletsMap.get(this.action.actionlet)
    console.log('Loaded action with actionlet: ', this.actionlet)
  }


  set actionMeta(actionMeta) {
    console.log("Setting actionMeta: ", actionMeta.key())
    this._actionMeta = actionMeta
    this._actionMeta.once('value', this.onSetActionMeta.bind(this))
  }

  get actionMeta() {
    return this._actionMeta;
  }

  setActionlet(actionletId){
    console.log('Setting actionlet id to: ', actionletId)
    this.action.actionlet = actionletId
    this.actionlet =  actionletsMap.get(this.action.actionlet)
    this.updateAction()
  }


  updateAction() {
    console.log('Updating RuleAction: ', this.action)
    this.actionMeta.set(this.action)

  }

  removeRuleAction() {
    this.actionMeta.remove()
  }
}
