/// <reference path="../../../typings/dotcms/dotcms-core-web.d.ts" />
/// <reference path="../../../typings/entity-forge/entity-forge.d.ts" />
import XDebug from 'debug';
let log = XDebug('RuleEngineView.RuleActionComponent');

import {NgFor, NgIf, Component, Directive, View} from 'angular2/angular2';
var actionletsAry = []
var actionletsMap = new Map()
var actionletsPromise;


export let initActionlets = function () {
  let actionletsRef:EntityMeta = new EntityMeta('/api/v1/system/ruleengine/actionlets')
  actionletsPromise = new Promise((resolve, reject) => {
    actionletsRef.once('value').then((snap) => {
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
  template: RuleEngine.templates.ruleActionTemplate,
  directives: [NgIf, NgFor],
})
export class RuleActionComponent {
  _actionMeta:any;
  action:any;
  actionValue:string;
  actionlet:any;
  actionlets:Array;

  constructor() {
    log('Creating actionComponent')
    this.actionlets = []
    actionletsPromise.then(()=> {
      this.actionlets = actionletsAry
    })
    this.action = {}
    this.actionValue = ''
    this.actionlet = {}
  }


  onSetActionMeta(snapshot){
    log("Action's type is ", this.action, snapshot);
    this.action = snapshot.val()
    this.actionlet = actionletsMap.get(this.action.actionlet)
    log('Loaded action with actionlet: ', this.actionlet)
  }


  set actionMeta(actionMeta) {
    log("Setting actionMeta: ", actionMeta.key())
    this._actionMeta = actionMeta
    this._actionMeta.once('value', this.onSetActionMeta.bind(this))
  }

  get actionMeta() {
    return this._actionMeta;
  }

  setActionlet(actionletId){
    log('Setting actionlet id to: ', actionletId)
    this.action.actionlet = actionletId
    this.actionlet =  actionletsMap.get(this.action.actionlet)
    this.updateAction()
  }


  updateAction() {
    log('Updating RuleAction: ', this.action)
    this.actionMeta.set(this.action)

  }

  removeRuleAction() {
    this.actionMeta.remove()
  }
}
